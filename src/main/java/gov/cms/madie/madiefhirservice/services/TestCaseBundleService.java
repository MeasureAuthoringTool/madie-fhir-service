package gov.cms.madie.madiefhirservice.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.dto.TestCaseExportMetaData;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.*;

import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.exceptions.BundleOperationException;
import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.models.common.BundleType;
import gov.cms.madie.models.dto.ExportDTO;
import gov.cms.madie.packaging.utils.PackagingUtility;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseBundleService {

  private final FhirContext qicoreFhirContext;

  public Map<String, Bundle> getTestCaseExportBundle(
      Measure measure, List<TestCase> testCases, ExportDTO exportDTO) {
    if (measure == null || testCases == null || testCases.isEmpty()) {
      throw new InternalServerException("Unable to find Measure and/or test case");
    }

    IParser parser =
        qicoreFhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);

    Map<String, Bundle> testCaseBundle = new HashMap<>();
    for (TestCase testCase : testCases) {
      Bundle bundle;
      try {
        // If the test case is empty or malformed skip adding it to the map
        if (testCase.getJson() == null || testCase.getJson().isEmpty()) {
          throw new DataFormatException("TestCase Json is empty");
        }
        if (!testCase.isValidResource()) {
          throw new DataFormatException("TestCase Json is not valid");
        }
        bundle = parser.parseResource(Bundle.class, testCase.getJson());
      } catch (DataFormatException | ClassCastException ex) {
        log.error(
            "Unable to parse test case bundle resource for test case [{}] from Measure [{}]",
            testCase.getId(),
            measure.getId(),
            ex);
        continue;
      }

      // MAT-6204 Here we're modifying the bundle based on export choice,
      // but we don't want to modify it permanently
      if (exportDTO.getBundleType() != null) {
        BundleType bundleType = BundleType.valueOf(exportDTO.getBundleType().name());
        bundle = updateEntry(bundle, bundleType, parser, testCase.getPatientId().toString());
        String json = parser.encodeResourceToString(bundle);
        testCase.setJson(json);
      }

      String fileName = ExportFileNamesUtil.getTestCaseExportFileName(measure, testCase);
      var measureReport = buildMeasureReport(testCase, measure, bundle);
      var bundleEntryComponent =
          FhirResourceHelpers.getBundleEntryComponent(
              measureReport, String.valueOf(bundle.getType()));
      bundle.getEntry().add(bundleEntryComponent);
      testCaseBundle.put(fileName, bundle);
    }

    // Don't return an empty zip file
    if (testCaseBundle.isEmpty()) {
      throw new ResourceNotFoundException("test cases", "measure", measure.getId());
    }

    return testCaseBundle;
  }

  public Bundle updateEntry(
      Bundle bundle, BundleType bundleType, IParser parser, String patientId) {
    Bundle bundleCopy = bundle.copy();
    org.hl7.fhir.r4.model.Bundle.BundleType fhirBundleType =
        org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(bundleType.toString().toUpperCase());
    bundleCopy.setType(fhirBundleType);

    // Generating a new UUID for each resource and updating all its references across the bundle.
    // for example replaces string that matches "Patient/patient-id" with
    // "Patient/madie-generated-uuid"
    String bundleString = parser.encodeResourceToString(bundleCopy);
    for (Bundle.BundleEntryComponent entry : bundleCopy.getEntry()) {
      var resourceType = entry.getResource().getResourceType() + "/";
      var resourceID = resourceType.equals("Patient/") ? patientId : UUID.randomUUID().toString();
      bundleString =
          bundleString.replaceAll(
              resourceType + entry.getResource().getIdPart(), resourceType + resourceID);
    }
    bundleCopy = parser.parseResource(Bundle.class, bundleString);

    // Modifying Request attribute for each Resource
    // Also updating the resource Id with the MADiE generated UUID
    bundleCopy.setEntry(
        bundleCopy.getEntry().stream()
            .map(
                entry -> {
                  entry
                      .getResource()
                      .setId(StringUtils.substringAfterLast(entry.getFullUrl(), "/"));
                  if (bundleType == BundleType.TRANSACTION) {
                    FhirResourceHelpers.setRequestForResourceEntry(
                        entry.getResource(), entry, Bundle.HTTPVerb.PUT);
                    return entry;
                  } else if (bundleType == BundleType.COLLECTION) {
                    entry.setRequest(null);
                  }
                  return entry;
                })
            .collect(Collectors.toList()));
    return bundleCopy;
  }

  private MeasureReport buildMeasureReport(
      TestCase testCase, Measure measure, Bundle testCaseBundle) {
    MeasureReport measureReport = new MeasureReport();
    measureReport.setId(UUID.randomUUID().toString());
    measureReport.setMeta(new Meta().addProfile(UriConstants.CqfTestCases.CQFM_TEST_CASES));
    measureReport.setContained(buildContained(testCase, testCaseBundle));
    measureReport.setExtension(buildExtensions(testCase, measureReport));
    measureReport.setModifierExtension(buildModifierExtension());
    measureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    measureReport.setType(MeasureReport.MeasureReportType.INDIVIDUAL);
    measureReport.setMeasure(
        FhirResourceHelpers.buildResourceFullUrl("Measure", measure.getCqlLibraryName()));
    measureReport.setPeriod(
        FhirResourceHelpers.getPeriodFromDates(
            getUTCDates(measure.getMeasurementPeriodStart()),
            getUTCDates(measure.getMeasurementPeriodEnd())));

    measureReport.setGroup(buildMeasureReportGroupComponents(testCase, measure.getGroups()));
    measureReport.setEvaluatedResource(buildEvaluatedResource(testCaseBundle));
    return measureReport;
  }

  /**
   * @param testCase test case
   * @param testCaseBundle test case bundle
   * @return a list of resources of type Parameters which contains a unique ID and patient id as a
   *     "subject"
   */
  private List<Resource> buildContained(TestCase testCase, Bundle testCaseBundle) {
    var patientResource =
        testCaseBundle.getEntry().stream()
            .filter(
                entry ->
                    "Patient".equalsIgnoreCase(entry.getResource().getResourceType().toString()))
            .findFirst();
    if (patientResource.isPresent()) {
      var parameter =
          new Parameters.ParametersParameterComponent()
              .setName("subject")
              .setValue(new StringType(patientResource.get().getResource().getIdPart()));
      var parameters =
          new Parameters().addParameter(parameter).setId(UUID.randomUUID() + "-parameters");
      return Collections.singletonList(parameters);
    } else {
      log.error(
          "Unable to find Patient resource in test case bundle for test case [{}]",
          testCase.getId());
      throw new ResourceNotFoundException("Patient resource", "test case", testCase.getId());
    }
  }

  /**
   * @param testCase test case
   * @return a list of extensions where parameter extension will always be referring to the
   *     parameter created in "Contained", description extension will only be returned if
   *     Description is provided in madie testcase.
   */
  private List<Extension> buildExtensions(TestCase testCase, MeasureReport measureReport) {
    var parametersExtension =
        new Extension()
            .setUrl(UriConstants.CqfTestCases.CQFM_INPUT_PARAMETERS)
            .setValue(new Reference("#" + measureReport.getContained().get(0).getId()));
    var descriptionExtension =
        new Extension()
            .setUrl(UriConstants.CqfTestCases.CQFM_TEST_CASE_DESCRIPTION)
            .setValue(new MarkdownType(testCase.getDescription()));
    List<Extension> extensions = new ArrayList<>();
    extensions.add(parametersExtension);
    extensions.add(descriptionExtension);
    return extensions;
  }

  private List<Extension> buildModifierExtension() {
    var modifierExtension =
        new Extension(UriConstants.CqfTestCases.IS_TEST_CASE, new BooleanType(true));
    return Collections.singletonList(modifierExtension);
  }

  private List<MeasureReport.MeasureReportGroupComponent> buildMeasureReportGroupComponents(
      //      TestCase testCase, List<Group> groups) {
      TestCase testCase, List<Group> groups) {
    if (CollectionUtils.isEmpty(testCase.getGroupPopulations())) {
      return List.of();
    }

    // track number of observations per group+observation type for building friendly observation
    // display ID
    final Map<String, AtomicInteger> observationCounts = new HashMap<>();

    return testCase.getGroupPopulations().stream()
        .map(
            population -> {
              var measureReportGroupComponent = new MeasureReport.MeasureReportGroupComponent();
              Group matchingGroup = getGroup(groups, population.getGroupId());
              measureReportGroupComponent.setId(matchingGroup.getDisplayId());
              // adding populations
              if (population.getPopulationValues() != null) {
                var measureReportGroupPopulationComponents =
                    population.getPopulationValues().stream()
                        .map(
                            testCasePopulationValue -> {
                              MeasureReport.MeasureReportGroupPopulationComponent groupComponent =
                                  getMeasureReportGroupPopulationComponent(testCasePopulationValue);
                              String groupPopulationDisplayId =
                                  getDisplayId(
                                      observationCounts, matchingGroup, testCasePopulationValue);
                              groupComponent.setId(groupPopulationDisplayId);
                              return groupComponent;
                            })
                        .collect(Collectors.toList());
                measureReportGroupComponent.setPopulation(measureReportGroupPopulationComponents);
              }
              // adding measure score
              measureReportGroupComponent.setMeasureScore(
                  new Quantity()
                      .setValue(
                          StringUtils.equalsIgnoreCase("boolean", population.getPopulationBasis())
                              ? 1.0
                              : 0));
              // adding stratification for patient basis
              if (population.getStratificationValues() != null) {
                if (StringUtils.equalsIgnoreCase("boolean", population.getPopulationBasis())) {
                  measureReportGroupComponent.setStratifier(
                      buildGroupStratifierComponent(population, true, matchingGroup));
                } else {
                  measureReportGroupComponent.setStratifier(
                      buildGroupStratifierComponent(population, false, matchingGroup));
                }
              }
              return measureReportGroupComponent;
            })
        .collect(Collectors.toList());
  }

  /**
   * Warning - this method modifies input parameters: observationCounts map
   *
   * @param observationCounts map representing count of group/observation -> current count in test
   *     case populations
   * @param group MADiE group object
   * @param testCasePopulationValue current test case population value
   * @return
   */
  protected String getDisplayId(
      Map<String, AtomicInteger> observationCounts,
      Group group,
      TestCasePopulationValue testCasePopulationValue) {
    boolean testCaseObservation =
        FhirResourceHelpers.isTestCaseObservation(testCasePopulationValue.getName());
    String groupPopulationDisplayId;
    if (testCaseObservation) {
      String obsCountKey = group.getId() + testCasePopulationValue.getName().toCode();
      AtomicInteger obsCount = observationCounts.get(obsCountKey);
      if (obsCount == null) {
        obsCount = new AtomicInteger(0);
        observationCounts.put(obsCountKey, obsCount);
      }
      boolean patientBased = StringUtils.equalsIgnoreCase("boolean", group.getPopulationBasis());
      groupPopulationDisplayId =
          FhirResourceHelpers.getGroupObservationDisplayId(
              group, testCasePopulationValue, obsCount.incrementAndGet(), patientBased);
    } else {
      groupPopulationDisplayId =
          FhirResourceHelpers.getGroupPopulationDisplayId(group, testCasePopulationValue.getId());
    }
    return groupPopulationDisplayId;
  }

  private MeasureReport.MeasureReportGroupPopulationComponent
      getMeasureReportGroupPopulationComponent(TestCasePopulationValue testCasePopulationValue) {
    var groupPopComponent =
        new MeasureReport.MeasureReportGroupPopulationComponent()
            .setCode(
                FhirResourceHelpers.buildCodeableConcept(
                    getPopulationType(testCasePopulationValue.getName()).toCode(),
                    UriConstants.CodeSystem.POPULATION_SYSTEM_URI,
                    getPopulationType(testCasePopulationValue.getName()).getDisplay()));
    // Exclude the count attribute when Expected Value is empty
    if (FhirResourceHelpers.getExpectedValue(testCasePopulationValue.getExpected()) >= 0) {
      groupPopComponent.setCount(
          FhirResourceHelpers.getExpectedValue(testCasePopulationValue.getExpected()));
    }
    return groupPopComponent;
  }

  // MAT-8349: Denominator and Numerator Observations are not part of measure report population
  PopulationType getPopulationType(PopulationType populationType) {
    return (populationType.equals(PopulationType.DENOMINATOR_OBSERVATION)
            || populationType.equals(PopulationType.NUMERATOR_OBSERVATION)
            || populationType.equals(PopulationType.MEASURE_POPULATION_OBSERVATION))
        ? PopulationType.MEASURE_OBSERVATION
        : populationType;
  }

  private List<MeasureReport.MeasureReportGroupStratifierComponent> buildGroupStratifierComponent(
      TestCaseGroupPopulation population, boolean isPatientBased, Group group) {
    return population.getStratificationValues().stream()
        .map(
            testCaseStratificationValue -> {
              List<CodeableConcept> code = new ArrayList<CodeableConcept>();
              var codeText =
                  isPatientBased
                      ? getStratificationDefinition(group, testCaseStratificationValue.getId())
                      : testCaseStratificationValue.getName();
              code.add(new CodeableConcept().setText(codeText));

              var stratifierComponent =
                  new MeasureReport.MeasureReportGroupStratifierComponent()
                      .setCode(code)
                      .setStratum(
                          buildStratum(
                              testCaseStratificationValue,
                              isPatientBased,
                              testCaseStratificationValue.getName(),
                              group));
              stratifierComponent.setId(
                  getGroupStratificationDisplayId(group, testCaseStratificationValue.getId()));
              return stratifierComponent;
            })
        .collect(Collectors.toList());
  }

  private String getStratificationDefinition(Group group, String testCaseStratificationId) {
    return group.getStratifications().stream()
        .filter(stratification -> stratification.getId().equals(testCaseStratificationId))
        .map(selectedStratification -> selectedStratification.getCqlDefinition())
        .findFirst()
        .orElse(null);
  }

  private List<MeasureReport.StratifierGroupComponent> buildStratum(
      TestCaseStratificationValue testCaseStratificationValue,
      boolean isPatientBased,
      String testCaseStratificationName,
      Group group) {

    // stratum
    List<MeasureReport.StratifierGroupComponent> stratum =
        new ArrayList<MeasureReport.StratifierGroupComponent>();

    MeasureReport.StratifierGroupComponent stratifierGroupComponent =
        new MeasureReport.StratifierGroupComponent();

    if (isPatientBased) {
      // when value is true
      stratifierGroupComponent.setValue(new CodeableConcept().setText("true"));
      stratifierGroupComponent.setPopulation(
          FhirResourceHelpers.buildStratumPopulation(
              testCaseStratificationValue, true, true, group));
      stratum.add(stratifierGroupComponent);

      // when value is false (i.e., inverted )
      MeasureReport.StratifierGroupComponent stratifierGroupComponentForInvertedValue =
          new MeasureReport.StratifierGroupComponent();
      stratifierGroupComponentForInvertedValue.setValue(new CodeableConcept().setText("false"));
      stratifierGroupComponentForInvertedValue.setPopulation(
          FhirResourceHelpers.buildStratumPopulation(
              testCaseStratificationValue, false, true, group));
      stratum.add(stratifierGroupComponentForInvertedValue);
    } else {
      // Non-patient based measures
      stratifierGroupComponent.setValue(new CodeableConcept().setText(testCaseStratificationName));
      stratifierGroupComponent.setPopulation(
          FhirResourceHelpers.buildStratumPopulation(
              testCaseStratificationValue, null, false, group));
      stratum.add(stratifierGroupComponent);
    }
    return stratum;
  }

  /**
   * @param testCaseBundle test case bundle
   * @return a list of all resources in the test case bundle along with their unique identifier ex:
   *     [{ "reference": "Encounter/Encounter-1" }]
   */
  private List<Reference> buildEvaluatedResource(Bundle testCaseBundle) {
    List<Reference> references = new ArrayList<>();
    testCaseBundle
        .getEntry()
        // remove the madie url to provide relative urls
        .forEach(
            entry ->
                references.add(
                    new Reference(
                        entry.getResource().getResourceType()
                            + "/"
                            + entry.getResource().getIdPart())));
    return references;
  }

  private Date getUTCDates(Date date) {
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
      var utcFormattedString =
          DateFormatUtils.format(date, "MM/dd/yyyy", TimeZone.getTimeZone("UTC"));
      return simpleDateFormat.parse(utcFormattedString);
    } catch (ParseException parseException) {
      throw new RuntimeException("Unable to parse date ", parseException);
    }
  }

  private String generateReadMe(List<TestCase> testCases, List<TestCase> excludedTestCases) {
    String readMe =
        "The purpose of this file is to allow users to view the mapping of test case names to their test case "
            + "UUIDs. In order to find a specific test case file in the export, first locate the test case "
            + "name in this document and then use the associated UUID to find the name of the folder in "
            + "the export.\n";

    readMe +=
        testCases.stream()
            .map(
                testCase ->
                    "\n"
                        + "Case # "
                        + testCase.getCaseNumber()
                        + " - "
                        + testCase.getPatientId()
                        + " = "
                        + testCase.getSeries()
                        + " "
                        + testCase.getTitle())
            .collect(Collectors.joining());

    if (!CollectionUtils.isEmpty(excludedTestCases)) {
      readMe += "\n\nThe following test cases are invalid and not included in this export:\n";
      readMe +=
          excludedTestCases.stream()
              .map(
                  testCase ->
                      "\n"
                          + "Case # "
                          + testCase.getCaseNumber()
                          + " - "
                          + testCase.getPatientId()
                          + " = "
                          + testCase.getSeries()
                          + " "
                          + testCase.getTitle())
              .collect(Collectors.joining());
    }

    return readMe;
  }

  private String generateMadieMetadataFile(List<TestCase> testCases)
      throws JsonProcessingException {
    if (CollectionUtils.isEmpty(testCases)) {
      return "";
    }
    List<TestCaseExportMetaData> metaDataList =
        testCases.stream()
            .map(
                testCase ->
                    TestCaseExportMetaData.builder()
                        .testCaseId(testCase.getId())
                        .title(testCase.getTitle())
                        .series(testCase.getSeries())
                        .description(testCase.getDescription())
                        .patientId(
                            testCase.getPatientId() == null
                                ? null
                                : testCase.getPatientId().toString())
                        .build())
            .toList();
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(metaDataList);
  }

  /**
   * Combines the zip from Packaging Utility and a generated ReadMe file for the testcases
   *
   * @param measure MADiE Measure
   * @param exportableTestCaseBundle Exportable TestCase bundles that includes measure report
   * @param testCases List of test cases to be exported, used to generate ReadMe
   * @param excludedTestCases List of test cases that were excluded from the export due to errors
   * @return zipped content
   */
  public byte[] zipTestCaseContents(
      Measure measure,
      Map<String, Bundle> exportableTestCaseBundle,
      List<TestCase> testCases,
      List<TestCase> excludedTestCases) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      PackagingUtility utility = PackagingUtilityFactory.getInstance(measure.getModel());
      byte[] bytes = utility.getZipBundle(exportableTestCaseBundle, null);

      try (ZipOutputStream zos = new ZipOutputStream(baos);
          ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {

        // Add the README file to the zip
        String readme = generateReadMe(testCases, excludedTestCases);
        ZipEntry entry = new ZipEntry("README.txt");
        entry.setSize(readme.length());
        zos.putNextEntry(entry);
        zos.write(readme.getBytes());
        // Add the .madie metadata file
        String metadata = generateMadieMetadataFile(testCases);
        ZipEntry metaDataEntry = new ZipEntry(".madie");
        entry.setSize(metadata.length());
        zos.putNextEntry(metaDataEntry);
        zos.write(metadata.getBytes());
        // Add the TestCases back the zip
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
          zos.putNextEntry(zipEntry);
          zos.write(zis.readAllBytes());
          zipEntry = zis.getNextEntry();
        }
      }

      // return after the zip streams are closed
      return baos.toByteArray();
    } catch (RestClientException
        | IllegalArgumentException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException
        | IOException ex) {

      log.error("An error occurred while bundling testcases for measure {}", measure.getId(), ex);
      throw new BundleOperationException("Measure", measure.getId(), ex);
    }
  }

  Group getGroup(List<Group> groups, String groupId) {
    Optional<Group> groupOpt = groups.stream().filter(g -> groupId.equals(g.getId())).findFirst();
    if (groupOpt.isEmpty()) {
      throw new ResourceNotFoundException("TestCase", "Group Populations", groupId);
    }
    return groupOpt.get();
  }

  String getGroupStratificationDisplayId(Group group, String groupStratId) {
    String stratificationDisplayId = groupStratId;

    if (!CollectionUtils.isEmpty(group.getStratifications())) {
      Optional<Stratification> stratification =
          group.getStratifications().stream()
              .filter(strat -> groupStratId.equals(strat.getId()))
              .findFirst();
      if (stratification.isPresent()) {
        stratificationDisplayId = stratification.get().getDisplayId();
      }
    }

    return stratificationDisplayId;
  }
}

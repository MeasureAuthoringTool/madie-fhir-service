package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql.CqlFormatter;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureBundleService {
  private final MeasureTranslatorService measureTranslatorService;
  private final LibraryTranslatorService libraryTranslatorService;
  private final LibraryService libraryService;
  private final ElmTranslatorClient elmTranslatorClient;
  private final HumanReadableService humanReadableService;

  /**
   * Creates measure bundle that contains measure, main library, and included libraries resources
   */
  public Bundle createMeasureBundle(
      Measure madieMeasure, Principal principal, String bundleType, String accessToken) {
    log.info(
        "Generating measure bundle of type [{}] for measure {}", bundleType, madieMeasure.getId());
    madieMeasure.setCql(CqlFormatter.formatCql(madieMeasure.getCql(), principal));

    log.info("CQL formatting completed successfully for measure {}", madieMeasure.getId());
    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure);
    Set<String> expressions = getExpressions(measure);

    log.info("Mapping of MADiE measure to FHIR measure completed successfully {}", madieMeasure.getId());
    // Bundle entry for Measure resource
    Bundle.BundleEntryComponent measureEntryComponent =
        FhirResourceHelpers.getBundleEntryComponent(measure, "Transaction");
    Bundle bundle =
        new Bundle().setType(Bundle.BundleType.TRANSACTION).addEntry(measureEntryComponent);
    log.info("Measure bundle entry created successfully {}", madieMeasure.getId());
    // Bundle entries for all the library resources of a MADiE Measure
    List<Bundle.BundleEntryComponent> libraryEntryComponents =
        createBundleComponentsForLibrariesOfMadieMeasure(
            expressions, madieMeasure, bundleType, accessToken);
    libraryEntryComponents.forEach(bundle::addEntry);
    log.info("Included library components created successfully {}", madieMeasure.getId());

    if (BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT.equals(bundleType)) {
      CqlLibraryDetails libraryDetails =
          CqlLibraryDetails.builder()
              .libraryName(madieMeasure.getCqlLibraryName())
              .cql(madieMeasure.getCql())
              .expressions(expressions)
              .build();
      // get effective DataRequirements
      log.info("Getting effective data requirements for measure: {}", measure.getId());
      org.hl7.fhir.r5.model.Library effectiveDataRequirements =
          elmTranslatorClient.getEffectiveDataRequirements(libraryDetails, true, accessToken);
      // get human-readable for measure
      String humanReadable =
          humanReadableService.generateMeasureHumanReadable(
              madieMeasure, bundle, effectiveDataRequirements);
      // set narrative and effective DataRequirements to measure
      setNarrativeText(measure, humanReadable);
      addEffectiveDataRequirementsToMeasure(measure, effectiveDataRequirements);

      // set narrative to measure library
      var measureLibrary =
          (org.hl7.fhir.r4.model.Library) ResourceUtils.getResource(bundle, "Library");
      String libraryHr = humanReadableService.generateLibraryHumanReadable(measureLibrary);
      setNarrativeText(measureLibrary, libraryHr);
    }
    return bundle;
  }

  /**
   * Collects BundleEntryComponents for main measure library and included libraries
   *
   * @param madieMeasure instance of MADiE Measure
   * @return list of Library BundleEntryComponents
   */
  public List<Bundle.BundleEntryComponent> createBundleComponentsForLibrariesOfMadieMeasure(
      Set<String> expressions,
      Measure madieMeasure,
      final String bundleType,
      final String accessToken) {
    Library library =
        getMeasureLibraryResourceForMadieMeasure(expressions, madieMeasure, accessToken);
    Bundle.BundleEntryComponent mainLibraryBundleComponent =
        FhirResourceHelpers.getBundleEntryComponent(library, "Transaction");
    Map<String, Library> includedLibraryMap = new HashMap<>();
    libraryService.getIncludedLibraries(
        madieMeasure.getCql(), includedLibraryMap, bundleType, accessToken);
    List<Bundle.BundleEntryComponent> libraryBundleComponents =
        includedLibraryMap.values().stream()
            .map((lib) -> FhirResourceHelpers.getBundleEntryComponent(lib, "Transaction"))
            .collect(Collectors.toList());
    // add main library first in the list
    libraryBundleComponents.add(0, mainLibraryBundleComponent);
    return libraryBundleComponents;
  }

  /**
   * Creates a Library resource for main library of MADiE Measure
   *
   * @param expressions- measure populations, SDEs, Stratification
   * @param madieMeasure
   * @param accessToken
   * @return library- r4 library
   */
  public Library getMeasureLibraryResourceForMadieMeasure(
      Set<String> expressions, Measure madieMeasure, String accessToken) {
    log.info("Preparing Measure library resource for measure: {}", madieMeasure.getId());
    CqlLibrary cqlLibrary = createCqlLibraryForMadieMeasure(madieMeasure);
    CqlLibraryDetails libraryDetails =
        CqlLibraryDetails.builder()
            .libraryName(cqlLibrary.getCqlLibraryName())
            .cql(cqlLibrary.getCql())
            .expressions(expressions)
            .build();
    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary);
    org.hl7.fhir.r5.model.Library r5moduleDefinition =
        elmTranslatorClient.getModuleDefinitionLibrary(libraryDetails, false, accessToken);
    updateLibraryDataRequirements(library, r5moduleDefinition);
    return library;
  }

  /**
   * Creates CqlLibrary resource for main library of MADiE Measure this will most likely go in
   * measure service once we have main library for measure
   *
   * @param madieMeasure instance of MADiE Measure
   * @return CqlLibrary
   */
  public CqlLibrary createCqlLibraryForMadieMeasure(Measure madieMeasure) {
    return CqlLibrary.builder()
        .id(madieMeasure.getCqlLibraryName())
        .cqlLibraryName(madieMeasure.getCqlLibraryName())
        .version(madieMeasure.getVersion())
        .description(madieMeasure.getCqlLibraryName())
        .cql(madieMeasure.getCql())
        .elmJson(madieMeasure.getElmJson())
        .elmXml(madieMeasure.getElmXml())
        .build();
  }

  private void setNarrativeText(DomainResource resource, String humanReadable) {
    Narrative narrative = new Narrative();
    narrative.setStatus(Narrative.NarrativeStatus.EXTENSIONS);

    narrative.setDivAsString(humanReadable);
    resource.setText(narrative);
  }

  private void addEffectiveDataRequirementsToMeasure(
      org.hl7.fhir.r4.model.Measure measure,
      org.hl7.fhir.r5.model.Library effectiveDataRequirements) {
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r4.model.Library r4EffectiveDataRequirements =
        (org.hl7.fhir.r4.model.Library)
            versionConvertor_40_50.convertResource(effectiveDataRequirements);
    // TODO: verify effective data requirement profile compliance:
    // http://hl7.org/fhir/us/cqfmeasures/StructureDefinition-module-definition-library-cqfm.html
    measure.addContained(r4EffectiveDataRequirements);
    Extension extension =
        new Extension()
            .setUrl(UriConstants.CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL)
            .setValue(new Reference().setReference("#effective-data-requirements"));
    measure.getExtension().add(extension);
  }

  private Set<String> getExpressions(org.hl7.fhir.r4.model.Measure r5Measure) {
    Set<String> expressionSet = new HashSet<>();
    r5Measure
        .getSupplementalData()
        .forEach(supData -> expressionSet.add(supData.getCriteria().getExpression()));
    r5Measure
        .getGroup()
        .forEach(
            groupMember -> {
              groupMember
                  .getPopulation()
                  .forEach(
                      population -> expressionSet.add(population.getCriteria().getExpression()));
              groupMember
                  .getStratifier()
                  .forEach(
                      stratifier -> expressionSet.add(stratifier.getCriteria().getExpression()));
            });
    return expressionSet;
  }

  private void updateLibraryDataRequirements(
      org.hl7.fhir.r4.model.Library library,
      org.hl7.fhir.r5.model.Library r5moduleDefinitionLibrary) {
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r4.model.Library r4moduleDefinitionLibrary =
        (org.hl7.fhir.r4.model.Library)
            versionConvertor_40_50.convertResource(r5moduleDefinitionLibrary);
    library.setDataRequirement(r4moduleDefinitionLibrary.getDataRequirement());
  }
}

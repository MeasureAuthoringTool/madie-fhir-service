package gov.cms.madie.madiefhirservice.services;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.dstu2.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.BundleType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import gov.cms.madie.packaging.utils.qicore411.PackagingUtilityImpl;

@ExtendWith(MockitoExtension.class)
class TestCaseBundleServiceTest implements ResourceFileUtil {

  @InjectMocks private TestCaseBundleService testCaseBundleService;

  @Spy private FhirContext fhirContext;

  @Spy private FhirResourceHelpers fhirResourceHelpers;

  private Measure madieMeasure;

  private TestCase testCase;
  private static MockedStatic<PackagingUtilityFactory> factory;

  @BeforeAll
  public static void staticSetup() {
    factory = Mockito.mockStatic(PackagingUtilityFactory.class);
  }

  @AfterAll
  public static void close() {
    factory.close();
  }

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
    testCase = Objects.requireNonNull(madieMeasure).getTestCases().get(0);
    ReflectionTestUtils.setField(fhirResourceHelpers, "madieUrl", "madie.cms.gov");
  }

  @Test
  void updateEntryTest() {
    IParser parser =
        fhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);
    Bundle bundle = parser.parseResource(Bundle.class, testCase.getJson());
    assertNull(bundle.getEntry().get(0).getRequest().getMethod());
    testCaseBundleService.updateEntry(testCase, BundleType.TRANSACTION);

    bundle = parser.parseResource(Bundle.class, testCase.getJson());
    assertEquals(
        bundle.getEntry().get(0).getRequest().getMethod().toString(), HTTPVerb.PUT.toString());
  }

  @Test
  void zipTestCaseContentsTest() {

    PackagingUtilityImpl utility = Mockito.mock(PackagingUtilityImpl.class);

    factory.when(() -> PackagingUtilityFactory.getInstance("QI-Core v4.1.1")).thenReturn(utility);
    doReturn("THis is a test".getBytes()).when(utility).getZipBundle(any(), isNull());
    IParser parser =
        fhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);

    Bundle bundle = parser.parseResource(Bundle.class, testCase.getJson());
    Map<String, Bundle> exportableTestCaseBundle = new HashMap<>();
    exportableTestCaseBundle.put("Test", bundle);
    List<TestCase> testCaseList = new ArrayList<>();
    testCaseList.add(testCase);
    byte[] results =
        testCaseBundleService.zipTestCaseContents(
            madieMeasure, exportableTestCaseBundle, testCaseList);
    assertNotNull(results);
  }

  @Test
  void getTestCaseExportBundleMulti() {
    Map<String, Bundle> exportMap =
        testCaseBundleService.getTestCaseExportBundle(madieMeasure, madieMeasure.getTestCases());
    assertEquals(2, exportMap.size());

    // first test case bundle(collection)
    Bundle bundle =
        exportMap.get(
            "285d114d-9c36-4d66-b0a0-06f395bbf23d/title-v0.0.000-testcaseseries-testcasetitle");
    assertEquals(5, bundle.getEntry().size());
    assertEquals(bundle.getType(), Bundle.BundleType.COLLECTION);
    Bundle.BundleEntryComponent bundleEntry = bundle.getEntry().get(4);
    assertNull(bundleEntry.getRequest().getMethod());
    assertNull(bundleEntry.getRequest().getUrl());
    MeasureReport measureReport = (MeasureReport) bundleEntry.getResource();
    assertEquals(
        "madie.cms.gov/MeasureReport/" + measureReport.getIdPart(),
        bundle.getEntry().get(4).getFullUrl());
    assertEquals("MeasureReport", measureReport.getResourceType().toString());
    assertEquals(
        UriConstants.CqfTestCases.CQFM_TEST_CASES,
        measureReport.getMeta().getProfile().get(0).asStringValue());

    Parameters parameters = (Parameters) measureReport.getContained().get(0);
    assertEquals("Patient-1", parameters.getParameter().get(0).getValue().toString());

    // Reference to parameter created above
    Reference reference = (Reference) measureReport.getExtension().get(0).getValue();
    assertEquals("#" + parameters.getId(), reference.getReference());
    assertEquals(MeasureReport.MeasureReportStatus.COMPLETE, measureReport.getStatus());
    assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, measureReport.getType());
    assertEquals("madie.cms.gov/Measure/SimpleFhirMeasureLib", measureReport.getMeasure());

    assertEquals(
        "01/01/2023", DateFormatUtils.format(measureReport.getPeriod().getStart(), "MM/dd/yyyy"));
    assertThat(
        DateFormatUtils.format(measureReport.getPeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    // Groups
    assertEquals(1, measureReport.getGroup().size());
    assertEquals(6, measureReport.getGroup().get(0).getPopulation().size());
    measureReport.getGroup().get(0).getPopulation().stream()
        .filter(
            p -> "initial-population".equalsIgnoreCase(p.getCode().getCoding().get(0).getCode()))
        .findFirst()
        .ifPresent(e -> assertEquals(0, e.getCount()));

    // evaluated resources
    assertEquals(4, measureReport.getEvaluatedResource().size());
    assertEquals("Patient/Patient-1", measureReport.getEvaluatedResource().get(0).getReference());
    assertEquals(
        "Encounter/Encounter-1", measureReport.getEvaluatedResource().get(1).getReference());

    // second test case bundle(transactional)
    bundle =
        exportMap.get(
            "0ec1197a-4895-43ed-b2eb-27971f8fb95b/title-v0.0.000-testcaseseries-testcasetitle1");
    assertEquals(5, bundle.getEntry().size());
    assertEquals(bundle.getType(), Bundle.BundleType.TRANSACTION);
    bundleEntry = bundle.getEntry().get(4);
    assertEquals(bundleEntry.getRequest().getMethod(), Bundle.HTTPVerb.PUT);
    assertEquals(
        bundleEntry.getRequest().getUrl(),
        "MeasureReport/" + bundleEntry.getResource().getIdPart());
  }

  @Test
  void getTestCaseExportBundleMultiReducedResult() {
    madieMeasure.getTestCases().get(1).setJson("malformed");
    Map<String, Bundle> exportMap =
        testCaseBundleService.getTestCaseExportBundle(madieMeasure, madieMeasure.getTestCases());
    // The service should remove the malformed testCase and return only the valid one
    assertEquals(1, exportMap.size());

    Bundle bundle =
        exportMap.get(
            "285d114d-9c36-4d66-b0a0-06f395bbf23d/title-v0.0.000-testcaseseries-testcasetitle");
    assertEquals(5, bundle.getEntry().size());
    MeasureReport measureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    assertEquals("MeasureReport", measureReport.getResourceType().toString());
    assertEquals(
        UriConstants.CqfTestCases.CQFM_TEST_CASES,
        measureReport.getMeta().getProfile().get(0).asStringValue());

    Parameters parameters = (Parameters) measureReport.getContained().get(0);
    assertEquals("Patient-1", parameters.getParameter().get(0).getValue().toString());

    // Reference to parameter created above
    Reference reference = (Reference) measureReport.getExtension().get(0).getValue();
    assertEquals("#" + parameters.getId(), reference.getReference());
    assertEquals(MeasureReport.MeasureReportStatus.COMPLETE, measureReport.getStatus());
    assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, measureReport.getType());
    assertEquals("madie.cms.gov/Measure/SimpleFhirMeasureLib", measureReport.getMeasure());

    assertEquals(
        "01/01/2023", DateFormatUtils.format(measureReport.getPeriod().getStart(), "MM/dd/yyyy"));
    assertThat(
        DateFormatUtils.format(measureReport.getPeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    // Groups
    assertEquals(1, measureReport.getGroup().size());
    assertEquals(6, measureReport.getGroup().get(0).getPopulation().size());
    measureReport.getGroup().get(0).getPopulation().stream()
        .filter(
            p -> "initial-population".equalsIgnoreCase(p.getCode().getCoding().get(0).getCode()))
        .findFirst()
        .ifPresent(e -> assertEquals(0, e.getCount()));

    // evaluated resources
    assertEquals(4, measureReport.getEvaluatedResource().size());
    assertEquals("Patient/Patient-1", measureReport.getEvaluatedResource().get(0).getReference());
    assertEquals(
        "Encounter/Encounter-1", measureReport.getEvaluatedResource().get(1).getReference());
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenTestCaseIsNotFound() {
    List<TestCase> testCaseList = null;
    assertThrows(
        InternalServerException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, testCaseList));
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenTestCaseListIsEmpty() {
    assertThrows(
        InternalServerException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, emptyList()));
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenMeasureIsNotFound() {
    madieMeasure = null;
    assertThrows(
        InternalServerException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, singletonList(testCase)));
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenTestCaseJsonIsNull() {
    testCase.setJson(null);
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, singletonList(testCase)));
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenTestCaseJsonIsEmpty() {
    testCase.setJson("");
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, singletonList(testCase)));
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenAllTestCaseJsonIsMalformed() {
    testCase.setJson("test");
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, singletonList(testCase)));
  }

  @Test
  void getTestCaseExportBundleReturnsMeasureReportWithNoGroupPopulations() {

    madieMeasure.getTestCases().get(0).setGroupPopulations(null);
    Map<String, Bundle> exportMap =
        testCaseBundleService.getTestCaseExportBundle(madieMeasure, madieMeasure.getTestCases());
    assertEquals(2, exportMap.size());

    Bundle bundle =
        exportMap.get(
            "285d114d-9c36-4d66-b0a0-06f395bbf23d/title-v0.0.000-testcaseseries-testcasetitle");
    MeasureReport measureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    assertEquals(0, measureReport.getGroup().size());
  }

  @Disabled
  @Test
  void zipTestCaseContents() throws IOException {
    Map<String, Bundle> testCaseBundleMap = new HashMap<>();
    testCaseBundleMap.put(
        "test1",
        FhirContext.forR4()
            .newJsonParser()
            .parseResource(Bundle.class, madieMeasure.getTestCases().get(0).getJson()));
    testCaseBundleMap.put(
        "test2",
        FhirContext.forR4()
            .newJsonParser()
            .parseResource(Bundle.class, madieMeasure.getTestCases().get(1).getJson()));

    byte[] result =
        testCaseBundleService.zipTestCaseContents(
            madieMeasure, testCaseBundleMap, madieMeasure.getTestCases());

    Map<String, String> zipContents = getZipContents(result);
    assertEquals(3, zipContents.size());
    assertTrue(zipContents.containsKey("test1.json"));
    assertTrue(zipContents.containsKey("test2.json"));
    assertTrue(zipContents.containsKey("README.txt"));
  }

  private Map<String, String> getZipContents(byte[] inputBytes) throws IOException {
    Map<String, String> zipContents = new HashMap<>();
    try (var zipInputStream = new ZipInputStream(new ByteArrayInputStream(inputBytes))) {
      ZipEntry entry;
      byte[] buffer = new byte[2048];

      while ((entry = zipInputStream.getNextEntry()) != null) {
        int size;
        String filename = FilenameUtils.getName(entry.getName());
        var byteArrayOutputStream = new ByteArrayOutputStream();
        while ((size = zipInputStream.read(buffer)) > 0) {
          byteArrayOutputStream.write(buffer, 0, size);
        }

        String fileContents = byteArrayOutputStream.toString();
        byteArrayOutputStream.flush();
        zipInputStream.closeEntry();
        zipContents.put(filename, fileContents);
      }

      zipInputStream.closeEntry();
    }
    return zipContents;
  }
}

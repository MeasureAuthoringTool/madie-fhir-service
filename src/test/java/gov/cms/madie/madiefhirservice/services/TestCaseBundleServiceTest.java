package gov.cms.madie.madiefhirservice.services;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TestCaseBundleServiceTest implements ResourceFileUtil {

  @InjectMocks private TestCaseBundleService testCaseBundleService;

  @Spy private FhirContext fhirContext;

  @Spy private FhirResourceHelpers fhirResourceHelpers;

  private Measure madieMeasure;

  private TestCase testCase;

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
    testCase = Objects.requireNonNull(madieMeasure).getTestCases().get(0);
    ReflectionTestUtils.setField(fhirResourceHelpers, "fhirBaseUrl", "cms.gov");
  }

  @Test
  void getTestCaseExportBundleMulti() {
    Map<String, Bundle> exportMap =
        testCaseBundleService.getTestCaseExportBundle(madieMeasure, madieMeasure.getTestCases());
    assertEquals(2, exportMap.size());

    Bundle bundle =
        exportMap.get(
            "285d114d-9c36-4d66-b0a0-06f395bbf23d/title-v0.0.000-test case series-test case title");
    assertEquals(5, bundle.getEntry().size());
    MeasureReport measureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    assertEquals("MeasureReport", measureReport.getResourceType().toString());
    assertEquals(
        UriConstants.CqfTestCases.CQFM_TEST_CASES,
        measureReport.getMeta().getProfile().get(0).asStringValue());

    Parameters parameters = (Parameters) measureReport.getContained().get(0);
    assertEquals("test case title-parameters", parameters.getId());
    assertEquals("Patient-1", parameters.getParameter().get(0).getValue().toString());

    // Reference to parameter created above
    Reference reference = (Reference) measureReport.getExtension().get(0).getValue();
    assertEquals("#test case title-parameters", reference.getReference());
    assertEquals(MeasureReport.MeasureReportStatus.COMPLETE, measureReport.getStatus());
    assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, measureReport.getType());
    assertEquals("cms.gov/Measure/SimpleFhirMeasureLib", measureReport.getMeasure());

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
  void getTestCaseExportBundleMultiReducedResult() {
    madieMeasure.getTestCases().get(1).setJson("malformed");
    Map<String, Bundle> exportMap =
        testCaseBundleService.getTestCaseExportBundle(madieMeasure, madieMeasure.getTestCases());
    // The service should remove the malformed testCase and return only the valid one
    assertEquals(1, exportMap.size());

    Bundle bundle =
        exportMap.get(
            "285d114d-9c36-4d66-b0a0-06f395bbf23d/title-v0.0.000-test case series-test case title");
    assertEquals(5, bundle.getEntry().size());
    MeasureReport measureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    assertEquals("MeasureReport", measureReport.getResourceType().toString());
    assertEquals(
        UriConstants.CqfTestCases.CQFM_TEST_CASES,
        measureReport.getMeta().getProfile().get(0).asStringValue());

    Parameters parameters = (Parameters) measureReport.getContained().get(0);
    assertEquals("test case title-parameters", parameters.getId());
    assertEquals("Patient-1", parameters.getParameter().get(0).getValue().toString());

    // Reference to parameter created above
    Reference reference = (Reference) measureReport.getExtension().get(0).getValue();
    assertEquals("#test case title-parameters", reference.getReference());
    assertEquals(MeasureReport.MeasureReportStatus.COMPLETE, measureReport.getStatus());
    assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, measureReport.getType());
    assertEquals("cms.gov/Measure/SimpleFhirMeasureLib", measureReport.getMeasure());

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
            "285d114d-9c36-4d66-b0a0-06f395bbf23d/title-v0.0.000-test case series-test case title");
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

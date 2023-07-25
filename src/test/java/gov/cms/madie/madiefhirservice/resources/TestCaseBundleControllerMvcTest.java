package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.madiefhirservice.services.TestCaseBundleService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.dto.ExportDTO;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TestCaseBundleController.class})
class TestCaseBundleControllerMvcTest implements ResourceFileUtil {

  private static final String TEST_USER_ID = "john_doe";

  private static final String TEST_CASE_ID = "62fe4466848fd80e1dd3edd0";
  private static final String TEST_CASE_ID_2 = "62fe4466848fd80e1dd3edd1";

  @MockBean private TestCaseBundleService testCaseBundleService;

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper mapper;

  private Bundle testCaseBundle;

  private String madieMeasureJson;

  @BeforeEach
  public void setUp() {
    madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    String testCaseJson = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    testCaseBundle = FhirContext.forR4().newJsonParser().parseResource(Bundle.class, testCaseJson);
  }

  @Test
  void getTestCaseExportBundleThrowExceptionWhenTestCasesAreNotFoundInMeasure() throws Exception {
    var madieMeasureWithNoTestCases =
        getStringFromTestResource(
            "/measures/SimpleFhirMeasureLib/madie_measure_no_test_cases.json");
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/test-cases/" + TEST_CASE_ID + "/exports")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(madieMeasureWithNoTestCases)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTestCaseExportBundleThrowExceptionWhenTestCaseIsNotFound() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/test-cases/example-test-case-id/exports")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTestCaseExportBundle() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    when(testCaseBundleService.getTestCaseExportBundle(any(Measure.class), any(TestCase.class)))
        .thenReturn(testCaseBundle);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/test-cases/" + TEST_CASE_ID + "/exports")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk());
    verify(testCaseBundleService, times(1))
        .getTestCaseExportBundle(any(Measure.class), any(TestCase.class));
  }

  @Test
  void getTestCaseExportBundleMulti() throws Exception {
    List<String> testCaseIdList = new ArrayList<>();
    testCaseIdList.add(TEST_CASE_ID);
    testCaseIdList.add(TEST_CASE_ID_2);

    ExportDTO dto =
        ExportDTO.builder()
            .measure(mapper.readValue(madieMeasureJson, Measure.class))
            .testCaseIds(testCaseIdList)
            .build();
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    Map<TestCase, Bundle> testCaseBundleMap = new HashMap<>();
    testCaseBundleMap.put(TestCase.builder().id("test1").title("test1").build(), testCaseBundle);
    testCaseBundleMap.put(TestCase.builder().id("test2").title("test2").build(), testCaseBundle);
    when(testCaseBundleService.getTestCaseExportBundle(any(Measure.class), any(List.class)))
        .thenReturn(testCaseBundleMap);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/test-cases/export-all")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(mapper.writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk());
    verify(testCaseBundleService, times(1))
        .getTestCaseExportBundle(any(Measure.class), any(List.class));
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenTestCasesAreNotFoundInMeasure() throws Exception {
    var madieMeasureWithNoTestCases =
            getStringFromTestResource(
                    "/measures/SimpleFhirMeasureLib/madie_measure_no_test_cases.json");

    ExportDTO dto =
            ExportDTO.builder()
                    .measure(mapper.readValue(madieMeasureWithNoTestCases, Measure.class))
                    .build();
    mockMvc
            .perform(
                    MockMvcRequestBuilders.put("/fhir/test-cases/export-all")
                            .with(user(TEST_USER_ID))
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, "test-okta")
                            .content(mapper.writeValueAsString(dto))
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenTestCaseIsNotFound() throws Exception {

    ExportDTO dto =
            ExportDTO.builder()
                    .measure(mapper.readValue(madieMeasureJson, Measure.class))
                    .build();
    mockMvc
            .perform(
                    MockMvcRequestBuilders.put("/fhir/test-cases/export-all")
                            .with(user(TEST_USER_ID))
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, "test-okta")
                            .content(mapper.writeValueAsString(dto))
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());
  }
}

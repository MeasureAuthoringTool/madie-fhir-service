package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.madiefhirservice.dto.TestCaseExecutionBundlesDTO;
import gov.cms.madie.madiefhirservice.factories.ModelAwareFhirFactory;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import gov.cms.madie.madiefhirservice.services.TestCaseBundleService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.BundleType;
import gov.cms.madie.models.dto.ExportDTO;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.Principal;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TestCaseBundleController.class})
class TestCaseBundleControllerMvcTest implements ResourceFileUtil {

  private static final String TEST_USER_ID = "john_doe";

  private static final String TEST_CASE_ID = "62fe4466848fd80e1dd3edd0";
  private static final String TEST_CASE_ID_2 = "62fe4466848fd80e1dd3edd1";

  @MockitoBean private ModelAwareFhirFactory fhirModelFactory;
  @MockitoBean private TestCaseBundleService testCaseBundleService;
  @MockitoBean private ResourceValidationService validationService;

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper mapper;

  @Captor private ArgumentCaptor<List<TestCase>> testCaseListCaptor;

  private Bundle testCaseBundle;

  private ExportDTO dto;

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");
    String testCaseJson = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    testCaseBundle = FhirContext.forR4().newJsonParser().parseResource(Bundle.class, testCaseJson);
    dto =
        ExportDTO.builder()
            .measure(mapper.readValue(madieMeasureJson, Measure.class))
            .testCaseIds(asList(TEST_CASE_ID, TEST_CASE_ID_2))
            .bundleType(BundleType.COLLECTION)
            .build();
  }

  @Test
  void getTestCaseExportBundleMulti() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    Map<String, Bundle> testCaseBundleMap = new HashMap<>();
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(0).getPatientId().toString(), testCaseBundle);
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(1).getPatientId().toString(), testCaseBundle);
    when(testCaseBundleService.getTestCaseExportBundle(
            any(Measure.class), anyList(), any(ExportDTO.class)))
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
        .getTestCaseExportBundle(any(Measure.class), anyList(), any(ExportDTO.class));
  }

  @Test
  void getTestCaseExportBundleMultiWithBundleTypeCollection() throws Exception {

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    Map<String, Bundle> testCaseBundleMap = new HashMap<>();
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(0).getPatientId().toString(), testCaseBundle);
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(1).getPatientId().toString(), testCaseBundle);
    when(testCaseBundleService.getTestCaseExportBundle(
            any(Measure.class), anyList(), any(ExportDTO.class)))
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
        .getTestCaseExportBundle(any(Measure.class), anyList(), any(ExportDTO.class));
  }

  @Test
  void getTestCaseExportBundleMultiWithBundleTypeCollectionWithMissingTestCases() throws Exception {

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    List<TestCase> allTestCases = new ArrayList<>(dto.getMeasure().getTestCases());
    TestCase errorTestCase =
        TestCase.builder()
            .id("ErrorId1")
            .patientId(UUID.randomUUID())
            .title("ErrorTC")
            .series("FAIL")
            .json(
                "{\n  \"resourceType\": \"Bundle\",\n  \"id\": \"DENEXPass-NarcolepsyOnsetsEndOfMP\",\n  \"type\": \"collection\",\n  \"entry\": [\n    {\n      \"fullUrl\": \"https://madie.cms.gov/Encounter/Encounter-1\",\n      \"resource\": {\n        \"resourceType\": \"Encounter\",\n   } } ] }")
            .build();
    allTestCases.add(errorTestCase);

    Map<String, Bundle> testCaseBundleMap = new HashMap<>();
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(0).getPatientId().toString(), testCaseBundle);
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(1).getPatientId().toString(), testCaseBundle);
    when(testCaseBundleService.getTestCaseExportBundle(
            any(Measure.class), anyList(), any(ExportDTO.class)))
        .thenReturn(testCaseBundleMap);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/test-cases/export-all")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(
                    mapper.writeValueAsString(
                        dto.toBuilder()
                            .measure(dto.getMeasure().toBuilder().testCases(allTestCases).build())
                            .testCaseIds(
                                asList(TEST_CASE_ID, TEST_CASE_ID_2, errorTestCase.getId()))
                            .build()))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isPartialContent());
    verify(testCaseBundleService, times(1))
        .getTestCaseExportBundle(any(Measure.class), anyList(), any(ExportDTO.class));
    verify(testCaseBundleService, times(1))
        .zipTestCaseContents(any(Measure.class), anyMap(), anyList(), testCaseListCaptor.capture());
  }

  @Test
  void getTestCaseExportBundleMultiWithBundleTypeTransaction() throws Exception {

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    Map<String, Bundle> testCaseBundleMap = new HashMap<>();
    dto.setBundleType(BundleType.TRANSACTION);
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(0).getPatientId().toString(), testCaseBundle);
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(1).getPatientId().toString(), testCaseBundle);
    when(testCaseBundleService.getTestCaseExportBundle(
            any(Measure.class), anyList(), any(ExportDTO.class)))
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
        .getTestCaseExportBundle(any(Measure.class), anyList(), any(ExportDTO.class));
  }

  @Test
  void getTestCaseExportAllThrowExceptionWhenTestCasesAreNotFoundInMeasure() throws Exception {
    var madieMeasureWithNoTestCases =
        getStringFromTestResource("/measures/madie_measure_no_test_cases.json");

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
    dto.setTestCaseIds(null);
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
  void getTestCaseExportAllReturnPartialContent() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    Map<String, Bundle> testCaseBundleMap = new HashMap<>();
    testCaseBundleMap.put(
        dto.getMeasure().getTestCases().get(0).getPatientId().toString(), testCaseBundle);
    when(testCaseBundleService.getTestCaseExportBundle(
            any(Measure.class), anyList(), any(ExportDTO.class)))
        .thenReturn(testCaseBundleMap);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/test-cases/export-all")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(mapper.writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().is(206));
    verify(testCaseBundleService, times(1))
        .getTestCaseExportBundle(any(Measure.class), anyList(), any(ExportDTO.class));
  }

  @Test
  void testReferencesInBundleValidation() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    // Arrange
    String testCaseJson = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .id("test-case-valid-refs")
                .patientId(UUID.randomUUID())
                .title("Test Case with Valid References")
                .series("HAPPY_PATH")
                .json(testCaseJson)
                .build());

    // Mock the factory to return our test bundle
    when(fhirModelFactory.parseForModel(any(), eq(testCaseJson))).thenReturn(new Bundle());
    when(fhirModelFactory.getJsonParserForModel(any()))
        .thenReturn(FhirContext.forR4().newJsonParser());
    when(fhirModelFactory.getContextForModel(any())).thenReturn(FhirContext.forR4());

    // Mock the validation service to indicate no invalid references
    when(validationService.findResourcesWithInvalidReferences(
            any(FhirContext.class), any(Bundle.class)))
        .thenReturn(new HashSet<>()); // No invalid references

    // Act
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/test-cases/qicore/4.1.1/execution-bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(mapper.writeValueAsString(testCases))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> {
              String responseContent = result.getResponse().getContentAsString();
              assertThat(responseContent, is(notNullValue()));
              TestCaseExecutionBundlesDTO dto =
                  mapper.readValue(responseContent, TestCaseExecutionBundlesDTO.class);
              assertThat(dto.getTestCases().size(), is(testCases.size()));
              assertThat(dto.getModifiedTestCaseIds().size(), is(0));
              Bundle returnedBundle =
                  FhirContext.forR4()
                      .newJsonParser()
                      .parseResource(Bundle.class, dto.getTestCases().get(0).getJson());
              assertThat(returnedBundle.getEntry().size(), is(testCaseBundle.getEntry().size()));
              assertTrue(
                  returnedBundle
                      .getEntry()
                      .get(0)
                      .getResource()
                      .equalsDeep(testCaseBundle.getEntry().get(0).getResource()));
              assertTrue(
                  returnedBundle
                      .getEntry()
                      .get(1)
                      .getResource()
                      .equalsDeep(testCaseBundle.getEntry().get(1).getResource()));
            })
        .andExpect(status().isOk());

    // Assert
    verify(fhirModelFactory, times(1)).parseForModel(any(), anyString());
    verify(validationService, times(1))
        .findResourcesWithInvalidReferences(any(FhirContext.class), any(Bundle.class));
  }

  @Test
  void testExecutionBundleReturnsModifiedBundles() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    // Arrange
    String testCaseJson = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    String testCaseJsonWithInvalidReference = testCaseJson.replace("Patient\\/1", "Patient\\/nope");

    Bundle bundleWithInvalidReference =
        FhirContext.forR4()
            .newJsonParser()
            .parseResource(Bundle.class, testCaseJsonWithInvalidReference);
    Bundle bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle.class, testCaseJson);

    List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .id("test-case-invalid-refs")
                .patientId(UUID.randomUUID())
                .title("Test Case with Invalid References")
                .series("HAPPY_PATH")
                .json(testCaseJsonWithInvalidReference)
                .build(),
            TestCase.builder()
                .id("test-case-valid-refs")
                .patientId(UUID.randomUUID())
                .title("Test Case with Valid References")
                .series("HAPPY_PATH")
                .json(testCaseJson)
                .build());

    // Mock the factory to return our test bundle
    when(fhirModelFactory.parseForModel(any(), eq(testCaseJson))).thenReturn(bundle);
    when(fhirModelFactory.parseForModel(any(), eq(testCaseJsonWithInvalidReference)))
        .thenReturn(bundleWithInvalidReference);
    when(fhirModelFactory.getJsonParserForModel(any()))
        .thenReturn(FhirContext.forR4().newJsonParser());
    when(fhirModelFactory.getContextForModel(any())).thenReturn(FhirContext.forR4());

    when(validationService.findResourcesWithInvalidReferences(any(FhirContext.class), eq(bundle)))
        .thenReturn(new HashSet<>());
    when(validationService.findResourcesWithInvalidReferences(
            any(FhirContext.class), eq(bundleWithInvalidReference)))
        .thenReturn(
            Set.of(
                bundleWithInvalidReference
                    .getEntry()
                    .get(0)
                    .getResource())); // First entry has invalid reference

    // Act
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/test-cases/qicore/4.1.1/execution-bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(mapper.writeValueAsString(testCases))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> {
              String responseContent = result.getResponse().getContentAsString();
              assertThat(responseContent, is(notNullValue()));
              TestCaseExecutionBundlesDTO dto =
                  mapper.readValue(responseContent, TestCaseExecutionBundlesDTO.class);
              assertThat(dto.getTestCases().size(), is(testCases.size()));
              assertThat(dto.getModifiedTestCaseIds().size(), is(1));
              assertThat(dto.getModifiedTestCaseIds().get(0), is("test-case-invalid-refs"));
              assertFalse(dto.getModifiedTestCaseIds().contains("test-case-valid-refs"));
              Bundle returnedModifiedBundle =
                  FhirContext.forR4()
                      .newJsonParser()
                      .parseResource(Bundle.class, dto.getTestCases().get(0).getJson());
              assertThat(
                  returnedModifiedBundle.getEntry().size(),
                  is(bundleWithInvalidReference.getEntry().size() - 1));
              assertTrue(
                  returnedModifiedBundle
                      .getEntry()
                      .get(0)
                      .getResource()
                      .equalsDeep(bundleWithInvalidReference.getEntry().get(1).getResource()));

              Bundle returnedBundle =
                  FhirContext.forR4()
                      .newJsonParser()
                      .parseResource(Bundle.class, dto.getTestCases().get(1).getJson());
              assertThat(returnedBundle.getEntry().size(), is(bundle.getEntry().size()));
              assertTrue(
                  returnedBundle
                      .getEntry()
                      .get(0)
                      .getResource()
                      .equalsDeep(bundle.getEntry().get(0).getResource()));
            })
        .andExpect(status().isOk());

    // Assert
    verify(fhirModelFactory, times(2)).parseForModel(any(), anyString());
    verify(validationService, times(2))
        .findResourcesWithInvalidReferences(any(FhirContext.class), any(Bundle.class));
  }

  @Test
  void testExecutionBundleThrowsOnEmptyJson() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);

    // Arrange
    List<TestCase> testCases =
        List.of(
            TestCase.builder()
                .id("test-case-valid-refs")
                .patientId(UUID.randomUUID())
                .title("Test Case with Valid References")
                .series("ERROR_PATH")
                .json("{}")
                .build());

    // Mock the factory to return our test bundle
    when(fhirModelFactory.parseForModel(any(), anyString())).thenThrow(new DataFormatException());
    when(fhirModelFactory.getJsonParserForModel(any()))
        .thenReturn(FhirContext.forR4().newJsonParser());
    when(fhirModelFactory.getContextForModel(any())).thenReturn(FhirContext.forR4());

    // Mock the validation service to indicate no invalid references
    when(validationService.findResourcesWithInvalidReferences(
            any(FhirContext.class), any(Bundle.class)))
        .thenReturn(new HashSet<>()); // No invalid references

    // Act
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/test-cases/qicore/4.1.1/execution-bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(mapper.writeValueAsString(testCases))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isBadRequest());

    // Assert
    verify(fhirModelFactory, times(1)).parseForModel(any(), anyString());
    verify(validationService, times(0))
        .findResourcesWithInvalidReferences(any(FhirContext.class), any(Bundle.class));
  }
}

package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.madie.madiefhirservice.factories.ModelAwareFhirFactory;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ValidationController.class})
@Import(ValidationMvcTestConfiguration.class)
@ActiveProfiles("MvcTest")
class ValidationControllerMvcTest implements ResourceFileUtil {
  private static final String TEST_USER_ID = "john_doe";

  @Autowired private FhirContext qicoreFhirContext;
  @Autowired private FhirContext qicore6FhirContext;
  @Autowired private FhirContext fhirContextForR5;

  @MockBean private ResourceValidationService validationService;

  @Autowired private MockMvc mockMvc;

  @Autowired FhirValidator qicoreNpmFhirValidator;
  @Autowired FhirValidator qicore6NpmFhirValidator;

  @MockBean private ModelAwareFhirFactory validatorFactory;

  private IParser r4Parser;

  @BeforeEach
  void beforeEach() {
    r4Parser =
        qicoreFhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForInvalidFhirJson() throws Exception {
    final String testCaseJson = "{ }";
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(qicoreFhirContext);
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(r4Parser);
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String bundleString = invocationOnMock.getArgument(1);
              return r4Parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, bundleString);
            });
    when(validationService.invalidErrorOutcome(
            any(FhirContext.class), any(IParser.class), anyString(), anyString()))
        .thenReturn(HapiOperationOutcome.builder().code(400).successful(false).build());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(testCaseJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.successful").value(false));

    verify(validatorFactory, times(1)).parseForModel(any(ModelType.class), anyString());
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForBadResourceType() throws Exception {
    final String testCaseJson = "{\"resourceType\": \"Patient\" }";
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(qicoreFhirContext);
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(r4Parser);
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String bundleString = invocationOnMock.getArgument(1);
              return r4Parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, bundleString);
            });
    when(validationService.invalidErrorOutcome(
            any(FhirContext.class), any(IParser.class), anyString(), anyString()))
        .thenReturn(HapiOperationOutcome.builder().code(400).successful(false).build());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(testCaseJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.successful").value(false));
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForInvalidEncounter() throws Exception {
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(new OperationOutcome());
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String bundleString = invocationOnMock.getArgument(1);
              return r4Parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, bundleString);
            });
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(qicoreFhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class)))
        .thenReturn(qicoreNpmFhirValidator);
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(r4Parser);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(tc1Json)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.successful").value(false));
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForMissingProfile() throws Exception {
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    OperationOutcome operationOutcomeWithIssues = new OperationOutcome();
    operationOutcomeWithIssues.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING);
    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(operationOutcomeWithIssues);
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(operationOutcomeWithIssues);
    when(validationService.isSuccessful(any(FhirContext.class), any(OperationOutcome.class)))
        .thenReturn(true);

    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String bundleString = invocationOnMock.getArgument(1);
              return r4Parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, bundleString);
            });
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(qicoreFhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class)))
        .thenReturn(qicoreNpmFhirValidator);
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(r4Parser);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(tc1Json)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.successful").value(true));
  }

  @Test
  void testSuccessfulOutcomeReturnedForValidTestCaseJson() throws Exception {
    String tc1Json = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(new OperationOutcome());
    when(validationService.isSuccessful(any(FhirContext.class), any(OperationOutcome.class)))
        .thenReturn(true);

    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String bundleString = invocationOnMock.getArgument(1);
              return r4Parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, bundleString);
            });
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(qicoreFhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class)))
        .thenReturn(qicoreNpmFhirValidator);
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(r4Parser);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(tc1Json)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.successful").value(true));
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForMissingProfileValidTestCaseJson() throws Exception {
    String tc1Json = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    OperationOutcome profileWarningOutcome = new OperationOutcome();
    profileWarningOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING);
    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(profileWarningOutcome);
    OperationOutcome invalidIdErrorOutcome = new OperationOutcome();
    invalidIdErrorOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR);
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(invalidIdErrorOutcome);
    OperationOutcome combinedOutcome = new OperationOutcome();
    combinedOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR);
    combinedOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING);
    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(combinedOutcome);
    when(validationService.isSuccessful(any(FhirContext.class), any(OperationOutcome.class)))
        .thenReturn(false);

    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenAnswer(
            invocationOnMock -> {
              String bundleString = invocationOnMock.getArgument(1);
              return r4Parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, bundleString);
            });
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(qicoreFhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class)))
        .thenReturn(qicoreNpmFhirValidator);
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(r4Parser);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(tc1Json)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.successful").value(false));
  }
}

package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.IParserErrorHandler;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.madiefhirservice.exceptions.HapiJsonException;
import gov.cms.madie.madiefhirservice.factories.ModelAwareFhirFactory;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationControllerTest implements ResourceFileUtil {

  @Spy FhirContext fhirContext;

  @Mock ResourceValidationService validationService;

  @Mock FhirValidator fhirValidator;

  @Mock HttpEntity<String> entity;

  @Mock JsonParser parser;

  @Mock ObjectMapper mapper;

  @Mock ModelAwareFhirFactory validatorFactory;

  @InjectMocks private ValidationController validationController;

  @BeforeEach
  void beforeEach() {
    fhirContext = Mockito.spy(FhirContext.forR4());
    Mockito.lenient().when(fhirContext.newJsonParser()).thenReturn(parser);
    Mockito.lenient()
        .when(parser.setParserErrorHandler(any(IParserErrorHandler.class)))
        .thenReturn(parser);
    Mockito.lenient().when(parser.setPrettyPrint(anyBoolean())).thenReturn(parser);
  }

  @Test
  void testValidationControllerReturnsOutcomeForDataFormatException() {
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenThrow(new DataFormatException("BAD JSON, BAD!"));
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validationService.invalidErrorOutcome(
            any(FhirContext.class), any(IParser.class), anyString(), anyString()))
        .thenReturn(
            HapiOperationOutcome.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("BAD")
                .successful(false)
                .outcomeResponse(null)
                .build());

    when(entity.getBody()).thenReturn("{\"foo\": \"foo2\" }");
    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
    assertThat(output.isSuccessful(), is(false));
  }

  @Test
  void testValidationControllerReturnsOutcomeForClassCastException() {
    // given
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenThrow(new ClassCastException("wrong resource type!"));
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validationService.invalidErrorOutcome(
            any(FhirContext.class), any(IParser.class), anyString(), anyString()))
        .thenReturn(
            HapiOperationOutcome.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("BAD")
                .successful(false)
                .outcomeResponse(null)
                .build());
    when(entity.getBody()).thenReturn("{\"foo\": \"foo2\" }");

    // when
    HapiOperationOutcome output = validationController.validateBundle(entity);

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
    assertThat(output.isSuccessful(), is(false));
  }

  @Test
  void testValidationControllerReturnsExceptionForProcessingError() throws JsonProcessingException {
    // given
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenThrow(new ClassCastException("wrong resource type!"));
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validationService.invalidErrorOutcome(
            any(FhirContext.class), any(IParser.class), anyString(), anyString()))
        .thenThrow(HapiJsonException.class);
    when(entity.getBody()).thenReturn("{\"foo\": \"foo2\" }");

    // when/then
    assertThrows(HapiJsonException.class, () -> validationController.validateBundle(entity));
  }

  @Test
  void testValidationControllerReturnsOutcomeForMissingProfile() {
    // given
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenReturn(new Bundle());
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class))).thenReturn(fhirValidator);
    when(parser.encodeResourceToString(any(OperationOutcome.class))).thenReturn("{}");
    when(entity.getBody()).thenReturn("{\"foo\": \"foo2\" }");

    OperationOutcome operationOutcomeWithIssues = new OperationOutcome();
    operationOutcomeWithIssues.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR);
    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(operationOutcomeWithIssues);
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());

    ValidationResult result = Mockito.mock(ValidationResult.class);
    when(result.toOperationOutcome()).thenReturn(new OperationOutcome());
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);
    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(operationOutcomeWithIssues);
    when(validationService.isSuccessful(any(FhirContext.class), any(OperationOutcome.class)))
        .thenReturn(false);

    // when
    HapiOperationOutcome output = validationController.validateBundle(entity);

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
    assertThat(output.isSuccessful(), is(false));
  }

  @Test
  void testValidationControllerReturnsExceptionForErrorProcessingOutput()
      throws JsonProcessingException {
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenReturn(new Bundle());
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class))).thenReturn(fhirValidator);
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    when(entity.getBody()).thenReturn(tc1Json);
    ValidationResult result = Mockito.mock(ValidationResult.class);
    when(mapper.readValue(anyString(), any(Class.class)))
        .thenThrow(new RuntimeException("JsonProcessingException"));

    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    OperationOutcome outcome = new OperationOutcome();
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR);
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING);
    when(result.toOperationOutcome()).thenReturn(outcome);
    when(result.isSuccessful()).thenReturn(false);
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);
    when(parser.encodeResourceToString(any(OperationOutcome.class)))
        .thenReturn("{ \"resourceType\": \"OperationOutcome\" }");
    assertThrows(HapiJsonException.class, () -> validationController.validateBundle(entity));
  }

  @Test
  void testValidationControllerReturnsOutcomeWithIssues() throws JsonProcessingException {
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenReturn(new Bundle());
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class))).thenReturn(fhirValidator);
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    when(entity.getBody()).thenReturn(tc1Json);

    Map<String, Object> mockOutcome = new HashMap<>();
    mockOutcome.put("resourceType", "OperationOutcome");
    when(mapper.readValue(anyString(), any(Class.class))).thenReturn(mockOutcome);

    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());

    when(parser.encodeResourceToString(any(OperationOutcome.class)))
        .thenReturn("{ \"resourceType\": \"OperationOutcome\" }");

    ValidationResult result = Mockito.mock(ValidationResult.class);
    when(result.toOperationOutcome()).thenReturn(new OperationOutcome());
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);
    OperationOutcome outcome = new OperationOutcome();
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR);
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING);
    when(result.toOperationOutcome()).thenReturn(outcome);
    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(outcome);
    when(validationService.isSuccessful(any(FhirContext.class), any(OperationOutcome.class)))
        .thenReturn(false);

    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.OK.value())));
    assertThat(output.isSuccessful(), is(false));
    assertThat(output.getOutcomeResponse() instanceof Map, is(true));
    Map outcomeResponse = (Map) output.getOutcomeResponse();
    Object resourceType = outcomeResponse.get("resourceType");
    assertThat(resourceType, is(equalTo("OperationOutcome")));
  }

  @Test
  void testValidationControllerReturnsOutcomeWithUniqueIdIssues() throws JsonProcessingException {
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenReturn(new Bundle());
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class))).thenReturn(fhirValidator);
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    when(entity.getBody()).thenReturn(tc1Json);

    Map<String, Object> mockOutcome = new HashMap<>();
    mockOutcome.put("resourceType", "OperationOutcome");
    when(mapper.readValue(anyString(), any(Class.class))).thenReturn(mockOutcome);

    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    OperationOutcome errorOutcome = new OperationOutcome();
    errorOutcome
        .addIssue()
        .setDiagnostics(
            "All resources in bundle must have unique ID regardless of type. Multiple resources detected with ID 1234")
        .setSeverity(OperationOutcome.IssueSeverity.ERROR);
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(errorOutcome);
    when(parser.encodeResourceToString(any(OperationOutcome.class)))
        .thenReturn("{ \"resourceType\": \"OperationOutcome\" }");

    ValidationResult result = Mockito.mock(ValidationResult.class);
    when(result.toOperationOutcome()).thenReturn(new OperationOutcome());
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);

    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(errorOutcome);
    when(validationService.isSuccessful(any(FhirContext.class), any(OperationOutcome.class)))
        .thenReturn(false);

    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
    assertThat(output.getOutcomeResponse() instanceof Map, is(true));
    Map outcomeResponse = (Map) output.getOutcomeResponse();
    Object resourceType = outcomeResponse.get("resourceType");
    assertThat(resourceType, is(equalTo("OperationOutcome")));
  }

  @Test
  void testValidationControllerReturnsOutcomeWithInvalidIdIssues() throws JsonProcessingException {
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenReturn(new Bundle());
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class))).thenReturn(fhirValidator);
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    when(entity.getBody()).thenReturn(tc1Json);

    Map<String, Object> mockOutcome = new HashMap<>();
    mockOutcome.put("resourceType", "OperationOutcome");
    when(mapper.readValue(anyString(), any(Class.class))).thenReturn(mockOutcome);

    when(validationService.validateBundleResourcesProfiles(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());
    OperationOutcome errorOutcome = new OperationOutcome();
    errorOutcome
        .addIssue()
        .setDiagnostics("All resources must have an Id")
        .setSeverity(OperationOutcome.IssueSeverity.ERROR);
    when(validationService.validateBundleResourcesIdValid(
            any(FhirContext.class), any(IBaseBundle.class)))
        .thenReturn(errorOutcome);
    when(parser.encodeResourceToString(any(OperationOutcome.class)))
        .thenReturn("{ \"resourceType\": \"OperationOutcome\" }");

    ValidationResult result = Mockito.mock(ValidationResult.class);
    when(result.toOperationOutcome()).thenReturn(new OperationOutcome());
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);

    when(validationService.combineOutcomes(any(FhirContext.class), any(), any(), any()))
        .thenReturn(errorOutcome);
    when(validationService.isSuccessful(any(FhirContext.class), any(OperationOutcome.class)))
        .thenReturn(false);

    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
    assertThat(output.getOutcomeResponse() instanceof Map, is(true));
    Map outcomeResponse = (Map) output.getOutcomeResponse();
    Object resourceType = outcomeResponse.get("resourceType");
    assertThat(resourceType, is(equalTo("OperationOutcome")));
  }

  @Test
  void testValidationControllerReturnsSuccessfulOutcome() {
    when(validatorFactory.parseForModel(any(ModelType.class), anyString()))
        .thenReturn(new Bundle());
    when(validatorFactory.getJsonParserForModel(any(ModelType.class))).thenReturn(parser);
    when(validatorFactory.getContextForModel(any(ModelType.class))).thenReturn(fhirContext);
    when(validatorFactory.getValidatorForModel(any(ModelType.class))).thenReturn(fhirValidator);
    String tc1Json = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    when(entity.getBody()).thenReturn(tc1Json);
    ValidationResult result = Mockito.mock(ValidationResult.class);

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
    OperationOutcome outcome = new OperationOutcome();
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
    when(result.toOperationOutcome()).thenReturn(outcome);
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);
    when(parser.encodeResourceToString(any(OperationOutcome.class)))
        .thenReturn("{ \"resourceType\": \"OperationOutcome\" }");
    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.OK.value())));
    assertThat(output.isSuccessful(), is(true));
  }
}

package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cms.madie.models.measure.HapiOperationOutcome;
import gov.cms.madie.madiefhirservice.exceptions.HapiJsonException;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/validations")
@Tag(
    name = "HAPI-FHIR-Validation-Controller",
    description = "API for validating resources using HAPI utilities")
@AllArgsConstructor
public class ValidationController {

  private FhirContext fhirContext;
  private FhirValidator validator;
  private ResourceValidationService validationService;

  private ObjectMapper mapper;

  @PostMapping(
      path = "/bundles",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public HapiOperationOutcome validateBundle(HttpEntity<String> request) {
    LenientErrorHandler lenientErrorHandler =
        new LenientErrorHandler().setErrorOnInvalidValue(false);
    IParser parser =
        fhirContext.newJsonParser().setParserErrorHandler(lenientErrorHandler).setPrettyPrint(true);

    Bundle bundle;

    try {
      bundle = parser.parseResource(Bundle.class, request.getBody());
    } catch (DataFormatException | ClassCastException ex) {
      return invalidErrorOutcome(
          parser, "An error occurred while parsing the resource", ex.getMessage());
    }

    // only validate bundles
    if (!"BUNDLE".equalsIgnoreCase(bundle.fhirType())) {
      return invalidErrorOutcome(
          parser,
          "\"Resource must have resourceType of 'Bundle'",
          "Resource must have resourceType of 'Bundle'");
    }

    OperationOutcome requiredProfilesOutcome =
        validationService.validateBundleResourcesProfiles(bundle);
    OperationOutcome uniqueIdsOutcome =
        validationService.validateBundleResourcesIdUniqueness(bundle);

    ValidationResult result = validator.validateWithResult(bundle);
    try {
      final OperationOutcome combinedOutcome =
          validationService.combineOutcomes(
              requiredProfilesOutcome,
              uniqueIdsOutcome,
              (OperationOutcome) result.toOperationOutcome());
      String outcomeString = parser.encodeResourceToString(combinedOutcome);
      return HapiOperationOutcome.builder()
          .code(
              requiredProfilesOutcome.hasIssue() || uniqueIdsOutcome.hasIssue()
                  ? HttpStatus.BAD_REQUEST.value()
                  : HttpStatus.OK.value())
          .successful(validationService.isSuccessful(combinedOutcome))
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (Exception ex) {
      throw new HapiJsonException("An error occurred processing the validation results", ex);
    }
  }

  private HapiOperationOutcome invalidErrorOutcome(
      IParser parser, String message, String exceptionMessage) {
    OperationOutcome operationOutcome = new OperationOutcome();
    operationOutcome
        .addIssue()
        .setSeverity(OperationOutcome.IssueSeverity.ERROR)
        .setCode(OperationOutcome.IssueType.INVALID)
        .setDiagnostics(exceptionMessage);
    return encodeOutcome(parser, HttpStatus.BAD_REQUEST.value(), false, message, operationOutcome);
  }

  protected HapiOperationOutcome encodeOutcome(
      IParser parser, int code, boolean successful, String message, OperationOutcome outcome) {
    try {
      String outcomeString = parser.encodeResourceToString(outcome);
      return HapiOperationOutcome.builder()
          .code(code)
          .message(message)
          .successful(successful)
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (Exception ex) {
      throw new HapiJsonException("An error occurred processing the validation results", ex);
    }
  }
}

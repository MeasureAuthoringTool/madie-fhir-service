package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cms.madie.madiefhirservice.factories.ModelAwareFhirFactory;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import gov.cms.madie.madiefhirservice.exceptions.HapiJsonException;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.cms.madie.madiefhirservice.utils.ModelEndpointMap.QICORE_VERSION_MODELTYPE_MAP;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/validations")
@Tag(
    name = "HAPI-FHIR-Validation-Controller",
    description = "API for validating resources using HAPI utilities")
@AllArgsConstructor
public class ValidationController {

  private ResourceValidationService validationService;
  private ModelAwareFhirFactory validatorFactory;

  private ObjectMapper mapper;

  @PostMapping(
      path = "/qicore/{model}/bundles",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public HapiOperationOutcome validateBundleByModel(
      @PathVariable("model") String modelVersion, HttpEntity<String> request) {
    final ModelType modelType = QICORE_VERSION_MODELTYPE_MAP.get(modelVersion);
    FhirContext fhirContext = validatorFactory.getContextForModel(modelType);
    IParser parser = validatorFactory.getJsonParserForModel(modelType);
    FhirValidator fhirValidator = validatorFactory.getValidatorForModel(modelType);
    IBaseBundle bundle;
    try {
      bundle = validatorFactory.parseForModel(modelType, request.getBody());
    } catch (DataFormatException | ClassCastException ex) {
      return validationService.invalidErrorOutcome(
          fhirContext, parser, "An error occurred while parsing the resource", ex.getMessage());
    }

    // only validate bundles
    if (!"BUNDLE".equalsIgnoreCase(bundle.fhirType())) {
      return validationService.invalidErrorOutcome(
          fhirContext,
          parser,
          "\"Resource must have resourceType of 'Bundle'",
          "Resource must have resourceType of 'Bundle'");
    }

    IBaseOperationOutcome requiredProfilesOutcome =
        validationService.validateBundleResourcesProfiles(fhirContext, bundle);
    IBaseOperationOutcome validIdsOutcome =
        validationService.validateBundleResourcesIdValid(fhirContext, bundle);

    ValidationResult result = fhirValidator.validateWithResult(bundle);
    try {
      final IBaseOperationOutcome combinedOutcome =
          validationService.combineOutcomes(
              fhirContext, requiredProfilesOutcome, validIdsOutcome, result.toOperationOutcome());
      String outcomeString = parser.encodeResourceToString(combinedOutcome);
      return HapiOperationOutcome.builder()
          .code(
              OperationOutcomeUtil.hasIssues(fhirContext, requiredProfilesOutcome)
                      || OperationOutcomeUtil.hasIssues(fhirContext, validIdsOutcome)
                  ? HttpStatus.BAD_REQUEST.value()
                  : HttpStatus.OK.value())
          .successful(validationService.isSuccessful(fhirContext, combinedOutcome))
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (Exception ex) {
      throw new HapiJsonException("An error occurred processing the validation results", ex);
    }
  }
}

package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.*;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.Optional;

@Slf4j
public class QiCoreLenientTerminologyValidator extends LenientTerminologyValidator {

  public QiCoreLenientTerminologyValidator(FhirContext qicore6FhirContext) {
    super(qicore6FhirContext);
  }

  @Override
  public CodeValidationResult validateCodeInValueSet(
      ValidationSupportContext theValidationSupportContext,
      ConceptValidationOptions theOptions,
      String theCodeSystemUrlAndVersion,
      String theCode,
      String theDisplay,
      @Nonnull IBaseResource theValueSet) {

    CodeValidationResult codeValidationResult =
        super.validateCodeInValueSet(
            theValidationSupportContext,
            theOptions,
            theCodeSystemUrlAndVersion,
            theCode,
            theDisplay,
            theValueSet);

    Optional.ofNullable(codeValidationResult)
        .filter(
            result ->
                StringUtils.isNotBlank(result.getSeverityCode())
                    && result.getSeverityCode().equalsIgnoreCase(IssueSeverity.ERROR.getCode()))
        .ifPresent(
            result -> {
              log.debug("codeValidationResult:{}", result.getMessage());
              result.setSeverity(IssueSeverity.WARNING);
            });
    return codeValidationResult;
  }
}

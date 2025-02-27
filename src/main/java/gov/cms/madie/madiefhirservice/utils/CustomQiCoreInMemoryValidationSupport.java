package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import jakarta.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ValueSet;

import static org.apache.commons.lang3.StringUtils.*;

public class CustomQiCoreInMemoryValidationSupport extends InMemoryTerminologyServerValidationSupport {
  /**
   * Constructor
   *
   * @param theCtx A FhirContext for the FHIR version being validated
   */
  public CustomQiCoreInMemoryValidationSupport(FhirContext theCtx) {
    super(theCtx);
  }

  @Override
  public CodeValidationResult validateCodeInValueSet(
      ValidationSupportContext theValidationSupportContext,
      ConceptValidationOptions theOptions,
      String theCodeSystemUrlAndVersion,
      String theCode,
      String theDisplay,
      @Nonnull IBaseResource theValueSet) {
    ValueSet valueSet = (ValueSet) theValueSet;
    if (valueSet.getExpansion() != null
        && CollectionUtils.isNotEmpty(valueSet.getExpansion().getContains())) {
      String vsUrl =
          CommonCodeSystemsTerminologyService.getValueSetUrl(getFhirContext(), theValueSet);
      String codeSystemUrlToValidate = theCodeSystemUrlAndVersion;
      String codeSystemVersionToValidate = null;
      if (theCodeSystemUrlAndVersion != null) {
        int versionIndex = theCodeSystemUrlAndVersion.indexOf("|");
        if (versionIndex > -1) {
          codeSystemUrlToValidate = theCodeSystemUrlAndVersion.substring(0, versionIndex);
          codeSystemVersionToValidate = theCodeSystemUrlAndVersion.substring(versionIndex + 1);
        }
      }
      for (var contains : valueSet.getExpansion().getContains()) {
        if (StringUtils.equals(theCode, contains.getCode())) {
          IssueSeverity severity = null;
          if (theOptions.isInferSystem()
              || (StringUtils.equals(codeSystemUrlToValidate, contains.getSystem())
                  && (codeSystemVersionToValidate == null
                      || StringUtils.equals(codeSystemVersionToValidate, contains.getVersion())))) {
            if (theOptions.isValidateDisplay()) {
              if (!StringUtils.equals(theDisplay, contains.getDisplay())) {
                severity = getIssueSeverityForCodeDisplayMismatch();
              }
            }
          }
          return createCodeValidationResult(
            theCode,
            contains.getDisplay(),
            contains.getSystem(),
            contains.getVersion(),
            vsUrl,
            severity);
        }
      }
    }
    return null;
  }

  private CodeValidationResult createCodeValidationResult(
      String theCode,
      String theDisplay,
      String theCodeSystem,
      String theCodeSystemVersion,
      String vsUrl,
      IssueSeverity theSeverity) {
    CodeValidationResult codeValidationResult =
        new CodeValidationResult()
            .setCode(theCode)
            .setDisplay(theDisplay)
            .setCodeSystemName(theCodeSystem)
            .setCodeSystemVersion(theCodeSystemVersion)
            .setSeverity(theSeverity);
    if (isNotBlank(vsUrl)) {
      codeValidationResult.setSourceDetails(
          "Code was validated against in-memory expansion of ValueSet: " + vsUrl);
    }
    return codeValidationResult;
  }
}

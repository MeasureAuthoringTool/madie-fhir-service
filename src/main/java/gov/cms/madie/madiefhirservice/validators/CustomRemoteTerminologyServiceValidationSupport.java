package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class CustomRemoteTerminologyServiceValidationSupport
    extends RemoteTerminologyServiceValidationSupport {
  public CustomRemoteTerminologyServiceValidationSupport(
      FhirContext theFhirContext, String theBaseUrl) {
    super(theFhirContext, theBaseUrl);
  }

  @Override
  public IBaseResource fetchCodeSystem(String theSystem) {
    // disable code system validation for remote terminology service because it appears that VSAC
    // doesn't support code system search with summary mode
    // return fetchCodeSystem(theSystem, SummaryEnum.FALSE);
    return null;
  }

  @Override
  public String getErrorMessage(String errorCode, Object... theParams) {
    return getFhirContext()
        .getLocalizer()
        .getMessage(getClass().getSuperclass(), errorCode, theParams);
  }

  @Override
  public IBaseResource fetchValueSet(String theValueSetUrl) {
    // disable code value set fetch for remote terminology service because it appears that VSAC
    // doesn't support value set search with summary mode
    // return fetchValueSet(theValueSetUrl, SummaryEnum.FALSE);
    return null;
  }
}

package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.util.BundleUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;

import java.util.List;

@Slf4j
public class CustomRemoteTerminologyServiceValidationSupport
    extends RemoteTerminologyServiceValidationSupport {
  private final IGenericClient client;

  public CustomRemoteTerminologyServiceValidationSupport(
      FhirContext theFhirContext, String theBaseUrl, BasicAuthInterceptor basicAuthInterceptor) {
    super(theFhirContext, theBaseUrl);
    this.addClientInterceptor(basicAuthInterceptor);
    client = super.myCtx.newRestfulGenericClient(getBaseUrl());
    client.registerInterceptor(basicAuthInterceptor);
  }

  @Override
  public IBaseResource fetchCodeSystem(String theSystem) {
    // super class fetchCodeSystem doesn't support code system search with summary mode. therefore,
    // overriding it
    if (StringUtils.isBlank(theSystem)) {
      return null;
    } else {
      Class<? extends IBaseBundle> bundleType =
          this.myCtx.getResourceDefinition("Bundle").getImplementingClass(IBaseBundle.class);
      IQuery<IBaseBundle> codeSystemQuery =
          client
              .search()
              .forResource("CodeSystem")
              .where(CodeSystem.URL.matches().value(theSystem))
              .count(1);
      IBaseBundle bundles = codeSystemQuery.returnBundle(bundleType).execute();
      List<IBaseResource> codeSystems = BundleUtil.toListOfResources(this.myCtx, bundles);
      return CollectionUtils.isNotEmpty(codeSystems) ? codeSystems.get(0) : null;
    }
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
    return null;
  }
}

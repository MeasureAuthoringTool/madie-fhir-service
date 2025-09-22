package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.util.BundleUtil;
import gov.cms.madie.madiefhirservice.config.ValidationConfig;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.r4.model.ValueSet;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class CustomRemoteTerminologyServiceValidationSupport
    extends RemoteTerminologyServiceValidationSupport {
  private final IGenericClient client;
  private final ValidationConfig validationConfig;

  public CustomRemoteTerminologyServiceValidationSupport(
      FhirContext theFhirContext,
      String theBaseUrl,
      BasicAuthInterceptor basicAuthInterceptor,
      ValidationConfig theValidationConfig) {
    super(theFhirContext, theBaseUrl);
    this.addClientInterceptor(basicAuthInterceptor);
    client = super.myCtx.newRestfulGenericClient(getBaseUrl());
    client.registerInterceptor(basicAuthInterceptor);
    this.validationConfig = theValidationConfig;
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
  public CodeValidationResult validateCode(
      ValidationSupportContext theValidationSupportContext,
      ConceptValidationOptions theOptions,
      String theCodeSystem,
      String theCode,
      String theDisplay,
      String theValueSetUrl) {
    log.info(
        "validateCode called with: theCodeSystem={}, theCode={}, theDisplay={}, theValueSetUrl={}",
        theCodeSystem,
        theCode,
        theDisplay,
        theValueSetUrl);
    return invokeValidateCode(
        theCodeSystem,
        theCode,
        validationConfig.isValidateDisplay() ? theDisplay : null,
        theValueSetUrl,
        null);
  }

  @Override
  public CodeValidationResult validateCodeInValueSet(
      ValidationSupportContext theValidationSupportContext,
      ConceptValidationOptions theOptions,
      String theCodeSystem,
      String theCode,
      String theDisplay,
      @Nonnull IBaseResource theValueSet) {

    IBaseResource valueSet = theValueSet;

    // some external validators require the system when the code is passed
    // so let's try to get it from the VS if is not present
    String codeSystem = theCodeSystem;
    if (isNotBlank(theCode) && isBlank(codeSystem)) {
      codeSystem = ValidationSupportUtils.extractCodeSystemForCode(theValueSet, theCode);
    }

    String valueSetUrl = DefaultProfileValidationSupport.getConformanceResourceUrl(myCtx, valueSet);
    if (isNotBlank(valueSetUrl)) {
      valueSet = null;
    } else {
      valueSetUrl = null;
    }
    String display = validationConfig.isValidateDisplay() ? theDisplay : null;
    return invokeValidateCode(codeSystem, theCode, display, valueSetUrl, valueSet);
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
    // Added cases it calls for cts value sets
    if (StringUtils.isBlank(theValueSetUrl)
        || !StringUtils.contains(theValueSetUrl, "cts.nlm.nih.gov")) {
      return null;
    } else {
      Class<? extends IBaseBundle> bundleType =
          this.myCtx.getResourceDefinition("Bundle").getImplementingClass(IBaseBundle.class);
      IQuery<IBaseBundle> valueSetQuery =
          client
              .search()
              .forResource("ValueSet")
              .where(ValueSet.URL.matches().value(theValueSetUrl));
      IBaseBundle results = valueSetQuery.returnBundle(bundleType).execute();
      List<IBaseResource> resultsList = BundleUtil.toListOfResources(this.myCtx, results);
      return !resultsList.isEmpty() ? resultsList.get(0) : null;
    }
  }

  protected CodeValidationResult invokeSuperRemoteValidateCode(
      String theCodeSystem,
      String theCode,
      String theDisplay,
      String theValueSetUrl,
      @Valid IBaseResource theValueSet) {
    return super.invokeRemoteValidateCode(
        theCodeSystem, theCode, theDisplay, theValueSetUrl, theValueSet);
  }

  private CodeValidationResult invokeValidateCode(
      String theCodeSystem,
      String theCode,
      String theDisplay,
      String theValueSetUrl,
      @Valid IBaseResource theValueSet) {
    CodeValidationResult validationResult =
        invokeSuperRemoteValidateCode(
            theCodeSystem, theCode, theDisplay, theValueSetUrl, theValueSet);
    List<CodeValidationIssue> validationIssues = validationResult.getIssues();
    // this is to get away with HAPI's remote terminology service validation support not able to
    // validate non-required bindings correctly.
    // RemoteTerminologyServiceValidationSupport.invokeRemoteValidateCode
    // returns "invalid-code" when the code is not found in the value set or code system(that's
    // hardcoded).
    // However, binding strength check expects it to be "not-found"(check
    // HAPI's InstanceValidator.getTxIssueWithCalculatedSeverity).
    // This ugly fixup is to change the issue code from "invalid-code" to "not-found"
    // TODO: revisit this when HAPI fixes it
    if (validationResult.getSeverity() == IssueSeverity.ERROR
        && CollectionUtils.isNotEmpty(validationIssues)) {
      CodeValidationIssueDetails issueDetails = validationIssues.get(0).getDetails();
      CodeValidationIssueCoding issueCoding = issueDetails.getCodings().get(0);
      if (StringUtils.equals(issueCoding.getCode(), "invalid-code")) {
        issueDetails.addCoding(CodeValidationIssueCoding.TX_ISSUE_SYSTEM, "not-found");
        issueDetails.getCodings().remove(issueCoding);
      }
    }
    return validationResult;
  }
}

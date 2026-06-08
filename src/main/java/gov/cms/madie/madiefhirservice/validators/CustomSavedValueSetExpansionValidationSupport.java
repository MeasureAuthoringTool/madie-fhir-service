package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.util.BundleUtil;
import gov.cms.madie.madiefhirservice.config.ValidationConfig;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

import java.util.Collections;
import java.util.List;

/**
 * Validation support module that fetches previously-saved ValueSet expansions from an external
 * service. The service is expected to expose an endpoint that returns a serialized ValueSet
 * (preferably FHIR JSON) for a given ValueSet URL. service. The service is expected to expose an
 * endpoint that returns a serialized ValueSet (preferably FHIR JSON) for a given ValueSet URL.
 */
@Slf4j
public class CustomSavedValueSetExpansionValidationSupport
    extends RemoteTerminologyServiceValidationSupport {

  private final String serviceBaseUrl;
  private final String apiKey;
  //  private final IParser parser;
  private final IGenericClient client;
  private ValidationConfig validationConfig;

  public CustomSavedValueSetExpansionValidationSupport(
      FhirContext theFhirContext,
      String serviceBaseUrl,
      String apiKey,
      BasicAuthInterceptor basicAuthInterceptor,
      ValidationConfig validationConfig) {
    // Pass an empty base URL to parent to prevent it from attempting remote calls itself
    super(theFhirContext, serviceBaseUrl);
    this.serviceBaseUrl = serviceBaseUrl;
    this.addClientInterceptor(basicAuthInterceptor);
    client = super.myCtx.newRestfulGenericClient(getBaseUrl());
    client.registerInterceptor(basicAuthInterceptor);
    // Force JSON encoding to handle servers that return incorrect Content-Type headers
    client.setEncoding(EncodingEnum.JSON);
    this.apiKey = apiKey;
    this.validationConfig = validationConfig;
  }

  // Override the extended class to force our custom fetch call
  @Override
  public boolean isValueSetSupported(
      ValidationSupportContext theValidationSupportContext, String theValueSetUrl) {
    return fetchValueSet(theValueSetUrl) != null;
  }

  @Override
  public IBaseResource fetchValueSet(String theValueSetUrl) {
    Class<? extends IBaseBundle> bundleType =
        this.myCtx.getResourceDefinition("Bundle").getImplementingClass(IBaseBundle.class);
    IQuery<IBaseBundle> valueSetQuery =
        client.search().forResource("ValueSet").where(ValueSet.URL.matches().value(theValueSetUrl));
    IBaseBundle results = valueSetQuery.returnBundle(bundleType).execute();
    if (results == null) {
      return null;
    }
    List<IBaseResource> resultsList = BundleUtil.toListOfResources(this.myCtx, results);
    return !resultsList.isEmpty() ? resultsList.get(0) : null;
  }

  // Override the extended class to force our custom fetch call
  @Override
  public boolean isCodeSystemSupported(
      ValidationSupportContext theValidationSupportContext, String theSystem) {
    return fetchCodeSystem(theSystem) != null;
  }

  @Override
  public IBaseResource fetchCodeSystem(String theSystem) {
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
  public String getName() {
    return "Saved ValueSet Expansion Validation Support";
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
    theOptions.setValidateDisplay(validationConfig.isValidateDisplay());
    if (valueSet != null
        && valueSet.getExpansion() != null
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
      if (StringUtils.isBlank(theCode)) {
        log.info("Code validation failed for null/missing code");
        return createMissingCodeValidationResult(
            theDisplay, codeSystemUrlToValidate, codeSystemVersionToValidate, vsUrl);
      }

      for (var contains : valueSet.getExpansion().getContains()) {
        if (StringUtils.equals(theCode, contains.getCode())) {
          // always assume code system is valid and check for equality
          if (theOptions.isInferSystem()
              || (StringUtils.equals(codeSystemUrlToValidate, contains.getSystem())
                  && (codeSystemVersionToValidate == null
                      || StringUtils.equals(codeSystemVersionToValidate, contains.getVersion())))) {
            return validateMatchedCodeAndCodeSystem(
                theCodeSystemUrlAndVersion,
                theCode,
                theDisplay,
                vsUrl,
                codeSystemUrlToValidate,
                codeSystemVersionToValidate,
                theOptions,
                contains);
          } else {
            // code system mismatch - kick validation to next in chain
            return null;
          }
        }
      }
      // VS matched, but code not found in VS - return NOT_IN_VS error
      CodeValidationResult notInVsCodeValidationResult =
          createNotInVsValidationResult(
              theCode, theDisplay, codeSystemUrlToValidate, theCodeSystemUrlAndVersion, vsUrl);
      log.info(
          "Code validation failed: {}",
          notInVsCodeValidationResult.getIssues().get(0).getDiagnostics());
      return notInVsCodeValidationResult;
    }
    return null;
  }

  @Override
  public CodeValidationResult validateCode(
      @Nonnull ValidationSupportContext theValidationSupportContext,
      @Nonnull ConceptValidationOptions theOptions,
      String theCodeSystem,
      String theCode,
      String theDisplay,
      String theValueSetUrl) {
    return null;
  }

  private CodeValidationResult validateMatchedCodeAndCodeSystem(
      String theCodeSystemUrlAndVersion,
      String theCode,
      String theDisplay,
      String vsUrl,
      String codeSystemUrlToValidate,
      String codeSystemVersionToValidate,
      ConceptValidationOptions theOptions,
      ValueSet.ValueSetExpansionContainsComponent contains) {
    IssueSeverity severity = null;
    // check if display matches
    if (theOptions.isValidateDisplay()
        && StringUtils.isNotBlank(theDisplay)
        && !StringUtils.equals(theDisplay, contains.getDisplay())) {
      severity = IssueSeverity.ERROR;
    }
    CodeValidationResult validationResult =
        createCodeValidationResult(
            theCode,
            theDisplay,
            codeSystemUrlToValidate,
            codeSystemVersionToValidate,
            vsUrl,
            severity);
    if (severity != null) {
      log.info("Code validation failed: {}", validationResult.getMessage());
      String message =
          getFhirContext()
                  .getLocalizer()
                  .getMessage(
                      InMemoryTerminologyServerValidationSupport.class,
                      "displayMismatch",
                      theDisplay,
                      contains.getDisplay(),
                      theCodeSystemUrlAndVersion,
                      theCode)
              + " for MADiE in-memory expansion of ValueSet: "
              + vsUrl;
      validationResult.setIssues(
          Collections.singletonList(
              new CodeValidationIssue(
                  message,
                  severity,
                  CodeValidationIssueCode.INVALID,
                  CodeValidationIssueCoding.INVALID_DISPLAY)));
      if (StringUtils.isNotBlank(message)) {
        validationResult.setSourceDetails(message);
      }
    }
    return validationResult;
  }

  private CodeValidationResult createMissingCodeValidationResult(
      String theDisplay, String theCodeSystem, String theCodeSystemVersion, String vsUrl) {
    CodeValidationResult codeValidationResult =
        new CodeValidationResult()
            .setCode("[missing]")
            .setDisplay(theDisplay)
            .setCodeSystemName(theCodeSystem)
            .setCodeSystemVersion(theCodeSystemVersion)
            .setSeverity(IssueSeverity.ERROR);
    String message = "Invalid - missing code";
    if (StringUtils.isNotBlank(vsUrl)) {
      message = message + " for in-memory expansion of ValueSet '" + vsUrl + "'";
    }
    codeValidationResult.setIssues(
        Collections.singletonList(
            new CodeValidationIssue(
                message,
                IssueSeverity.ERROR,
                CodeValidationIssueCode.INVALID,
                CodeValidationIssueCoding.INVALID_CODE)));
    if (StringUtils.isNotBlank(message)) {
      codeValidationResult.setSourceDetails(message);
    }
    return codeValidationResult;
  }

  private CodeValidationResult createNotInVsValidationResult(
      String theCode,
      String theDisplay,
      String theCodeSystem,
      String theCodeSystemUrlAndVersion,
      String vsUrl) {
    IValidationSupport.CodeValidationIssueCode issueCode = CodeValidationIssueCode.CODE_INVALID;
    IValidationSupport.CodeValidationIssueCoding issueCoding =
        CodeValidationIssueCoding.INVALID_CODE;
    String formattedCodeSystemAndCode =
        getFormattedCodeSystemAndCodeForMessage(theCodeSystemUrlAndVersion, theCode);
    String message = "Unknown code '" + formattedCodeSystemAndCode + "'";
    IssueSeverity severity = IssueSeverity.ERROR;

    if (StringUtils.isNotBlank(vsUrl)) {
      message = message + " for in-memory expansion of ValueSet '" + vsUrl + "'";
      issueCoding = CodeValidationIssueCoding.NOT_IN_VS;
    }

    return (new IValidationSupport.CodeValidationResult())
        .setSeverity(severity)
        .setCode(theCode)
        .setDisplay(theDisplay)
        .setCodeSystemName(theCodeSystem)
        .setMessage(message)
        .addIssue(
            new IValidationSupport.CodeValidationIssue(message, severity, issueCode, issueCoding));
  }

  private String getFormattedCodeSystemAndCodeForMessage(
      String theCodeSystemUrlAndVersionToValidate, String theCodeToValidate) {
    String formattedCodeSystem =
        StringUtils.isNotBlank(theCodeSystemUrlAndVersionToValidate)
            ? theCodeSystemUrlAndVersionToValidate + "#"
            : "";
    return formattedCodeSystem + theCodeToValidate;
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
    if (StringUtils.isNotBlank(vsUrl)) {
      codeValidationResult.setSourceDetails(
          "Code was validated against in-memory expansion of ValueSet: " + vsUrl);
    }
    return codeValidationResult;
  }
}

package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.*;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

class LenientTerminologyValidator implements IValidationSupport {
  private final InMemoryTerminologyServerValidationSupport
      inMemoryTerminologyServerValidationSupport;

  public LenientTerminologyValidator(FhirContext theCtx) {
    inMemoryTerminologyServerValidationSupport =
        new InMemoryTerminologyServerValidationSupport(theCtx);
  }

  @Override
  public FhirContext getFhirContext() {
    return inMemoryTerminologyServerValidationSupport.getFhirContext();
  }

  @Nullable
  @Override
  public ValueSetExpansionOutcome expandValueSet(
      ValidationSupportContext theValidationSupportContext,
      @Nullable ValueSetExpansionOptions theExpansionOptions,
      @Nonnull IBaseResource theValueSetToExpand) {
    return inMemoryTerminologyServerValidationSupport.expandValueSet(
        theValidationSupportContext, theExpansionOptions, theValueSetToExpand);
  }

  @Nullable
  @Override
  public ValueSetExpansionOutcome expandValueSet(
      ValidationSupportContext theValidationSupportContext,
      @Nullable ValueSetExpansionOptions theExpansionOptions,
      @Nonnull String theValueSetUrlToExpand)
      throws ResourceNotFoundException {
    return inMemoryTerminologyServerValidationSupport.expandValueSet(
        theValidationSupportContext, theExpansionOptions, theValueSetUrlToExpand);
  }

  @Nullable
  @Override
  public List<IBaseResource> fetchAllConformanceResources() {
    return inMemoryTerminologyServerValidationSupport.fetchAllConformanceResources();
  }

  @Nullable
  @Override
  public <T extends IBaseResource> List<T> fetchAllSearchParameters() {
    return inMemoryTerminologyServerValidationSupport.fetchAllSearchParameters();
  }

  @Nullable
  @Override
  public <T extends IBaseResource> List<T> fetchAllStructureDefinitions() {
    return inMemoryTerminologyServerValidationSupport.fetchAllStructureDefinitions();
  }

  @Nullable
  @Override
  public <T extends IBaseResource> List<T> fetchAllNonBaseStructureDefinitions() {
    return inMemoryTerminologyServerValidationSupport.fetchAllNonBaseStructureDefinitions();
  }

  @Nullable
  @Override
  public IBaseResource fetchCodeSystem(String theSystem) {
    return inMemoryTerminologyServerValidationSupport.fetchCodeSystem(theSystem);
  }

  @Nullable
  @Override
  public <T extends IBaseResource> T fetchResource(@Nullable Class<T> theClass, String theUri) {
    return inMemoryTerminologyServerValidationSupport.fetchResource(theClass, theUri);
  }

  @Nullable
  @Override
  public IBaseResource fetchStructureDefinition(String theUrl) {
    return inMemoryTerminologyServerValidationSupport.fetchStructureDefinition(theUrl);
  }

  @Override
  public boolean isCodeSystemSupported(
      ValidationSupportContext theValidationSupportContext, String theSystem) {
    return inMemoryTerminologyServerValidationSupport.isCodeSystemSupported(
        theValidationSupportContext, theSystem);
  }

  @Override
  public boolean isRemoteTerminologyServiceConfigured() {
    return inMemoryTerminologyServerValidationSupport.isRemoteTerminologyServiceConfigured();
  }

  @Nullable
  @Override
  public IBaseResource fetchValueSet(String theValueSetUrl) {
    return inMemoryTerminologyServerValidationSupport.fetchValueSet(theValueSetUrl);
  }

  @Override
  public byte[] fetchBinary(String binaryKey) {
    return inMemoryTerminologyServerValidationSupport.fetchBinary(binaryKey);
  }

  @Nullable
  @Override
  public CodeValidationResult validateCode(
      ValidationSupportContext theValidationSupportContext,
      ConceptValidationOptions theOptions,
      String theCodeSystem,
      String theCode,
      String theDisplay,
      String theValueSetUrl) {
    return inMemoryTerminologyServerValidationSupport.validateCode(
        theValidationSupportContext,
        theOptions,
        theCodeSystem,
        theCode,
        theDisplay,
        theValueSetUrl);
  }

  @Nullable
  @Override
  public CodeValidationResult validateCodeInValueSet(
      ValidationSupportContext theValidationSupportContext,
      ConceptValidationOptions theOptions,
      String theCodeSystem,
      String theCode,
      String theDisplay,
      @Nonnull IBaseResource theValueSet) {
    return inMemoryTerminologyServerValidationSupport.validateCodeInValueSet(
        theValidationSupportContext, theOptions, theCodeSystem, theCode, theDisplay, theValueSet);
  }

  @Nullable
  @Override
  public LookupCodeResult lookupCode(
      ValidationSupportContext theValidationSupportContext,
      String theSystem,
      String theCode,
      String theDisplayLanguage) {
    return inMemoryTerminologyServerValidationSupport.lookupCode(
        theValidationSupportContext, theSystem, theCode, theDisplayLanguage);
  }

  @Nullable
  @Override
  public LookupCodeResult lookupCode(
      ValidationSupportContext theValidationSupportContext, String theSystem, String theCode) {
    return inMemoryTerminologyServerValidationSupport.lookupCode(
        theValidationSupportContext, theSystem, theCode);
  }

  @Nullable
  @Override
  public LookupCodeResult lookupCode(
      ValidationSupportContext theValidationSupportContext,
      @Nonnull LookupCodeRequest theLookupCodeRequest) {
    return inMemoryTerminologyServerValidationSupport.lookupCode(
        theValidationSupportContext, theLookupCodeRequest);
  }

  @Override
  public boolean isValueSetSupported(
      ValidationSupportContext theValidationSupportContext, String theValueSetUrl) {
    return inMemoryTerminologyServerValidationSupport.isValueSetSupported(
        theValidationSupportContext, theValueSetUrl);
  }

  @Nullable
  @Override
  public IBaseResource generateSnapshot(
      ValidationSupportContext theValidationSupportContext,
      IBaseResource theInput,
      String theUrl,
      String theWebUrl,
      String theProfileName) {
    return inMemoryTerminologyServerValidationSupport.generateSnapshot(
        theValidationSupportContext, theInput, theUrl, theWebUrl, theProfileName);
  }

  @Override
  public void invalidateCaches() {
    inMemoryTerminologyServerValidationSupport.invalidateCaches();
  }

  @Nullable
  @Override
  public TranslateConceptResults translateConcept(TranslateCodeRequest theRequest) {
    return inMemoryTerminologyServerValidationSupport.translateConcept(theRequest);
  }

  @Override
  public String getName() {
    return inMemoryTerminologyServerValidationSupport.getName();
  }
  //
  //  @Override
  //  public boolean isEnabledValidationForCodingsLogicalAnd() {
  //    return inMemoryTerminologyServerValidationSupport.isEnabledValidationForCodingsLogicalAnd();
  //  }
}

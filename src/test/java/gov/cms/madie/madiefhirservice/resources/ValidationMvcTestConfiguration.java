package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Profile("MvcTest")
public class ValidationMvcTestConfiguration {

  @Bean
  public FhirContext qicoreFhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  public FhirContext qicore6FhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  public FhirContext fhirContextForR5() {
    return FhirContext.forR5();
  }

  @Bean
  public ValidationSupportChain validationSupportChain411(@Autowired FhirContext qicoreFhirContext)
      throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(qicoreFhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-4.1.1.tgz");

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(qicoreFhirContext),
        new InMemoryTerminologyServerValidationSupport(qicoreFhirContext),
        new CommonCodeSystemsTerminologyService(qicoreFhirContext));
  }

  @Bean
  public FhirValidator qicoreNpmFhirValidator(
      @Autowired FhirContext qicoreFhirContext,
      @Autowired ValidationSupportChain validationSupportChain411) {
    // Ask the context for a validator
    FhirValidator validator = qicoreFhirContext.newValidator();

    // Create a validation module and register it
    IValidatorModule module = new FhirInstanceValidator(validationSupportChain411);
    validator.registerValidatorModule(module);
    return validator;
  }

  @Bean
  public ValidationSupportChain validationSupportChainQiCore600(
      @Autowired FhirContext qicore6FhirContext) throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(qicore6FhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-4.1.1.tgz");

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(qicore6FhirContext),
        new InMemoryTerminologyServerValidationSupport(qicore6FhirContext),
        new CommonCodeSystemsTerminologyService(qicore6FhirContext));
  }

  @Bean
  public FhirValidator qicore6NpmFhirValidator(
      @Autowired FhirContext qicore6FhirContext,
      @Autowired ValidationSupportChain validationSupportChainQiCore600) {
    // Ask the context for a validator
    FhirValidator validator = qicore6FhirContext.newValidator();

    // Create a validation module and register it
    IValidatorModule module = new FhirInstanceValidator(validationSupportChainQiCore600);
    validator.registerValidatorModule(module);
    return validator;
  }
}

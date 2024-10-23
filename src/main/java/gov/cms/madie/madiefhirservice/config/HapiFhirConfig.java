package gov.cms.madie.madiefhirservice.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.UnknownCodeSystemWarningValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class HapiFhirConfig {

  @Bean
  @Qualifier("qicoreFhirContext")
  public FhirContext qicoreFhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  @Qualifier("qicore6FhirContext")
  public FhirContext qicore6FhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  @Qualifier("fhirContextForR5")
  public FhirContext fhirContextForR5() {
    return FhirContext.forR5();
  }

  @Bean
  public IValidationSupport validationSupportChain411(@Autowired FhirContext qicoreFhirContext)
      throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(qicoreFhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-4.1.1.tgz");
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-3.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.0.13.tgz");

    UnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        new UnknownCodeSystemWarningValidationSupport(qicoreFhirContext);
    unknownCodeSystemWarningValidationSupport.setNonExistentCodeSystemSeverity(
        IValidationSupport.IssueSeverity.WARNING);

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(qicoreFhirContext),
        new InMemoryTerminologyServerValidationSupport(qicoreFhirContext),
        new CommonCodeSystemsTerminologyService(qicoreFhirContext),
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean
  public IValidationSupport validationSupportChainQiCore600(
      @Autowired FhirContext qicore6FhirContext) throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(qicore6FhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-6.0.0.tgz");
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-6.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.1.0.tgz");

    UnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        new UnknownCodeSystemWarningValidationSupport(qicore6FhirContext);
    unknownCodeSystemWarningValidationSupport.setNonExistentCodeSystemSeverity(
        IValidationSupport.IssueSeverity.WARNING);

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(qicore6FhirContext),
        new InMemoryTerminologyServerValidationSupport(qicore6FhirContext),
        new CommonCodeSystemsTerminologyService(qicore6FhirContext),
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean
  public FhirValidator qicoreNpmFhirValidator(
      @Autowired FhirContext qicoreFhirContext,
      @Autowired IValidationSupport validationSupportChain411) {
    log.info("validator config on FHIR Context v{}", qicoreFhirContext.getVersion());
    // Ask the context for a validator
    FhirValidator validator = qicoreFhirContext.newValidator();

    // Create a validation module and register it
    IValidatorModule module = new FhirInstanceValidator(validationSupportChain411);
    validator.registerValidatorModule(module);
    return validator;
  }

  @Bean
  public FhirValidator qicore6NpmFhirValidator(
      @Autowired FhirContext qicore6FhirContext,
      @Autowired IValidationSupport validationSupportChainQiCore600) {
    log.info("validator config on FHIR Context v{}", qicore6FhirContext.getVersion());
    // Ask the context for a validator
    FhirValidator validator = qicore6FhirContext.newValidator();

    // Create a validation module and register it
    IValidatorModule module = new FhirInstanceValidator(validationSupportChainQiCore600);
    validator.registerValidatorModule(module);
    return validator;
  }

  @Bean
  public LiquidEngine liquidEngine() throws IOException {
    return new LiquidEngine(new SimpleWorkerContext.SimpleWorkerContextBuilder().build(), null);
  }
}

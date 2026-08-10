package gov.cms.madie.madiefhirservice.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.madiefhirservice.validators.*;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.liquid.LiquidEngine;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class HapiFhirConfig {

  @Value("${vsac.api-key}")
  private String vsacApiKey;

  @Value("${vsac.terminology-server-url}")
  private String terminologyServerBase;

  @Value("${madie.terminology.service.base-url:}")
  private String savedExpansionServiceBaseUrl;

  @Value("${madie.terminology.service.api-key:}")
  private String savedExpansionServiceApiKey;

  @Autowired private ValidationConfig validationConfig;

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
  @Qualifier("uscore6FhirContext")
  public FhirContext uscore6FhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  @Qualifier("usqualitycore05FhirContext")
  public FhirContext usqualitycore05FhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  @Qualifier("fhirContextForR5")
  public FhirContext fhirContextForR5() {
    return FhirContext.forR5();
  }

  @Bean(name = {"validationSupportChain411", "qicoreValidationSupportChain"})
  public IValidationSupport validationSupportChain411(@Autowired FhirContext qicoreFhirContext)
      throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(qicoreFhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-4.1.1.tgz");
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-3.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.0.13.tgz");

    CustomUnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        getUnknownCodeSystemValidationSupport(qicoreFhirContext);

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(qicoreFhirContext),
        new InMemoryTerminologyServerValidationSupport(qicoreFhirContext),
        new CommonCodeSystemsTerminologyService(qicoreFhirContext),
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean(name = {"validationSupportChainQiCore600", "qicore6ValidationSupportChain"})
  public IValidationSupport validationSupportChainQiCore600(
      @Autowired FhirContext qicore6FhirContext) throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(qicore6FhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-6.0.0.tgz");
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-6.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.uv.extensions.r4-5.2.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.1.0.tgz");

    CustomUnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        getUnknownCodeSystemValidationSupport(qicore6FhirContext);

    RemoteTerminologyServiceValidationSupport remoteTerminologyServiceValidationSupport =
        getRemoteTerminologyServiceValidationSupport(qicore6FhirContext);

    VSESValidationSupport vsesValidationSupport =
        new VSESValidationSupport(
            qicore6FhirContext,
            savedExpansionServiceBaseUrl,
            new BasicAuthInterceptor("api-key", savedExpansionServiceApiKey),
            validationConfig);

    return new ValidationSupportChain(
        vsesValidationSupport,
        npmPackageSupport,
        new DefaultProfileValidationSupport(qicore6FhirContext),
        new CustomQiCoreInMemoryValidationSupport(qicore6FhirContext, validationConfig),
        new CommonCodeSystemsTerminologyService(qicore6FhirContext),
        remoteTerminologyServiceValidationSupport,
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean(name = {"uscore6ValidationSupportChain"})
  public IValidationSupport uscore6ValidationSupportChain(@Autowired FhirContext uscore6FhirContext)
      throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(uscore6FhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-6.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.uv.extensions.r4-5.2.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.1.0.tgz");

    CustomUnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        getUnknownCodeSystemValidationSupport(uscore6FhirContext);

    RemoteTerminologyServiceValidationSupport remoteTerminologyServiceValidationSupport =
        getRemoteTerminologyServiceValidationSupport(uscore6FhirContext);

    VSESValidationSupport vsesValidationSupport =
        new VSESValidationSupport(
            uscore6FhirContext,
            savedExpansionServiceBaseUrl,
            new BasicAuthInterceptor("api-key", savedExpansionServiceApiKey),
            validationConfig);

    return new ValidationSupportChain(
        vsesValidationSupport,
        npmPackageSupport,
        new DefaultProfileValidationSupport(uscore6FhirContext),
        new CustomQiCoreInMemoryValidationSupport(uscore6FhirContext, validationConfig),
        new CommonCodeSystemsTerminologyService(uscore6FhirContext),
        remoteTerminologyServiceValidationSupport,
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean(name = {"usqualitycore05ValidationSupportChain"})
  public IValidationSupport usqualitycore05ValidationSupportChain(
      @Autowired FhirContext usqualitycore05FhirContext) throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(usqualitycore05FhirContext);
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/fhir.onc.us-quality-core-0.5.0.tgz");
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-6.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.uv.extensions.r4-5.2.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.1.0.tgz");

    CustomUnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        getUnknownCodeSystemValidationSupport(usqualitycore05FhirContext);

    RemoteTerminologyServiceValidationSupport remoteTerminologyServiceValidationSupport =
        getRemoteTerminologyServiceValidationSupport(usqualitycore05FhirContext);

    VSESValidationSupport vsesValidationSupport =
        new VSESValidationSupport(
            usqualitycore05FhirContext,
            savedExpansionServiceBaseUrl,
            new BasicAuthInterceptor("api-key", savedExpansionServiceApiKey),
            validationConfig);

    return new ValidationSupportChain(
        vsesValidationSupport,
        npmPackageSupport,
        new DefaultProfileValidationSupport(usqualitycore05FhirContext),
        new CustomQiCoreInMemoryValidationSupport(usqualitycore05FhirContext, validationConfig),
        new CommonCodeSystemsTerminologyService(usqualitycore05FhirContext),
        remoteTerminologyServiceValidationSupport,
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean
  public Map<String, IValidationSupport> validationSupportChainMap(ApplicationContext context) {
    Map<String, IValidationSupport> finalMap = new HashMap<>();

    // 1. Get all beans of the target type (this gives you primary names)
    Map<String, IValidationSupport> primaryBeans = context.getBeansOfType(IValidationSupport.class);

    for (Map.Entry<String, IValidationSupport> entry : primaryBeans.entrySet()) {
      String primaryName = entry.getKey();
      IValidationSupport beanInstance = entry.getValue();

      // 2. Map the primary name to the instance
      finalMap.put(primaryName, beanInstance);

      // 3. Find and map all aliases pointing to this specific primary name
      String[] aliases = context.getAliases(primaryName);
      for (String alias : aliases) {
        finalMap.put(alias, beanInstance);
      }
    }

    return finalMap;
  }

  private RemoteTerminologyServiceValidationSupport getRemoteTerminologyServiceValidationSupport(
      FhirContext qicore6FhirContext) {
    return new CustomRemoteTerminologyServiceValidationSupport(
        qicore6FhirContext,
        terminologyServerBase,
        new BasicAuthInterceptor("apikey", vsacApiKey),
        validationConfig);
  }

  private CustomUnknownCodeSystemWarningValidationSupport getUnknownCodeSystemValidationSupport(
      FhirContext qicore6FhirContext) {
    var unknownCodeSystemSupportChain =
        new CustomUnknownCodeSystemWarningValidationSupport(qicore6FhirContext);
    unknownCodeSystemSupportChain.setNonExistentCodeSystemSeverity(
        IValidationSupport.IssueSeverity.WARNING);
    return unknownCodeSystemSupportChain;
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
  public FhirValidator uscore6NpmFhirValidator(
      @Autowired FhirContext uscore6FhirContext,
      @Autowired IValidationSupport uscore6ValidationSupportChain) {
    log.info("validator config on FHIR Context v{}", uscore6FhirContext.getVersion());
    FhirValidator validator = uscore6FhirContext.newValidator();
    IValidatorModule module = new FhirInstanceValidator(uscore6ValidationSupportChain);
    validator.registerValidatorModule(module);
    return validator;
  }

  @Bean
  public FhirValidator usqualitycore05NpmFhirValidator(
      @Autowired FhirContext usqualitycore05FhirContext,
      @Autowired IValidationSupport usqualitycore05ValidationSupportChain) {
    log.info("validator config on FHIR Context v{}", usqualitycore05FhirContext.getVersion());
    FhirValidator validator = usqualitycore05FhirContext.newValidator();
    IValidatorModule module = new FhirInstanceValidator(usqualitycore05ValidationSupportChain);
    validator.registerValidatorModule(module);
    return validator;
  }

  @Bean
  public LiquidEngine liquidEngine() throws IOException {
    // WorkerContext based on NPM package used per guidance provided in
    // https://github.com/cqframework/sample-content-ig/issues/121#issuecomment-2725717942
    try (InputStream is =
        ClasspathUtil.loadResourceAsStream("classpath:packages/hl7.fhir.r5.core.tgz")) {
      var ctx =
          new SimpleWorkerContext.SimpleWorkerContextBuilder()
              .withAllowLoadingDuplicates(true)
              .fromPackage(NpmPackage.fromPackage(is));
      LiquidEngine liquidEngine = new LiquidEngine(ctx, null);
      liquidEngine.setIncludeResolver(new IncludeResolver());

      return liquidEngine;
    }
  }

  static class IncludeResolver implements LiquidEngine.ILiquidEngineIncludeResolver {
    @Override
    public String fetchInclude(LiquidEngine liquidEngine, String s) {
      return ResourceUtils.getData("/templates/" + s);
    }
  }
}

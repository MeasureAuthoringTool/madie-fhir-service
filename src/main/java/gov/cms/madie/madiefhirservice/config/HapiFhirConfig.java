package gov.cms.madie.madiefhirservice.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.madiefhirservice.validators.CustomRemoteTerminologyServiceValidationSupport;
import gov.cms.madie.madiefhirservice.validators.CustomUnknownCodeSystemWarningValidationSupport;
import gov.cms.madie.madiefhirservice.validators.VSESValidationSupport;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.liquid.LiquidEngine;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    CustomUnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        getUnknownCodeSystemValidationSupport(qicoreFhirContext);

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
        "classpath:packages/hl7.fhir.uv.extensions.r4-5.2.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.1.0.tgz");
    PrePopulatedValidationSupport prePopulatedValidationSupport =
        buildPrePopulatedValidationSupportFromZip(
            qicore6FhirContext, "classpath:packages/tx-qicore-6.0.0.zip");

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
        prePopulatedValidationSupport,
        npmPackageSupport,
        vsesValidationSupport,
        new DefaultProfileValidationSupport(qicore6FhirContext),
        //        new CustomQiCoreInMemoryValidationSupport(qicore6FhirContext, validationConfig),
        new CommonCodeSystemsTerminologyService(qicore6FhirContext),
        remoteTerminologyServiceValidationSupport,
        unknownCodeSystemWarningValidationSupport);
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

  public PrePopulatedValidationSupport buildPrePopulatedValidationSupportFromZip(
      FhirContext qicore6FhirContext, String zipFileName) throws IOException {
    PrePopulatedValidationSupport prePopulatedValidationSupport =
        new PrePopulatedValidationSupport(qicore6FhirContext);
    IParser xmlParser = qicore6FhirContext.newXmlParser();

    try (InputStream is = ClasspathUtil.loadResourceAsStream(zipFileName);
        ZipInputStream zipInputStream = new ZipInputStream(is)) {

      if (is == null) {
        throw new IllegalArgumentException(
            "ZIP file not found in resources/packages: " + zipFileName);
      }

      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (!entry.isDirectory()
            && !entry.getName().startsWith("__MACOSX")
            && !entry.getName().endsWith(".DS_Store")) {

          StringBuilder fileContent = new StringBuilder();
          byte[] buffer = new byte[1024];
          int read;
          while ((read = zipInputStream.read(buffer)) != -1) {
            fileContent.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
          }

          String xmlContent = fileContent.toString();
          if (xmlContent.startsWith("\uFEFF")) {
            xmlContent = xmlContent.substring(1);
          }
          IBaseResource baseResource = xmlParser.parseResource(xmlContent);
          if (baseResource instanceof ValueSet) {
            prePopulatedValidationSupport.addValueSet(baseResource);
          }
        }
        zipInputStream.closeEntry();
      }
    }

    return prePopulatedValidationSupport;
  }

  static class IncludeResolver implements LiquidEngine.ILiquidEngineIncludeResolver {
    @Override
    public String fetchInclude(LiquidEngine liquidEngine, String s) {
      return ResourceUtils.getData("/templates/" + s);
    }
  }
}

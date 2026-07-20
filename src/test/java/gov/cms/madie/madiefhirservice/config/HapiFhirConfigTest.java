package gov.cms.madie.madiefhirservice.config;

import ca.uhn.fhir.context.support.IValidationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HapiFhirConfigTest {

  @Test
  void validationSupportChainMapShouldIncludePrimaryBeanNamesAndAliases() {
    HapiFhirConfig hapiFhirConfig = new HapiFhirConfig();
    ApplicationContext context = mock(ApplicationContext.class);

    IValidationSupport validationSupport411 = mock(IValidationSupport.class);
    IValidationSupport validationSupport600 = mock(IValidationSupport.class);

    Map<String, IValidationSupport> primaryBeans = new LinkedHashMap<>();
    primaryBeans.put("validationSupportChain411", validationSupport411);
    primaryBeans.put("validationSupportChainQiCore600", validationSupport600);

    when(context.getBeansOfType(IValidationSupport.class)).thenReturn(primaryBeans);
    when(context.getAliases("validationSupportChain411"))
        .thenReturn(new String[] {"qicoreValidationSupportChain"});
    when(context.getAliases("validationSupportChainQiCore600"))
        .thenReturn(new String[] {"qicore6ValidationSupportChain"});

    Map<String, IValidationSupport> result = hapiFhirConfig.validationSupportChainMap(context);

    assertThat(result)
        .containsEntry("validationSupportChain411", validationSupport411)
        .containsEntry("qicoreValidationSupportChain", validationSupport411)
        .containsEntry("validationSupportChainQiCore600", validationSupport600)
        .containsEntry("qicore6ValidationSupportChain", validationSupport600)
        .hasSize(4);
  }

  @Test
  void validationSupportChainMapShouldIncludePrimaryBeanWhenNoAliasesExist() {
    HapiFhirConfig hapiFhirConfig = new HapiFhirConfig();
    ApplicationContext context = mock(ApplicationContext.class);

    IValidationSupport validationSupport = mock(IValidationSupport.class);

    when(context.getBeansOfType(IValidationSupport.class))
        .thenReturn(Map.of("uscore6ValidationSupportChain", validationSupport));
    when(context.getAliases("uscore6ValidationSupportChain")).thenReturn(new String[0]);

    Map<String, IValidationSupport> result = hapiFhirConfig.validationSupportChainMap(context);

    assertThat(result).containsEntry("uscore6ValidationSupportChain", validationSupport).hasSize(1);
  }

  @Test
  void validationSupportChainMapShouldReturnEmptyMapWhenNoValidationSupportBeansExist() {
    HapiFhirConfig hapiFhirConfig = new HapiFhirConfig();
    ApplicationContext context = mock(ApplicationContext.class);

    when(context.getBeansOfType(IValidationSupport.class)).thenReturn(Map.of());

    Map<String, IValidationSupport> result = hapiFhirConfig.validationSupportChainMap(context);

    assertThat(result).isEmpty();
  }
}

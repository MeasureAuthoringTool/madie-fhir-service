package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.config.AppConfigServiceConfig;
import gov.cms.madie.madiefhirservice.dto.MadieFeatureFlag;
import gov.cms.madie.madiefhirservice.dto.ServiceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppConfigServiceTest {
  @Mock AppConfigServiceConfig appConfigServiceConfig;

  @Mock RestTemplate appConfigRestTemplate;

  @InjectMocks AppConfigService appConfigService;

  @Test
  public void isFlagEnabled() {
    final Map<String, Boolean> flagMap =
        Map.of(MadieFeatureFlag.QiCore_STU4_UPDATES.toString(), false);
    ReflectionTestUtils.setField(appConfigService, "featureFlags", flagMap);
    assertThat(appConfigService.isFlagEnabled(MadieFeatureFlag.QiCore_STU4_UPDATES), is(false));
  }

  @Test
  public void isFlagEnabledMissingFlag() {
    final Map<String, Boolean> flagMap = Map.of("NOT_REAL", true);
    ReflectionTestUtils.setField(appConfigService, "featureFlags", flagMap);
    assertThat(appConfigService.isFlagEnabled(MadieFeatureFlag.QiCore_STU4_UPDATES), is(false));
  }

  @Test
  public void testFetchServicConfig() {
    when(appConfigServiceConfig.getServiceConfigJsonUrl())
        .thenReturn("test.aws/serviceConfig.json");

    final Map<String, Boolean> flagMap =
        Map.of(MadieFeatureFlag.QiCore_STU4_UPDATES.toString(), true);
    when(appConfigRestTemplate.getForObject(anyString(), any(Class.class)))
        .thenReturn(ServiceConfig.builder().features(flagMap).build());
    appConfigService.refreshAppConfig();
    assertThat(appConfigService.isFlagEnabled(MadieFeatureFlag.QiCore_STU4_UPDATES), is(true));
  }
}

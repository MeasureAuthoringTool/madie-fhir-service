package gov.cms.madie.madiefhirservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppConfigServiceConfig {

  @Value("${madie.service-config.json-url}")
  private String serviceConfigJsonUrl;
}

package gov.cms.madie.madiefhirservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class ElmTranslatorClientConfig {
  @Value("${madie.cql-elm.service.base-url}")
  private String cqlElmServiceBaseUrl;

  @Value("${madie.cql-elm.service.effective-data-requirements-uri}")
  private String effectiveDataRequirementsDataUri;

  @Value("${madie.url}")
  private String madieUrl;

  @Bean
  public RestTemplate elmTranslatorRestTemplate() {
    return new RestTemplate();
  }
}

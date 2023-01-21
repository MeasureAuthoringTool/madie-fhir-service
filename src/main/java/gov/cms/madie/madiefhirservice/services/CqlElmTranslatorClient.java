package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.config.CqlElmTranslatorConfig;
import gov.cms.madie.madiefhirservice.exceptions.CqlElmTranslationServiceException;
import java.net.URI;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@AllArgsConstructor
public class CqlElmTranslatorClient {

  private CqlElmTranslatorConfig cqlElmTranslatorConfig;
  private RestTemplate elmTranslatorRestTemplate;

  public String getFormattedCql(String cql, String accessToken) {
    try {
      HttpEntity<String> cqlEntity = getHttpEntity(cql, accessToken, null, null);
      return elmTranslatorRestTemplate
          .exchange(getCqlFormatterURI(), HttpMethod.PUT, cqlEntity, String.class)
          .getBody();
    } catch (Exception ex) {
      log.error("An error occurred calling the CQL to ELM translation service", ex);
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service", ex);
    }
  }

  protected URI getCqlFormatterURI() {
    return URI.create(
        cqlElmTranslatorConfig.getCqlElmServiceBaseUrl()
            + cqlElmTranslatorConfig.getCqlFormatter());
  }

  protected HttpEntity<String> getHttpEntity(
      final String content, String accessToken, String apiKey, String harpId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    if (accessToken != null) {
      headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    } else if (apiKey != null && harpId != null) {
      headers.set("api-key", apiKey);
      headers.set("harp-id", harpId);
    }
    return new HttpEntity<>(content, headers);
  }
}

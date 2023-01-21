package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import gov.cms.madie.madiefhirservice.config.CqlElmTranslatorConfig;
import gov.cms.madie.madiefhirservice.exceptions.CqlElmTranslationServiceException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class CqlElmTranslatorClientTest {

  @Mock private CqlElmTranslatorConfig cqlElmTranslatorConfig;
  @Mock private RestTemplate restTemplate;

  @InjectMocks private CqlElmTranslatorClient cqlElmTranslatorClient;

  @BeforeEach
  void beforeEach() {
    lenient().when(cqlElmTranslatorConfig.getCqlElmServiceBaseUrl()).thenReturn("http://test");
    lenient().when(cqlElmTranslatorConfig.getCqlFormatter()).thenReturn("/cql/format");
  }

  @Test
  void testGetFormattedCqlHandlesClientErrorException() {
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        CqlElmTranslationServiceException.class,
        () -> cqlElmTranslatorClient.getFormattedCql("TEST_CQL", "TEST_TOKEN"));
  }

  @Test
  void testGetFormattedCql() {
    String jsonString = "{}";
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(jsonString));
    String output = cqlElmTranslatorClient.getFormattedCql(jsonString, "TEST_TOKEN");
    assertThat(output, is(equalTo(jsonString)));
  }
}

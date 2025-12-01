package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.exceptions.CqlLibraryNotFoundException;
import gov.cms.madie.models.library.CqlLibrary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@Slf4j
@RequiredArgsConstructor
public class CqlLibraryService {

  private final RestTemplate restTemplate;

  @Value("${madie.library.service.baseUrl}")
  private String madieLibraryService;

  @Value("${madie.library.service.versioned.uri}")
  private String librariesVersionedUri;

  @Cacheable(value = "libraries", key = "{ #root.methodName, #name, #version }")
  public CqlLibrary getLibrary(
      String name,
      String version,
      String accessToken,
      CqlCompilerException.ErrorSeverity errorSeverity) {
    URI uri = buildMadieLibraryServiceUri(name, version, errorSeverity);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    ResponseEntity<CqlLibrary> responseEntity =
        restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), CqlLibrary.class);

    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      if (responseEntity.hasBody()) {
        log.debug("Retrieved a valid cqlPayload");
        return responseEntity.getBody();
      } else {
        log.error("Cannot find Cql payload in the response");
        return null;
      }
    } else if (responseEntity.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
      throw new CqlLibraryNotFoundException(name, version);
    } else if (responseEntity.getStatusCode().equals(HttpStatus.CONFLICT)) {
      log.error(
          "Multiple libraries found with name: {}, version: {}, but only one was expected",
          name,
          version);
    }
    return null;
  }

  private URI buildMadieLibraryServiceUri(
      String name, String version, CqlCompilerException.ErrorSeverity errorSeverity) {
    return UriComponentsBuilder.fromHttpUrl(madieLibraryService + librariesVersionedUri)
        .queryParam("name", name)
        .queryParam("version", version)
        .queryParam("errorSeverity", errorSeverity)
        .build()
        .encode()
        .toUri();
  }
}

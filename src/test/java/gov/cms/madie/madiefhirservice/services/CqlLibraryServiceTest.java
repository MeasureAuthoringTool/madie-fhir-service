package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.exceptions.CqlLibraryNotFoundException;
import gov.cms.madie.models.dto.CqlLibraryDto;
import org.cqframework.cql.cql2elm.CqlCompilerException;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CqlLibraryServiceTest {

  @InjectMocks private CqlLibraryService cqlLibraryService;

  @Mock private RestTemplate restTemplate;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(
        cqlLibraryService, "madieLibraryService", "http://test.libraries-url");
    ReflectionTestUtils.setField(
        cqlLibraryService, "librariesVersionedUri", "/cql-libraries/versioned");
  }

  @Test
  void getLibraryReturnsLibrary() {
    CqlLibraryDto theLibrary =
        CqlLibraryDto.builder().cqlLibraryName("FHIRHelpers").version("4.0.001").build();
    ResponseEntity<CqlLibraryDto> response = ResponseEntity.ok(theLibrary);
    when(restTemplate.exchange(
            any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenReturn(response);
    CqlLibraryDto output =
        cqlLibraryService.getLibrary(
            "FHIRHelpers",
            "4.0.001",
            Optional.empty(),
            "OKTA_TOKEN",
            CqlCompilerException.ErrorSeverity.Info);
    assertThat(output, is(notNullValue()));
    assertThat(output, is(equalTo(theLibrary)));
  }

  @Test
  void getLibraryReturnsExceptionForLibraryNotFound() {
    ResponseEntity<Object> response = ResponseEntity.notFound().build();
    when(restTemplate.exchange(
            any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenReturn(response);
    Exception ex =
        assertThrows(
            CqlLibraryNotFoundException.class,
            () ->
                cqlLibraryService.getLibrary(
                    "FHIRHelpers",
                    "4.0.001",
                    Optional.empty(),
                    "OKTA_TOKEN",
                    CqlCompilerException.ErrorSeverity.Info));
    assertThat(
        ex.getMessage(),
        is(equalTo("Cannot find a CQL Library with name: FHIRHelpers, version: 4.0.001")));
  }

  @Test
  void getLibraryReturnsNullForConflict() {
    ResponseEntity<Object> response = ResponseEntity.status(HttpStatus.CONFLICT).build();
    when(restTemplate.exchange(
            any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenReturn(response);
    CqlLibraryDto output =
        cqlLibraryService.getLibrary(
            "FHIRHelpers",
            "4.0.001",
            Optional.empty(),
            "OKTA_TOKEN",
            CqlCompilerException.ErrorSeverity.Info);
    assertThat(output, is(nullValue()));
  }

  @Test
  void getLibraryReturnsNullForOkNoBody() {
    ResponseEntity<Object> response = ResponseEntity.ok().build();
    when(restTemplate.exchange(
            any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenReturn(response);
    CqlLibraryDto output =
        cqlLibraryService.getLibrary(
            "FHIRHelpers",
            "4.0.001",
            Optional.empty(),
            "OKTA_TOKEN",
            CqlCompilerException.ErrorSeverity.Info);
    assertThat(output, is(nullValue()));
  }
}

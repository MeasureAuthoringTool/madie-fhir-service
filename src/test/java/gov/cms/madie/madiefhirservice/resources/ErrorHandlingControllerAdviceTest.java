package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.exceptions.BundleOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorHandlingControllerAdviceTest {
  private ErrorHandlingControllerAdvice errorHandlingControllerAdvice;
  @Mock private HttpServletRequest mockRequest;

  @BeforeEach
  void setUp() {
    errorHandlingControllerAdvice = new ErrorHandlingControllerAdvice();
  }

  @Test
  void handleBundleOperationExceptionWithCauseShouldReturnProperResponse() {
    String rootCauseMessage = "Root cause error message";
    Exception rootCause = new Exception(rootCauseMessage);
    BundleOperationException exception =
        new BundleOperationException("Test Case", "123", rootCause);

    when(mockRequest.getRequestURI()).thenReturn("/test/uri");
    ResponseEntity<Map<String, Object>> response =
        errorHandlingControllerAdvice.handleBundleOperationException(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(response.getBody().get("error")).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
    assertThat(response.getBody().get("path")).isEqualTo("/test/uri");
    assertThat(response.getBody().get("message")).isEqualTo(rootCauseMessage);
    assertThat(response.getBody().get("timestamp")).isNotNull();
  }

  @Test
  void handleBundleOperationExceptionWithoutCauseShouldReturnResponseWithNullMessage() {
    BundleOperationException exception = new BundleOperationException("Test Case", "123", null);

    when(mockRequest.getRequestURI()).thenReturn("/test/uri");
    ResponseEntity<Map<String, Object>> response =
        errorHandlingControllerAdvice.handleBundleOperationException(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(response.getBody().get("error")).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
    assertThat(response.getBody().get("path")).isEqualTo("/test/uri");
    assertThat(response.getBody().get("message")).isNull();
    assertThat(response.getBody().get("timestamp")).isNotNull();
  }
}

package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.exceptions.BundleOperationException;
import gov.cms.madie.madiefhirservice.exceptions.UnsupportedTypeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@ControllerAdvice
public class ErrorHandlingControllerAdvice {

  @ExceptionHandler(UnsupportedTypeException.class)
  public ResponseEntity<Map<String, Object>> handleUnsupportedTypeException(
      UnsupportedTypeException ex, HttpServletRequest request) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
    errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
    errorResponse.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    errorResponse.put("path", request.getRequestURI());
    errorResponse.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(BundleOperationException.class)
  public ResponseEntity<Map<String, Object>> handleBundleOperationException(
      BundleOperationException ex, HttpServletRequest request) {
    Map<String, Object> errorResponse = new HashMap<>();

    errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
    errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
    errorResponse.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    errorResponse.put("path", request.getRequestURI());
    if (ex.getCause() != null) {
      errorResponse.put("message", ex.getCause().getMessage());
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }
}

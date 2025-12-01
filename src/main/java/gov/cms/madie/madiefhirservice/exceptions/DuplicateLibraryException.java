package gov.cms.madie.madiefhirservice.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DuplicateLibraryException extends RuntimeException {
  private static final String DUPLICATE_LIBRARY_MESSAGE =
      "Library resource with name: %s, version: %s already exists.";

  public DuplicateLibraryException(String name, String version) {
    super(String.format(DUPLICATE_LIBRARY_MESSAGE, name, version));
    log.error(getMessage());
  }
}

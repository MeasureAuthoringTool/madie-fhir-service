package gov.cms.madie.madiefhirservice.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
@Slf4j
public class CqlLibraryNotFoundException extends RuntimeException {

  private static final String NOT_FOUND_BY_FIND_DATA =
      "Cannot find a CQL Library with name: %s, version: %s";

  public CqlLibraryNotFoundException(String name, String version) {
    super(String.format(NOT_FOUND_BY_FIND_DATA, name, version));
    log.error(getMessage());
  }
}

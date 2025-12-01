package gov.cms.madie.madiefhirservice.exceptions;

import gov.cms.madie.models.library.CqlLibrary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
@Slf4j
public class MissingCqlException extends RuntimeException {
  private static final String NONE_FOUND = "Cannot find CQL for library name: %s, version: %s";

  public MissingCqlException(CqlLibrary library) {
    super(String.format(NONE_FOUND, library.getCqlLibraryName(), library.getVersion()));
    log.warn(getMessage());
  }
}

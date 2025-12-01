package gov.cms.madie.madiefhirservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
  private static final long serialVersionUID = -4751307368219100228L;
  private static final String MESSAGE = "Could not find %s resource for measure: %s";

  private static final String GENERIC_MESSAGE = "Could not find %s for %s with Id: %s";

  public ResourceNotFoundException(String type, String id) {
    super(String.format(MESSAGE, type, id));
  }

  public ResourceNotFoundException(String resource, String type, String id) {
    super(String.format(GENERIC_MESSAGE, resource, type, id));
  }
}

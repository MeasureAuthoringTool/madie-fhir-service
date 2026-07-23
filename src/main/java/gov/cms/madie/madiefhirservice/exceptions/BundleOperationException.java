package gov.cms.madie.madiefhirservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BundleOperationException extends RuntimeException {
  private static final String MESSAGE =
      "An error occurred while bundling %s with ID %s. "
          + "Please try again later or contact a System Administrator if this continues to occur.";

  public BundleOperationException(String message) {
    super(message);
  }

  public BundleOperationException(String type, String resourceId, Exception cause) {
    super(String.format(MESSAGE, type, resourceId), cause);
  }
}

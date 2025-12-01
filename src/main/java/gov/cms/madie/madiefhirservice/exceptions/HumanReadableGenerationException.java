package gov.cms.madie.madiefhirservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class HumanReadableGenerationException extends RuntimeException {
  private static final long serialVersionUID = 615844328607179490L;
  private static final String MESSAGE = "Could not find %s with id: %s";

  public HumanReadableGenerationException(String message) {
    super(message);
  }

  public HumanReadableGenerationException(String type, String id) {
    super(String.format(MESSAGE, type, id));
  }
}

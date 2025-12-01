package gov.cms.madie.madiefhirservice.exceptions;

public class HapiJsonException extends RuntimeException {

  public HapiJsonException(String message, Throwable cause) {
    super(message, cause);
  }
}

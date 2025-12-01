package gov.cms.madie.madiefhirservice.exceptions;

public class CqlElmTranslationServiceException extends RuntimeException {

  public CqlElmTranslationServiceException(String message, Exception cause) {
    super(message, cause);
  }
}

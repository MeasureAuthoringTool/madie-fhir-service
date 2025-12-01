package gov.cms.madie.madiefhirservice.constants;

public enum IdentifierType {
  CODE_VERSION_INDEPENDENT("version-independent", "Version Independent"),
  CODE_VERSION_SPECIFIC("version-specific", "Version Specific"),
  CODE_ENDORSER("endorser", "Endorser"),
  CODE_SHORT_NAME("short-name", "Short Name"),
  CODE_PUBLISHER("publisher", "Publisher");

  private final String code;
  private final String display;

  IdentifierType(String code, String display) {
    this.code = code;
    this.display = display;
  }

  public String getCode() {
    return code;
  }

  public String getDisplay() {
    return display;
  }
}

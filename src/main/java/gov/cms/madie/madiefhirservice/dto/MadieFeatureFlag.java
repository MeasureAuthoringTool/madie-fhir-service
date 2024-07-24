package gov.cms.madie.madiefhirservice.dto;

/** Feature flags relevant to the measure-service */
public enum MadieFeatureFlag {
  QiCore_STU4_UPDATES("qiCoreStu4Updates");

  private final String flag;

  MadieFeatureFlag(String flag) {
    this.flag = flag;
  }

  @Override
  public String toString() {
    return this.flag;
  }
}

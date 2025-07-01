package gov.cms.madie.madiefhirservice.dto;

/** Feature flags relevant to the madie-fhir-service */
public enum MadieFeatureFlag {
  QiCore_STU4_UPDATES("qiCoreStu4Updates"),
  STU6_TEST_CASE_VALIDATION("stu6TestCaseValidation"),
  ENHANCED_TEXT_FORMATTING("EnhancedTextFormatting");

  private final String flag;

  MadieFeatureFlag(String flag) {
    this.flag = flag;
  }

  @Override
  public String toString() {
    return this.flag;
  }
}

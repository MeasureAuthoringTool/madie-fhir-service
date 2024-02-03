package gov.cms.madie.madiefhirservice.constants;

public final class UriConstants {
  public static final String POPULATION_SYSTEM_URI =
      "http://terminology.hl7.org/CodeSystem/measure-population";
  public static final String SCORING_SYSTEM_URI =
      "http://terminology.hl7.org/CodeSystem/measure-scoring";
  public static final String LIBRARY_SYSTEM_TYPE_URI =
      "http://terminology.hl7.org/CodeSystem/library-type";

  public static final class CqfMeasures {
    public static final String EFFECTIVE_DATA_REQUIREMENT_URL =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-effectiveDataRequirements";
    public static final String SCORING_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoring";
    public static final String SCORING_UNIT_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoringUnit";
    public static final String POPULATION_BASIS =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-populationBasis";
    public static final String CQFM_TYPE =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-type";
    public static final String PROPORTION_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/proportion-measure-cqfm";
    public static final String COHORT_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cohort-measure-cqfm";
    public static final String CV_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cv-measure-cqfm";
    public static final String RATIO_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/ratio-measure-cqfm";
    public static final String CRITERIA_REFERENCE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference";
    public static final String AGGREGATE_METHOD_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-aggregateMethod";
    public static final String APPLIES_TO_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-appliesTo";

    public static final String COMPUTABLE_MEASURE_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-measure-cqfm";

    public static final String PUBLISHABLE_MEASURE_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/publishable-measure-cqfm";

    public static final String EXECUTABLE_MEASURE_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/executable-measure-cqfm";

    public static final String CODE_SYSTEM_IDENTIFIER_TYPE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/CodeSystem/identifier-type";

    public static final String SUPPLEMENTAL_DATA_GUIDANCE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-supplementalDataGuidance";

    public static final String CODE_SYSTEM_MEASURE_DATA_USAGE_URI =
        "http://terminology.hl7.org/CodeSystem/measure-data-usage";

    public static final String DIRECT_REFERENCE_CODE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-directReferenceCode";

    public static final String RATE_AGGREGATION_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-rateAggregation";

    public static final String IMPROVEMENT_NOTATION_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-improvementNotation";

    public static final String IMPROVEMENT_NOTATION_CODE_SYSTEM_URI =
        "http://terminology.hl7.org/CodeSystem/measure-improvement-notation";
  }

  public static final class CqfTestCases {
    public static final String CQFM_TEST_CASES =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/test-case-cqfm";
    public static final String IS_TEST_CASE =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-isTestCase";
    public static final String CQFM_INPUT_PARAMETERS =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-inputParameters";
    public static final String CQFM_TEST_CASE_DESCRIPTION =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-testCaseDescription";
  }

  public static final class UseContext {
    public static final String CODE_SYSTEM_URI =
        "http://terminology.hl7.org/CodeSystem/usage-context-type";
    public static final String VALUE_CODABLE_CONTEXT_CODING_SYSTEM_URI =
        "http://hl7.org/fhir/us/cqfmeasures/CodeSystem/quality-programs";
  }

  public static final class QiCore {
    public static final String PATIENT_PROFILE_URI =
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient";
  }

  public static final class MadieMeasure {
    public static final String CMS_ID = "https://madie.cms.gov/measure/cmsId";
    public static final String SHORT_NAME = "https://madie.cms.gov/measure/shortName";
    public static final String CBE_ID = "https://madie.cms.gov/measure/cbeId";
  }
}

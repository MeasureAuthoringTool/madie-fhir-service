package gov.cms.madie.madiefhirservice.services;

import static gov.cms.madie.madiefhirservice.constants.UriConstants.CodeSystem.CODE_SYSTEM_IDENTIFIER_TYPE_URI;
import static gov.cms.madie.madiefhirservice.constants.UriConstants.MadieMeasure.SHORT_NAME;
import static gov.cms.madie.madiefhirservice.constants.IdentifierType.CODE_ENDORSER;
import static gov.cms.madie.madiefhirservice.constants.IdentifierType.CODE_PUBLISHER;
import static gov.cms.madie.madiefhirservice.constants.IdentifierType.CODE_SHORT_NAME;
import static gov.cms.madie.madiefhirservice.constants.IdentifierType.CODE_VERSION_INDEPENDENT;
import static gov.cms.madie.madiefhirservice.constants.IdentifierType.CODE_VERSION_SPECIFIC;
import static gov.cms.madie.madiefhirservice.utils.BundleUtil.MEASURE_BUNDLE_TYPE_CALCULATION;
import static gov.cms.madie.madiefhirservice.utils.BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.measure.AssociationType;
import gov.cms.madie.models.measure.Endorsement;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.MeasureReportType;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.MeasureSet;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class MeasureTranslatorServiceTest implements ResourceFileUtil {
  @InjectMocks private MeasureTranslatorService measureTranslatorService;
  @Mock private FhirResourceHelpers fhirResourceHelpers;
  @Mock private AppConfigService appConfigService;

  private Measure madieMeasure;
  private Measure madieRatioMeasure;
  private Measure madieCVMeasure;

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
    String madieRatioMeasureJson = getStringFromTestResource("/measures/madie_ratio_measure.json");
    madieRatioMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieRatioMeasureJson);
    String cvMeasureJson = getStringFromTestResource("/measures/madie_cv_measure.json");
    madieCVMeasure = MeasureTestHelper.createMadieMeasureFromJson(cvMeasureJson);
    ReflectionTestUtils.setField(fhirResourceHelpers, "madieUrl", "madie.cms.gov");
  }

  @Test
  public void testCreateFhirMeasureForMadieMeasure() {
    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(
            madieMeasure, MEASURE_BUNDLE_TYPE_EXPORT);

    assertThat(measure.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertThat(measure.getUsage(), is(equalTo(madieMeasure.getMeasureMetaData().getGuidance())));
    assertThat(
        measure.getRationale(), is(equalTo(madieMeasure.getMeasureMetaData().getRationale())));
    assertThat(measure.getPurpose(), is(equalTo(madieMeasure.getMeasureMetaData().getPurpose())));
    assertThat(measure.getPublisher(), is(equalTo("UNKNOWN")));
    assertThat(
        measure.getUrl(), is(equalTo("madie.cms.gov/Measure/" + madieMeasure.getCqlLibraryName())));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
        is(equalTo("01/01/2023")));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    assertThat(
        DateFormatUtils.format(measure.getApprovalDate(), "MM/dd/yyyy"), is(equalTo("01/13/2023")));
    assertThat(
        DateFormatUtils.format(measure.getLastReviewDate(), "MM/dd/yyyy"),
        is(equalTo("02/13/2023")));
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(7)));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI), is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI), is(true));
    assertThat(measure.getMeta().hasProfile(UriConstants.CqfMeasures.RATIO_PROFILE_URI), is(true));
    assertThat(measure.getGroup().size(), is(equalTo(madieMeasure.getGroups().size())));
    assertThat(measure.getStatus(), is(equalTo(PublicationStatus.ACTIVE)));
    assertThat(
        measure.getDescription(), is(equalTo(madieMeasure.getMeasureMetaData().getDescription())));
    assertThat(measure.getUsage(), is(equalTo(madieMeasure.getMeasureMetaData().getGuidance())));
    assertThat(
        measure.getAuthor().size(),
        is(equalTo(madieMeasure.getMeasureMetaData().getDevelopers().size())));
    assertThat(
        measure.getAuthor().get(0).getName(),
        is(equalTo(madieMeasure.getMeasureMetaData().getDevelopers().get(0).getName())));
    assertThat(measure.getAuthor().get(0).getTelecomFirstRep(), is(notNullValue()));
    assertThat(
        measure.getAuthor().get(0).getTelecomFirstRep().getValue(),
        is(equalTo(madieMeasure.getMeasureMetaData().getDevelopers().get(0).getUrl())));
    assertThat(
        measure.getClinicalRecommendationStatement(),
        is(equalTo(madieMeasure.getMeasureMetaData().getClinicalRecommendation())));
    assertThat(measure.getDate(), is(equalTo(Date.from(madieMeasure.getLastModifiedAt()))));
    assertNotNull(measure.getUseContext());
    assertThat(
        measure.hasExtension(UriConstants.CqfMeasures.SUPPLEMENTAL_DATA_GUIDANCE_URI), is(true));
    List<Extension> guidanceExtensions =
        measure.getExtensionsByUrl(UriConstants.CqfMeasures.SUPPLEMENTAL_DATA_GUIDANCE_URI);
    assertThat(guidanceExtensions, is(notNullValue()));
    assertThat(guidanceExtensions.size(), is(equalTo(2)));
    // Supplemental Data Guidance
    Extension sdegExt = guidanceExtensions.get(0);
    assertThat(sdegExt, is(notNullValue()));
    assertThat(sdegExt.getExtension(), is(notNullValue()));
    assertThat(sdegExt.getExtension().size(), is(equalTo(2)));
    // Risk Adjustment Factors
    Extension ravgExt = guidanceExtensions.get(1);
    assertThat(ravgExt, is(notNullValue()));
    assertThat(ravgExt.getExtension(), is(notNullValue()));
    assertThat(ravgExt.getExtension().size(), is(equalTo(2)));

    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("Group_1")));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("Account")));
    assertEquals(
        madieMeasure.getGroups().get(0).getRateAggregation(),
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.RATE_AGGREGATION_URI)
            .getValue()
            .primitiveValue());
    assertThat(
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.SCORING_PRECISION_URI)
            .getValue()
            .primitiveValue(),
        is(equalTo("2")));
    Extension improvementNotationExt =
        group1.getExtensionByUrl(UriConstants.CqfMeasures.IMPROVEMENT_NOTATION_URI);
    Extension improvementNotationGudianceExt =
        group1.getExtensionByUrl(UriConstants.CqfMeasures.IMPROVEMENT_NOTATION_GUIDANCE_URI);
    CodeableConcept improvementNotation =
        improvementNotationExt.getValue().castToCodeableConcept(improvementNotationExt.getValue());
    assertEquals(
        madieMeasure.getGroups().get(0).getImprovementNotation(),
        improvementNotation.getCoding().get(0).getDisplay());
    assertEquals("increase", improvementNotation.getCoding().get(0).getCode());
    assertEquals(
        UriConstants.CodeSystem.IMPROVEMENT_NOTATION_CODE_SYSTEM_URI,
        improvementNotation.getCoding().get(0).getSystem());
    assertEquals(
        improvementNotationGudianceExt.getUrl(),
        UriConstants.CqfMeasures.IMPROVEMENT_NOTATION_GUIDANCE_URI);
    assertThat(
        improvementNotationGudianceExt.getValue().toString(),
        is(equalTo("Improvement notation description")));

    Extension scoringUnitExt1 = group1.getExtensionByUrl(UriConstants.CqfMeasures.SCORING_UNIT_URI);
    assertThat(scoringUnitExt1, is(notNullValue()));
    assertThat(scoringUnitExt1.getValue(), is(notNullValue()));
    CodeableConcept scoringUnit1 =
        scoringUnitExt1.getValue().castToCodeableConcept(scoringUnitExt1.getValue());
    assertThat(scoringUnit1.getCoding(), is(notNullValue()));
    assertThat(scoringUnit1.getCoding().get(0), is(notNullValue()));
    assertThat(scoringUnit1.getCoding().get(0).getDisplay(), is(equalTo("kg kilogram")));
    assertThat(scoringUnit1.getCoding().get(0).getCode(), is(equalTo("kg")));
    assertThat(scoringUnit1.getCoding().get(0).getSystem(), is(equalTo("http://thesystem")));
    Extension group1Ex = group1.getExtension().get(0);
    assertThat(group1Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group1CodeableConcept = group1Ex.castToCodeableConcept(group1Ex.getValue());
    assertThat(group1CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group1CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group1CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.CodeSystem.SCORING_SYSTEM_URI)));
    assertThat(group1CodeableConcept.getCoding().get(0).getCode(), is(equalTo("ratio")));

    List<Extension> groupTypes =
        group1.getExtension().stream()
            .filter(
                extension ->
                    extension.hasUrlElement()
                        && extension.getUrl().equals(UriConstants.CqfMeasures.CQFM_TYPE))
            .toList();
    assertThat(groupTypes.size(), is(2));
    for (Extension ext : groupTypes) {
      CodeableConcept type = ext.castToCodeableConcept(ext.getValue());
      assertThat(type.getCoding().get(0).getCode(), is(notNullValue()));
      assertThat(
          type.getCoding().get(0).getSystem(),
          is("http://terminology.hl7.org/CodeSystem/measure-type"));
    }

    // numeratorExclusion is not assigned (null), so it is not added to the measure resource.
    assertNotEquals(
        measure.getGroup().get(0).getPopulation().size(),
        madieMeasure.getGroups().get(0).getPopulations().size());
    MeasureGroupPopulationComponent groupComponent =
        measure.getGroup().get(0).getPopulation().get(0);
    assertThat(groupComponent.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupComponent.getCriteria().getExpression(), is(equalTo("SDE Ethnicity")));
    assertThat(
        groupComponent.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupComponent.getCode().getCoding().get(0).getCode(), is(equalTo("initial-population")));
    assertThat(groupComponent.getId(), is(notNullValue()));

    assertThat(measure.getGroup().get(1), is(notNullValue()));
    MeasureGroupComponent group2 = measure.getGroup().get(1);
    assertThat(group2.getId(), is(equalTo("Group_2")));
    assertThat(
        group2.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group2.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group2
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("boolean")));
    assertNull(group2.getExtensionByUrl(UriConstants.CqfMeasures.RATE_AGGREGATION_URI));
    Extension improvementNotationExt1 =
        group2.getExtensionByUrl(UriConstants.CqfMeasures.IMPROVEMENT_NOTATION_URI);
    CodeableConcept improvementNotation1 =
        improvementNotationExt1
            .getValue()
            .castToCodeableConcept(improvementNotationExt1.getValue());
    assertEquals(
        madieMeasure.getGroups().get(1).getImprovementNotation(),
        improvementNotation1.getCoding().get(0).getDisplay());
    assertEquals("decrease", improvementNotation1.getCoding().get(0).getCode());
    assertEquals(
        UriConstants.CodeSystem.IMPROVEMENT_NOTATION_CODE_SYSTEM_URI,
        improvementNotation1.getCoding().get(0).getSystem());
    Extension group2Ex = group2.getExtension().get(0);
    assertThat(group2Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group2CodeableConcept = group2Ex.castToCodeableConcept(group2Ex.getValue());
    assertThat(group2CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group2CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group2CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group2CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.CodeSystem.SCORING_SYSTEM_URI)));
    assertThat(group2CodeableConcept.getCoding().get(0).getCode(), is(equalTo("ratio")));

    // verifies if SupplemenntalData is populated, it includes both SDE and RiskAdjustmentFators
    assertEquals(5, measure.getSupplementalData().size());
    assertEquals("sde-race", measure.getSupplementalData().get(0).getId());
    assertEquals("SDE Race", measure.getSupplementalData().get(0).getCriteria().getExpression());
    assertEquals("SDE Race", measure.getSupplementalData().get(0).getDescription());
    assertFalse(measure.getSupplementalData().get(0).getUsage().get(0).getCoding().isEmpty());
    // SDE Race has no Include in Report Type
    assertThat(measure.getSupplementalData().get(0).getExtension().isEmpty(), is(true));
    // SDE Payor has one Include In Report Type
    assertThat(measure.getSupplementalData().get(1).getExtension().size(), is(equalTo(1)));
    assertThat(
        measure.getSupplementalData().get(1).getExtension().get(0).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(1)
            .getExtension()
            .get(0)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.INDIVIDUAL.toCode())));
    // RAV example has one Include In Report Type
    assertThat(measure.getSupplementalData().get(2).getExtension().isEmpty(), is(true));
    // RAV example2 has one Include In Report Type
    assertThat(measure.getSupplementalData().get(3).getExtension().size(), is(equalTo(3)));
    assertThat(
        measure.getSupplementalData().get(3).getExtension().get(0).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(3)
            .getExtension()
            .get(0)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.INDIVIDUAL.toCode())));
    assertThat(
        measure.getSupplementalData().get(3).getExtension().get(1).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(3)
            .getExtension()
            .get(1)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.SUMMARY.toCode())));
    assertThat(
        measure.getSupplementalData().get(3).getExtension().get(2).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(3)
            .getExtension()
            .get(2)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.DATA_COLLECTION.toCode())));
    // RAV example3 has four Include In Report Type
    assertThat(measure.getSupplementalData().get(4).getExtension().size(), is(equalTo(4)));
    assertThat(
        measure.getSupplementalData().get(4).getExtension().get(0).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(4)
            .getExtension()
            .get(0)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.INDIVIDUAL.toCode())));
    assertThat(
        measure.getSupplementalData().get(4).getExtension().get(1).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(4)
            .getExtension()
            .get(1)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.SUBJECT_LIST.toCode())));
    assertThat(
        measure.getSupplementalData().get(4).getExtension().get(2).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(4)
            .getExtension()
            .get(2)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.SUMMARY.toCode())));
    assertThat(
        measure.getSupplementalData().get(4).getExtension().get(3).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI)));
    assertThat(
        measure
            .getSupplementalData()
            .get(4)
            .getExtension()
            .get(3)
            .getValueAsPrimitive()
            .getValueAsString(),
        is(equalTo(MeasureReportType.DATA_COLLECTION.toCode())));

    assertEquals("risk-adjustments-example", measure.getSupplementalData().get(2).getId());
    assertEquals(
        "Risk Adjustments example",
        measure.getSupplementalData().get(2).getCriteria().getExpression());
    assertEquals("Risk Adjustments example", measure.getSupplementalData().get(2).getDescription());
    assertThat(measure.getUseContext().size(), is(equalTo(1)));
    assertThat(measure.getUseContext().get(0).getCode().getDisplay(), is(equalTo("Venue")));
    assertThat(measure.getUseContext().get(0).hasValue(), is(equalTo(true)));
    assertFalse(measure.getSupplementalData().get(2).getUsage().get(0).getCoding().isEmpty());
    assertEquals("0.0.000", measure.getVersion());

    assertEquals(measure.getDefinition().size(), 2);
    assertEquals(measure.getDefinition().get(0).toString(), "test term1 - test definition1" + "\n");
    assertEquals(measure.getDefinition().get(1).toString(), "test term2 - test definition2" + "\n");
  }

  @Test
  public void testCreateFhirMeasureForMadieRatioMeasure() {
    madieRatioMeasure
        .getMeasureMetaData()
        .setSteward(Organization.builder().name("testSteward").url("test-steward-url.com").build());
    madieRatioMeasure.getMeasureMetaData().setCopyright("testCopyright");
    madieRatioMeasure.getMeasureMetaData().setDisclaimer("testDisclaimer");
    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(
            madieRatioMeasure, MEASURE_BUNDLE_TYPE_EXPORT);

    assertThat(measure.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertFalse(measure.getExperimental());
    assertThat(measure.getUsage(), is(equalTo(madieMeasure.getMeasureMetaData().getGuidance())));
    assertThat(
        measure.getRationale(), is(equalTo(madieMeasure.getMeasureMetaData().getRationale())));
    assertThat(measure.getPurpose(), is(equalTo(null)));
    assertThat(measure.getPublisher(), is(equalTo("testSteward")));
    assertThat(measure.getContact(), is(notNullValue()));
    assertThat(measure.getContactFirstRep(), is(notNullValue()));
    assertThat(measure.getContactFirstRep().getTelecomFirstRep(), is(notNullValue()));
    assertThat(
        measure.getContactFirstRep().getTelecomFirstRep().getValue(),
        is(equalTo("test-steward-url.com")));
    assertThat(measure.getCopyright(), is(equalTo("testCopyright")));
    assertThat(measure.getDisclaimer(), is(equalTo("testDisclaimer")));
    assertThat(
        measure.getUrl(),
        is(equalTo("madie.cms.gov/Measure/" + madieRatioMeasure.getCqlLibraryName())));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
        is(equalTo("01/01/2023")));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    assertThat(
        DateFormatUtils.format(measure.getApprovalDate(), "MM/dd/yyyy"), is(equalTo("01/13/2023")));
    assertThat(
        DateFormatUtils.format(measure.getLastReviewDate(), "MM/dd/yyyy"),
        is(equalTo("02/13/2023")));
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(7)));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI), is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI), is(true));
    assertThat(measure.getMeta().hasProfile(UriConstants.CqfMeasures.RATIO_PROFILE_URI), is(true));
    assertThat(measure.getStatus(), is(equalTo(PublicationStatus.ACTIVE)));
    assertThat(
        measure.getDescription(),
        is(equalTo(madieRatioMeasure.getMeasureMetaData().getDescription())));
    assertThat(
        measure.getUsage(), is(equalTo(madieRatioMeasure.getMeasureMetaData().getGuidance())));
    assertThat(
        measure.getAuthor().size(),
        is(equalTo(madieRatioMeasure.getMeasureMetaData().getDevelopers().size())));
    assertThat(
        measure.getClinicalRecommendationStatement(),
        is(equalTo(madieRatioMeasure.getMeasureMetaData().getClinicalRecommendation())));
    assertThat(measure.getDate(), is(equalTo(Date.from(madieRatioMeasure.getLastModifiedAt()))));
    assertThat(measure.getUseContext(), is(Collections.emptyList()));
    assertThat(measure.getGroup().size(), is(equalTo(madieRatioMeasure.getGroups().size())));
    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("626be4370ca8110d3b22404b")));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("Account")));
    Extension group1Ex = group1.getExtension().get(0);
    assertThat(group1Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group1CodeableConcept = group1Ex.castToCodeableConcept(group1Ex.getValue());
    assertThat(group1CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group1CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group1CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.CodeSystem.SCORING_SYSTEM_URI)));
    assertThat(group1CodeableConcept.getCoding().get(0).getCode(), is(equalTo("ratio")));

    MeasureGroupPopulationComponent groupPopComponent = group1.getPopulation().get(0);
    assertThat(groupPopComponent.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponent.getCriteria().getExpression(), is(equalTo("ipp")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getCode(),
        is(equalTo("initial-population")));
    assertThat(groupPopComponent.getId(), is(notNullValue()));
    assertEquals("InitialPopulation_1_1", groupPopComponent.getId());

    MeasureGroupPopulationComponent groupPopComponent2 = group1.getPopulation().get(1);
    assertThat(groupPopComponent2.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponent2.getCriteria().getExpression(), is(equalTo("ipp2")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getCode(),
        is(equalTo("initial-population")));
    assertThat(groupPopComponent2.getId(), is(notNullValue()));
    assertEquals("InitialPopulation_1_2", groupPopComponent2.getId());

    MeasureGroupPopulationComponent groupPopComponent3 = group1.getPopulation().get(2);
    assertThat(groupPopComponent3.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponent3.getCriteria().getExpression(), is(equalTo("denom")));
    assertThat(
        groupPopComponent3.getCode().getCoding().get(0).getDisplay(), is(equalTo("Denominator")));
    assertThat(
        groupPopComponent3.getCode().getCoding().get(0).getCode(), is(equalTo("denominator")));
    assertThat(groupPopComponent3.getId(), is(notNullValue()));
    assertEquals("Denominator_1", groupPopComponent3.getId());

    MeasureGroupPopulationComponent groupPopComponent4 = group1.getPopulation().get(3);
    assertThat(groupPopComponent4.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponent4.getCriteria().getExpression(), is(equalTo("num")));
    assertThat(
        groupPopComponent4.getCode().getCoding().get(0).getDisplay(), is(equalTo("Numerator")));
    assertThat(groupPopComponent4.getCode().getCoding().get(0).getCode(), is(equalTo("numerator")));
    assertThat(groupPopComponent4.getId(), is(notNullValue()));
    assertEquals("Numerator_1", groupPopComponent4.getId());

    MeasureGroupPopulationComponent groupPopComponentObs = group1.getPopulation().get(4);
    assertThat(
        groupPopComponentObs.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponentObs.getCriteria().getExpression(), is(equalTo("fun")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Measure Observation")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getCode(),
        is(equalTo("measure-observation")));
    assertThat(groupPopComponentObs.getId(), is(notNullValue()));
    assertEquals("MeasureObservation_1", groupPopComponentObs.getId());

    List<MeasureGroupStratifierComponent> strats = group1.getStratifier();
    assertEquals(2, strats.size());
    assertEquals("Stratification_1_1", strats.get(0).getId());
    assertEquals("Stratification_1_2", strats.get(1).getId());
  }

  @Test
  public void testCreateFhirMeasureForMadieCVMeasure() {
    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(
            madieCVMeasure, MEASURE_BUNDLE_TYPE_CALCULATION);

    assertThat(measure.getName(), is(equalTo(madieCVMeasure.getCqlLibraryName())));
    assertThat(
        measure.getPublisher(),
        is(equalTo(madieCVMeasure.getMeasureMetaData().getSteward().getName())));
    assertThat(
        measure.getRationale(), is(equalTo(madieCVMeasure.getMeasureMetaData().getRationale())));
    assertThat(measure.getPurpose(), is(equalTo(null)));
    assertThat(
        measure.getUrl(),
        is(equalTo("madie.cms.gov/Measure/" + madieCVMeasure.getCqlLibraryName())));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
        is(equalTo("01/01/2022")));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("01/01/2023")));
    assertNull(measure.getApprovalDate());
    assertNull(measure.getLastReviewDate());
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(7)));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI), is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI), is(true));
    assertThat(measure.getMeta().hasProfile(UriConstants.CqfMeasures.CV_PROFILE_URI), is(true));
    assertThat(measure.getUseContext(), is(Collections.emptyList()));
    assertThat(measure.getGroup().size(), is(equalTo(madieCVMeasure.getGroups().size())));

    assertThat(measure.getStatus(), is(equalTo(PublicationStatus.ACTIVE)));
    assertThat(measure.getDate(), is(equalTo(Date.from(madieCVMeasure.getLastModifiedAt()))));
    assertThat(
        measure.getDescription(),
        is(equalTo(madieCVMeasure.getMeasureMetaData().getDescription())));
    assertThat(measure.getUsage(), is(equalTo(madieCVMeasure.getMeasureMetaData().getGuidance())));
    assertThat(
        measure.getAuthor().size(),
        is(equalTo(madieCVMeasure.getMeasureMetaData().getDevelopers().size())));
    assertThat(
        measure.getClinicalRecommendationStatement(),
        is(equalTo(madieCVMeasure.getMeasureMetaData().getClinicalRecommendation())));

    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("63346f711633644d64ac2d83")));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("Encounter")));
    Extension group1Ex = group1.getExtension().get(0);
    assertThat(group1Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group1CodeableConcept = group1Ex.castToCodeableConcept(group1Ex.getValue());
    assertThat(group1CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group1CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group1CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.CodeSystem.SCORING_SYSTEM_URI)));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getCode(), is(equalTo("continuous-variable")));

    MeasureGroupPopulationComponent groupPopComponent = group1.getPopulation().get(0);
    assertThat(groupPopComponent.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponent.getCriteria().getExpression(), is(equalTo("ipp")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getCode(),
        is(equalTo("initial-population")));
    assertThat(groupPopComponent.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponent2 = group1.getPopulation().get(1);
    assertThat(groupPopComponent2.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponent2.getCriteria().getExpression(), is(equalTo("mpop")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Measure Population")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getCode(),
        is(equalTo("measure-population")));
    assertThat(groupPopComponent2.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponentObs =
        group1.getPopulation().get(group1.getPopulation().size() - 1);
    assertThat(groupPopComponentObs.getId(), is(notNullValue()));
    assertThat(
        groupPopComponentObs.getCriteria().getLanguage(), is(equalTo("text/cql-identifier")));
    assertThat(groupPopComponentObs.getCriteria().getExpression(), is(equalTo("fun")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Measure Observation")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getCode(),
        is(equalTo("measure-observation")));
    assertThat(groupPopComponentObs.getExtension().size(), is(equalTo(2)));
    assertThat(
        groupPopComponentObs.getExtensionByUrl(UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI),
        is(notNullValue()));
    assertThat(
        groupPopComponentObs
            .getExtensionByUrl(UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI)
            .getValue()
            .primitiveValue(),
        is(equalTo("53808b19-54c7-45f7-95c4-dd4ee58f4730")));
    assertThat(
        groupPopComponentObs.getExtensionByUrl(UriConstants.CqfMeasures.AGGREGATE_METHOD_URI),
        is(notNullValue()));
    assertThat(
        groupPopComponentObs
            .getExtensionByUrl(UriConstants.CqfMeasures.AGGREGATE_METHOD_URI)
            .getValue()
            .primitiveValue(),
        is(equalTo("Minimum")));
  }

  @Test
  public void testBuildFhirPopulationGroupsWithAssocations() {
    Population ip1 = new Population();
    ip1.setName(PopulationType.INITIAL_POPULATION);
    ip1.setAssociationType(AssociationType.DENOMINATOR);
    ip1.setId("initial-population-1");
    ip1.setDescription("initial-population-description-1");
    Population ip2 = new Population();
    ip2.setName(PopulationType.INITIAL_POPULATION);
    ip2.setAssociationType(AssociationType.NUMERATOR);
    ip2.setId("initial-population-2");
    ip2.setDescription("initial-population-description-2");
    Population denom = new Population();
    denom.setName(PopulationType.DENOMINATOR);
    denom.setDescription("denom-description");
    Population numer = new Population();
    numer.setName(PopulationType.NUMERATOR);
    numer.setDescription("numer-description");
    Group group = new Group();
    group.setGroupDescription("group-description");
    group.setScoring(MeasureScoring.RATIO.toString());
    List<Population> pops = new ArrayList<>();
    pops.add(numer);
    pops.add(denom);
    pops.add(ip1);
    pops.add(ip2);
    group.setPopulations(pops);
    List<Group> groups = new ArrayList<>();
    groups.add(group);

    List<MeasureGroupComponent> groupComponent =
        measureTranslatorService.buildGroups(groups, MEASURE_BUNDLE_TYPE_CALCULATION);
    assertNotNull(groupComponent);

    groupComponent.forEach(
        (mgc) -> {
          assertThat(mgc.getDescription(), is(equalTo("group-description")));
          List<MeasureGroupPopulationComponent> gpcs = mgc.getPopulation();
          gpcs.forEach(
              (gpc) -> {
                CodeableConcept code = gpc.getCode();
                code.getCoding()
                    .forEach(
                        coding -> {
                          switch (coding.getCode()) {
                            case "denominator":
                              // this needs to be associated with denominator IP
                              Extension ext2 =
                                  gpc.getExtensionByUrl(
                                      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference");
                              assertNotNull(ext2);
                              assertEquals("initial-population-1", ext2.getValue().toString());
                              assertEquals("denom-description", gpc.getDescription());
                              break;
                            case "numerator":
                              // this needs to be associated with numerator IP
                              Extension ext1 =
                                  gpc.getExtensionByUrl(
                                      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference");
                              assertNotNull(ext1);
                              assertEquals("initial-population-2", ext1.getValue().toString());
                              assertEquals("numer-description", gpc.getDescription());
                              break;
                            case "initial-population":
                              // get associationType
                              Extension ext3 =
                                  gpc.getExtensionByUrl(
                                      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference");
                              assertNull(ext3);
                              assertThat(
                                  gpc.getDescription(),
                                  containsString("initial-population-description"));
                              break;
                            default:
                              System.out.println(coding.getCode());
                              break;
                          }
                        });
              });
        });
  }

  @Test
  public void testBuildFhirPopulationGroupsWithStratifications() {
    Population ip1 = new Population();
    ip1.setName(PopulationType.INITIAL_POPULATION);
    ip1.setAssociationType(AssociationType.DENOMINATOR);
    ip1.setId("initial-population-1");
    Population ip2 = new Population();
    ip2.setName(PopulationType.INITIAL_POPULATION);
    ip2.setAssociationType(AssociationType.NUMERATOR);
    ip2.setId("initial-population-2");
    Population denom = new Population();
    denom.setName(PopulationType.DENOMINATOR);
    Population numer = new Population();
    numer.setName(PopulationType.NUMERATOR);
    Group group = new Group();
    group.setScoring(MeasureScoring.RATIO.toString());
    List<Population> pops = new ArrayList<>();
    pops.add(numer);
    pops.add(denom);
    pops.add(ip1);
    pops.add(ip2);
    group.setPopulations(pops);
    List<Stratification> stratifications = new ArrayList<>();
    Stratification strat1 = new Stratification();
    strat1.setId("testStrat1Id");
    strat1.setDescription("strat-description");
    strat1.setAssociations(
        List.of(PopulationType.INITIAL_POPULATION, PopulationType.MEASURE_POPULATION));
    stratifications.add(strat1);
    Stratification strat2 = new Stratification();
    strat2.setId("testStrat2Id");
    strat2.setDescription("strat2-description");
    strat2.setAssociations(
        List.of(PopulationType.INITIAL_POPULATION, PopulationType.MEASURE_POPULATION));
    stratifications.add(strat2);
    Stratification strat3 = new Stratification(); // no associations, not included in output
    strat3.setId("testStrat3Id");
    strat3.setDescription("strat3-description");
    stratifications.add(strat3);
    group.setStratifications(stratifications);
    List<Group> groups = new ArrayList<>();
    groups.add(group);

    List<MeasureGroupComponent> groupComponent =
        measureTranslatorService.buildGroups(groups, MEASURE_BUNDLE_TYPE_CALCULATION);
    assertNotNull(groupComponent);

    assertThat(groupComponent.size(), is(equalTo(1)));
    MeasureGroupComponent measureGroupComponent = groupComponent.get(0);
    assertThat(measureGroupComponent, is(notNullValue()));
    List<MeasureGroupStratifierComponent> stratifier = measureGroupComponent.getStratifier();
    assertThat(stratifier, is(notNullValue()));
    assertThat(stratifier.size(), is(equalTo(3)));
    MeasureGroupStratifierComponent measureGroupStratifierComponent = stratifier.get(0);
    assertThat(measureGroupStratifierComponent, is(notNullValue()));
    assertThat(measureGroupStratifierComponent.getDescription(), is(equalTo("strat-description")));
    assertThat(measureGroupStratifierComponent.getId(), is(equalTo("testStrat1Id")));
    MeasureGroupStratifierComponent measureGroupStratifierComponent2 = stratifier.get(1);
    assertThat(measureGroupStratifierComponent2, is(notNullValue()));
    assertThat(
        measureGroupStratifierComponent2.getDescription(), is(equalTo("strat2-description")));
    assertThat(measureGroupStratifierComponent2.getId(), is(equalTo("testStrat2Id")));
    Expression expression = measureGroupStratifierComponent.getCriteria();
    assertThat(expression, is(notNullValue()));
    List<Extension> appliesToExt = measureGroupStratifierComponent.getExtension();
    assertThat(appliesToExt.size(), is(2));
    Type value = appliesToExt.get(0).getValue();
    CodeableConcept codeableConcept = value.castToCodeableConcept(value);
    assertThat(codeableConcept.getCoding(), is(notNullValue()));
    assertThat(codeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(codeableConcept.getCodingFirstRep(), is(notNullValue()));
    assertThat(
        codeableConcept.getCodingFirstRep().getSystem(),
        is(equalTo(UriConstants.CodeSystem.POPULATION_SYSTEM_URI)));
    assertThat(
        codeableConcept.getCodingFirstRep().getCode(),
        is(equalTo(PopulationType.INITIAL_POPULATION.toCode())));
  }

  @Test
  public void testBuildFhirPopulationGroupsWithStratificationsOfMultipleAssociations() {

    Population ip1 = new Population();
    ip1.setName(PopulationType.INITIAL_POPULATION);
    ip1.setAssociationType(AssociationType.DENOMINATOR);
    ip1.setId("initial-population-1");
    Population ip2 = new Population();
    ip2.setName(PopulationType.MEASURE_POPULATION);
    ip2.setAssociationType(AssociationType.NUMERATOR);
    ip2.setId("measure-population-2");
    Population ip3 = new Population();
    ip3.setName(PopulationType.MEASURE_OBSERVATION);
    ip3.setAssociationType(AssociationType.NUMERATOR);
    ip3.setId("measure-observation-3");

    Group group = new Group();
    group.setScoring(MeasureScoring.CONTINUOUS_VARIABLE.toString());
    List<Population> pops = new ArrayList<>();
    pops.add(ip1);
    pops.add(ip2);
    pops.add(ip3);
    group.setPopulations(pops);

    List<Stratification> stratifications = new ArrayList<>();
    Stratification strat1 = new Stratification();
    strat1.setId("testStrat1Id");
    strat1.setDescription("strat-description");
    strat1.setAssociations(
        List.of(PopulationType.INITIAL_POPULATION, PopulationType.MEASURE_POPULATION));
    stratifications.add(strat1);
    Stratification strat2 = new Stratification();
    strat2.setDescription("strat-description2");
    strat1.setAssociations(
        List.of(PopulationType.MEASURE_POPULATION, PopulationType.INITIAL_POPULATION));
    stratifications.add(strat2);
    group.setStratifications(stratifications);
    List<Group> groups = new ArrayList<>();
    groups.add(group);

    List<MeasureGroupComponent> groupComponent =
        measureTranslatorService.buildGroups(groups, MEASURE_BUNDLE_TYPE_CALCULATION);
    assertNotNull(groupComponent);

    assertThat(groupComponent.size(), is(equalTo(1)));
    MeasureGroupComponent measureGroupComponent = groupComponent.get(0);
    assertThat(measureGroupComponent, is(notNullValue()));
    List<MeasureGroupStratifierComponent> stratifier = measureGroupComponent.getStratifier();
    assertThat(stratifier, is(notNullValue()));
    assertThat(stratifier.size(), is(equalTo(2)));
    MeasureGroupStratifierComponent measureGroupStratifierComponent = stratifier.get(0);
    assertThat(measureGroupStratifierComponent, is(notNullValue()));
    assertThat(measureGroupStratifierComponent.getDescription(), is(equalTo("strat-description")));
    Expression expression = measureGroupStratifierComponent.getCriteria();
    assertThat(expression, is(notNullValue()));

    List<Extension> appliesToExt = measureGroupStratifierComponent.getExtension();
    assertThat(appliesToExt.size(), is(2));
    Type value = appliesToExt.get(0).getValue();
    CodeableConcept codeableConcept = value.castToCodeableConcept(value);
    assertThat(codeableConcept.getCoding(), is(notNullValue()));
    assertThat(codeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(codeableConcept.getCodingFirstRep(), is(notNullValue()));
    assertThat(
        codeableConcept.getCodingFirstRep().getSystem(),
        is(equalTo(UriConstants.CodeSystem.POPULATION_SYSTEM_URI)));
    assertThat(
        codeableConcept.getCodingFirstRep().getCode(),
        is(equalTo(PopulationType.MEASURE_POPULATION.toCode())));
  }

  @Test
  public void testBuildFhirPopulationGroupsWithStratificationsOfNoAssociations() {

    Population ip1 = new Population();
    ip1.setName(PopulationType.INITIAL_POPULATION);
    ip1.setAssociationType(AssociationType.DENOMINATOR);
    ip1.setId("initial-population-1");
    Population ip2 = new Population();
    ip2.setName(PopulationType.MEASURE_POPULATION);
    ip2.setAssociationType(AssociationType.NUMERATOR);
    ip2.setId("measure-population-2");
    Population ip3 = new Population();
    ip3.setName(PopulationType.MEASURE_OBSERVATION);
    ip3.setAssociationType(AssociationType.NUMERATOR);
    ip3.setId("measure-observation-3");

    Group group = new Group();
    group.setScoring(MeasureScoring.CONTINUOUS_VARIABLE.toString());
    List<Population> pops = new ArrayList<>();
    pops.add(ip1);
    pops.add(ip2);
    pops.add(ip3);
    group.setPopulations(pops);

    List<Stratification> stratifications = new ArrayList<>();
    Stratification strat1 = new Stratification();
    strat1.setId("testStrat1Id");
    strat1.setDescription("strat-description");
    stratifications.add(strat1);
    Stratification strat2 = new Stratification();
    strat2.setDescription("strat-description2");
    stratifications.add(strat2);
    group.setStratifications(stratifications);
    List<Group> groups = new ArrayList<>();
    groups.add(group);

    List<MeasureGroupComponent> groupComponent =
        measureTranslatorService.buildGroups(groups, MEASURE_BUNDLE_TYPE_CALCULATION);
    assertNotNull(groupComponent);

    assertThat(groupComponent.size(), is(equalTo(1)));
    MeasureGroupComponent measureGroupComponent = groupComponent.get(0);
    assertThat(measureGroupComponent, is(notNullValue()));
    List<MeasureGroupStratifierComponent> stratifier = measureGroupComponent.getStratifier();
    assertThat(stratifier, is(notNullValue()));
    assertThat(stratifier.size(), is(equalTo(2)));
    MeasureGroupStratifierComponent measureGroupStratifierComponent = stratifier.get(0);
    assertNull(measureGroupStratifierComponent);
  }

  @Test
  void testBuildMeasureMetaHandlesValidInput() {
    final Meta output =
        measureTranslatorService.buildMeasureMeta(
            List.of(Group.builder().scoring("Proportion").build()));
    assertThat(output, is(notNullValue()));
    assertThat(output.hasProfile(), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.PROPORTION_PROFILE_URI), is(true));
  }

  @Test
  void testBuildMeasureMetaHandlesEmptyInput() {
    final Meta output = measureTranslatorService.buildMeasureMeta(Collections.emptyList());
    assertThat(output, is(notNullValue()));
    assertThat(output.hasProfile(), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI), is(true));
  }

  @Test
  void testBuildMeasureMetaHandlesMultipleScoring() {
    final Meta output =
        measureTranslatorService.buildMeasureMeta(
            List.of(
                Group.builder().scoring("Proportion").build(),
                Group.builder().scoring("Ratio").build()));
    assertThat(output, is(notNullValue()));
    assertThat(output.hasProfile(), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI), is(true));
  }

  @Test
  void testBuildMeasureMetaHandlesMultipleOfSameScoring() {
    final Meta output =
        measureTranslatorService.buildMeasureMeta(
            List.of(
                Group.builder().scoring("Cohort").build(),
                Group.builder().scoring("Cohort").build()));
    assertThat(output, is(notNullValue()));
    assertThat(output.hasProfile(), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI), is(true));
    assertThat(output.hasProfile(UriConstants.CqfMeasures.COHORT_PROFILE_URI), is(true));
  }

  @Test
  public void testBuildMeasureIdentifiersReturnsEmptyListForNullMeasure() {
    final Measure madieMeasure = null;
    List<Identifier> output = measureTranslatorService.buildMeasureIdentifiers(madieMeasure);
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(true));
  }

  @Test
  public void testBuildMeasureIdentifiersReturnsIdentifiersForMeasure() {
    final Measure madieMeasure =
        Measure.builder()
            .id("MEASURE_ID_1")
            .versionId("UUID_1")
            .measureSetId("MEASURE_SET_ID_99")
            .ecqmTitle("ECQM_TITLE")
            .measureSet(MeasureSet.builder().measureSetId("MEASURE_SET_ID_99").cmsId(986).build())
            .measureMetaData(
                MeasureMetaData.builder()
                    .endorsements(
                        List.of(
                            Endorsement.builder().endorsementId("CBE1234").endorser("CBE").build()))
                    .build())
            .build();
    List<Identifier> output = measureTranslatorService.buildMeasureIdentifiers(madieMeasure);
    assertThat(output, is(notNullValue()));
    // Short Name
    assertThat(output.size(), is(equalTo(5)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getUse(), is(equalTo(Identifier.IdentifierUse.USUAL)));
    assertThat(output.get(0).getSystem(), is(equalTo(SHORT_NAME)));
    assertThat(output.get(0).getValue(), is(equalTo("ECQM_TITLE")));
    assertThat(output.get(0).getType(), is(notNullValue()));
    assertThat(
        output.get(0).getType().getCodingFirstRep().getSystem(),
        is(equalTo(CODE_SYSTEM_IDENTIFIER_TYPE_URI)));
    assertThat(
        output.get(0).getType().getCodingFirstRep().getCode(),
        is(equalTo(CODE_SHORT_NAME.getCode())));
    assertThat(
        output.get(0).getType().getCodingFirstRep().getDisplay(),
        is(equalTo(CODE_SHORT_NAME.getDisplay())));
    // Measure Set ID
    assertThat(output.get(1), is(notNullValue()));
    assertThat(output.get(1).getUse(), is(equalTo(Identifier.IdentifierUse.OFFICIAL)));
    assertThat(output.get(1).getSystem(), is(equalTo(UriConstants.URN_IETF_RFC_3986)));
    assertThat(
        output.get(1).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "MEASURE_SET_ID_99")));
    assertThat(output.get(1).getType(), is(notNullValue()));
    assertThat(
        output.get(1).getType().getCodingFirstRep().getSystem(),
        is(equalTo(CODE_SYSTEM_IDENTIFIER_TYPE_URI)));
    assertThat(
        output.get(1).getType().getCodingFirstRep().getCode(),
        is(equalTo(CODE_VERSION_INDEPENDENT.getCode())));
    assertThat(
        output.get(1).getType().getCodingFirstRep().getDisplay(),
        is(equalTo(CODE_VERSION_INDEPENDENT.getDisplay())));
    // Measure ID
    assertThat(output.get(2), is(notNullValue()));
    assertThat(output.get(2).getUse(), is(equalTo(Identifier.IdentifierUse.OFFICIAL)));
    assertThat(output.get(2).getSystem(), is(equalTo(UriConstants.URN_IETF_RFC_3986)));
    assertThat(output.get(2).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "UUID_1")));
    assertThat(output.get(2).getType(), is(notNullValue()));
    assertThat(
        output.get(2).getType().getCodingFirstRep().getSystem(),
        is(equalTo(CODE_SYSTEM_IDENTIFIER_TYPE_URI)));
    assertThat(
        output.get(2).getType().getCodingFirstRep().getCode(),
        is(equalTo(CODE_VERSION_SPECIFIC.getCode())));
    // CBE ID
    assertThat(output.get(3), is(notNullValue()));
    assertThat(output.get(3).getUse(), is(equalTo(Identifier.IdentifierUse.OFFICIAL)));
    assertThat(output.get(3).getSystem(), is(equalTo(UriConstants.MadieMeasure.CBE_ID)));
    assertThat(output.get(3).getValue(), is(equalTo("CBE1234")));
    assertThat(output.get(3).getType(), is(notNullValue()));
    assertThat(
        output.get(3).getType().getCodingFirstRep().getSystem(),
        is(equalTo(CODE_SYSTEM_IDENTIFIER_TYPE_URI)));
    assertThat(
        output.get(3).getType().getCodingFirstRep().getCode(),
        is(equalTo(CODE_ENDORSER.getCode())));
    assertThat(
        output.get(3).getType().getCodingFirstRep().getDisplay(),
        is(equalTo(CODE_ENDORSER.getDisplay())));
    assertThat(output.get(3).getAssigner(), is(notNullValue()));
    assertThat(output.get(3).getAssigner().getDisplay(), is(equalTo("CBE")));
    // CMS ID
    assertThat(output.get(4), is(notNullValue()));
    assertThat(output.get(4).getUse(), is(equalTo(Identifier.IdentifierUse.OFFICIAL)));
    assertThat(output.get(4).getSystem(), is(equalTo(UriConstants.MadieMeasure.CMS_ID)));
    assertThat(output.get(4).getValue(), is(equalTo("986FHIR")));
    assertThat(output.get(4).getType(), is(notNullValue()));
    assertThat(
        output.get(4).getType().getCodingFirstRep().getSystem(),
        is(equalTo(CODE_SYSTEM_IDENTIFIER_TYPE_URI)));
    assertThat(
        output.get(4).getType().getCodingFirstRep().getCode(),
        is(equalTo(CODE_PUBLISHER.getCode())));
    assertThat(
        output.get(4).getType().getCodingFirstRep().getDisplay(),
        is(equalTo(CODE_PUBLISHER.getDisplay())));
    assertThat(output.get(4).getAssigner(), is(notNullValue()));
    assertThat(output.get(4).getAssigner().getDisplay(), is(equalTo("CMS")));
  }

  @Test
  public void testBuildMeasureIdentifiersReturnsIdentifiersWithCmsIdWithoutCbeForMeasure() {
    final Measure madieMeasure =
        Measure.builder()
            .id("MEASURE_ID_1")
            .versionId("UUID_1")
            .measureSetId("MEASURE_SET_ID_99")
            .ecqmTitle("ECQM_TITLE")
            .measureSet(MeasureSet.builder().cmsId(22).measureSetId("MEASURE_SET_ID_99").build())
            .build();
    List<Identifier> output = measureTranslatorService.buildMeasureIdentifiers(madieMeasure);
    assertThat(output, is(notNullValue()));
    // Short Name
    assertThat(output.size(), is(equalTo(4)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getSystem(), is(equalTo(SHORT_NAME)));
    // Measure Set ID
    assertThat(output.get(1), is(notNullValue()));
    assertThat(
        output.get(1).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "MEASURE_SET_ID_99")));
    // Measure ID
    assertThat(output.get(2), is(notNullValue()));
    assertThat(output.get(2).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "UUID_1")));
    // CMS ID
    assertThat(output.get(3), is(notNullValue()));
    assertThat(output.get(3).getValue(), is(equalTo("22FHIR")));
  }

  @Test
  public void testBuildMeasureIdentifiersReturnsIdentifiersWithoutCmsIdWithCbeForMeasure() {
    final Measure madieMeasure =
        Measure.builder()
            .id("MEASURE_ID_1")
            .versionId("UUID_1")
            .measureSetId("MEASURE_SET_ID_99")
            .ecqmTitle("ECQM_TITLE")
            .measureMetaData(
                MeasureMetaData.builder()
                    .endorsements(
                        List.of(
                            Endorsement.builder().endorsementId("CBE1234").endorser("CBE").build()))
                    .build())
            .measureSet(MeasureSet.builder().measureSetId("MEASURE_SET_ID_99").build())
            .build();
    List<Identifier> output = measureTranslatorService.buildMeasureIdentifiers(madieMeasure);
    assertThat(output, is(notNullValue()));
    // Short Name
    assertThat(output.size(), is(equalTo(4)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getSystem(), is(equalTo(SHORT_NAME)));
    // Measure Set ID
    assertThat(output.get(1), is(notNullValue()));
    assertThat(
        output.get(1).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "MEASURE_SET_ID_99")));
    // Measure ID
    assertThat(output.get(2), is(notNullValue()));
    assertThat(output.get(2).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "UUID_1")));
    // CBE ID
    assertThat(output.get(3), is(notNullValue()));
    assertThat(output.get(3).getValue(), is(equalTo("CBE1234")));
  }

  @Test
  public void testBuildMeasureIdentifiersReturnsIdentifiersWithoutCbeForMeasure() {
    final Measure madieMeasure =
        Measure.builder()
            .id("MEASURE_ID_1")
            .versionId("UUID_1")
            .measureSetId("MEASURE_SET_ID_99")
            .ecqmTitle("ECQM_TITLE")
            .measureMetaData(
                MeasureMetaData.builder()
                    .endorsements(
                        List.of(Endorsement.builder().endorsementId("").endorser("").build()))
                    .build())
            .build();
    List<Identifier> output = measureTranslatorService.buildMeasureIdentifiers(madieMeasure);
    assertThat(output, is(notNullValue()));
    // Short Name
    assertThat(output.size(), is(equalTo(3)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getSystem(), is(equalTo(SHORT_NAME)));
    // Measure Set ID
    assertThat(output.get(1), is(notNullValue()));
    assertThat(
        output.get(1).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "MEASURE_SET_ID_99")));
    // Measure ID
    assertThat(output.get(2), is(notNullValue()));
    assertThat(output.get(2).getValue(), is(equalTo(UriConstants.URN_UUID_PREFIX + "UUID_1")));
  }

  @Test
  public void testBuildGroupsWithNull() {
    List<MeasureGroupComponent> listOfComponent =
        measureTranslatorService.buildGroups(new ArrayList<>(), MEASURE_BUNDLE_TYPE_CALCULATION);
    assertNull(listOfComponent);
  }

  @Test
  public void testBuildScoringConceptNullScoring() {
    CodeableConcept codeConcept = measureTranslatorService.buildScoringConcept(null);
    assertNull(codeConcept);
  }

  @Test
  public void testBuildScoringConceptContinuousVariable() {
    CodeableConcept codeConcept =
        measureTranslatorService.buildScoringConcept("Continuous Variable");
    assertNotNull(codeConcept);
    assertEquals("continuous-variable", codeConcept.getCoding().get(0).getCode());
  }

  @Test
  public void testCreateFhirMeasureForDraftMadieMeasure() {
    madieMeasure.getMeasureMetaData().setDraft(true);
    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(
            madieMeasure, MEASURE_BUNDLE_TYPE_CALCULATION);
    assertEquals("Draft based on 0.0.000", measure.getVersion());
  }
}

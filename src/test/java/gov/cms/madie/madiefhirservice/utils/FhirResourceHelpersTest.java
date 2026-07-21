package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FhirResourceHelpersTest {

  Group ratioGroup;

  @BeforeEach
  void beforeEach() {
    ratioGroup =
        Group.builder()
            .id("group1_id")
            .displayId("Group_1")
            .populations(
                List.of(
                    Population.builder()
                        .id("group1_ip_id")
                        .definition("Initial Population")
                        .name(PopulationType.INITIAL_POPULATION)
                        .displayId("InitialPopulation_1")
                        .build(),
                    Population.builder()
                        .id("group1_denom_id")
                        .definition("Denominator")
                        .name(PopulationType.DENOMINATOR)
                        .displayId("Denominator_1")
                        .build(),
                    Population.builder()
                        .id("group1_numer_id")
                        .definition("Numerator")
                        .name(PopulationType.NUMERATOR)
                        .displayId("Numerator_1")
                        .build()))
            .measureObservations(
                List.of(
                    MeasureObservation.builder()
                        .id("group1_denomObs_id")
                        .criteriaReference("group1_denom_id")
                        .definition("Denom Obs")
                        .displayId("MeasureObservation_1_1")
                        .build(),
                    MeasureObservation.builder()
                        .id("group1_numerObs_id")
                        .criteriaReference("group1_numer_id")
                        .definition("Numer Obs")
                        .displayId("MeasureObservation_1_2")
                        .build()))
            .build();
  }

  @Test
  void testExpectedInverseValue() {
    assertEquals(1, FhirResourceHelpers.getExpectedInverseValue(0));
    assertEquals(0, FhirResourceHelpers.getExpectedInverseValue(1));
  }

  @Test
  void testBuildStratumPopulationForValueIndexOfTrue() {
    TestCaseStratificationValue stratValue1 =
        TestCaseStratificationValue.builder().name("Strata-1").id("strat1Id").expected(1).build();
    stratValue1.setPopulationValues(
        List.of(
            TestCasePopulationValue.builder()
                .id("1")
                .name(PopulationType.INITIAL_POPULATION)
                .expected(1)
                .build(),
            TestCasePopulationValue.builder()
                .id("2")
                .name(PopulationType.DENOMINATOR)
                .expected(1)
                .build(),
            TestCasePopulationValue.builder()
                .id("3")
                .name(PopulationType.NUMERATOR)
                .expected(0)
                .build()));

    Population pop1 = Population.builder().id("1").displayId("InitialPopulation_1").build();
    Population pop2 = Population.builder().id("2").displayId("Denominator_1").build();
    Population pop3 = Population.builder().id("3").displayId("Numerator_1").build();
    Group group1 =
        Group.builder()
            .id("group1Id")
            .displayId("Group_1")
            .populations(List.of(pop1, pop2, pop3))
            .build();

    List<MeasureReport.StratifierGroupPopulationComponent> stratifierGroupPopulationComponents =
        FhirResourceHelpers.buildStratumPopulation(stratValue1, true, true, group1);

    assertThat(stratifierGroupPopulationComponents.size(), is(3));
    assertThat(
        stratifierGroupPopulationComponents.get(0).getCode().getCoding().get(0).getCode(),
        is("initial-population"));
    assertThat(stratifierGroupPopulationComponents.get(0).getCount(), is(1));
    assertThat(
        stratifierGroupPopulationComponents.get(1).getCode().getCoding().get(0).getCode(),
        is("denominator"));
    assertThat(stratifierGroupPopulationComponents.get(1).getCount(), is(1));
    assertThat(
        stratifierGroupPopulationComponents.get(2).getCode().getCoding().get(0).getCode(),
        is("numerator"));
    assertThat(stratifierGroupPopulationComponents.get(2).getCount(), is(0));

    assertEquals("InitialPopulation_1", stratifierGroupPopulationComponents.get(0).getId());
    assertEquals("Denominator_1", stratifierGroupPopulationComponents.get(1).getId());
    assertEquals("Numerator_1", stratifierGroupPopulationComponents.get(2).getId());
  }

  @Test
  void testBuildStratumPopulationForValueIndexOfFalse() {
    TestCaseStratificationValue stratValue1 =
        TestCaseStratificationValue.builder().name("Strata-1").id("strat1Id").expected(1).build();
    stratValue1.setPopulationValues(
        List.of(
            TestCasePopulationValue.builder()
                .id("1")
                .name(PopulationType.INITIAL_POPULATION)
                .expected(1)
                .build(),
            TestCasePopulationValue.builder()
                .id("2")
                .name(PopulationType.DENOMINATOR)
                .expected(1)
                .build(),
            TestCasePopulationValue.builder()
                .id("3")
                .name(PopulationType.NUMERATOR)
                .expected(0)
                .build()));

    Population pop1 = Population.builder().id("1").displayId("InitialPopulation_1").build();
    Population pop2 = Population.builder().id("2").displayId("Denominator_1").build();
    Population pop3 = Population.builder().id("3").displayId("Numerator_1").build();
    Group group1 =
        Group.builder()
            .id("group1Id")
            .displayId("Group_1")
            .populations(List.of(pop1, pop2, pop3))
            .build();

    List<MeasureReport.StratifierGroupPopulationComponent> stratifierGroupPopulationComponents =
        FhirResourceHelpers.buildStratumPopulation(stratValue1, false, true, group1);

    assertThat(stratifierGroupPopulationComponents.size(), is(3));
    assertThat(
        stratifierGroupPopulationComponents.get(0).getCode().getCoding().get(0).getCode(),
        is("initial-population"));
    assertThat(stratifierGroupPopulationComponents.get(0).getCount(), is(0));
    assertThat(
        stratifierGroupPopulationComponents.get(1).getCode().getCoding().get(0).getCode(),
        is("denominator"));
    assertThat(stratifierGroupPopulationComponents.get(1).getCount(), is(0));
    assertThat(
        stratifierGroupPopulationComponents.get(2).getCode().getCoding().get(0).getCode(),
        is("numerator"));
    assertThat(stratifierGroupPopulationComponents.get(2).getCount(), is(1));

    assertEquals("InitialPopulation_1", stratifierGroupPopulationComponents.get(0).getId());
    assertEquals("Denominator_1", stratifierGroupPopulationComponents.get(1).getId());
    assertEquals("Numerator_1", stratifierGroupPopulationComponents.get(2).getId());
  }

  @Test
  void testBuildStratumPopulationForNonPatientBasedMeasures() {
    TestCaseStratificationValue stratValue1 =
        TestCaseStratificationValue.builder().name("Strata-1").id("strat1Id").expected(1).build();
    stratValue1.setPopulationValues(
        List.of(
            TestCasePopulationValue.builder()
                .id("1")
                .name(PopulationType.INITIAL_POPULATION)
                .expected(5)
                .build(),
            TestCasePopulationValue.builder()
                .id("2")
                .name(PopulationType.DENOMINATOR)
                .expected(4)
                .build(),
            TestCasePopulationValue.builder()
                .id("3")
                .name(PopulationType.NUMERATOR)
                .expected(2)
                .build()));

    Population pop1 = Population.builder().id("1").displayId("InitialPopulation_1").build();
    Population pop2 = Population.builder().id("2").displayId("Denominator_1").build();
    Population pop3 = Population.builder().id("3").displayId("Numerator_1").build();
    Group group1 =
        Group.builder()
            .id("group1Id")
            .displayId("Group_1")
            .populations(List.of(pop1, pop2, pop3))
            .build();

    List<MeasureReport.StratifierGroupPopulationComponent> stratifierGroupPopulationComponents =
        FhirResourceHelpers.buildStratumPopulation(stratValue1, null, false, group1);

    assertThat(stratifierGroupPopulationComponents.size(), is(3));
    assertThat(
        stratifierGroupPopulationComponents.get(0).getCode().getCoding().get(0).getCode(),
        is("initial-population"));
    assertThat(stratifierGroupPopulationComponents.get(0).getCount(), is(5));
    assertThat(
        stratifierGroupPopulationComponents.get(1).getCode().getCoding().get(0).getCode(),
        is("denominator"));
    assertThat(stratifierGroupPopulationComponents.get(1).getCount(), is(4));
    assertThat(
        stratifierGroupPopulationComponents.get(2).getCode().getCoding().get(0).getCode(),
        is("numerator"));
    assertThat(stratifierGroupPopulationComponents.get(2).getCount(), is(2));

    assertEquals("InitialPopulation_1", stratifierGroupPopulationComponents.get(0).getId());
    assertEquals("Denominator_1", stratifierGroupPopulationComponents.get(1).getId());
    assertEquals("Numerator_1", stratifierGroupPopulationComponents.get(2).getId());
  }

  @Test
  void testBuildStratumPopulationForNonPatientBasedMeasuresWithEmptyExpectedValues() {
    TestCaseStratificationValue stratValue1 =
        TestCaseStratificationValue.builder().name("Strata-1").id("strat1Id").expected(1).build();
    stratValue1.setPopulationValues(
        List.of(
            TestCasePopulationValue.builder()
                .id("1")
                .name(PopulationType.INITIAL_POPULATION)
                .expected(null)
                .build(),
            TestCasePopulationValue.builder()
                .id("2")
                .name(PopulationType.DENOMINATOR)
                .expected("")
                .build(),
            TestCasePopulationValue.builder()
                .id("3")
                .name(PopulationType.NUMERATOR)
                .expected(1)
                .build()));

    Population pop1 = Population.builder().id("1").displayId("InitialPopulation_1").build();
    Population pop2 = Population.builder().id("2").displayId("Denominator_1").build();
    Population pop3 = Population.builder().id("3").displayId("Numerator_1").build();
    Group group1 =
        Group.builder()
            .id("group1Id")
            .displayId("Group_1")
            .populations(List.of(pop1, pop2, pop3))
            .build();

    List<MeasureReport.StratifierGroupPopulationComponent> stratifierGroupPopulationComponents =
        FhirResourceHelpers.buildStratumPopulation(stratValue1, null, false, group1);

    assertEquals(3, stratifierGroupPopulationComponents.size());
    assertEquals(
        "initial-population",
        stratifierGroupPopulationComponents.get(0).getCode().getCoding().get(0).getCode());
    assertNull(stratifierGroupPopulationComponents.get(0).getCountElement().getValue());
    assertEquals(
        "denominator",
        stratifierGroupPopulationComponents.get(1).getCode().getCoding().get(0).getCode());
    assertNull(stratifierGroupPopulationComponents.get(1).getCountElement().getValue());
    assertEquals(
        "numerator",
        stratifierGroupPopulationComponents.get(2).getCode().getCoding().get(0).getCode());
    assertEquals(1, stratifierGroupPopulationComponents.get(2).getCount());

    assertEquals("InitialPopulation_1", stratifierGroupPopulationComponents.get(0).getId());
    assertEquals("Denominator_1", stratifierGroupPopulationComponents.get(1).getId());
    assertEquals("Numerator_1", stratifierGroupPopulationComponents.get(2).getId());
  }

  @Test
  void testGetGroupStratificationDisplayIdPopulationsNotFound() {
    Group group1 =
        Group.builder()
            .id("group1Id")
            .displayId("Group_1")
            .populations(Collections.emptyList())
            .build();

    String result = FhirResourceHelpers.getGroupPopulationDisplayId(group1, "1");

    assertEquals("1", result);
  }

  @Test
  void testGetGroupStratificationDisplayIdPopulationIdNotFound() {
    Population pop1 = Population.builder().id("1").displayId("InitialPopulation_1").build();
    Population pop2 = Population.builder().id("2").displayId("Denominator_1").build();
    Population pop3 = Population.builder().id("3").displayId("Numerator_1").build();
    Group group1 =
        Group.builder()
            .id("group1Id")
            .displayId("Group_1")
            .populations(List.of(pop1, pop2, pop3))
            .build();

    String result = FhirResourceHelpers.getGroupPopulationDisplayId(group1, "populationId");

    assertEquals("populationId", result);
  }

  @Test
  public void testGetGroupObservationDisplayIdWithMatchingObservationForDenomObs() {
    // Given
    TestCasePopulationValue populationValue =
        TestCasePopulationValue.builder().criteriaReference("group1_denom_id").build();

    // When
    final String output =
        FhirResourceHelpers.getGroupObservationDisplayId(ratioGroup, populationValue, 1, false);

    // Then
    assertThat(output, is(equalTo("MeasureObservation_1_1_1")));
  }

  @Test
  public void testGetGroupObservationDisplayIdWithMatchingObservationForDenomObsPatientBased() {
    // Given
    TestCasePopulationValue populationValue =
        TestCasePopulationValue.builder().criteriaReference("group1_denom_id").build();

    // When
    final String output =
        FhirResourceHelpers.getGroupObservationDisplayId(ratioGroup, populationValue, 1, true);

    // Then
    assertThat(output, is(equalTo("MeasureObservation_1_1")));
  }

  @Test
  public void testGetGroupObservationDisplayIdWithMatchingObservationForNumerObs() {
    // Given
    TestCasePopulationValue populationValue =
        TestCasePopulationValue.builder().criteriaReference("group1_numer_id").build();

    // When
    final String output =
        FhirResourceHelpers.getGroupObservationDisplayId(ratioGroup, populationValue, 1, false);

    // Then
    assertThat(output, is(equalTo("MeasureObservation_1_2_1")));
  }

  @Test
  public void testGetGroupObservationDisplayIdWithNoMatchingObservation() {
    // Given
    TestCasePopulationValue populationValue =
        TestCasePopulationValue.builder()
            .id("measurePopulationObservation0")
            .criteriaReference("group1_measurepop_id")
            .build();

    // When
    final String output =
        FhirResourceHelpers.getGroupObservationDisplayId(ratioGroup, populationValue, 2, false);

    // Then
    assertThat(output, is(equalTo("measurePopulationObservation0")));
  }

  @Test
  public void testGetGroupObservationDisplayIdWithNullObservations() {
    // Given
    TestCasePopulationValue populationValue =
        TestCasePopulationValue.builder()
            .id("measurePopulationObservation0")
            .criteriaReference("group1_measurepop_id")
            .build();
    ratioGroup.setMeasureObservations(null);

    // When
    final String output =
        FhirResourceHelpers.getGroupObservationDisplayId(ratioGroup, populationValue, 2, false);

    // Then
    assertThat(output, is(equalTo("measurePopulationObservation0")));
  }

  @Test
  public void testGetGroupObservationDisplayIdWithEmptyObservations() {
    // Given
    TestCasePopulationValue populationValue =
        TestCasePopulationValue.builder()
            .id("measurePopulationObservation0")
            .criteriaReference("group1_measurepop_id")
            .build();
    ratioGroup.setMeasureObservations(List.of());

    // When
    final String output =
        FhirResourceHelpers.getGroupObservationDisplayId(ratioGroup, populationValue, 2, true);

    // Then
    assertThat(output, is(equalTo("measurePopulationObservation0")));
  }

  @Test
  void testFlattenValueSetContainsReturnsEmptyListForNullOrEmptyInput() {
    List<ValueSet.ValueSetExpansionContainsComponent> flattenedFromNull =
        FhirResourceHelpers.flattenValueSetContains(null);
    List<ValueSet.ValueSetExpansionContainsComponent> flattenedFromEmpty =
        FhirResourceHelpers.flattenValueSetContains(Collections.emptyList());

    assertThat(flattenedFromNull.isEmpty(), is(true));
    assertThat(flattenedFromEmpty.isEmpty(), is(true));
  }

  @Test
  void testFlattenValueSetContainsReturnsTopLevelElementsWhenNoNestedContains() {
    ValueSet.ValueSetExpansionContainsComponent firstContains =
        new ValueSet.ValueSetExpansionContainsComponent().setCode("code-1");
    ValueSet.ValueSetExpansionContainsComponent secondContains =
        new ValueSet.ValueSetExpansionContainsComponent().setCode("code-2");

    List<ValueSet.ValueSetExpansionContainsComponent> flattened =
        FhirResourceHelpers.flattenValueSetContains(List.of(firstContains, secondContains));

    assertThat(flattened.size(), is(2));
    assertThat(flattened.get(0), is(firstContains));
    assertThat(flattened.get(1), is(secondContains));
  }

  @Test
  void testFlattenValueSetContainsRecursivelyFlattensNestedContainsInOrder() {
    ValueSet.ValueSetExpansionContainsComponent parent =
        new ValueSet.ValueSetExpansionContainsComponent().setCode("parent");
    ValueSet.ValueSetExpansionContainsComponent childOne =
        new ValueSet.ValueSetExpansionContainsComponent().setCode("child-1");
    ValueSet.ValueSetExpansionContainsComponent grandChild =
        new ValueSet.ValueSetExpansionContainsComponent().setCode("grand-child");
    ValueSet.ValueSetExpansionContainsComponent childTwo =
        new ValueSet.ValueSetExpansionContainsComponent().setCode("child-2");
    ValueSet.ValueSetExpansionContainsComponent sibling =
        new ValueSet.ValueSetExpansionContainsComponent().setCode("sibling");

    childOne.addContains(grandChild);
    parent.addContains(childOne);
    parent.addContains(childTwo);

    List<ValueSet.ValueSetExpansionContainsComponent> flattened =
        FhirResourceHelpers.flattenValueSetContains(List.of(parent, sibling));

    assertThat(flattened.size(), is(5));
    assertThat(flattened.get(0), is(parent));
    assertThat(flattened.get(1), is(childOne));
    assertThat(flattened.get(2), is(grandChild));
    assertThat(flattened.get(3), is(childTwo));
    assertThat(flattened.get(4), is(sibling));
  }
}

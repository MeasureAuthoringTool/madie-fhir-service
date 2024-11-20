package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FhirResourceHelpersTest {
  @Test
  void testexpectedInverseValue() {
    assertEquals(1, FhirResourceHelpers.getExpectedInverseValue(0));
    assertEquals(0, FhirResourceHelpers.getExpectedInverseValue(1));
  }

  @Test
  void testBuildStratumPopulationForValueIndexOfTrue() {
    TestCaseStratificationValue stratValue1 =
        TestCaseStratificationValue.builder().name("Strata-1").expected(1).build();
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

    List<MeasureReport.StratifierGroupPopulationComponent> stratifierGroupPopulationComponents =
        FhirResourceHelpers.buildStratumPopulation(stratValue1, true, true);

    assertEquals(stratifierGroupPopulationComponents.size(), 3);
    assertEquals(
        stratifierGroupPopulationComponents.get(0).getCode().getCoding().get(0).getCode(),
        "initial-population");
    assertEquals(stratifierGroupPopulationComponents.get(0).getCount(), 1);
    assertEquals(
        stratifierGroupPopulationComponents.get(1).getCode().getCoding().get(0).getCode(),
        "denominator");
    assertEquals(stratifierGroupPopulationComponents.get(1).getCount(), 1);
    assertEquals(
        stratifierGroupPopulationComponents.get(2).getCode().getCoding().get(0).getCode(),
        "numerator");
    assertEquals(stratifierGroupPopulationComponents.get(2).getCount(), 0);
  }

  @Test
  void testBuildStratumPopulationForValueIndexOfFalse() {
    TestCaseStratificationValue stratValue1 =
        TestCaseStratificationValue.builder().name("Strata-1").expected(1).build();
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

    List<MeasureReport.StratifierGroupPopulationComponent> stratifierGroupPopulationComponents =
        FhirResourceHelpers.buildStratumPopulation(stratValue1, false, true);

    assertEquals(stratifierGroupPopulationComponents.size(), 3);
    assertEquals(
        stratifierGroupPopulationComponents.get(0).getCode().getCoding().get(0).getCode(),
        "initial-population");
    assertEquals(stratifierGroupPopulationComponents.get(0).getCount(), 0);
    assertEquals(
        stratifierGroupPopulationComponents.get(1).getCode().getCoding().get(0).getCode(),
        "denominator");
    assertEquals(stratifierGroupPopulationComponents.get(1).getCount(), 0);
    assertEquals(
        stratifierGroupPopulationComponents.get(2).getCode().getCoding().get(0).getCode(),
        "numerator");
    assertEquals(stratifierGroupPopulationComponents.get(2).getCount(), 1);
  }

  @Test
  void testBuildStratumPopulationForNonPatientBasedMeasures() {
    TestCaseStratificationValue stratValue1 =
        TestCaseStratificationValue.builder().name("Strata-1").expected(1).build();
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

    List<MeasureReport.StratifierGroupPopulationComponent> stratifierGroupPopulationComponents =
        FhirResourceHelpers.buildStratumPopulation(stratValue1, null, false);

    assertEquals(stratifierGroupPopulationComponents.size(), 3);
    assertEquals(
        stratifierGroupPopulationComponents.get(0).getCode().getCoding().get(0).getCode(),
        "initial-population");
    assertEquals(stratifierGroupPopulationComponents.get(0).getCount(), 5);
    assertEquals(
        stratifierGroupPopulationComponents.get(1).getCode().getCoding().get(0).getCode(),
        "denominator");
    assertEquals(stratifierGroupPopulationComponents.get(1).getCount(), 4);
    assertEquals(
        stratifierGroupPopulationComponents.get(2).getCode().getCoding().get(0).getCode(),
        "numerator");
    assertEquals(stratifierGroupPopulationComponents.get(2).getCount(), 2);
  }
}

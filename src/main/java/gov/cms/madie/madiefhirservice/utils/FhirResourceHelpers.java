package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.TestCasePopulationValue;
import gov.cms.madie.models.measure.TestCaseStratificationValue;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FhirResourceHelpers {
  private static String madieUrl;

  @Value("${madie.url}")
  public void setMadieUrl(String url) {
    FhirResourceHelpers.madieUrl = url;
  }

  public static Bundle.BundleEntryComponent getBundleEntryComponent(
      Resource resource, String bundleType) {
    Bundle.BundleEntryComponent entryComponent =
        new Bundle.BundleEntryComponent()
            .setFullUrl(buildResourceFullUrl(resource.fhirType(), resource.getIdPart()))
            .setResource(resource);
    // for the transaction bundles, add request object to the entry
    if ("Transaction".equalsIgnoreCase(bundleType)) {
      // We want to enforce PUT request temporarily
      // https://oncprojectracking.healthit.gov/support/browse/BONNIEMAT-1877
      if (resource.getResourceType().toString().equals("MeasureReport")) {
        setRequestForResourceEntry(resource, entryComponent, Bundle.HTTPVerb.PUT);
      } else {
        setRequestForResourceEntry(resource, entryComponent, Bundle.HTTPVerb.POST);
      }
    }
    return entryComponent;
  }

  public static void setRequestForResourceEntry(
      Resource resource, Bundle.BundleEntryComponent entryComponent, Bundle.HTTPVerb httpMethod) {
    Bundle.BundleEntryRequestComponent requestComponent =
        new Bundle.BundleEntryRequestComponent()
            .setMethod(httpMethod)
            .setUrl(resource.getResourceType() + "/" + resource.getIdPart());
    entryComponent.setRequest(requestComponent);
  }

  public static Period getPeriodFromDates(Date startDate, Date endDate) {
    return new Period()
        .setStart(startDate, TemporalPrecisionEnum.DAY)
        .setEnd(endDate, TemporalPrecisionEnum.DAY);
  }

  public static CodeableConcept buildCodeableConcept(String code, String system, String display) {
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setCoding(new ArrayList<>());
    codeableConcept.getCoding().add(buildCoding(code, system, display));
    return codeableConcept;
  }

  /**
   * @param expectedValue expected value for a population, can be a string or boolean
   * @return an equivalent integer
   */
  public static int getExpectedValue(Object expectedValue) {
    if (expectedValue == null || StringUtils.isBlank(expectedValue.toString())) {
      return -1; // To be excluded
    } else if (expectedValue instanceof Boolean) {
      return (Boolean) expectedValue ? 1 : 0;
    } else {
      return Integer.parseInt(expectedValue.toString());
    }
  }

  public static int getExpectedInverseValue(int expectedValue) {
    if (expectedValue == 0) {
      return 1;
    }
    return 0;
  }

  public static List<MeasureReport.StratifierGroupPopulationComponent> buildStratumPopulation(
      TestCaseStratificationValue testCaseStratificationValue,
      Boolean valueIndex,
      boolean isPatientBased,
      Group group) {
    var measureTestCaseStratificationComponents =
        testCaseStratificationValue.getPopulationValues().stream()
            .map(
                populationValue -> {
                  MeasureReport.StratifierGroupPopulationComponent
                      stratifierGroupPopulationComponent =
                          new MeasureReport.StratifierGroupPopulationComponent();

                  stratifierGroupPopulationComponent.setId(
                      getGroupPopulationDisplayId(group, populationValue.getId()));
                  stratifierGroupPopulationComponent.setCode(
                      buildCodeableConcept(
                          populationValue.getName().toCode(),
                          UriConstants.CodeSystem.POPULATION_SYSTEM_URI,
                          populationValue.getName().getDisplay()));

                  // Exclude the count attribute when Expected Value is empty
                  if (getExpectedValue(populationValue.getExpected()) >= 0) {
                    if (isPatientBased) {
                      stratifierGroupPopulationComponent.setCount(
                          valueIndex
                              ? getExpectedValue(populationValue.getExpected())
                              : getExpectedInverseValue(
                                  getExpectedValue(populationValue.getExpected())));
                    } else {
                      stratifierGroupPopulationComponent.setCount(
                          Integer.parseInt(populationValue.getExpected().toString()));
                    }
                  }

                  return stratifierGroupPopulationComponent;
                })
            .collect(Collectors.toList());
    return measureTestCaseStratificationComponents;
  }

  public static Coding buildCoding(String code, String system, String display) {
    return new Coding().setCode(code).setSystem(system).setDisplay(display);
  }

  public static String buildResourceFullUrl(String resourceType, String resourceName) {
    return madieUrl + "/" + resourceType + "/" + resourceName;
  }

  public static String getGroupPopulationDisplayId(Group group, String popId) {
    String popDisplayId = popId;
    if (!CollectionUtils.isEmpty(group.getPopulations())) {
      Optional<gov.cms.madie.models.measure.Population> population =
          group.getPopulations().stream().filter(pop -> popId.equals(pop.getId())).findFirst();
      if (population.isPresent()) {
        popDisplayId = population.get().getDisplayId();
      }
    }
    return popDisplayId;
  }

  public static String getGroupObservationDisplayId(
      Group group,
      TestCasePopulationValue populationValue,
      int observationCount,
      boolean patientBased) {
    return Optional.ofNullable(group.getMeasureObservations()).stream()
        .flatMap(Collection::stream)
        .filter(
            obs ->
                StringUtils.equals(
                    obs.getCriteriaReference(), populationValue.getCriteriaReference()))
        .findFirst()
        .map(o -> o.getDisplayId() + (patientBased ? "" : "_" + observationCount))
        .orElse(populationValue.getId());
  }

  public static boolean isTestCaseObservation(PopulationType populationType) {
    return populationType == PopulationType.MEASURE_OBSERVATION
        || populationType == PopulationType.MEASURE_POPULATION_OBSERVATION
        || populationType == PopulationType.DENOMINATOR_OBSERVATION
        || populationType == PopulationType.NUMERATOR_OBSERVATION;
  }
}

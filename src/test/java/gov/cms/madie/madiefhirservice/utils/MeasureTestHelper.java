package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.context.FhirContext;
import tools.jackson.databind.ObjectMapper;
import gov.cms.madie.models.measure.Measure;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public class MeasureTestHelper {

  public static Measure createMadieMeasureFromJson(String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(json, Measure.class);
  }

  public static <T extends Resource> T createFhirResourceFromJson(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return FhirContext.forR4().newJsonParser().parseResource(clazz, json);
  }

  public static Bundle createTestMeasureBundle() {
    org.hl7.fhir.r4.model.Measure measure = new org.hl7.fhir.r4.model.Measure();
    measure
        .setName("TestCMS0001")
        .setTitle("TestTitle001")
        .setExperimental(false)
        .setUrl("/Measure/TestCMS0001")
        .setPublisher("CMS")
        .setCopyright("CMS copyright")
        .setVersion("0.0.001");
    return new Bundle()
        .setType(Bundle.BundleType.TRANSACTION)
        .addEntry(new Bundle.BundleEntryComponent().setResource(measure));
  }
}

package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.madie.models.measure.Measure;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public class MeasureTestHelper {

  public static Measure createMadieMeasureFromJson(String json) throws JsonProcessingException {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
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

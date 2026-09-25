package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.context.FhirContext;
import tools.jackson.databind.ObjectMapper;
import gov.cms.madie.models.measure.Measure;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Resource;

import java.nio.charset.StandardCharsets;

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

  /**
   * Builds a measure Bundle containing an included CQL Library resource with the given version, to
   * exercise the ballot-version fix: a library version such as "2.0.0-ballot" should no longer
   * cause export generation to fail.
   */
  public static Bundle createTestMeasureBundleWithLibrary(
      String libraryName, String libraryVersion, String cql) {
    org.hl7.fhir.r4.model.Measure measure = new org.hl7.fhir.r4.model.Measure();
    measure
        .setName("TestCMS0001")
        .setTitle("TestTitle001")
        .setExperimental(false)
        .setUrl("/Measure/TestCMS0001")
        .setPublisher("CMS")
        .setCopyright("CMS copyright")
        .setVersion("0.0.001");
    Narrative narrative = new Narrative().setStatus(Narrative.NarrativeStatus.GENERATED);
    narrative.setDivAsString("<div xmlns=\"http://www.w3.org/1999/xhtml\">Test</div>");
    measure.setText(narrative);

    Library library = new Library();
    library.setName(libraryName);
    library.setVersion(libraryVersion);
    Attachment attachment = new Attachment();
    attachment.setContentType("text/cql");
    attachment.setData(cql.getBytes(StandardCharsets.UTF_8));
    library.addContent(attachment);

    return new Bundle()
        .setType(Bundle.BundleType.TRANSACTION)
        .addEntry(new Bundle.BundleEntryComponent().setResource(measure))
        .addEntry(new Bundle.BundleEntryComponent().setResource(library));
  }
}

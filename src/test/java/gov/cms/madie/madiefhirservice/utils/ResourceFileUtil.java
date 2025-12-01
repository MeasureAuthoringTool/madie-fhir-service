package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Resource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Objects;

public interface ResourceFileUtil {
  default String getStringFromTestResource(String resource) {
    File file = new File(Objects.requireNonNull(this.getClass().getResource(resource)).getFile());

    try {
      return new String(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  default <T extends Resource> T convertToFhirR4Resource(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return FhirContext.forR4().newJsonParser().parseResource(clazz, json);
  }

  default <T extends org.hl7.fhir.r5.model.Resource> T convertToFhirR5Resource(
      Class<T> clazz, String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return FhirContext.forR5().newJsonParser().parseResource(clazz, json);
  }
}

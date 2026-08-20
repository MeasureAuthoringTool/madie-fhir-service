package gov.cms.madie.madiefhirservice.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
public final class TranslatorConfigUtil {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String CQL_TO_ELM_INFO_TYPE = "CqlToElmInfo";

  public static Extension getCqlOptionExtension() {
    return new Extension()
        .setUrl(UriConstants.Library.CQL_OPTIONS_URL)
        .setValue(new Reference("#options"));
  }

  public static Parameters getCqlOptionParameters(String elmJson) {
    Parameters parameters = new Parameters();
    parameters.setId("options");
    if (elmJson != null) {
      // Add translator options to parameters
      TranslatorConfig translatorConfig = getTranslationConfigs(elmJson);
      if (translatorConfig.translatorOptions() != null
          && !translatorConfig.translatorOptions().isEmpty()) {
        parameters
            .addParameter()
            .setName("translatorVersion")
            .setValue(new StringType(translatorConfig.translatorVersion()));
        translatorConfig
            .translatorOptions()
            .forEach(
                option ->
                    parameters.addParameter().setName("option").setValue(new StringType(option)));
        parameters
            .addParameter()
            .setName("signatureLevel")
            .setValue(new StringType(translatorConfig.signatureLevel()));
      }
      // because resource contains both JSON and CML format
      parameters.addParameter().setName("format").setValue(new StringType("JSON"));
      parameters.addParameter().setName("format").setValue(new StringType("XML"));
    }
    return parameters;
  }

  public static TranslatorConfig getTranslationConfigs(String elmJson) {
    if (StringUtils.isBlank(elmJson)) {
      return TranslatorConfig.empty();
    }

    try {
      JsonNode annotations = OBJECT_MAPPER.readTree(elmJson).path("library").path("annotation");
      if (!annotations.isArray()) {
        return TranslatorConfig.empty();
      }

      return StreamSupport.stream(annotations.spliterator(), false)
          .filter(annotation -> CQL_TO_ELM_INFO_TYPE.equals(annotation.path("type").asText()))
          .findFirst()
          .map(
              annotation ->
                  new TranslatorConfig(
                      textOrNull(annotation, "translatorVersion"),
                      parseTranslatorOptions(annotation.path("translatorOptions").asText("")),
                      textOrNull(annotation, "signatureLevel")))
          .orElseGet(TranslatorConfig::empty);
    } catch (IOException exception) {
      log.warn("Unable to parse ELM JSON translator configuration.", exception);
    }

    return TranslatorConfig.empty();
  }

  private static List<String> parseTranslatorOptions(String translatorOptions) {
    if (StringUtils.isBlank(translatorOptions)) {
      return List.of();
    }

    return Arrays.stream(translatorOptions.split(","))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .toList();
  }

  private static String textOrNull(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText(null);
    return StringUtils.isBlank(value) ? null : value.trim();
  }

  public record TranslatorConfig(
      String translatorVersion, List<String> translatorOptions, String signatureLevel) {
    public static TranslatorConfig empty() {
      return new TranslatorConfig(null, List.of(), null);
    }
  }
}

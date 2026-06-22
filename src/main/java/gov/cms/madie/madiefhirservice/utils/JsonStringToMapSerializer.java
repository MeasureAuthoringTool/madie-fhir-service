package gov.cms.madie.madiefhirservice.utils;

import java.util.Map;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class JsonStringToMapSerializer extends ValueSerializer<String> {
  @Override
  public void serialize(String value, JsonGenerator gen, SerializationContext serializers) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> map = mapper.readValue(value, Map.class);
    gen.writePOJO(map);
  }
}

package gov.cms.madie.madiefhirservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.madie.madiefhirservice.utils.JsonStringToMapSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StructureDefinitionDto {
  private String resourceName;
  private String category;
  private String primaryCodePath;

  @JsonSerialize(using = JsonStringToMapSerializer.class)
  private String definition;
}

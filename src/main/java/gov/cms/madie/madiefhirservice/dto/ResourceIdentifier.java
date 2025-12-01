package gov.cms.madie.madiefhirservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ResourceIdentifier {
  private String id;
  private String title;
  private String type;
  private String category;
  private String profile;
}

package gov.cms.madie.madiefhirservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderResourceMetadata {
  private List<String> resourcePaths;
  private ResourceIdentifier primaryPatientProfile;
}

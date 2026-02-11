package gov.cms.madie.madiefhirservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ExecutionBundleDTO {
  private String bundle;
  private String testCaseId;
}

package gov.cms.madie.madiefhirservice.dto;

import gov.cms.madie.models.measure.TestCase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class TestCaseExecutionBundlesDTO {
  private List<TestCase> testCases;
  private List<String> modifiedTestCaseIds;
}

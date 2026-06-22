package gov.cms.madie.madiefhirservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CqlLibraryDetails {
  private String cql;
  private String libraryName;
  private Set<String> expressions;
}

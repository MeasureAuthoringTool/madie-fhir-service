package gov.cms.madie.madiefhirservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CqlLibraryDetails {
  private String cql;
  private String libraryName;
  private Set<String> expressions;
}

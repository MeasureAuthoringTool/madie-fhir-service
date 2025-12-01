package gov.cms.madie.madiefhirservice.cql;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class ReadableArtifacts {
  private Set<CodeModel> terminologyCodeModels = new HashSet<>();
  private Set<ValuesetModel> terminologyValueSetModels = new HashSet<>();
  private Set<CodeModel> dataReqCodes = new HashSet<>();
  private Set<TypeModel> dataReqTypes = new HashSet<>();

  private Set<ValuesetModel> dataReqValueSets = new HashSet<>();
}

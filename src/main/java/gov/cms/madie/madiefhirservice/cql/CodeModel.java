package gov.cms.madie.madiefhirservice.cql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CodeModel {
  private String name;
  private String oid;
  private String codeSystemOid;
  private String codesystemName;
  private String codesystemVersion;
  private boolean isCodesystemVersionIncluded;
  private String datatype;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CodeModel that = (CodeModel) o;

    if (!oid.equals(that.oid)) {
      return false;
    }
    if (!codesystemName.equals(that.codesystemName)) {
      return false;
    }
    return codesystemVersion != null
        ? codesystemVersion.equals(that.codesystemVersion)
        : that.codesystemVersion == null;
  }

  @Override
  public int hashCode() {
    int result = oid.hashCode();
    result = 31 * result + codesystemName.hashCode();
    result = 31 * result + (codesystemVersion != null ? codesystemVersion.hashCode() : 0);
    return result;
  }
}

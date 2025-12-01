package gov.cms.madie.madiefhirservice.cql;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValuesetModel {
  private String name;
  private String oid;
  private String version;
  private String datatype;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ValuesetModel that = (ValuesetModel) o;

    if (oid != null ? !oid.equals(that.oid) : that.oid != null) {
      return false;
    }
    return version != null ? version.equals(that.version) : that.version == null;
  }

  @Override
  public int hashCode() {
    int result = oid != null ? oid.hashCode() : 0;
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }
}

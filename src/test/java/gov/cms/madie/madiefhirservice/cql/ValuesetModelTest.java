package gov.cms.madie.madiefhirservice.cql;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValuesetModelTest {

  @Test
  void testValuesetModelAllArgsConstructor() {
    ValuesetModel valuesetModel = new ValuesetModel("Test Valueset", "1.2.3.4", "1.0", "Condition");

    assertThat(valuesetModel.getName(), is(equalTo("Test Valueset")));
    assertThat(valuesetModel.getOid(), is(equalTo("1.2.3.4")));
    assertThat(valuesetModel.getVersion(), is(equalTo("1.0")));
    assertThat(valuesetModel.getDatatype(), is(equalTo("Condition")));
  }

  @Test
  void testValuesetModelSettersAndGetters() {
    ValuesetModel valuesetModel = new ValuesetModel();
    valuesetModel.setName("Test Valueset");
    valuesetModel.setOid("1.2.3.4");
    valuesetModel.setVersion("1.0");
    valuesetModel.setDatatype("Condition");

    assertThat(valuesetModel.getName(), is(equalTo("Test Valueset")));
    assertThat(valuesetModel.getOid(), is(equalTo("1.2.3.4")));
    assertThat(valuesetModel.getVersion(), is(equalTo("1.0")));
    assertThat(valuesetModel.getDatatype(), is(equalTo("Condition")));
  }

  @Test
  void testValuesetModelEqualsReturnsTrueForSameObject() {
    ValuesetModel valuesetModel = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    assertThat(valuesetModel.equals(valuesetModel), is(true));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseForNull() {
    ValuesetModel valuesetModel = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    assertFalse(valuesetModel.equals(null));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseForDifferentClass() {
    ValuesetModel valuesetModel = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    assertThat(valuesetModel.equals("Different Class"), is(false));
  }

  @Test
  void testValuesetModelEqualsReturnsTrueForSameOidAndVersion() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test1", "1.2.3.4", "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test2", "1.2.3.4", "1.0", "Observation");

    assertThat(valuesetModel1.equals(valuesetModel2), is(true));
    assertThat(valuesetModel1.hashCode(), is(equalTo(valuesetModel2.hashCode())));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseForDifferentOid() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.5", "1.0", "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(false));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseForDifferentVersion() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.4", "2.0", "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(false));
  }

  @Test
  void testValuesetModelEqualsReturnsTrueForBothNullOids() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", null, "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", null, "1.0", "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(true));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseWhenOneOidIsNull() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", null, "1.0", "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(false));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseWhenOtherOidIsNull() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", null, "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(false));
  }

  @Test
  void testValuesetModelEqualsReturnsTrueForBothNullVersions() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", null, "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.4", null, "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(true));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseWhenOneVersionIsNull() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.4", null, "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(false));
  }

  @Test
  void testValuesetModelEqualsReturnsFalseWhenOtherVersionIsNull() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", null, "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");

    assertThat(valuesetModel1.equals(valuesetModel2), is(false));
  }

  @Test
  void testValuesetModelHashCodeIsSameForEqualObjects() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test1", "1.2.3.4", "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test2", "1.2.3.4", "1.0", "Observation");

    assertThat(valuesetModel1.hashCode(), is(equalTo(valuesetModel2.hashCode())));
  }

  @Test
  void testValuesetModelHashCodeIsDifferentForDifferentOids() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.5", "1.0", "Condition");

    assertThat(valuesetModel1.hashCode(), is(not(equalTo(valuesetModel2.hashCode()))));
  }

  @Test
  void testValuesetModelHashCodeWithNullOid() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", null, "1.0", "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", null, "1.0", "Condition");

    assertThat(valuesetModel1.hashCode(), is(equalTo(valuesetModel2.hashCode())));
  }

  @Test
  void testValuesetModelHashCodeWithNullVersion() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", "1.2.3.4", null, "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", "1.2.3.4", null, "Condition");

    assertThat(valuesetModel1.hashCode(), is(equalTo(valuesetModel2.hashCode())));
  }

  @Test
  void testValuesetModelHashCodeWithBothFieldsNull() {
    ValuesetModel valuesetModel1 = new ValuesetModel("Test", null, null, "Condition");
    ValuesetModel valuesetModel2 = new ValuesetModel("Test", null, null, "Condition");

    assertThat(valuesetModel1.hashCode(), is(equalTo(valuesetModel2.hashCode())));
  }
}

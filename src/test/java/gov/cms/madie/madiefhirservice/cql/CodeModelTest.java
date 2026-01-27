package gov.cms.madie.madiefhirservice.cql;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CodeModelTest {

  @Test
  void testCodeModelBuilder() {
    CodeModel codeModel =
        CodeModel.builder()
            .name("Test Code")
            .oid("1.2.3.4")
            .codeSystemOid("1.2.3")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .isCodesystemVersionIncluded(true)
            .datatype("Condition")
            .build();

    assertThat(codeModel, is(notNullValue()));
    assertThat(codeModel.getName(), is(equalTo("Test Code")));
    assertThat(codeModel.getOid(), is(equalTo("1.2.3.4")));
    assertThat(codeModel.getCodeSystemOid(), is(equalTo("1.2.3")));
    assertThat(codeModel.getCodesystemName(), is(equalTo("Test System")));
    assertThat(codeModel.getCodesystemVersion(), is(equalTo("1.0")));
    assertThat(codeModel.isCodesystemVersionIncluded(), is(true));
    assertThat(codeModel.getDatatype(), is(equalTo("Condition")));
  }

  @Test
  void testCodeModelEqualsReturnsTrueForSameObject() {
    CodeModel codeModel =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    assertThat(codeModel.equals(codeModel), is(true));
  }

  @Test
  void testCodeModelEqualsReturnsFalseForNull() {
    CodeModel codeModel =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    assertFalse(codeModel.equals(null));
  }

  @Test
  void testCodeModelEqualsReturnsFalseForDifferentClass() {
    CodeModel codeModel =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    assertThat(codeModel.equals("Different Class"), is(false));
  }

  @Test
  void testCodeModelEqualsReturnsTrueForSameValues() {
    CodeModel codeModel1 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    CodeModel codeModel2 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    assertThat(codeModel1.equals(codeModel2), is(true));
    assertThat(codeModel1.hashCode(), is(equalTo(codeModel2.hashCode())));
  }

  @Test
  void testCodeModelEqualsReturnsFalseForDifferentOid() {
    CodeModel codeModel1 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    CodeModel codeModel2 =
        CodeModel.builder()
            .oid("1.2.3.5")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    assertThat(codeModel1.equals(codeModel2), is(false));
  }

  @Test
  void testCodeModelEqualsReturnsFalseForDifferentCodesystemName() {
    CodeModel codeModel1 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    CodeModel codeModel2 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Different System")
            .codesystemVersion("1.0")
            .build();

    assertThat(codeModel1.equals(codeModel2), is(false));
  }

  @Test
  void testCodeModelEqualsReturnsTrueForBothNullVersions() {
    CodeModel codeModel1 = CodeModel.builder().oid("1.2.3.4").codesystemName("Test System").build();

    CodeModel codeModel2 = CodeModel.builder().oid("1.2.3.4").codesystemName("Test System").build();

    assertThat(codeModel1.equals(codeModel2), is(true));
  }

  @Test
  void testCodeModelEqualsReturnsFalseForOneNullVersion() {
    CodeModel codeModel1 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    CodeModel codeModel2 = CodeModel.builder().oid("1.2.3.4").codesystemName("Test System").build();

    assertThat(codeModel1.equals(codeModel2), is(false));
  }

  @Test
  void testCodeModelEqualsReturnsFalseForDifferentVersions() {
    CodeModel codeModel1 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    CodeModel codeModel2 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("2.0")
            .build();

    assertThat(codeModel1.equals(codeModel2), is(false));
  }

  @Test
  void testCodeModelHashCodeIsSameForEqualObjects() {
    CodeModel codeModel1 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    CodeModel codeModel2 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    assertThat(codeModel1.hashCode(), is(equalTo(codeModel2.hashCode())));
  }

  @Test
  void testCodeModelHashCodeIsDifferentForDifferentObjects() {
    CodeModel codeModel1 =
        CodeModel.builder()
            .oid("1.2.3.4")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    CodeModel codeModel2 =
        CodeModel.builder()
            .oid("1.2.3.5")
            .codesystemName("Test System")
            .codesystemVersion("1.0")
            .build();

    assertThat(codeModel1.hashCode(), is(not(equalTo(codeModel2.hashCode()))));
  }

  @Test
  void testCodeModelHashCodeWithNullVersion() {
    CodeModel codeModel1 = CodeModel.builder().oid("1.2.3.4").codesystemName("Test System").build();

    CodeModel codeModel2 = CodeModel.builder().oid("1.2.3.4").codesystemName("Test System").build();

    assertThat(codeModel1.hashCode(), is(equalTo(codeModel2.hashCode())));
  }

  @Test
  void testCodeModelSettersAndGetters() {
    CodeModel codeModel = new CodeModel();
    codeModel.setName("Test Code");
    codeModel.setOid("1.2.3.4");
    codeModel.setCodeSystemOid("1.2.3");
    codeModel.setCodesystemName("Test System");
    codeModel.setCodesystemVersion("1.0");
    codeModel.setCodesystemVersionIncluded(true);
    codeModel.setDatatype("Condition");

    assertThat(codeModel.getName(), is(equalTo("Test Code")));
    assertThat(codeModel.getOid(), is(equalTo("1.2.3.4")));
    assertThat(codeModel.getCodeSystemOid(), is(equalTo("1.2.3")));
    assertThat(codeModel.getCodesystemName(), is(equalTo("Test System")));
    assertThat(codeModel.getCodesystemVersion(), is(equalTo("1.0")));
    assertThat(codeModel.isCodesystemVersionIncluded(), is(true));
    assertThat(codeModel.getDatatype(), is(equalTo("Condition")));
  }

  @Test
  void testCodeModelAllArgsConstructor() {
    CodeModel codeModel =
        new CodeModel("Test Code", "1.2.3.4", "1.2.3", "Test System", "1.0", true, "Condition");

    assertThat(codeModel.getName(), is(equalTo("Test Code")));
    assertThat(codeModel.getOid(), is(equalTo("1.2.3.4")));
    assertThat(codeModel.getCodeSystemOid(), is(equalTo("1.2.3")));
    assertThat(codeModel.getCodesystemName(), is(equalTo("Test System")));
    assertThat(codeModel.getCodesystemVersion(), is(equalTo("1.0")));
    assertThat(codeModel.isCodesystemVersionIncluded(), is(true));
    assertThat(codeModel.getDatatype(), is(equalTo("Condition")));
  }
}

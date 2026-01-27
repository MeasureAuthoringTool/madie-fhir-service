package gov.cms.madie.madiefhirservice.cql;

import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class LibraryCqlVisitorTest implements ResourceFileUtil {
  private String cql;

  @BeforeEach
  void setUp() {
    cql = ResourceUtils.getData("/test-cql/cv_populations.cql");
  }

  @Test
  void testGetNameVersionFromInclude() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    List<Pair<String, String>> includedLibs = cqlVisitor.getIncludedLibraries();
    assertThat(includedLibs.size(), is(equalTo(3)));
    assertThat(includedLibs.get(0).getLeft(), is(equalTo("FHIRHelpers")));
    assertThat(includedLibs.get(1).getLeft(), is(equalTo("SupplementalDataElementsFHIR4")));
  }

  @Test
  void testLibraryNameAndVersion() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getName(), is(notNullValue()));
    assertThat(cqlVisitor.getVersion(), is(notNullValue()));
  }

  @Test
  void testValueSetsAreParsed() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getValueSets(), is(notNullValue()));
    assertThat(cqlVisitor.getValueSets().size(), is(greaterThan(0)));
  }

  @Test
  void testRelatedArtifactsIncludesValueSets() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getRelatedArtifacts(), is(notNullValue()));
    assertThat(cqlVisitor.getRelatedArtifacts().size(), is(greaterThan(0)));
  }

  @Test
  void testDataRequirementsAreParsed() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getDataRequirements(), is(notNullValue()));
  }

  @Test
  void testReadableArtifacts() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getReadableArtifacts(), is(notNullValue()));
    assertThat(
        cqlVisitor.getReadableArtifacts().getTerminologyValueSetModels(), is(notNullValue()));
  }

  @Test
  void testCodeSystemsAreParsed() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getCodeSystems(), is(notNullValue()));
  }

  @Test
  void testCodesAreParsed() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getCodes(), is(notNullValue()));
  }

  @Test
  void testDrcExtensions() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getDrcExtensions(), is(notNullValue()));
  }

  @Test
  void testValueSetNameUri() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    assertThat(cqlVisitor.getValueSetNameUri(), is(notNullValue()));
  }

  @Test
  void testGetValueSetUrlForExistingValueSet() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    // Try to get a valueset that should exist in the CQL
    if (!cqlVisitor.getReadableArtifacts().getTerminologyValueSetModels().isEmpty()) {
      String vsName =
          cqlVisitor.getReadableArtifacts().getTerminologyValueSetModels().stream()
              .findFirst()
              .get()
              .getName();
      ValuesetModel result = cqlVisitor.getValueSetUrl(vsName);
      assertThat(result, is(notNullValue()));
    }
  }

  @Test
  void testGetValueSetUrlForNonExistentValueSet() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    ValuesetModel result = cqlVisitor.getValueSetUrl("NonExistentValueSet");
    assertThat(result, is(nullValue()));
  }

  @Test
  void testGetValueSetUrlWithLibraryReference() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    // Test with library reference format (e.g., "TJC.valueset")
    ValuesetModel result = cqlVisitor.getValueSetUrl("SomeLib.SomeValueSet");
    assertThat(result, is(nullValue())); // Should return null as lib references are commented out
  }

  @Test
  void testGetCodeForExistingCode() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    // Try to get a code that should exist in the CQL
    if (!cqlVisitor.getReadableArtifacts().getTerminologyCodeModels().isEmpty()) {
      String codeName =
          cqlVisitor.getReadableArtifacts().getTerminologyCodeModels().stream()
              .findFirst()
              .get()
              .getName();
      CodeModel result = cqlVisitor.getCode(codeName);
      assertThat(result, is(notNullValue()));
    }
  }

  @Test
  void testGetCodeForNonExistentCode() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    CodeModel result = cqlVisitor.getCode("NonExistentCode");
    assertThat(result, is(nullValue()));
  }

  @Test
  void testGetCodeWithLibraryReference() {
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cql);
    // Test with library reference format (e.g., "TJC.code")
    CodeModel result = cqlVisitor.getCode("SomeLib.SomeCode");
    assertThat(result, is(nullValue())); // Should return null as lib references are commented out
  }

  @Test
  void testEmptyLibrary() {
    String emptyCql = "library TestLib version '1.0.0'";
    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(emptyCql);
    assertThat(cqlVisitor.getIncludes(), is(empty()));
    assertThat(cqlVisitor.getValueSets(), is(empty()));
    assertThat(cqlVisitor.getCodes(), is(empty()));
    assertThat(cqlVisitor.getCodeSystems(), is(empty()));
  }

  @Test
  void testLibraryWithCodeSystemAndCode() {
    String cqlWithCode =
        """
        library TestLib version '1.0.0'

        codesystem "LOINC": 'http://loinc.org' version '2.73'

        code "Birth date": '21112-8' from "LOINC" display 'Birth date'
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithCode);
    assertThat(cqlVisitor.getCodeSystems().size(), is(1));
    assertThat(cqlVisitor.getCodes().size(), is(1));
    assertThat(cqlVisitor.getDrcExtensions().size(), is(1));
  }

  @Test
  void testLibraryWithCodeSystemWithoutVersion() {
    String cqlWithCodeSystem =
        """
        library TestLib version '1.0.0'

        codesystem "SNOMEDCT": 'http://snomed.info/sct'
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithCodeSystem);
    assertThat(cqlVisitor.getCodeSystems().size(), is(1));
    assertThat(cqlVisitor.getRelatedArtifacts().size(), is(greaterThan(0)));
  }

  @Test
  void testLibraryWithValueSet() {
    String cqlWithValueSet =
        """
        library TestLib version '1.0.0'

        valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithValueSet);
    assertThat(cqlVisitor.getValueSets().size(), is(1));
    assertThat(cqlVisitor.getReadableArtifacts().getTerminologyValueSetModels().size(), is(1));
    assertThat(cqlVisitor.getRelatedArtifacts().size(), is(greaterThan(0)));
  }

  @Test
  void testLibraryWithInvalidCodeReference() {
    String cqlWithInvalidCode =
        """
        library TestLib version '1.0.0'

        code "Test Code": '12345' from "NonExistentCodeSystem" display 'Test'
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithInvalidCode);
    // Should still parse but log error - codes list should have 1 entry
    assertThat(cqlVisitor.getCodes().size(), is(1));
    // But drcExtensions should be empty because code system doesn't exist
    assertThat(cqlVisitor.getDrcExtensions().size(), is(0));
  }

  @Test
  void testLibraryWithRetrieve() {
    String cqlWithRetrieve =
        """
        library TestLib version '1.0.0'

        valueset "Test VS": 'http://example.com/vs'

        define "TestDef":
          [Encounter: "Test VS"]
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithRetrieve);
    assertThat(cqlVisitor.getDataRequirements().size(), is(greaterThan(0)));
  }

  @Test
  void testLibraryWithTypeRetrieve() {
    String cqlWithTypeRetrieve =
        """
        library TestLib version '1.0.0'

        define "TestDef":
          [Encounter]
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithTypeRetrieve);
    assertThat(cqlVisitor.getDataRequirements().size(), is(greaterThan(0)));
  }

  @Test
  void testLibraryWithInclude() {
    String cqlWithInclude =
        """
        library TestLib version '1.0.0'

        include FHIRHelpers version '4.0.1' called FH
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithInclude);
    assertThat(cqlVisitor.getIncludes().size(), is(1));
    assertThat(cqlVisitor.getIncludedLibraries().size(), is(1));
    assertThat(cqlVisitor.getRelatedArtifacts().size(), is(greaterThan(0)));
  }

  @Test
  void testLibraryWithCodeSystemVersion() {
    String cqlWithCodeSystemVersion =
        """
        library TestLib version '1.0.0'

        codesystem "LOINC": 'http://loinc.org' version '2.73'

        code "Test": '12345' from "LOINC"
        """;

    LibraryCqlVisitor cqlVisitor = new LibraryCqlVisitorFactory().visit(cqlWithCodeSystemVersion);
    assertThat(cqlVisitor.getCodeSystems().size(), is(1));
    assertThat(cqlVisitor.getCodes().size(), is(1));

    // Verify the code model has version info
    CodeModel code =
        cqlVisitor.getReadableArtifacts().getTerminologyCodeModels().stream().findFirst().get();
    assertThat(code.getCodesystemVersion(), is(notNullValue()));
    assertTrue(code.isCodesystemVersionIncluded());
  }
}

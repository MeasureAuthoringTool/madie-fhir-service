package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class VersionAgnosticProfileValidationSupportTest {

  private static final String CANONICAL_URL =
      "http://hl7.org/fhir/StructureDefinition/Coverage|4.0.1";
  private static final String UNVERSIONED_CANONICAL =
      "http://hl7.org/fhir/StructureDefinition/Coverage";

  @Test
  void testGetName() {
    IValidationSupport delegate = mock(IValidationSupport.class);
    VersionAgnosticProfileValidationSupport support =
        new VersionAgnosticProfileValidationSupport(FhirContext.forR4(), delegate);

    String name = support.getName();

    assertThat(name).isEqualTo("Version-Agnostic Custom Profile Validation Support");
  }

  @Test
  void testFetchStructureDefinitionWhenCanonicalUrlIsBlank() {
    IValidationSupport delegate = mock(IValidationSupport.class);
    VersionAgnosticProfileValidationSupport support =
        new VersionAgnosticProfileValidationSupport(FhirContext.forR4(), delegate);

    StructureDefinition result = support.fetchStructureDefinition("");

    assertThat(result).isNull();
    verifyNoMoreInteractions(delegate);
  }

  @Test
  void shouldReturnVersionedProfileWhenDirectMatchExists() {
    IValidationSupport delegate = mock(IValidationSupport.class);
    StructureDefinition expected = new StructureDefinition();
    when(delegate.fetchStructureDefinition(CANONICAL_URL)).thenReturn(expected);

    VersionAgnosticProfileValidationSupport support =
        new VersionAgnosticProfileValidationSupport(FhirContext.forR4(), delegate);

    StructureDefinition result = support.fetchStructureDefinition(CANONICAL_URL);

    assertThat(result).isSameAs(expected);
    verify(delegate).fetchStructureDefinition(CANONICAL_URL);
    verifyNoMoreInteractions(delegate);
  }

  @Test
  void shouldFallbackToUnversionedProfileWhenVersionedProfileDoesNotExist() {
    IValidationSupport delegate = mock(IValidationSupport.class);
    StructureDefinition expected = new StructureDefinition();

    when(delegate.fetchStructureDefinition(CANONICAL_URL)).thenReturn(null);
    when(delegate.fetchStructureDefinition(UNVERSIONED_CANONICAL)).thenReturn(expected);

    VersionAgnosticProfileValidationSupport support =
        new VersionAgnosticProfileValidationSupport(FhirContext.forR4(), delegate);

    StructureDefinition result = support.fetchStructureDefinition(CANONICAL_URL);

    assertThat(result).isSameAs(expected);
    verify(delegate).fetchStructureDefinition(CANONICAL_URL);
    verify(delegate).fetchStructureDefinition(UNVERSIONED_CANONICAL);
  }

  @Test
  void shouldReturnNullWhenNoProfileCanBeResolved() {
    IValidationSupport delegate = mock(IValidationSupport.class);

    when(delegate.fetchStructureDefinition(CANONICAL_URL)).thenReturn(null);
    when(delegate.fetchStructureDefinition(UNVERSIONED_CANONICAL)).thenReturn(null);

    VersionAgnosticProfileValidationSupport support =
        new VersionAgnosticProfileValidationSupport(FhirContext.forR4(), delegate);

    StructureDefinition result = support.fetchStructureDefinition(CANONICAL_URL);

    assertThat(result).isNull();
    verify(delegate).fetchStructureDefinition(CANONICAL_URL);
    verify(delegate).fetchStructureDefinition(UNVERSIONED_CANONICAL);
  }
}

package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport.CodeValidationResult;
import ca.uhn.fhir.context.support.IValidationSupport.IssueSeverity;
import ca.uhn.fhir.context.support.IValidationSupport.LookupCodeResult;
import ca.uhn.fhir.context.support.LookupCodeRequest;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUnknownCodeSystemWarningValidationSupportTest {

  private CustomUnknownCodeSystemWarningValidationSupport validationSupport;
  private FhirContext fhirContext;

  @Mock private ValidationSupportContext validationSupportContext;
  @Mock private IValidationSupport rootValidationSupport;

  @BeforeEach
  void setUp() {
    fhirContext = FhirContext.forR4();
    validationSupport = new CustomUnknownCodeSystemWarningValidationSupport(fhirContext);
  }

  @Test
  void testConstructorSetsFhirContext() {
    assertThat(validationSupport.getFhirContext(), is(notNullValue()));
    assertThat(validationSupport.getFhirContext(), is(fhirContext));
  }

  @Test
  void testGetNameReturnsExpectedValue() {
    String name = validationSupport.getName();
    assertThat(name, containsString("Unknown Code System Warning Validation Support"));
  }


  @Test
  void testIsValueSetSupportedAlwaysReturnsTrue() {
    boolean result =
        validationSupport.isValueSetSupported(
            validationSupportContext, "http://example.com/ValueSet/test");
    assertThat(result, is(true));
  }

  @Test
  void testIsValueSetSupportedReturnsTrueForNullUrl() {
    boolean result = validationSupport.isValueSetSupported(validationSupportContext, null);
    assertThat(result, is(true));
  }

  @Test
  void testIsCodeSystemSupportedReturnsFalseWhenSeverityIsError() {
    // Default severity is ERROR, so allowNonExistentCodeSystems returns false early
    // No mocks needed - the method returns false without checking code system
    boolean result =
        validationSupport.isCodeSystemSupported(
            validationSupportContext, "http://example.com/CodeSystem/test");

    assertThat(result, is(false));
  }

  @Test
  void testIsCodeSystemSupportedReturnsTrueWhenSeverityIsWarning() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    boolean result =
        validationSupport.isCodeSystemSupported(
            validationSupportContext, "http://example.com/CodeSystem/test");

    assertThat(result, is(true));
  }

  @Test
  void testIsCodeSystemSupportedReturnsFalseWhenCodeSystemExists() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(new CodeSystem());

    boolean result =
        validationSupport.isCodeSystemSupported(
            validationSupportContext, "http://example.com/CodeSystem/test");

    assertThat(result, is(false));
  }

  @Test
  void testIsCodeSystemSupportedReturnsFalseForNullCodeSystem() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);

    boolean result = validationSupport.isCodeSystemSupported(validationSupportContext, null);

    assertThat(result, is(false));
  }

  @Test
  void testLookupCodeReturnsNullWhenSeverityIsError() {
    // Default severity is ERROR, so canValidateCodeSystem returns false early
    // No mocks needed - the method returns null without checking code system
    LookupCodeRequest request = new LookupCodeRequest("http://example.com/CodeSystem/test", "code");
    LookupCodeResult result = validationSupport.lookupCode(validationSupportContext, request);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testLookupCodeReturnsFoundWhenSeverityIsWarning() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    LookupCodeRequest request = new LookupCodeRequest("http://example.com/CodeSystem/test", "code");
    LookupCodeResult result = validationSupport.lookupCode(validationSupportContext, request);

    assertThat(result, is(notNullValue()));
    assertThat(result.isFound(), is(true));
  }

  @Test
  void testLookupCodeReturnsFoundWhenSeverityIsInformation() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.INFORMATION);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    LookupCodeRequest request = new LookupCodeRequest("http://example.com/CodeSystem/test", "code");
    LookupCodeResult result = validationSupport.lookupCode(validationSupportContext, request);

    assertThat(result, is(notNullValue()));
    assertThat(result.isFound(), is(true));
  }

  @Test
  void testLookupCodeReturnsNullWhenCodeSystemExists() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(new CodeSystem());

    LookupCodeRequest request = new LookupCodeRequest("http://example.com/CodeSystem/test", "code");
    LookupCodeResult result = validationSupport.lookupCode(validationSupportContext, request);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeReturnsNullWhenSeverityIsError() {
    // Default severity is ERROR, so canValidateCodeSystem returns false early
    // No mocks needed - the method returns null without checking code system
    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "code",
            "display",
            "http://example.com/ValueSet/test");

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeReturnsWarningWhenSeverityIsWarning() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "testCode",
            "display",
            "http://example.com/ValueSet/test");

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IssueSeverity.WARNING));
    assertThat(result.getMessage(), containsString("CodeSystem is unknown"));
    assertThat(result.getMessage(), containsString("http://example.com/CodeSystem/test"));
    assertThat(result.getMessage(), containsString("testCode"));
    assertThat(result.getIssues(), is(notNullValue()));
    assertThat(result.getIssues().size(), is(1));
  }

  @Test
  void testValidateCodeReturnsInformationWithNullSeverityAndMessage() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.INFORMATION);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "testCode",
            "display",
            "http://example.com/ValueSet/test");

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getMessage(), is(nullValue()));
    assertThat(result.getCode(), is("testCode"));
  }

  @Test
  void testValidateCodeReturnsNullWhenCodeSystemExists() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(new CodeSystem());

    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "code",
            "display",
            "http://example.com/ValueSet/test");

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeReturnsNullForNullCodeSystem() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);

    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            null,
            "code",
            "display",
            "http://example.com/ValueSet/test");

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetReturnsNullWhenSeverityIsError() {
    // Default severity is ERROR, so canValidateCodeSystem returns false early
    // No mocks needed - the method returns null without checking code system
    ValueSet valueSet = new ValueSet();
    CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "code",
            "display",
            valueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetReturnsInformationWhenSeverityIsWarning() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    ValueSet valueSet = new ValueSet();
    CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "testCode",
            "display",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IssueSeverity.INFORMATION));
    assertThat(result.getCode(), is("testCode"));
    assertThat(result.getMessage(), containsString("was not checked"));
    assertThat(result.getMessage(), containsString("CodeSystem is not available"));
  }

  @Test
  void testValidateCodeInValueSetReturnsNullWhenCodeSystemExists() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(new CodeSystem());

    ValueSet valueSet = new ValueSet();
    CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "code",
            "display",
            valueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetReturnsNullForNullCodeSystem() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);

    ValueSet valueSet = new ValueSet();
    CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            null,
            "code",
            "display",
            valueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testSetNonExistentCodeSystemSeverityToWarning() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "code",
            "display",
            null);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IssueSeverity.WARNING));
  }

  @Test
  void testSetNonExistentCodeSystemSeverityThrowsOnNull() {
    assertThrows(
        NullPointerException.class, () -> validationSupport.setNonExistentCodeSystemSeverity(null));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testSetAllowNonExistentCodeSystemTrueSetsSeverityToWarning() {
    validationSupport.setAllowNonExistentCodeSystem(true);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem(anyString())).thenReturn(null);

    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "code",
            "display",
            null);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IssueSeverity.WARNING));
  }

  @Test
  void testSetAllowNonExistentCodeSystemFalseSetsSeverityToError() {
    // First set to WARNING
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    // Then set to not allow (ERROR)
    validationSupport.setAllowNonExistentCodeSystem(false);
    // ERROR severity means canValidateCodeSystem returns false early
    // No mocks needed - the method returns null without checking code system

    CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://example.com/CodeSystem/test",
            "code",
            "display",
            null);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidationFlowWithWarningForUnknownCodeSystem() {
    validationSupport.setNonExistentCodeSystemSeverity(IssueSeverity.WARNING);
    when(validationSupportContext.getRootValidationSupport()).thenReturn(rootValidationSupport);
    when(rootValidationSupport.fetchCodeSystem("http://unknown.system")).thenReturn(null);
    when(rootValidationSupport.fetchCodeSystem("http://known.system")).thenReturn(new CodeSystem());

    // Unknown code system should return warning
    CodeValidationResult unknownResult =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://unknown.system",
            "code",
            "display",
            null);
    assertThat(unknownResult, is(notNullValue()));
    assertThat(unknownResult.getSeverity(), is(IssueSeverity.WARNING));

    // Known code system should return null (let other validators handle it)
    CodeValidationResult knownResult =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://known.system",
            "code",
            "display",
            null);
    assertThat(knownResult, is(nullValue()));
  }
}

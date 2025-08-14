package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import gov.cms.madie.madiefhirservice.config.ValidationConfig;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class CustomQiCoreInMemoryValidationSupportTest {

  private CustomQiCoreInMemoryValidationSupport validationSupport;
  private ValueSet valueSet;

  @Mock private ValidationSupportContext validationSupportContext;
  @Mock private ValidationConfig validationConfig;

  @BeforeEach
  void setUp() {
    validationSupport =
        new CustomQiCoreInMemoryValidationSupport(FhirContext.forR4(), validationConfig);
    valueSet = new ValueSet();
    valueSet.setUrl("http://hl7.org/fhir/ValueSet/fm-status");
    valueSet.setExpansion(new ValueSet.ValueSetExpansionComponent());
  }

  @Test
  void testValidateCodeInValueSetWithValidCodeAndDisplay() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/fm-status");
    contains.setCode("cancelled");
    contains.setDisplay("Cancelled");
    valueSet.getExpansion().addContains(contains);

    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/fm-status",
            "cancelled",
            "Cancelled",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getCode(), is("cancelled"));
  }

  @Test
  void testValidateCodeInValueSetWithDisplayMismatch() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/fm-status");
    contains.setCode("test-code");
    contains.setDisplay("Correct Display");
    valueSet.getExpansion().addContains(contains);
    ConceptValidationOptions theOptions = new ConceptValidationOptions();
    theOptions.setValidateDisplay(true);

    when(validationConfig.isValidateDisplay()).thenReturn(true);

    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            theOptions,
            "http://hl7.org/fhir/fm-status",
            "test-code",
            "Wrong Display",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IValidationSupport.IssueSeverity.ERROR));
    assertThat(result.getIssues(), is(not(empty())));
  }

  @Test
  void testValidateCodeInValueSetWithDisplayMismatchAndDisplayValidationDisabled() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/fm-status");
    contains.setCode("test-code");
    contains.setDisplay("Correct Display");
    valueSet.getExpansion().addContains(contains);
    ConceptValidationOptions theOptions = new ConceptValidationOptions();
    theOptions.setValidateDisplay(true);

    when(validationConfig.isValidateDisplay()).thenReturn(false);

    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            theOptions,
            "http://hl7.org/fhir/fm-status",
            "test-code",
            "Wrong Display",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getIssues(), is(empty()));
  }

  @Test
  void testValidateCodeInValueSetWithInferSystemOption() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/ValueSet/encounter-reason");
    contains.setCode("test-code");
    contains.setDisplay("Test Display");
    valueSet.getExpansion().addContains(contains);

    var options = new ConceptValidationOptions().setInferSystem(true);
    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            options,
            "http://different-system.com",
            "test-code",
            "Test Display",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetWithCodeSystemVersion() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/ValueSet/encounter-reason");
    contains.setVersion("1.0");
    contains.setCode("test-code");
    contains.setDisplay("Test Display");
    valueSet.getExpansion().addContains(contains);

    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/ValueSet/encounter-reason|1.0",
            "test-code",
            "Test Display",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetWithNoExpansion() {
    valueSet.setExpansion(null);

    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/ValueSet/encounter-reason",
            "test-code",
            "Test Display",
            valueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetWithNullValueSet() {
    // when
    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/ValueSet/encounter-reason",
            "test-code",
            "Test Display",
            null);

    // then
    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetWithNullCode() {
    // given
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/ValueSet/encounter-reason");
    contains.setCode("test-code");
    contains.setDisplay("Test Display");
    valueSet.getExpansion().addContains(contains);

    // when
    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/ValueSet/encounter-reason",
            null,
            "Test Display",
            valueSet);

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(equalTo(IValidationSupport.IssueSeverity.ERROR)));
    assertThat(result.getIssues(), is(notNullValue()));
    assertThat(result.getIssues().size(), is(equalTo(1)));
    assertThat(
        result.getIssues().get(0).getDiagnostics(), containsString("Invalid - missing code"));
  }

  @Test
  void testValidateCodeInValueSetWithNullSystem() {
    // given
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/ValueSet/encounter-reason");
    contains.setCode("test-code");
    contains.setDisplay("Test Display");
    valueSet.getExpansion().addContains(contains);

    // when
    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            null,
            "test-code",
            "Test Display",
            valueSet);

    // then
    assertThat(result, is(nullValue()));
  }

  @Test
  void testValidateCodeInValueSetWithNullDisplay() {
    // given
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/ValueSet/encounter-reason");
    contains.setCode("test-code");
    contains.setDisplay("Test Display");
    valueSet.getExpansion().addContains(contains);

    // when
    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/ValueSet/encounter-reason",
            "test-code",
            null,
            valueSet);

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getCode(), is("test-code"));
  }

  @Test
  void testValidateCodeInValueSetWithCodeNotInValueSet() {
    // given
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/ValueSet/encounter-reason");
    contains.setCode("existing-code");
    contains.setDisplay("Existing Display");
    valueSet.getExpansion().addContains(contains);

    // when
    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/ValueSet/encounter-reason",
            "non-existent-code",
            "Non-existent Display",
            valueSet);

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IValidationSupport.IssueSeverity.ERROR));
    assertThat(result.getMessage(), containsString("Unknown code"));
    assertThat(result.getIssues(), is(not(empty())));
    assertThat(
        result.getIssues().get(0).getCoding(),
        is(IValidationSupport.CodeValidationIssueCoding.NOT_IN_VS));
  }

  @Test
  void testValidateCodeInValueSetWithCodeNotInValueSetAndNoDisplay() {
    // given
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/ValueSet/encounter-reason");
    contains.setCode("existing-code");
    contains.setDisplay("Existing Display");
    valueSet.getExpansion().addContains(contains);

    // when
    var result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/ValueSet/encounter-reason",
            "non-existent-code",
            null,
            valueSet);

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IValidationSupport.IssueSeverity.ERROR));
    assertThat(result.getMessage(), containsString("Unknown code"));
    assertThat(result.getIssues(), is(not(empty())));
    assertThat(
        result.getIssues().get(0).getCoding(),
        is(IValidationSupport.CodeValidationIssueCoding.NOT_IN_VS));
  }
}

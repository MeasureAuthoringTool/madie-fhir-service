package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomQiCoreInMemoryValidationSupportTest {

    private CustomQiCoreInMemoryValidationSupport validationSupport;
    private ValueSet valueSet;

    @Mock
    private ValidationSupportContext validationSupportContext;

    @BeforeEach
    void setUp() {
        validationSupport = new CustomQiCoreInMemoryValidationSupport(FhirContext.forR4());
        valueSet = new ValueSet();
        valueSet.setUrl("http://hl7.org/fhir/ValueSet/fm-status");
        valueSet.setExpansion(new ValueSet.ValueSetExpansionComponent());
    }

    @Test
    void testValidateCodeInValueSetWithValidCodeAndDisplay() {
        ValueSet.ValueSetExpansionContainsComponent contains = new ValueSet.ValueSetExpansionContainsComponent();
        contains.setSystem("http://hl7.org/fhir/fm-status");
        contains.setCode("cancelled");
        contains.setDisplay("Cancelled");
        valueSet.getExpansion().addContains(contains);

        var result = validationSupport.validateCodeInValueSet(
                validationSupportContext,
                new ConceptValidationOptions(),
                "http://hl7.org/fhir/fm-status",
                "cancelled",
                "Cancelled",
                valueSet
        );

        assertThat(result, is(notNullValue()));
        assertThat(result.getSeverity(), is(nullValue()));
        assertThat(result.getCode(), is("cancelled"));
    }

    @Test
    void testValidateCodeInValueSetWithDisplayMismatch() {
        ValueSet.ValueSetExpansionContainsComponent contains = new ValueSet.ValueSetExpansionContainsComponent();
        contains.setSystem("http://example.com/system");
        contains.setCode("test-code");
        contains.setDisplay("Correct Display");
        valueSet.getExpansion().addContains(contains);

        var result = validationSupport.validateCodeInValueSet(
                validationSupportContext,
                new ConceptValidationOptions(),
                "http://example.com/system",
                "test-code",
                "Wrong Display",
                valueSet
        );

        assertThat(result, is(notNullValue()));
        assertThat(result.getSeverity(), is(IValidationSupport.IssueSeverity.ERROR));
        assertThat(result.getIssues(), is(not(empty())));
    }

    @Test
    void testValidateCodeInValueSetWithInferSystemOption() {
        ValueSet.ValueSetExpansionContainsComponent contains = new ValueSet.ValueSetExpansionContainsComponent();
        contains.setSystem("http://example.com/system");
        contains.setCode("test-code");
        contains.setDisplay("Test Display");
        valueSet.getExpansion().addContains(contains);

        var options = new ConceptValidationOptions().setInferSystem(true);
        var result = validationSupport.validateCodeInValueSet(
                validationSupportContext,
                options,
                "http://different-system.com",
                "test-code",
                "Test Display",
                valueSet
        );

        assertThat(result, is(notNullValue()));
        assertThat(result.getSeverity(), is(nullValue()));
    }

    @Test
    void testValidateCodeInValueSetWithCodeSystemVersion() {
        ValueSet.ValueSetExpansionContainsComponent contains = new ValueSet.ValueSetExpansionContainsComponent();
        contains.setSystem("http://example.com/system");
        contains.setVersion("1.0");
        contains.setCode("test-code");
        contains.setDisplay("Test Display");
        valueSet.getExpansion().addContains(contains);

        var result = validationSupport.validateCodeInValueSet(
                validationSupportContext,
                new ConceptValidationOptions(),
                "http://example.com/system|1.0",
                "test-code",
                "Test Display",
                valueSet
        );

        assertThat(result, is(notNullValue()));
        assertThat(result.getSeverity(), is(nullValue()));
    }

    @Test
    void testValidateCodeInValueSetWithNoExpansion() {
        valueSet.setExpansion(null);

        var result = validationSupport.validateCodeInValueSet(
                validationSupportContext,
                new ConceptValidationOptions(),
                "http://example.com/system",
                "test-code",
                "Test Display",
                valueSet
        );

        assertThat(result, is(nullValue()));
    }
}
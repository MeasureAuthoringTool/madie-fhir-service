package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.util.BundleUtil;
import gov.cms.madie.madiefhirservice.config.ValidationConfig;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class VSESValidationSupportTest {

  @Mock(lenient = true)
  private FhirContext fhirContext;

  @Mock private BasicAuthInterceptor basicAuthInterceptor;
  @Mock private IGenericClient genericClient;
  @Mock private IUntypedQuery untypedQuery;
  @Mock private IQuery<IBaseBundle> query;
  @Mock private IBaseBundle bundle;
  @Mock private RuntimeResourceDefinition bundleDefinition;
  @Mock private ValidationConfig validationConfig;
  @Mock private ValidationSupportContext validationSupportContext;

  @Mock(lenient = true)
  private HapiLocalizer localizer;

  private VSESValidationSupport validationSupport;
  private ValueSet valueSet;
  private final String baseUrl = "http://example.com/fhir";
  private final String apiKey = "test-api-key";

  @BeforeEach
  void setUp() {
    when(fhirContext.newRestfulGenericClient(baseUrl)).thenReturn(genericClient);
    doNothing().when(genericClient).registerInterceptor(basicAuthInterceptor);

    validationSupport =
        new VSESValidationSupport(fhirContext, baseUrl, basicAuthInterceptor, validationConfig);
    valueSet = new ValueSet();
    valueSet.setUrl("http://hl7.org/fhir/ValueSet/test");
    valueSet.setExpansion(new ValueSet.ValueSetExpansionComponent());
  }

  @Test
  void constructorInitializesClientWithBaseUrlAndInterceptors() {
    verify(fhirContext).newRestfulGenericClient(baseUrl);
    verify(genericClient).registerInterceptor(basicAuthInterceptor);
    assertThat(validationSupport, is(notNullValue()));
  }

  @Test
  void getNameReturnsCorrectName() {
    String name = validationSupport.getName();
    assertThat(name, is("Saved ValueSet Expansion Validation Support"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void fetchValueSetReturnsResourceWhenFound() {
    String valueSetUrl = "http://hl7.org/fhir/ValueSet/test";
    IBaseResource mockResource = mock(IBaseResource.class);
    List<IBaseResource> resourceList = List.of(mockResource);

    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("ValueSet")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(resourceList);

      IBaseResource result = validationSupport.fetchValueSet(valueSetUrl);

      assertThat(result, is(mockResource));
      verify(untypedQuery).forResource("ValueSet");
      verify(query).where(any(ICriterion.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void fetchValueSetAppliesVersionCriteriaWhenVersionIsInUrl() {
    String valueSetUrlWithVersion = "http://hl7.org/fhir/ValueSet/test|1.0.0";
    IBaseResource mockResource = mock(IBaseResource.class);
    List<IBaseResource> resourceList = List.of(mockResource);

    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("ValueSet")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
      bundleUtilMock
              .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
              .thenReturn(resourceList);

      IBaseResource result = validationSupport.fetchValueSet(valueSetUrlWithVersion);

      assertThat(result, is(mockResource));
      verify(query, times(2)).where(any(ICriterion.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void fetchValueSetReturnsNullWhenBundleIsNull() {
    String valueSetUrl = "http://hl7.org/fhir/ValueSet/test";

    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("ValueSet")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(null);

    IBaseResource result = validationSupport.fetchValueSet(valueSetUrl);

    assertThat(result, is(nullValue()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void fetchValueSetReturnsNullWhenNoResourcesFound() {
    String valueSetUrl = "http://hl7.org/fhir/ValueSet/test";
    List<IBaseResource> emptyList = Collections.emptyList();

    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("ValueSet")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(emptyList);

      IBaseResource result = validationSupport.fetchValueSet(valueSetUrl);

      assertThat(result, is(nullValue()));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void isValueSetSupportedReturnsTrueWhenValueSetExists() {
    String valueSetUrl = "http://hl7.org/fhir/ValueSet/test";
    IBaseResource mockResource = mock(IBaseResource.class);
    List<IBaseResource> resourceList = List.of(mockResource);

    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("ValueSet")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(resourceList);

      boolean isSupported =
          validationSupport.isValueSetSupported(validationSupportContext, valueSetUrl);

      assertThat(isSupported, is(true));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void isValueSetSupportedReturnsFalseWhenValueSetNotFound() {
    String valueSetUrl = "http://hl7.org/fhir/ValueSet/notfound";
    List<IBaseResource> emptyList = Collections.emptyList();

    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("ValueSet")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(emptyList);

      boolean isSupported =
          validationSupport.isValueSetSupported(validationSupportContext, valueSetUrl);

      assertThat(isSupported, is(false));
    }
  }

  @Test
  void fetchCodeSystemReturnsNullForBlankSystem() {
    String blankSystem = "";

    IBaseResource result = validationSupport.fetchCodeSystem(blankSystem);

    assertThat(result, is(nullValue()));
  }

  @Test
  void fetchCodeSystemReturnsNullForNullSystem() {
    String nullSystem = null;

    IBaseResource result = validationSupport.fetchCodeSystem(nullSystem);

    assertThat(result, is(nullValue()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void fetchCodeSystemReturnsNull() {
    String codeSystem = "http://hl7.org/fhir/CodeSystem/test";
    IBaseResource result = validationSupport.fetchCodeSystem(codeSystem);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeReturnsNull() {
    String codeSystem = "http://hl7.org/fhir/CodeSystem/test";
    String code = "test-code";
    String display = "Test Code";
    String valueSetUrl = "http://hl7.org/fhir/ValueSet/test";

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCode(
            validationSupportContext,
            new ConceptValidationOptions(),
            codeSystem,
            code,
            display,
            valueSetUrl);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeInValueSetReturnsNullForNullValueSet() {
    String codeSystem = "http://hl7.org/fhir/CodeSystem/test";
    String code = "test-code";
    String display = "Test Code";

    when(validationConfig.isValidateDisplay()).thenReturn(false);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            codeSystem,
            code,
            display,
            null);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeInValueSetReturnsNullForValueSetWithoutExpansion() {
    ValueSet valueSetWithoutExpansion = new ValueSet();
    valueSetWithoutExpansion.setExpansion(null);

    when(validationConfig.isValidateDisplay()).thenReturn(false);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            "Test Code",
            valueSetWithoutExpansion);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeInValueSetReturnsNullForEmptyValueSetExpansion() {
    ValueSet emptyValueSet = new ValueSet();
    emptyValueSet.setExpansion(new ValueSet.ValueSetExpansionComponent());

    when(validationConfig.isValidateDisplay()).thenReturn(false);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            "Test Code",
            emptyValueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeInValueSetReturnsMissingCodeResultForBlankCode() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("existing-code");
    contains.setDisplay("Existing Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "",
            "Test Code",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IValidationSupport.IssueSeverity.ERROR));
    assertThat(result.getCode(), is("[missing]"));
    assertThat(result.getIssues(), is(not(empty())));
  }

  @Test
  void validateCodeInValueSetReturnsNotInVsResultWhenCodeNotFound() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("existing-code");
    contains.setDisplay("Existing Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "non-existent-code",
            "Non-existent Code",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IValidationSupport.IssueSeverity.ERROR));
    assertThat(result.getCode(), is("non-existent-code"));
    assertThat(result.getMessage(), containsString("Unknown code"));
    assertThat(
        result.getIssues().get(0).getCoding(),
        is(IValidationSupport.CodeValidationIssueCoding.NOT_IN_VS));
  }

  @Test
  void validateCodeInValueSetReturnsSuccessForMatchingCode() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getCode(), is("test-code"));
  }

  @Test
  void validateCodeInValueSetReturnsErrorForDisplayMismatch() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("test-code");
    contains.setDisplay("Correct Display");
    valueSet.getExpansion().addContains(contains);

    ConceptValidationOptions options = new ConceptValidationOptions();
    options.setValidateDisplay(true);

    when(validationConfig.isValidateDisplay()).thenReturn(true);
    when(fhirContext.getLocalizer()).thenReturn(localizer);
    when(localizer.getMessage(any(Class.class), eq("displayMismatch"), any(Object[].class)))
        .thenReturn("Display mismatch");

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            options,
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            "Wrong Display",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(IValidationSupport.IssueSeverity.ERROR));
    assertThat(result.getIssues(), is(not(empty())));
    assertThat(
        result.getIssues().get(0).getCoding(),
        is(IValidationSupport.CodeValidationIssueCoding.INVALID_DISPLAY));
  }

  @Test
  void validateCodeInValueSetAllowsDisplayMismatchWhenValidationDisabled() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("test-code");
    contains.setDisplay("Correct Display");
    valueSet.getExpansion().addContains(contains);

    ConceptValidationOptions options = new ConceptValidationOptions();
    options.setValidateDisplay(true);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            options,
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            "Wrong Display",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getIssues(), is(empty()));
  }

  @Test
  void validateCodeInValueSetWithInferSystemOptionIgnoresCodeSystemMismatch() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/original");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    ConceptValidationOptions options = new ConceptValidationOptions();
    options.setInferSystem(true);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            options,
            "http://hl7.org/fhir/CodeSystem/different",
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getCode(), is("test-code"));
  }

  @Test
  void validateCodeInValueSetReturnsNullForCodeSystemMismatch() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/original");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    ConceptValidationOptions options = new ConceptValidationOptions();
    options.setInferSystem(false);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            options,
            "http://hl7.org/fhir/CodeSystem/different",
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeInValueSetMatchesCodeSystemAndVersionWhenProvided() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setVersion("1.0");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test|1.0",
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getCode(), is("test-code"));
  }

  @Test
  void validateCodeInValueSetRejectsCodeWithVersionMismatch() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setVersion("1.0");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test|2.0",
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeInValueSetHandlesNullDisplay() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            null,
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSeverity(), is(nullValue()));
    assertThat(result.getCode(), is("test-code"));
  }

  @Test
  void validateCodeInValueSetHandlesNullCodeSystem() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            null,
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(nullValue()));
  }

  @Test
  void validateCodeInValueSetWithMultipleContainsReturnsFirstMatch() {
    ValueSet.ValueSetExpansionContainsComponent contains1 =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains1.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains1.setCode("different-code");
    contains1.setDisplay("Different Code");
    valueSet.getExpansion().addContains(contains1);

    ValueSet.ValueSetExpansionContainsComponent contains2 =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains2.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains2.setCode("test-code");
    contains2.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains2);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getCode(), is("test-code"));
  }

  @Test
  void validateCodeInValueSetIncludesValueSetUrlInSourceDetails() {
    ValueSet.ValueSetExpansionContainsComponent contains =
        new ValueSet.ValueSetExpansionContainsComponent();
    contains.setSystem("http://hl7.org/fhir/CodeSystem/test");
    contains.setCode("test-code");
    contains.setDisplay("Test Code");
    valueSet.getExpansion().addContains(contains);

    when(validationConfig.isValidateDisplay()).thenReturn(false);
    when(fhirContext.getLocalizer()).thenReturn(localizer);

    IValidationSupport.CodeValidationResult result =
        validationSupport.validateCodeInValueSet(
            validationSupportContext,
            new ConceptValidationOptions(),
            "http://hl7.org/fhir/CodeSystem/test",
            "test-code",
            "Test Code",
            valueSet);

    assertThat(result, is(notNullValue()));
    assertThat(result.getSourceDetails(), containsString("VSES expansion of ValueSet"));
    assertThat(result.getSourceDetails(), containsString(valueSet.getUrl()));
  }
}

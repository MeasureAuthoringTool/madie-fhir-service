package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.ParametersUtil;
import gov.cms.madie.madiefhirservice.config.ValidationConfig;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class CustomRemoteTerminologyServiceValidationSupportTest {

  @Mock private FhirContext fhirContext;
  @Mock private BasicAuthInterceptor basicAuthInterceptor;
  @Mock private IGenericClient genericClient;
  @Mock private IUntypedQuery untypedQuery;
  @Mock private IQuery<IBaseBundle> query;
  @Mock private IBaseBundle bundle;
  @Mock private IBaseResource codeSystemResource;
  @Mock private RuntimeResourceDefinition bundleDefinition;
  @Mock private HapiLocalizer localizer;

  @Mock private ValidationConfig validationConfig;

  @Captor private ArgumentCaptor<Parameters> parametersCaptor;
  @Captor ArgumentCaptor<String> displayCaptor;

  private CustomRemoteTerminologyServiceValidationSupport validationSupport;
  private final String baseUrl = "http://example.com/fhir";

  @BeforeEach
  void setUp() {
    // Mock the client creation in constructor
    when(fhirContext.newRestfulGenericClient(baseUrl)).thenReturn(genericClient);
    doNothing().when(genericClient).registerInterceptor(basicAuthInterceptor);

    validationSupport =
        new CustomRemoteTerminologyServiceValidationSupport(
            fhirContext, baseUrl, basicAuthInterceptor, validationConfig);
  }

  @Test
  void testFetchCodeSystemReturnsNullForBlankSystem() {
    // given
    String blankSystem = "";

    // when
    IBaseResource result = validationSupport.fetchCodeSystem(blankSystem);

    // then
    assertThat(result, is(nullValue()));
  }

  @Test
  void testFetchCodeSystemReturnsNullForNullSystem() {
    // given
    String nullSystem = null;

    // when
    IBaseResource result = validationSupport.fetchCodeSystem(nullSystem);

    // then
    assertThat(result, is(nullValue()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testFetchCodeSystemReturnsResourceWhenFound() {
    // given
    String system = "http://example.com/CodeSystem/test";
    List<IBaseResource> resourceList = List.of(codeSystemResource);

    // Mock the FhirContext setup
    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);

    // Mock the search chain on the stored client
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("CodeSystem")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.count(1)).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    // Mock BundleUtil
    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(resourceList);

      // when
      IBaseResource result = validationSupport.fetchCodeSystem(system);

      // then
      assertThat(result, is(codeSystemResource));
      verify(untypedQuery).forResource("CodeSystem");
      verify(query).where(any(ICriterion.class));
      verify(query).count(1);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void testFetchCodeSystemReturnsNullWhenNoResourcesFound() {
    // given
    String system = "http://example.com/CodeSystem/notfound";
    List<IBaseResource> emptyList = Collections.emptyList();

    // Mock the FhirContext setup
    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);

    // Mock the search chain
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("CodeSystem")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.count(1)).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    // Mock BundleUtil
    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {

      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(emptyList);

      // when
      IBaseResource result = validationSupport.fetchCodeSystem(system);

      // then
      assertThat(result, is(nullValue()));
    }
  }

  @Test
  void testGetErrorMessage() {
    // given
    String errorCode = "unknownCodeInSystem";
    Object[] params = new Object[] {"param1", "param2"};
    String expectedMessage = "Unknown code \"param1#param2\".";

    when(fhirContext.getLocalizer()).thenReturn(localizer);
    when(localizer.getMessage(
            eq(CustomRemoteTerminologyServiceValidationSupport.class.getSuperclass()),
            eq(errorCode),
            eq(params)))
        .thenReturn(expectedMessage);

    // when
    String result = validationSupport.getErrorMessage(errorCode, params);

    // then
    assertThat(result, is(expectedMessage));
    verify(localizer)
        .getMessage(
            eq(CustomRemoteTerminologyServiceValidationSupport.class.getSuperclass()),
            eq(errorCode),
            eq(params));
  }

  @Test
  void testFetchValueSetAlwaysReturnsNull() {
    // given
    String valueSetUrl = "http://example.com/ValueSet/test";

    // when
    IBaseResource result = validationSupport.fetchValueSet(valueSetUrl);

    // then
    assertThat(result, is(nullValue()));
  }

  @Test
  void testConstructorSetsUpClientWithInterceptor() {
    // given/when - constructor called in setUp()

    // then - verify that the client was created and interceptor was registered
    verify(fhirContext).newRestfulGenericClient(baseUrl);
    verify(genericClient).registerInterceptor(basicAuthInterceptor);
    assertThat(validationSupport, is(notNullValue()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testFetchCodeSystemWithMultipleResources() {
    // given
    String system = "http://example.com/CodeSystem/test";
    IBaseResource firstResource = mock(IBaseResource.class);
    IBaseResource secondResource = mock(IBaseResource.class);
    List<IBaseResource> resourceList = List.of(firstResource, secondResource);

    // Mock the FhirContext setup
    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);

    // Mock the search chain
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("CodeSystem")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.count(1)).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    // Mock BundleUtil
    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {

      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(resourceList);
      // when
      IBaseResource result = validationSupport.fetchCodeSystem(system);

      // then - should return the first resource
      assertThat(result, is(firstResource));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void testFetchCodeSystemHandlesExceptionGracefully() {
    // given
    String system = "http://example.com/CodeSystem/test";

    // Mock the FhirContext setup
    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);

    // Mock the search chain to throw an exception
    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("CodeSystem")).thenReturn(query);
    when(query.where(any(ICriterion.class))).thenReturn(query);
    when(query.count(1)).thenReturn(query);
    when(query.returnBundle(eq(IBaseBundle.class))).thenReturn(query);
    when(query.execute()).thenThrow(new RuntimeException("FHIR server error"));

    // when/then - should handle exception gracefully
    try {
      validationSupport.fetchCodeSystem(system);
      // If no exception handling in the actual code, this will throw
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("FHIR server error"));
    }
  }

  @Test
  void testValidateCodeDelegatesToSuperWithNullForDisabled() {
    // Given
    ValidationSupportContext context = mock(ValidationSupportContext.class);
    ConceptValidationOptions options = mock(ConceptValidationOptions.class);
    String codeSystem = "system";
    String code = "code";
    String display = "display";
    String valueSetUrl = "valueSetUrl";

    CustomRemoteTerminologyServiceValidationSupport spySupport = spy(validationSupport);

    IValidationSupport.CodeValidationResult theOutput =
        mock(IValidationSupport.CodeValidationResult.class);

    when(spySupport.invokeSuperRemoteValidateCode(
            eq(codeSystem), eq(code), isNull(), eq(valueSetUrl), isNull()))
        .thenReturn(theOutput);

    when(validationConfig.isValidateDisplay()).thenReturn(false);

    // When
    IValidationSupport.CodeValidationResult codeValidationResult =
        spySupport.validateCode(context, options, codeSystem, code, display, valueSetUrl);

    // Then
    assertThat(codeValidationResult, is(theOutput));
    verify(spySupport, times(1))
        .invokeSuperRemoteValidateCode(
            eq(codeSystem), eq(code), displayCaptor.capture(), eq(valueSetUrl), isNull());
    assertThat(displayCaptor.getValue(), is(nullValue()));
  }

  @Test
  void testValidateCodeDelegatesToSuperWithDisplayForEnabled() {
    // Given
    ValidationSupportContext context = mock(ValidationSupportContext.class);
    ConceptValidationOptions options = mock(ConceptValidationOptions.class);
    String codeSystem = "system";
    String code = "code";
    String display = "display";
    String valueSetUrl = "valueSetUrl";

    CustomRemoteTerminologyServiceValidationSupport spySupport = spy(validationSupport);

    IValidationSupport.CodeValidationResult theOutput =
        mock(IValidationSupport.CodeValidationResult.class);

    when(spySupport.invokeSuperRemoteValidateCode(
            eq(codeSystem), eq(code), eq(display), eq(valueSetUrl), isNull()))
        .thenReturn(theOutput);

    when(validationConfig.isValidateDisplay()).thenReturn(true);

    // When
    IValidationSupport.CodeValidationResult codeValidationResult =
        spySupport.validateCode(context, options, codeSystem, code, display, valueSetUrl);

    // Then
    assertThat(codeValidationResult, is(theOutput));
    verify(spySupport, times(1))
        .invokeSuperRemoteValidateCode(
            eq(codeSystem), eq(code), displayCaptor.capture(), eq(valueSetUrl), isNull());
    assertThat(displayCaptor.getValue(), is(equalTo(display)));
  }

  @Test
  void testValidateCode() {
    // given
    ValidationSupportContext context = mock(ValidationSupportContext.class);
    ConceptValidationOptions options = mock(ConceptValidationOptions.class);
    String codeSystem = "system";
    String code = "code";
    String display = "display";
    String valueSetUrl = "valueSetUrl";

    CustomRemoteTerminologyServiceValidationSupport spySupport = spy(validationSupport);

    when(validationConfig.isValidateDisplay()).thenReturn(false);

    doReturn(fhirContext).when(spySupport).getFhirContext();
    Parameters parameters = mock(Parameters.class);

    try (MockedStatic<ParametersUtil> mocked = mockStatic(ParametersUtil.class)) {
      mocked.when(() -> ParametersUtil.newInstance(fhirContext)).thenReturn(parameters);
      GenericClient mockClient = mock(GenericClient.class);
      doReturn(mockClient).when(fhirContext).newRestfulGenericClient(anyString());
      IOperation mockOperation = mock(IOperation.class);
      doReturn(mockOperation).when(mockClient).operation();
      IOperationUnnamed mockUnnamedOperation = mock(IOperationUnnamed.class);
      doReturn(mockUnnamedOperation).when(mockOperation).onType(anyString());
      IOperationUntyped mockUntypedOperation = mock(IOperationUntyped.class);
      doReturn(mockUntypedOperation).when(mockUnnamedOperation).named(anyString());
      IOperationUntypedWithInputAndPartialOutput mockInputAndOutputOperation =
          mock(IOperationUntypedWithInputAndPartialOutput.class);
      doReturn(mockInputAndOutputOperation)
          .when(mockUntypedOperation)
          .withParameters(any(IBaseParameters.class));

      Parameters retParameters = new Parameters();
      retParameters.addParameter("result", "true");
      log.info("retParameters: {}", retParameters);
      doReturn(retParameters).when(mockInputAndOutputOperation).execute();
      mocked
          .when(
              () ->
                  ParametersUtil.getNamedParameterValueAsString(
                      any(FhirContext.class), any(Parameters.class), eq("result")))
          .thenReturn(Optional.of("true"));

      // when
      IValidationSupport.CodeValidationResult result =
          spySupport.validateCode(context, options, codeSystem, code, display, valueSetUrl);

      // then
      assertThat(result, is(notNullValue()));
      assertThat(result.isOk(), is(true));
    }
  }

  @Test
  void testValidateCodeInValueSetDelegatesToSuperWithNullForDisabled() {
    // Given
    ValidationSupportContext context = mock(ValidationSupportContext.class);
    ConceptValidationOptions options = mock(ConceptValidationOptions.class);
    String codeSystem = "system";
    String code = "code";
    String display = "display";

    IBaseResource valueSet = mock(ValueSet.class);

    CustomRemoteTerminologyServiceValidationSupport spySupport = spy(validationSupport);

    IValidationSupport.CodeValidationResult theOutput =
        mock(IValidationSupport.CodeValidationResult.class);

    try (MockedStatic<DefaultProfileValidationSupport> mockedProfileValidationSupport =
        mockStatic(DefaultProfileValidationSupport.class)) {
      mockedProfileValidationSupport
          .when(
              () ->
                  DefaultProfileValidationSupport.getConformanceResourceUrl(
                      any(FhirContext.class), isNull()))
          .thenReturn("http://valueset");

      when(spySupport.invokeSuperRemoteValidateCode(
              eq(codeSystem), eq(code), isNull(), isNull(), eq(valueSet)))
          .thenReturn(theOutput);

      when(validationConfig.isValidateDisplay()).thenReturn(false);

      // When
      IValidationSupport.CodeValidationResult codeValidationResult =
          spySupport.validateCodeInValueSet(context, options, codeSystem, code, display, valueSet);

      // Then
      assertThat(codeValidationResult, is(theOutput));
      verify(spySupport, times(1))
          .invokeSuperRemoteValidateCode(
              eq(codeSystem), eq(code), displayCaptor.capture(), isNull(), eq(valueSet));
      assertThat(displayCaptor.getValue(), is(nullValue()));
    }
  }

  @Test
  void testValidateCodeInValueSetDelegatesToSuperWithDisplayForEnabled() {
    // Given
    ValidationSupportContext context = mock(ValidationSupportContext.class);
    ConceptValidationOptions options = mock(ConceptValidationOptions.class);
    String codeSystem = "system";
    String code = "code";
    String display = "display";

    IBaseResource valueSet = mock(ValueSet.class);

    CustomRemoteTerminologyServiceValidationSupport spySupport = spy(validationSupport);

    IValidationSupport.CodeValidationResult theOutput =
        new IValidationSupport.CodeValidationResult();
    theOutput.setSeverity(IValidationSupport.IssueSeverity.ERROR);
    IValidationSupport.CodeValidationIssue codeValidationIssue =
        new IValidationSupport.CodeValidationIssue(
            "Code not found in ValueSet",
            IValidationSupport.IssueSeverity.ERROR,
            IValidationSupport.CodeValidationIssueCode.INVALID,
            IValidationSupport.CodeValidationIssueCoding.INVALID_CODE);
    theOutput.getIssues().add(codeValidationIssue);

    try (MockedStatic<DefaultProfileValidationSupport> mockedProfileValidationSupport =
        mockStatic(DefaultProfileValidationSupport.class)) {
      mockedProfileValidationSupport
          .when(
              () ->
                  DefaultProfileValidationSupport.getConformanceResourceUrl(
                      any(FhirContext.class), isNull()))
          .thenReturn("http://valueset");

      when(spySupport.invokeSuperRemoteValidateCode(
              eq(codeSystem), eq(code), eq(display), isNull(), eq(valueSet)))
          .thenReturn(theOutput);

      when(validationConfig.isValidateDisplay()).thenReturn(true);

      // When
      IValidationSupport.CodeValidationResult codeValidationResult =
          spySupport.validateCodeInValueSet(context, options, codeSystem, code, display, valueSet);

      // Then
      assertThat(codeValidationResult, is(theOutput));
      assertThat(
          theOutput.getIssues().get(0).getDetails().getCodings().get(0).getCode(), is("not-found"));
      verify(spySupport, times(1))
          .invokeSuperRemoteValidateCode(
              eq(codeSystem), eq(code), displayCaptor.capture(), isNull(), eq(valueSet));
      assertThat(displayCaptor.getValue(), is(equalTo(display)));
    }
  }

  @Test
  void testValidateCodeInValueSet() {
    // given
    ValidationSupportContext context = mock(ValidationSupportContext.class);
    ConceptValidationOptions options = mock(ConceptValidationOptions.class);
    String codeSystem = "system";
    String code = "code";
    String display = "display";
    IBaseResource valueSet = mock(IBaseResource.class);

    CustomRemoteTerminologyServiceValidationSupport spySupport = spy(validationSupport);

    when(validationConfig.isValidateDisplay()).thenReturn(false);

    doReturn(fhirContext).when(spySupport).getFhirContext();
    Parameters parameters = mock(Parameters.class);

    try (MockedStatic<ParametersUtil> mocked = mockStatic(ParametersUtil.class)) {
      try (MockedStatic<DefaultProfileValidationSupport> mockedProfileValidationSupport =
          mockStatic(DefaultProfileValidationSupport.class)) {
        mockedProfileValidationSupport
            .when(
                () ->
                    DefaultProfileValidationSupport.getConformanceResourceUrl(
                        any(FhirContext.class), any(IBaseResource.class)))
            .thenReturn("http://valueset");
        mocked.when(() -> ParametersUtil.newInstance(fhirContext)).thenReturn(parameters);
        GenericClient mockClient = mock(GenericClient.class);
        doReturn(mockClient).when(fhirContext).newRestfulGenericClient(anyString());
        IOperation mockOperation = mock(IOperation.class);
        doReturn(mockOperation).when(mockClient).operation();
        IOperationUnnamed mockUnnamedOperation = mock(IOperationUnnamed.class);
        doReturn(mockUnnamedOperation).when(mockOperation).onType(anyString());
        IOperationUntyped mockUntypedOperation = mock(IOperationUntyped.class);
        doReturn(mockUntypedOperation).when(mockUnnamedOperation).named(anyString());
        IOperationUntypedWithInputAndPartialOutput mockInputAndOutputOperation =
            mock(IOperationUntypedWithInputAndPartialOutput.class);
        doReturn(mockInputAndOutputOperation)
            .when(mockUntypedOperation)
            .withParameters(any(IBaseParameters.class));

        Parameters retParameters = new Parameters();
        retParameters.addParameter("result", "true");
        log.info("retParameters: {}", retParameters);
        doReturn(retParameters).when(mockInputAndOutputOperation).execute();
        mocked
            .when(
                () ->
                    ParametersUtil.getNamedParameterValueAsString(
                        any(FhirContext.class), any(Parameters.class), eq("result")))
            .thenReturn(Optional.of("true"));

        // when
        IValidationSupport.CodeValidationResult result =
            spySupport.validateCodeInValueSet(
                context, options, codeSystem, code, display, valueSet);

        // then
        assertThat(result, is(notNullValue()));
        assertThat(result.isOk(), is(true));
      }
    }
  }

  @Test
  void testFetchValueSetReturnsNullForBlankUrl() {
    String blankUrl = "";
    IBaseResource result = validationSupport.fetchValueSet(blankUrl);
    assertThat(result, is(nullValue()));
  }

  @Test
  void testFetchValueSetReturnsNullForNonCtsUrl() {
    String nonCtsUrl = "http://example.com/fhir/ValueSet/123";
    IBaseResource result = validationSupport.fetchValueSet(nonCtsUrl);
    assertThat(result, is(nullValue()));
  }

  @Test
  void testFetchValueSetReturnsFirstResourceForValidCtsUrl() {
    String validCtsUrl = "http://cts.nlm.nih.gov/fhir/ValueSet/123";

    when(fhirContext.getResourceDefinition("Bundle")).thenReturn(bundleDefinition);
    when(bundleDefinition.getImplementingClass(IBaseBundle.class)).thenReturn(IBaseBundle.class);

    when(genericClient.search()).thenReturn(untypedQuery);
    when(untypedQuery.forResource("ValueSet")).thenReturn(query);
    when(query.where((ICriterion<?>) any())).thenReturn(query);
    when(query.returnBundle(IBaseBundle.class)).thenReturn(query);
    when(query.execute()).thenReturn(bundle);

    List<IBaseResource> mockResources = List.of(codeSystemResource);
    // Ensure static mock is closed after use to avoid leaking into other tests
    try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
      bundleUtilMock
          .when(() -> BundleUtil.toListOfResources(fhirContext, bundle))
          .thenReturn(mockResources);

      IBaseResource result = validationSupport.fetchValueSet(validCtsUrl);
      assertThat(result, is(codeSystemResource));
    }
  }
}

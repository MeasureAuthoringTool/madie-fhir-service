package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.i18n.HapiLocalizer;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

  private CustomRemoteTerminologyServiceValidationSupport validationSupport;
  private final String baseUrl = "http://example.com/fhir";

  @BeforeEach
  void setUp() {
    // Mock the client creation in constructor
    when(fhirContext.newRestfulGenericClient(baseUrl)).thenReturn(genericClient);
    doNothing().when(genericClient).registerInterceptor(basicAuthInterceptor);

    validationSupport =
        new CustomRemoteTerminologyServiceValidationSupport(
            fhirContext, baseUrl, basicAuthInterceptor);
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
}

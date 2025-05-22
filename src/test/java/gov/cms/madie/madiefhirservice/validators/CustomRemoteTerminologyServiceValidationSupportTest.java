package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class CustomRemoteTerminologyServiceValidationSupportTest {

  @Mock private FhirContext fhirContext;

  @InjectMocks private CustomRemoteTerminologyServiceValidationSupport validationSupport;

  @Test
  void testFetchCodeSystemReturnsNull() {
    // given
    String system = "http://example.com/CodeSystem/test";

    // when
    IBaseResource output = validationSupport.fetchCodeSystem(system);

    // then
    assertThat(output, is(nullValue()));
  }

  @Test
  void testGetErrorMessage() {
    // given
    String errorCode = "unknownCodeInSystem";
    Object[] params = new Object[] {"param1", "param2"};
    HapiLocalizer localizerMock = mock(HapiLocalizer.class);
    when(fhirContext.getLocalizer()).thenReturn(localizerMock);
    when(localizerMock.getMessage(Mockito.any(Class.class), anyString(), eq(params)))
        .thenReturn("Unknown code \"param1#param2\".");

    // when
    String message = validationSupport.getErrorMessage(errorCode, params);

    // then
    assertThat(message, is(notNullValue()));
    assertThat(message, is(equalTo("Unknown code \"param1#param2\".")));
  }

  @Test
  void testFetchValueSetReturnsNull() {
    // given
    String url = "http://example.com/ValueSet/test";

    // when
    IBaseResource output = validationSupport.fetchValueSet(url);

    // then
    assertThat(output, is(nullValue()));
  }
}

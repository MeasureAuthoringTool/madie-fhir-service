package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.config.ElmTranslatorClientConfig;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.exceptions.CqlElmTranslationServiceException;
import org.hl7.fhir.r5.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ElmTranslatorClientTest {

  @Mock private ElmTranslatorClientConfig elmTranslatorClientConfig;
  @Mock private RestTemplate restTemplate;
  @Mock FhirContext fhirContext;

  @InjectMocks private ElmTranslatorClient elmTranslatorClient;

  @BeforeEach
  void beforeEach() {
    lenient().when(elmTranslatorClientConfig.getCqlElmServiceBaseUrl()).thenReturn("http://test");
    lenient()
        .when(elmTranslatorClientConfig.getEffectiveDataRequirementsDataUri())
        .thenReturn("/geteffectivedatarequirements");
  }

  @Test
  public void testGetEffectiveDataRequirementsThrowsException() {
    assertThrows(
        CqlElmTranslationServiceException.class,
        () -> elmTranslatorClient.getEffectiveDataRequirements(null, false, "TEST_TOKEN"));
  }

  @Test
  public void testGetEffectiveDataRequirementsSuccess() {
    String effectiveDR =
        "{\n"
            + "  \"resourceType\": \"Library\",\n"
            + "  \"id\": \"effective-data-requirements\",\n"
            + "  \"status\": \"active\",\n"
            + "  \"type\": {\n"
            + "    \"coding\": [ {\n"
            + "      \"system\": \"http://terminology.hl7.org/CodeSystem/library-type\",\n"
            + "      \"code\": \"module-definition\"\n"
            + "    }]},\n"
            + "  \"relatedArtifact\": [{\n"
            + "      \"type\": \"depends-on\",\n"
            + "      \"display\": \"Library Status\",\n"
            + "      \"resource\": \"Library/Status|1.6.000\"\n"
            + "   }]\n"
            + "}";
    CqlLibraryDetails libraryDetails = CqlLibraryDetails.builder().libraryName("Test").build();
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(effectiveDR));

    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR5().newJsonParser());
    when(elmTranslatorClientConfig.getMadieUrl()).thenReturn("http://test.url");

    Library output =
        elmTranslatorClient.getEffectiveDataRequirements(libraryDetails, false, "TEST_TOKEN");
    assertThat(output.getId(), is(equalTo("effective-data-requirements")));
    assertThat(output.getRelatedArtifact().size(), is(equalTo(1)));
    assertThat(
        output.getRelatedArtifact().get(0).getResource(),
        is(equalTo("http://test.url/Library/Status|1.6.000")));
  }
}

package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.config.ElmTranslatorClientConfig;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.exceptions.CqlElmTranslationServiceException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Library;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
@AllArgsConstructor
public class ElmTranslatorClient {

  private ElmTranslatorClientConfig elmTranslatorClientConfig;
  private RestTemplate elmTranslatorRestTemplate;
  private final FhirContext fhirContextForR5;

  public Library getModuleDefinitionLibrary(
      CqlLibraryDetails libraryDetails, boolean recursive, String accessToken) {
    try {
      log.info(
          "Getting Module Definition Library for library: {}", libraryDetails.getLibraryName());
      URI uri =
          UriComponentsBuilder.fromHttpUrl(
                  elmTranslatorClientConfig.getCqlElmServiceBaseUrl()
                      + elmTranslatorClientConfig.getEffectiveDataRequirementsDataUri())
              .queryParam("recursive", recursive)
              .build()
              .encode()
              .toUri();

      HttpHeaders headers = new HttpHeaders();
      headers.set(HttpHeaders.AUTHORIZATION, accessToken);

      HttpEntity<CqlLibraryDetails> bundleEntity = new HttpEntity<>(libraryDetails, headers);
      String effectiveDrJson =
          elmTranslatorRestTemplate
              .exchange(uri, HttpMethod.PUT, bundleEntity, String.class)
              .getBody();
      return fhirContextForR5.newJsonParser().parseResource(Library.class, effectiveDrJson);
    } catch (Exception ex) {
      log.error(
          "An error occurred getting effective data requirements "
              + "from the CQL to ELM translation service",
          ex);
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service for getting effective data requirement",
          ex);
    }
  }

  public Library getEffectiveDataRequirements(
      CqlLibraryDetails libraryDetails, boolean recursive, String accessToken) {
    Library effectiveDataRequirements =
        getModuleDefinitionLibrary(libraryDetails, recursive, accessToken);
    // update relative urls of Library artifacts to be canonical
    effectiveDataRequirements
        .getRelatedArtifact()
        .forEach(
            relatedArtifact -> {
              if (relatedArtifact.getDisplay().startsWith("Library")) {
                relatedArtifact.setResource(
                    elmTranslatorClientConfig.getMadieUrl() + "/" + relatedArtifact.getResource());
              }
            });
    // effectiveDataRequirements needs to have fixed id: effective-data-requirements
    effectiveDataRequirements.setId("effective-data-requirements");
    return effectiveDataRequirements;
  }
}

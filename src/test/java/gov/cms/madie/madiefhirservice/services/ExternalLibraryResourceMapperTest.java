package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.madie.models.dto.CqlLibraryDto;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalLibraryResourceMapperTest {
  private ExternalLibraryResourceMapper externalLibraryResourceMapper;

  @BeforeEach
  void setUp() {
    externalLibraryResourceMapper = new ExternalLibraryResourceMapper(FhirContext.forR4Cached());
  }

  @Test
  void toFhirLibraryShouldPreserveSourceMetadataAndRestoreContent() {
    // given
    CqlLibraryDto cqlLibrary =
        CqlLibraryDto.builder()
            .cqlLibraryName("ExternalLibrary")
            .cql("library ExternalLibrary version '1.0.1'")
            .elmJson("ELM JSON")
            .elmXml("ELM XML")
            .fhirResource(
                """
                {
                  "resourceType": "Library",
                  "id": "source-library-id",
                  "url": "http://example.org/Library/ExternalLibrary",
                  "title": "Source Library Title",
                  "status": "active",
                  "content": [
                    {"contentType": "text/cql", "title": "Source CQL"},
                    {"contentType": "application/elm+json", "title": "Source ELM JSON"}
                  ]
                }
                """)
            .build();

    // when
    Library library = externalLibraryResourceMapper.toFhirLibrary(cqlLibrary);

    // then
    assertThat(library.getIdElement().getIdPart(), is(equalTo("source-library-id")));
    assertThat(library.getUrl(), is(equalTo("http://example.org/Library/ExternalLibrary")));
    assertThat(library.getTitle(), is(equalTo("Source Library Title")));
    assertThat(getContent(library, "text/cql").getTitle(), is(equalTo("Source CQL")));
    assertThat(
        getContentData(library, "text/cql"),
        is(equalTo("library ExternalLibrary version '1.0.1'")));
    assertThat(getContentData(library, "application/elm+json"), is(equalTo("ELM JSON")));
    assertThat(getContentData(library, "application/elm+xml"), is(equalTo("ELM XML")));
  }

  @Test
  void toFhirLibraryShouldRejectMalformedResource() {
    // given
    CqlLibraryDto cqlLibrary =
        CqlLibraryDto.builder()
            .cqlLibraryName("MalformedLibrary")
            .fhirResource("{not-json")
            .build();

    // when
    DataFormatException exception =
        assertThrows(
            DataFormatException.class,
            () -> externalLibraryResourceMapper.toFhirLibrary(cqlLibrary));

    // then
    assertThat(exception.getMessage(), containsString("MalformedLibrary"));
  }

  @Test
  void toFhirLibraryShouldRejectWrongResourceType() {
    // given
    CqlLibraryDto cqlLibrary =
        CqlLibraryDto.builder()
            .cqlLibraryName("ObservationResource")
            .fhirResource("{\"resourceType\":\"Observation\",\"status\":\"final\",\"code\":{}}")
            .build();

    // when
    DataFormatException exception =
        assertThrows(
            DataFormatException.class,
            () -> externalLibraryResourceMapper.toFhirLibrary(cqlLibrary));

    // then
    assertThat(exception.getMessage(), containsString("ObservationResource"));
  }

  private String getContentData(Library library, String contentType) {
    return new String(getContent(library, contentType).getData(), StandardCharsets.UTF_8);
  }

  private Attachment getContent(Library library, String contentType) {
    return library.getContent().stream()
        .filter(content -> contentType.equals(content.getContentType()))
        .findFirst()
        .orElseThrow();
  }
}

package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.models.dto.CqlLibraryDto;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.library.CqlLibrary;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class LibraryTranslatorServiceTest implements ResourceFileUtil, LibraryHelper {
  private static final String TOKEN = "token";
  private LibraryTranslatorService libraryTranslatorService;
  @Mock private LibraryCqlVisitorFactory libCqlVisitorFactory;
  @Mock private ElmTranslatorClient elmTranslatorClient;

  private CqlLibrary cqlLibrary;
  private CqlLibraryDto cqlLibraryDto;
  private String exm1234Cql;
  private org.hl7.fhir.r5.model.Library r5Library;

  @BeforeEach
  public void createCqlLibrary() {
    libraryTranslatorService =
        new LibraryTranslatorService(
            libCqlVisitorFactory,
            elmTranslatorClient,
            new ExternalLibraryResourceMapper(FhirContext.forR4Cached()));
    exm1234Cql = getStringFromTestResource("/test-cql/EXM124v7QICore4.cql");
    CqlLibrary cqlLibrary = createCqlLibrary(exm1234Cql);
    cqlLibraryDto =
        CqlLibraryDto.builder()
            .id(cqlLibrary.getId())
            .cqlLibraryName(cqlLibrary.getCqlLibraryName())
            .version(cqlLibrary.getVersion().toString())
            .publisher(cqlLibrary.getPublisher())
            .description(cqlLibrary.getDescription())
            .experimental(cqlLibrary.isExperimental())
            .cql(cqlLibrary.getCql())
            .elmJson(cqlLibrary.getElmJson())
            .elmXml(cqlLibrary.getElmXml())
            .build();
    r5Library =
        convertToFhirR5Resource(
            org.hl7.fhir.r5.model.Library.class,
            getStringFromTestResource("/humanReadable/effective-data-requirements.json"));
  }

  @Test
  public void convertToFhirLibraryForPublishableBundle() {
    var visitor = new LibraryCqlVisitorFactory().visit(exm1234Cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class),
            anyBoolean(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
        .thenReturn(r5Library);

    Library library =
        libraryTranslatorService.convertToFhirLibrary(
            cqlLibraryDto, null, BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT_PUBLISH, TOKEN);
    assertEquals(library.getName(), cqlLibraryDto.getCqlLibraryName());
    assertEquals(library.getVersion(), cqlLibraryDto.getVersion());
    assertThat(library.getTitle(), is(equalTo(cqlLibraryDto.getCqlLibraryName())));
    assertThat(library.getPublisher(), is(equalTo(cqlLibraryDto.getPublisher())));
    Identifier identifier = new Identifier();
    identifier.setUse(IdentifierUse.OFFICIAL);
    identifier.setSystem("https://madie.cms.gov/login");
    identifier.setValue(cqlLibraryDto.getId());
    assertThat(library.getIdentifier().get(0).getValue(), is(equalTo(identifier.getValue())));
    assertThat(library.getIdentifier().get(0).getSystem(), is(equalTo(identifier.getSystem())));
    assertThat(library.getIdentifier().get(0).getUse(), is(equalTo(identifier.getUse())));
    assertThat(library.getId(), is(equalTo(cqlLibraryDto.getCqlLibraryName())));
    assertThat(library.getId(), is(equalTo(cqlLibraryDto.getCqlLibraryName())));
    assertThat(
        library.getRelatedArtifact().get(0).getDisplay(),
        is(equalTo(r5Library.getRelatedArtifact().get(0).getDisplay())));
    assertThat(library.getMeta().getProfile().size(), is(equalTo(7)));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.SHAREABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.COMPUTABLE_LIBRARY_URI), is(true));
    assertThat(
        library.getMeta().hasProfile(UriConstants.Library.PUBLISHABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.EXECUTABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.CQL_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.ELM_JSON_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.ELM_XML_LIBRARY_URI), is(true));
    // no cqlOption extension or contained Parameters for publishable bundleType
    long cqlOptionExtensions =
        library.getExtension().stream()
            .filter(extension -> UriConstants.Library.CQL_OPTIONS_URL.equals(extension.getUrl()))
            .count();
    assertThat(cqlOptionExtensions, is(equalTo(0L)));
    assertThat(library.getContained().isEmpty(), is(true));
  }

  @Test
  public void testConvertToFhirLibraryHandlesElmJsonElmXml() {
    var visitor = new LibraryCqlVisitorFactory().visit(exm1234Cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class),
            anyBoolean(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
        .thenReturn(r5Library);

    cqlLibraryDto =
        CqlLibraryDto.builder()
            .id(cqlLibraryDto.getId())
            .cqlLibraryName(cqlLibraryDto.getCqlLibraryName())
            .version(cqlLibraryDto.getVersion())
            .publisher(cqlLibraryDto.getPublisher())
            .description(cqlLibraryDto.getDescription())
            .experimental(cqlLibraryDto.isExperimental())
            .cql(cqlLibraryDto.getCql())
            .elmJson("ELMJSON")
            .elmXml("ELMXML")
            .build();

    Library library =
        libraryTranslatorService.convertToFhirLibrary(
            cqlLibraryDto, null, BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT, TOKEN);
    assertThat(library.getName(), is(equalTo(cqlLibraryDto.getCqlLibraryName())));
    assertThat(library.getContent(), is(notNullValue()));
    assertThat(library.getContent().size(), is(equalTo(3)));
    assertThat(library.getExtension().size(), is(equalTo(2)));
    assertThat(library.getContained().size(), is(equalTo(1)));
    assertThat(library.getContained().get(0), is(instanceOf(Parameters.class)));
    assertThat(library.getContained().get(0).getIdElement().getIdPart(), is(equalTo("options")));
    assertThat(library.getMeta().getProfile().size(), is(equalTo(7)));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.SHAREABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.COMPUTABLE_LIBRARY_URI), is(true));
    assertThat(
        library.getMeta().hasProfile(UriConstants.Library.PUBLISHABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.EXECUTABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.CQL_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.ELM_JSON_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.ELM_XML_LIBRARY_URI), is(true));
  }

  @Test
  public void convertToFhirLibraryShouldUseExternalFhirResource() {
    // given - set up mocks
    String externalFhirResource =
        """
        {
          "resourceType": "Library",
          "id": "source-library-id",
          "meta": {"profile": ["http://example.org/source-profile"]},
          "url": "http://example.org/Library/ExternalLibrary",
          "identifier": [{
            "use": "official",
            "system": "http://example.org/libraries",
            "value": "source-identifier"
          }],
          "extension": [{
            "url": "http://example.org/source-extension",
            "valueString": "preserve-me"
          }],
          "name": "ExternalLibrary",
          "title": "Source Library Title",
          "status": "active",
          "version": "1.0.1",
          "publisher": "Source Publisher",
          "type": {"coding": [{"code": "logic-library"}]},
          "content": [
            {"contentType": "text/cql", "title": "Source CQL"},
            {"contentType": "application/elm+json", "title": "Source ELM JSON"}
          ],
          "relatedArtifact": [{
            "type": "depends-on",
            "display": "Source dependency",
            "resource": "Library/SourceDependency"
          }],
          "dataRequirement": [{"type": "Encounter"}]
        }
        """;
    CqlLibraryDto externalLibrary =
        CqlLibraryDto.builder()
            .id("external-library-id")
            .cqlLibraryName("ExternalLibrary")
            .version("1.0.1")
            .cql(exm1234Cql)
            .elmJson("ELM JSON")
            .elmXml("ELM XML")
            .namespacePrefix("hl7.fhir.us.qicore")
            .external(true)
            .fhirResource(externalFhirResource)
            .build();

    // when - call method under test
    Library library =
        libraryTranslatorService.convertToFhirLibrary(externalLibrary, null, null, TOKEN);

    // then - perform assertions
    assertThat(library.getIdElement().getIdPart(), is(equalTo("source-library-id")));
    assertThat(library.getUrl(), is(equalTo("http://example.org/Library/ExternalLibrary")));
    assertThat(library.getTitle(), is(equalTo("Source Library Title")));
    assertThat(library.getPublisher(), is(equalTo("Source Publisher")));
    assertThat(library.getVersion(), is(equalTo("1.0.1")));
    assertThat(library.getIdentifierFirstRep().getValue(), is(equalTo("source-identifier")));
    assertThat(
        library.getMeta().getProfile().get(0).getValue(),
        is(equalTo("http://example.org/source-profile")));
    assertThat(
        library.getExtension().get(0).getUrl(), is(equalTo("http://example.org/source-extension")));
    assertThat(library.getRelatedArtifactFirstRep().getDisplay(), is(equalTo("Source dependency")));
    assertThat(library.getDataRequirementFirstRep().getType(), is(equalTo("Encounter")));
    assertThat(getContent(library, "text/cql").getTitle(), is(equalTo("Source CQL")));
    assertThat(
        new String(getContent(library, "text/cql").getData(), StandardCharsets.UTF_8),
        is(equalTo(exm1234Cql)));
    assertThat(
        new String(getContent(library, "application/elm+json").getData(), StandardCharsets.UTF_8),
        is(equalTo("ELM JSON")));
    assertThat(
        new String(getContent(library, "application/elm+xml").getData(), StandardCharsets.UTF_8),
        is(equalTo("ELM XML")));
    // external libraries always get cqlOption extension and contained Parameters
    long cqlOptionExtensions =
        library.getExtension().stream()
            .filter(extension -> UriConstants.Library.CQL_OPTIONS_URL.equals(extension.getUrl()))
            .count();
    assertThat(cqlOptionExtensions, is(equalTo(1L)));
    assertThat(library.getContained().size(), is(equalTo(1)));
    assertThat(library.getContained().get(0), is(instanceOf(Parameters.class)));
    assertThat(library.getContained().get(0).getIdElement().getIdPart(), is(equalTo("options")));
    verifyNoInteractions(libCqlVisitorFactory, elmTranslatorClient);
  }

  @Test
  public void convertToFhirLibraryShouldNotDuplicateCqlOptionExtensionForExternalLibrary() {
    String externalFhirResource =
            """
        {
          "resourceType": "Library",
          "id": "source-library-id",
          "name": "ExternalLibrary",
          "status": "active",
          "version": "1.0.1",
          "type": {"coding": [{"code": "logic-library"}]},
          "extension": [
            {
              "url": "%s",
              "valueReference": {"reference": "#options"}
            }
          ]
        }
        """
            .formatted(UriConstants.Library.CQL_OPTIONS_URL);
    CqlLibraryDto externalLibrary =
        CqlLibraryDto.builder()
            .cqlLibraryName("ExternalLibrary")
            .version("1.0.1")
            .external(true)
            .elmJson("ELM JSON")
            .fhirResource(externalFhirResource)
            .build();

    Library library =
        libraryTranslatorService.convertToFhirLibrary(externalLibrary, null, null, TOKEN);

    long cqlOptionExtensions =
        library.getExtension().stream()
            .filter(extension -> UriConstants.Library.CQL_OPTIONS_URL.equals(extension.getUrl()))
            .count();
    assertThat(cqlOptionExtensions, is(equalTo(1L)));
  }

  @Test
  public void convertToFhirLibraryShouldEnrichMissingExternalModuleDefinition() {
    // given - set up mocks
    CqlLibraryDto externalLibrary =
        CqlLibraryDto.builder()
            .cqlLibraryName("ExternalLibrary")
            .version("1.0.1")
            .cql(exm1234Cql)
            .external(true)
            .fhirResource(
                """
                {
                  "resourceType": "Library",
                  "id": "source-library-id",
                  "name": "ExternalLibrary",
                  "status": "active",
                  "version": "1.0.1",
                  "type": {"coding": [{"code": "logic-library"}]}
                }
                """)
            .build();

    // when - call method under test
    Library library =
        libraryTranslatorService.convertToFhirLibrary(externalLibrary, null, null, TOKEN);

    // then - perform assertions
    assertThat(library.getIdElement().getIdPart(), is(equalTo("source-library-id")));
    assertThat(library.getRelatedArtifact().isEmpty(), is(true));
    assertThat(library.getDataRequirement().isEmpty(), is(true));
    // external libraries always get cqlOption extension and contained Parameters
    long cqlOptionExtensions =
        library.getExtension().stream()
            .filter(extension -> UriConstants.Library.CQL_OPTIONS_URL.equals(extension.getUrl()))
            .count();
    assertThat(cqlOptionExtensions, is(equalTo(1L)));
    assertThat(library.getContained().size(), is(equalTo(1)));
    assertThat(library.getContained().get(0), is(instanceOf(Parameters.class)));
    assertThat(library.getContained().get(0).getIdElement().getIdPart(), is(equalTo("options")));
  }

  @Test
  public void convertToFhirLibraryShouldRejectMalformedExternalFhirResource() {
    // given - set up mocks
    CqlLibraryDto malformedLibrary =
        CqlLibraryDto.builder()
            .cqlLibraryName("MalformedLibrary")
            .external(true)
            .fhirResource("{not-json")
            .build();

    // when - call method under test
    DataFormatException exception =
        assertThrows(
            DataFormatException.class,
            () ->
                libraryTranslatorService.convertToFhirLibrary(malformedLibrary, null, null, TOKEN));

    // then - perform assertions
    assertThat(exception.getMessage(), containsString("MalformedLibrary"));
  }

  @Test
  public void convertToFhirLibraryShouldRejectWrongExternalResourceType() {
    // given - set up mocks
    CqlLibraryDto externalLibrary =
        CqlLibraryDto.builder()
            .cqlLibraryName("ObservationResource")
            .external(true)
            .fhirResource("{\"resourceType\":\"Observation\",\"status\":\"final\",\"code\":{}}")
            .build();

    // when - call method under test
    DataFormatException exception =
        assertThrows(
            DataFormatException.class,
            () ->
                libraryTranslatorService.convertToFhirLibrary(externalLibrary, null, null, TOKEN));

    // then - perform assertions
    assertThat(exception.getMessage(), containsString("ObservationResource"));
  }

  @Test
  public void convertToFhirLibraryShouldGenerateResourceWhenExternalFhirResourceIsMissing() {
    // given - set up mocks
    var visitor = new LibraryCqlVisitorFactory().visit(exm1234Cql);
    CqlLibraryDto externalLibrary =
        CqlLibraryDto.builder()
            .id("external-library-id")
            .cqlLibraryName("ExternalLibrary")
            .version("1.0.1")
            .cql(exm1234Cql)
            .namespacePrefix("hl7.fhir.us.qicore")
            .external(true)
            .build();
    when(libCqlVisitorFactory.visit(exm1234Cql)).thenReturn(visitor);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class),
            anyBoolean(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
        .thenReturn(r5Library);

    // when - call method under test
    Library library =
        libraryTranslatorService.convertToFhirLibrary(externalLibrary, null, null, TOKEN);

    // then - perform assertions
    assertThat(library.getId(), is(equalTo("ExternalLibrary")));
    assertThat(library.getVersion(), is(equalTo("1.0.1")));
  }

  private org.hl7.fhir.r4.model.Attachment getContent(Library library, String contentType) {
    return library.getContent().stream()
        .filter(content -> contentType.equals(content.getContentType()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  public void testConvertToFhirLibraryIncludesDrcExtension() {
    String cql = getStringFromTestResource("/test-cql/EXM124v7QICore5.cql");
    var visitor = new LibraryCqlVisitorFactory().visit(cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class),
            anyBoolean(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
        .thenReturn(r5Library);
    CqlLibrary cqlLib = createCqlLibrary(cql);
    CqlLibraryDto cqlLibDto =
        CqlLibraryDto.builder()
            .id(cqlLib.getId())
            .cqlLibraryName(cqlLib.getCqlLibraryName())
            .version(cqlLib.getVersion().toString())
            .publisher(cqlLib.getPublisher())
            .description(cqlLib.getDescription())
            .experimental(cqlLib.isExperimental())
            .cql(cqlLib.getCql())
            .elmJson("ELMJSON")
            .elmXml("ELMXML")
            .build();

    Library library =
        libraryTranslatorService.convertToFhirLibrary(
            cqlLibDto, null, BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT, TOKEN);
    assertThat(library.getName(), is(equalTo(cqlLibDto.getCqlLibraryName())));
    assertThat(library.getContent(), is(notNullValue()));
    assertThat(library.getContent().size(), is(equalTo(3)));
    assertThat(library.getExtension().size(), is(equalTo(3)));
    assertThat(library.getContained().size(), is(equalTo(1)));
    assertThat(library.getContained().get(0), is(instanceOf(Parameters.class)));
    assertThat(library.getContained().get(0).getIdElement().getIdPart(), is(equalTo("options")));
    assertThat(library.getMeta().getProfile().size(), is(equalTo(7)));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.SHAREABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.COMPUTABLE_LIBRARY_URI), is(true));
    assertThat(
        library.getMeta().hasProfile(UriConstants.Library.PUBLISHABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.EXECUTABLE_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.CQL_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.ELM_JSON_LIBRARY_URI), is(true));
    assertThat(library.getMeta().hasProfile(UriConstants.Library.ELM_XML_LIBRARY_URI), is(true));
  }
}

package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.library.CqlLibrary;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class LibraryTranslatorServiceTest implements ResourceFileUtil, LibraryHelper {
  private static final String TOKEN = "token";
  @InjectMocks private LibraryTranslatorService libraryTranslatorService;
  @Mock private LibraryCqlVisitorFactory libCqlVisitorFactory;
  @Mock private ElmTranslatorClient elmTranslatorClient;
  ;

  private CqlLibrary cqlLibrary;
  private String exm1234Cql;
  private org.hl7.fhir.r5.model.Library r5Library;

  @BeforeEach
  public void createCqlLibrary() {
    exm1234Cql = getStringFromTestResource("/test-cql/EXM124v7QICore4.cql");
    cqlLibrary = createCqlLibrary(exm1234Cql);
    r5Library =
        convertToFhirR5Resource(
            org.hl7.fhir.r5.model.Library.class,
            getStringFromTestResource("/humanReadable/effective-data-requirements.json"));
  }

  @Test
  public void convertToFhirLibrary() {
    var visitor = new LibraryCqlVisitorFactory().visit(exm1234Cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class), anyBoolean(), anyString(), CqlCompilerException.ErrorSeverity.Info))
        .thenReturn(r5Library);

    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary, null, TOKEN);
    assertEquals(library.getName(), cqlLibrary.getCqlLibraryName());
    assertEquals(library.getVersion(), cqlLibrary.getVersion().toString());
    assertThat(library.getTitle(), is(equalTo(cqlLibrary.getCqlLibraryName())));
    assertThat(library.getPublisher(), is(equalTo(cqlLibrary.getPublisher())));
    Identifier identifier = new Identifier();
    identifier.setUse(IdentifierUse.OFFICIAL);
    identifier.setSystem("https://madie.cms.gov/login");
    identifier.setValue(cqlLibrary.getId());
    assertThat(library.getIdentifier().get(0).getValue(), is(equalTo(identifier.getValue())));
    assertThat(library.getIdentifier().get(0).getSystem(), is(equalTo(identifier.getSystem())));
    assertThat(library.getIdentifier().get(0).getUse(), is(equalTo(identifier.getUse())));
    assertThat(library.getId(), is(equalTo(cqlLibrary.getCqlLibraryName())));
    assertThat(library.getId(), is(equalTo(cqlLibrary.getCqlLibraryName())));
    assertThat(
        library.getRelatedArtifact().get(0).getDisplay(),
        is(equalTo(r5Library.getRelatedArtifact().get(0).getDisplay())));
  }

  @Test
  public void testConvertToFhirLibraryHandlesElmJsonElmXml() {
    var visitor = new LibraryCqlVisitorFactory().visit(exm1234Cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class), anyBoolean(), anyString(), CqlCompilerException.ErrorSeverity.Info))
        .thenReturn(r5Library);

    cqlLibrary.setElmJson("ELMJSON");
    cqlLibrary.setElmXml("ELMXML");

    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary, null, TOKEN);
    assertThat(library.getName(), is(equalTo(cqlLibrary.getCqlLibraryName())));
    assertThat(library.getContent(), is(notNullValue()));
    assertThat(library.getContent().size(), is(equalTo(3)));
    assertThat(library.getExtension().size(), is(equalTo(1)));
  }

  @Test
  public void testConvertToFhirLibraryIncludesDrcExtension() {
    String cql = getStringFromTestResource("/test-cql/EXM124v7QICore5.cql");
    var visitor = new LibraryCqlVisitorFactory().visit(cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class), anyBoolean(), anyString(), CqlCompilerException.ErrorSeverity.Info))
        .thenReturn(r5Library);
    CqlLibrary cqlLib = createCqlLibrary(cql);
    cqlLib.setElmJson("ELMJSON");
    cqlLib.setElmXml("ELMXML");

    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLib, null, TOKEN);
    assertThat(library.getName(), is(equalTo(cqlLib.getCqlLibraryName())));
    assertThat(library.getContent(), is(notNullValue()));
    assertThat(library.getContent().size(), is(equalTo(3)));
    assertThat(library.getExtension().size(), is(equalTo(2)));
  }
}

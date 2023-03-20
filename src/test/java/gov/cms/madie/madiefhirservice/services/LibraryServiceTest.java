package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.exceptions.DuplicateLibraryException;
import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.exceptions.LibraryAttachmentNotFoundException;
import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.library.CqlLibrary;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest implements LibraryHelper, ResourceFileUtil {

  @InjectMocks private LibraryService libraryService;

  @Mock private HapiFhirServer hapiFhirServer;
  @Mock private LibraryTranslatorService libraryTranslatorService;
  @Mock private LibraryCqlVisitorFactory libCqlVisitorFactory;
  @Mock private HumanReadableService humanReadableService;
  private Library fhirHelpersLibrary;

  Bundle bundle = new Bundle();

  @BeforeEach
  void buildLibraryBundle() {

    String fhirHelpersCql = getStringFromTestResource("/includes/FHIRHelpers.cql");
    fhirHelpersLibrary = createLibrary(fhirHelpersCql);

    Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
    bundleEntryComponent.setResource(fhirHelpersLibrary);
  }

  @Test
  void testSuccessfullyFindCqlInHapiLibrary() {
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
        .thenReturn(Optional.of(fhirHelpersLibrary));
    String testCql = libraryService.getLibraryCql("FHIRHelpers", "4.0.001");
    assertTrue(testCql.contains("FHIRHelpers"));
  }

  @Test
  void testLibraryBundleHasNoEntry() {
    bundle.setEntry(null);
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    Throwable exception =
        assertThrows(
            HapiLibraryNotFoundException.class,
            () -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
    assertEquals(
        "Cannot find a Hapi Fhir Library with name: FHIRHelpers, version: 4.0.001",
        exception.getMessage());
  }

  @Test
  void testLibraryIsNotReturnedByHapi() {
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
        .thenReturn(Optional.empty());

    Throwable exception =
        assertThrows(
            HapiLibraryNotFoundException.class,
            () -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
    assertEquals(
        "Cannot find a Hapi Fhir Library with name: FHIRHelpers, version: 4.0.001",
        exception.getMessage());
  }

  @Test
  void testLibraryDoesNotContainAnyAttachments() {
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
        .thenReturn(Optional.of(fhirHelpersLibrary));

    fhirHelpersLibrary.setContent(null);
    Throwable exception =
        assertThrows(
            LibraryAttachmentNotFoundException.class,
            () -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
    assertEquals(
        "Cannot find any attachment for library name: FHIRHelpers, version: 4.0.001",
        exception.getMessage());
  }

  @Test
  void testLibraryDoesNotContainCqlTextAttachment() {
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
        .thenReturn(Optional.of(fhirHelpersLibrary));

    fhirHelpersLibrary.getContent().get(0).setContentType("text/test");
    Throwable exception =
        assertThrows(
            LibraryAttachmentNotFoundException.class,
            () -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
    assertEquals(
        "Cannot find attachment type text/cql for library name: FHIRHelpers, version: 4.0.001",
        exception.getMessage());
  }

  @Test
  void createLibraryResourceForCqlLibraryWhenLibraryIsValidAndNotDuplicate() {
    String cql = getStringFromTestResource("/test-cql/EXM124v7QICore4.cql");
    CqlLibrary cqlLibrary = createCqlLibrary(cql);
    Library library = createLibrary(cql);

    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(new Bundle());
    when(libraryTranslatorService.convertToFhirLibrary(cqlLibrary, null)).thenReturn(library);
    when(hapiFhirServer.createResource(any(Library.class))).thenReturn(new MethodOutcome());
    when(humanReadableService.generateLibraryHumanReadable(any(Library.class)))
        .thenReturn("<div>Narrative Text</div>");

    Library libraryResource = libraryService.createLibraryResourceForCqlLibrary(cqlLibrary);

    assertEquals(libraryResource.getName(), cqlLibrary.getCqlLibraryName());
    assertEquals(libraryResource.getVersion(), cqlLibrary.getVersion().toString());
    assertEquals(
        libraryResource.getText().getDivAsString(),
        "<div xmlns=\"http://www.w3.org/1999/xhtml\">Narrative Text</div>");
    assertEquals(libraryResource.getText().getStatus().getDisplay(), "Extensions");
  }

  @Test
  void testGetLibraryResourceAsCqlLibraryHandlesNoEntry() {
    bundle.setEntry(null);
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    assertThrows(
        HapiLibraryNotFoundException.class,
        () -> libraryService.getLibraryResourceAsCqlLibrary("TestLibrary", "1.0.000"));
    verify(hapiFhirServer, times(0))
        .findLibraryResourceInBundle(any(Bundle.class), any(Class.class));
  }

  @Test
  void testGetLibraryResourceAsCqlLibraryHandlesNoLibrary() {
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    when(hapiFhirServer.findLibraryResourceInBundle(any(Bundle.class), any(Class.class)))
        .thenReturn(Optional.empty());
    assertThrows(
        HapiLibraryNotFoundException.class,
        () -> libraryService.getLibraryResourceAsCqlLibrary("TestLibrary", "1.0.000"));
    verify(hapiFhirServer, times(1))
        .findLibraryResourceInBundle(any(Bundle.class), any(Class.class));
  }

  @Test
  void testGetLibraryResourceAsCqlLibraryReturnsCqlLibrary() {
    Library library = (Library) bundle.getEntry().get(0).getResource();
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    when(hapiFhirServer.findLibraryResourceInBundle(any(Bundle.class), any(Class.class)))
        .thenReturn(Optional.of(library));
    CqlLibrary cqlLibrary = new CqlLibrary();
    when(libraryTranslatorService.convertToCqlLibrary(eq(library))).thenReturn(cqlLibrary);

    CqlLibrary output = libraryService.getLibraryResourceAsCqlLibrary("TestLibrary", "1.0.000");
    assertThat(output, is(equalTo(cqlLibrary)));
    verify(hapiFhirServer, times(1))
        .findLibraryResourceInBundle(any(Bundle.class), any(Class.class));
  }

  @Test
  void createLibraryResourceForCqlLibraryWhenDuplicateLibrary() {
    String cql = getStringFromTestResource("/test-cql/EXM124v7QICore4.cql");
    CqlLibrary cqlLibrary = createCqlLibrary(cql);
    when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString()))
        .thenReturn(bundle);
    when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
        .thenReturn(Optional.of(new Library()));

    Throwable exception =
        assertThrows(
            DuplicateLibraryException.class,
            () -> libraryService.createLibraryResourceForCqlLibrary(cqlLibrary));
    String exceptionMessage =
        String.format(
            "Library resource with name: %s, version: %s already exists.",
            cqlLibrary.getCqlLibraryName(), cqlLibrary.getVersion());
    assertEquals(exceptionMessage, exception.getMessage());
  }

  @Test
  public void testGetIncludedLibraries() {
    String mainLibrary =
        "   library MainLibrary version '1.1.000'\n"
            + "   using FHIR version '4.0.1'\n"
            + "   include IncludedLibrary version '0.1.000' called IncludedLib\n";

    String includedLibrary =
        "library IncludedLibrary version '0.1.000'\nusing FHIR version '4.0.1'";

    var visitor1 = new LibraryCqlVisitorFactory().visit(mainLibrary);
    var visitor2 = new LibraryCqlVisitorFactory().visit(includedLibrary);

    Attachment attachment =
        new Attachment().setContentType("text/cql").setData(includedLibrary.getBytes());
    Library library =
        new Library()
            .setName("IncludedLibrary")
            .setVersion("0.1.0")
            .setContent(List.of(attachment));

    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor1).thenReturn(visitor2);
    when(hapiFhirServer.fetchHapiLibrary(anyString(), anyString()))
        .thenReturn(Optional.of(library));

    Map<String, Library> includedLibraryMap = new HashMap<>();
    libraryService.getIncludedLibraries(mainLibrary, includedLibraryMap);
    assertThat(includedLibraryMap.size(), is(equalTo(1)));
    assertNotNull(includedLibraryMap.get("IncludedLibrary0.1.0"));
  }

  @Test
  public void testGetIncludedLibrariesWhenBlankCql() {
    String mainLibrary = "";
    Map<String, Library> libraries = new HashMap<>();

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> libraryService.getIncludedLibraries(mainLibrary, libraries));

    assertThat(exception.getMessage(), is(equalTo("Please provide valid arguments.")));
  }

  @Test
  public void testGetIncludedLibrariesWhenIncludedLibraryNotInHapi() {
    String mainLibrary =
        "  library MainLibrary version '1.1.000'\n"
            + "  using FHIR version '4.0.1'\n"
            + "  include IncludedLibrary version '0.1.000' called IncludedLib\n";

    String includedLibrary =
        "library IncludedLibrary version '0.1.000'\nusing FHIR version '4.0.1'";

    var visitor1 = new LibraryCqlVisitorFactory().visit(mainLibrary);
    var visitor2 = new LibraryCqlVisitorFactory().visit(includedLibrary);

    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor1).thenReturn(visitor2);
    when(hapiFhirServer.fetchHapiLibrary(anyString(), anyString())).thenReturn(Optional.empty());

    Map<String, Library> libraries = new HashMap<>();
    Exception exception =
        assertThrows(
            HapiLibraryNotFoundException.class,
            () -> libraryService.getIncludedLibraries(mainLibrary, libraries));

    assertThat(
        exception.getMessage(),
        is(
            equalTo(
                "Cannot find a Hapi Fhir Library with name: IncludedLibrary, version: 0.1.000")));
  }
}

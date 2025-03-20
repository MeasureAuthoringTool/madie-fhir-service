package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.exceptions.*;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.library.CqlLibrary;
import org.cqframework.cql.cql2elm.CqlCompilerException;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest implements LibraryHelper, ResourceFileUtil {

  @InjectMocks private LibraryService libraryService;

  @Mock private CqlLibraryService cqlLibraryService;
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

    CqlLibrary cqlLibrary =
        CqlLibrary.builder()
            .cqlLibraryName("IncludedLibrary")
            .version(Version.builder().major(0).minor(1).revisionNumber(0).build())
            .build();

    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor1).thenReturn(visitor2);
    when(cqlLibraryService.getLibrary(
            anyString(), anyString(), anyString(), any(CqlCompilerException.ErrorSeverity.class)))
        .thenReturn(cqlLibrary);
    when(libraryTranslatorService.convertToFhirLibrary(any(CqlLibrary.class), any(), anyString()))
        .thenReturn(library);

    Map<String, Library> includedLibraryMap = new HashMap<>();
    libraryService.getIncludedLibraries(
        mainLibrary,
        includedLibraryMap,
        BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT,
        CqlCompilerException.ErrorSeverity.Info,
        "TOKEN");
    assertThat(includedLibraryMap.size(), is(equalTo(1)));
    assertNotNull(includedLibraryMap.get("IncludedLibrary0.1.000"));
  }

  @Test
  public void testGetIncludedLibrariesWhenBlankCql() {
    String mainLibrary = "";
    Map<String, Library> libraries = new HashMap<>();

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                libraryService.getIncludedLibraries(
                    mainLibrary,
                    libraries,
                    BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT,
                    CqlCompilerException.ErrorSeverity.Info,
                    "TOKEN"));

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
    when(cqlLibraryService.getLibrary(
            anyString(), anyString(), anyString(), any(CqlCompilerException.ErrorSeverity.class)))
        .thenThrow(new CqlLibraryNotFoundException("Test Exception Here!", "0.1.000"));

    Map<String, Library> libraries = new HashMap<>();
    Exception exception =
        assertThrows(
            CqlLibraryNotFoundException.class,
            () ->
                libraryService.getIncludedLibraries(
                    mainLibrary,
                    libraries,
                    BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT,
                    CqlCompilerException.ErrorSeverity.Info,
                    "TOKEN"));

    assertThat(
        exception.getMessage(),
        is(equalTo("Cannot find a CQL Library with name: Test Exception Here!, version: 0.1.000")));
  }
}

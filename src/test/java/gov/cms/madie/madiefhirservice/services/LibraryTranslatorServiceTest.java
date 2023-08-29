package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.common.Version;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class LibraryTranslatorServiceTest implements ResourceFileUtil, LibraryHelper {
  @InjectMocks private LibraryTranslatorService libraryTranslatorService;
  @Mock private LibraryCqlVisitorFactory libCqlVisitorFactory;

  private CqlLibrary cqlLibrary;
  private String exm1234Cql;

  @BeforeEach
  public void createCqlLibrary() {
    exm1234Cql = getStringFromTestResource("/test-cql/EXM124v7QICore4.cql");
    cqlLibrary = createCqlLibrary(exm1234Cql);
  }

  @Test
  public void convertToFhirLibrary() {
    var visitor = new LibraryCqlVisitorFactory().visit(exm1234Cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);

    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary);
    assertEquals(library.getName(), cqlLibrary.getCqlLibraryName());
    assertEquals(library.getVersion(), cqlLibrary.getVersion().toString());
    assertEquals(library.getDataRequirement().size(), visitor.getDataRequirements().size());
    assertThat(library.getTitle(), is(equalTo(cqlLibrary.getCqlLibraryName())));
    assertThat(library.getPublisher(), is(equalTo(cqlLibrary.getPublisher())));
    Identifier identifier = new Identifier();
    identifier.setUse(IdentifierUse.OFFICIAL);
    identifier.setSystem("https://madie.cms.gov/login");
    identifier.setValue(library.getId());
    assertThat(library.getIdentifier().get(0).getValue(), is(equalTo(identifier.getValue())));
    assertThat(library.getIdentifier().get(0).getSystem(), is(equalTo(identifier.getSystem())));
    assertThat(library.getIdentifier().get(0).getUse(), is(equalTo(identifier.getUse())));
  }

  @Test
  public void testConvertToFhirLibraryHandlesElmJsonElmXml() {
    var visitor = new LibraryCqlVisitorFactory().visit(exm1234Cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);
    cqlLibrary.setElmJson("ELMJSON");
    cqlLibrary.setElmXml("ELMXML");

    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary);
    assertThat(library.getName(), is(equalTo(cqlLibrary.getCqlLibraryName())));
    assertThat(library.getContent(), is(notNullValue()));
    assertThat(library.getContent().size(), is(equalTo(3)));
  }

  @Test
  public void testConvertToCqlLibrary() {
    Library library = new Library();
    library.setName("LibraryName");
    library.setVersion("1.2.000");
    Attachment a1 = new Attachment();
    a1.setContentType(LibraryTranslatorService.JSON_ELM_CONTENT_TYPE);
    a1.setData("JSON_ELM_CONTENT".getBytes());
    Attachment a2 = new Attachment();
    a2.setContentType(LibraryTranslatorService.XML_ELM_CONTENT_TYPE);
    a2.setData("XML_ELM_CONTENT".getBytes());
    Attachment a3 = new Attachment();
    a3.setContentType(LibraryTranslatorService.CQL_CONTENT_TYPE);
    a3.setData("CQL_CONTENT".getBytes());
    library.setContent(List.of(a1, a2, a3));

    CqlLibrary output = libraryTranslatorService.convertToCqlLibrary(library);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCqlLibraryName(), is(equalTo("LibraryName")));
    assertThat(output.getVersion(), is(equalTo(Version.parse("1.2.000"))));
    assertThat(output.getCql(), is(equalTo("CQL_CONTENT")));
    assertThat(output.getElmJson(), is(equalTo("JSON_ELM_CONTENT")));
    assertThat(output.getElmXml(), is(equalTo("XML_ELM_CONTENT")));
  }

  @Test
  public void testAttachmentToStringHandlesNull() {
    String output = libraryTranslatorService.attachmentToString(null);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testAttachmentToStringReturnsString() {
    final String input = "Test Data";
    Attachment attachment = new Attachment();
    attachment.setData(input.getBytes());
    String output = libraryTranslatorService.attachmentToString(attachment);
    assertThat(output, is(notNullValue()));
    assertThat(output, is(equalTo(input)));
  }

  @Test
  public void testFindAttachmentOfContentTypeHandlesNullLibrary() {
    Attachment output =
        libraryTranslatorService.findAttachmentOfContentType(
            null, LibraryTranslatorService.CQL_CONTENT_TYPE);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testFindAttachmentOfContentTypeHandlesNullAttachments() {
    Library library = new Library();
    library.setContent(null);
    Attachment output =
        libraryTranslatorService.findAttachmentOfContentType(
            library, LibraryTranslatorService.CQL_CONTENT_TYPE);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testFindAttachmentOfContentTypeHandlesEmptyAttachments() {
    Library library = new Library();
    library.setContent(List.of());
    Attachment output =
        libraryTranslatorService.findAttachmentOfContentType(
            library, LibraryTranslatorService.CQL_CONTENT_TYPE);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testFindAttachmentOfContentTypeHandlesNonMatchingAttachment() {
    Library library = new Library();
    Attachment a1 = new Attachment();
    a1.setContentType(LibraryTranslatorService.JSON_ELM_CONTENT_TYPE);
    library.setContent(List.of(a1));
    Attachment output =
        libraryTranslatorService.findAttachmentOfContentType(
            library, LibraryTranslatorService.CQL_CONTENT_TYPE);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testFindAttachmentOfContentTypeHandlesMatchingAttachment() {
    Library library = new Library();
    Attachment a1 = new Attachment();
    a1.setContentType(LibraryTranslatorService.JSON_ELM_CONTENT_TYPE);
    a1.setData("Attachment1".getBytes());
    Attachment a2 = new Attachment();
    a2.setContentType(LibraryTranslatorService.CQL_CONTENT_TYPE);
    a2.setData("Attachment2".getBytes());
    library.setContent(List.of(a1, a2));
    Attachment output =
        libraryTranslatorService.findAttachmentOfContentType(
            library, LibraryTranslatorService.CQL_CONTENT_TYPE);
    assertThat(output, is(notNullValue()));
    assertThat(output.getContentType(), is(equalTo(LibraryTranslatorService.CQL_CONTENT_TYPE)));
    assertThat(output.getData(), is(equalTo("Attachment2".getBytes())));
  }
}

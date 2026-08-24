package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.madie.madiefhirservice.constants.LibraryContentTypeConstants;
import gov.cms.madie.models.dto.CqlLibraryDto;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ExternalLibraryResourceMapper {
  private final FhirContext qicoreFhirContext;

  public ExternalLibraryResourceMapper(
      @Qualifier("qicoreFhirContext") FhirContext qicoreFhirContext) {
    this.qicoreFhirContext = qicoreFhirContext;
  }

  public Library toFhirLibrary(CqlLibraryDto cqlLibrary) {
    Library library = parseExternalFhirLibrary(cqlLibrary);
    restoreContent(library, cqlLibrary);
    return library;
  }

  private Library parseExternalFhirLibrary(CqlLibraryDto cqlLibrary) {
    try {
      return qicoreFhirContext
          .newJsonParser()
          .parseResource(Library.class, cqlLibrary.getFhirResource());
    } catch (DataFormatException ex) {
      throw new DataFormatException(
          "Unable to parse external FHIR Library [" + cqlLibrary.getCqlLibraryName() + "]", ex);
    }
  }

  private void restoreContent(Library library, CqlLibraryDto cqlLibrary) {
    restoreAttachment(library, LibraryContentTypeConstants.CQL, cqlLibrary.getCql());
    restoreAttachment(library, LibraryContentTypeConstants.ELM_XML, cqlLibrary.getElmXml());
    restoreAttachment(library, LibraryContentTypeConstants.ELM_JSON, cqlLibrary.getElmJson());
  }

  private void restoreAttachment(Library library, String contentType, String content) {
    if (content == null) {
      return;
    }
    Attachment attachment =
        library.getContent().stream()
            .filter(existing -> contentType.equals(existing.getContentType()))
            .findFirst()
            .orElseGet(() -> library.addContent().setContentType(contentType));
    attachment.setData(content.getBytes(StandardCharsets.UTF_8));
  }
}

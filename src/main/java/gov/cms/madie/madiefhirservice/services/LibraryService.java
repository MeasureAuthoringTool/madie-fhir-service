package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitor;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.exceptions.LibraryAttachmentNotFoundException;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.models.library.CqlLibrary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryService {

  private final CqlLibraryService cqlLibraryService;
  private final LibraryTranslatorService libraryTranslatorService;
  private final LibraryCqlVisitorFactory libCqlVisitorFactory;
  private final HumanReadableService humanReadableService;

  public Library cqlLibraryToFhirLibrary(
      CqlLibrary cqlLibrary, final String bundleType, String accessToken) {
    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary, null, accessToken);
    if (BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT.equals(bundleType)) {
      library.setText(createLibraryNarrativeText(library));
    }
    return library;
  }

  public void getIncludedLibraries(
      String cql,
      Map<String, Library> libraryMap,
      final String bundleType,
      final String accessToken) {
    if (StringUtils.isBlank(cql) || libraryMap == null) {
      log.error("Invalid method arguments provided to getIncludedLibraries");
      throw new IllegalArgumentException("Please provide valid arguments.");
    }

    LibraryCqlVisitor visitor = libCqlVisitorFactory.visit(cql);
    for (Pair<String, String> libraryNameValuePair : visitor.getIncludedLibraries()) {
      String key = libraryNameValuePair.getLeft() + libraryNameValuePair.getRight();
      if (!libraryMap.containsKey(key)) {
        CqlLibrary cqlLibrary =
            cqlLibraryService.getLibrary(
                libraryNameValuePair.getLeft(), libraryNameValuePair.getRight(), accessToken);
        Library library = cqlLibraryToFhirLibrary(cqlLibrary, bundleType, accessToken);
        libraryMap.put(key, library);

        Attachment attachment = findCqlAttachment(library);
        getIncludedLibraries(new String(attachment.getData()), libraryMap, bundleType, accessToken);
      }
    }
  }

  private Narrative createLibraryNarrativeText(Library library) {
    Narrative narrative = new Narrative();
    narrative.setStatus(NarrativeStatus.EXTENSIONS);
    narrative.setDivAsString(humanReadableService.generateLibraryHumanReadable(library));
    return narrative;
  }

  private Attachment findCqlAttachment(Library library) {
    return library.getContent().stream()
        .filter(a -> a.getContentType().equals("text/cql"))
        .findFirst()
        .orElseThrow(() -> new LibraryAttachmentNotFoundException(library, "text/cql"));
  }
}

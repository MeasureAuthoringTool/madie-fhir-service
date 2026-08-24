package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.constants.LibraryContentTypeConstants;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitor;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.models.dto.CqlLibraryDto;
import gov.cms.madie.madiefhirservice.exceptions.LibraryAttachmentNotFoundException;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryService {

  private final CqlLibraryService cqlLibraryService;
  private final LibraryTranslatorService libraryTranslatorService;
  private final LibraryCqlVisitorFactory libCqlVisitorFactory;
  private final HumanReadableService humanReadableService;

  public Library cqlLibraryToFhirLibrary(
      CqlLibraryDto cqlLibrary, final String bundleType, String accessToken) {
    Library library =
        libraryTranslatorService.convertToFhirLibrary(cqlLibrary, null, bundleType, accessToken);
    if (BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT.equals(bundleType)) {
      library.setText(createLibraryNarrativeText(library));
    }
    return library;
  }

  public void getIncludedLibraries(
      String cql,
      Map<String, Library> libraryMap,
      final String bundleType,
      CqlCompilerException.ErrorSeverity errorSeverity,
      final String accessToken) {
    if (StringUtils.isBlank(cql) || libraryMap == null) {
      log.error("Invalid method arguments provided to getIncludedLibraries");
      throw new IllegalArgumentException("Please provide valid arguments.");
    }

    LibraryCqlVisitor visitor = libCqlVisitorFactory.visit(cql);
    for (Pair<String, String> libraryNameValuePair : visitor.getIncludedLibraries()) {
      String key = libraryNameValuePair.getLeft() + libraryNameValuePair.getRight();
      if (!libraryMap.containsKey(key)) {
        var libraryParts = parseLibraryString(libraryNameValuePair.getLeft());
        CqlLibraryDto cqlLibrary =
            cqlLibraryService.getLibrary(
                libraryParts[1], // name
                libraryNameValuePair.getRight(), // version
                Optional.ofNullable(libraryParts[0]), // prefix
                accessToken,
                errorSeverity);
        // Exclude external libraries from publishable bundles
        if (BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT_PUBLISH.equals(bundleType)
            && cqlLibrary.isExternal()) {
          continue;
        }
        // Todo If the library is already in libraryMap, we can skip the call to
        // cqlLibraryToFhirLibrary and assume the library is already in the correct format.
        // We can also skip the call to findCqlAttachment and getIncludedLibraries since we would
        // have already done that for the library when we first added it to the libraryMap.
        Library library = cqlLibraryToFhirLibrary(cqlLibrary, bundleType, accessToken);
        libraryMap.put(key, library);

        Attachment attachment = findCqlAttachment(library);
        getIncludedLibraries(
            new String(attachment.getData()), libraryMap, bundleType, errorSeverity, accessToken);
      }
    }
  }

  /**
   * Parses a library identifier into namespace and library name parts.
   *
   * <p>The split is performed on the last {@code '.'} character:
   *
   * <ul>
   *   <li>If {@code fullLibraryString} is {@code null} or blank, returns {@code {"", ""}}.
   *   <li>If no {@code '.'} is present, returns {@code {"", trimmedInput}}.
   *   <li>If {@code '.'} is present, returns {@code {trimmedNamespace, trimmedLibraryName}}.
   * </ul>
   *
   * @param fullLibraryString the raw library identifier, optionally namespace-qualified
   * @return a two-element array where index {@code 0} is the namespace (or empty string) and index
   *     {@code 1} is the library name (or empty string)
   */
  public String[] parseLibraryString(String fullLibraryString) {
    if (fullLibraryString == null || fullLibraryString.trim().isEmpty()) {
      return new String[] {"", ""};
    }

    int lastDotIndex = fullLibraryString.lastIndexOf('.');

    if (lastDotIndex == -1) {
      return new String[] {"", fullLibraryString.trim()};
    }

    String namespace = fullLibraryString.substring(0, lastDotIndex).trim();
    String libraryName = fullLibraryString.substring(lastDotIndex + 1).trim();

    return new String[] {namespace, libraryName};
  }

  private Narrative createLibraryNarrativeText(Library library) {
    Narrative narrative = new Narrative();
    narrative.setStatus(NarrativeStatus.EXTENSIONS);
    narrative.setDivAsString(humanReadableService.generateLibraryHumanReadable(library));
    return narrative;
  }

  private Attachment findCqlAttachment(Library library) {
    return library.getContent().stream()
        .filter(a -> a.getContentType().equals(LibraryContentTypeConstants.CQL))
        .findFirst()
        .orElseThrow(
            () -> new LibraryAttachmentNotFoundException(library, LibraryContentTypeConstants.CQL));
  }
}

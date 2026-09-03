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
import org.cqframework.cql.cql2elm.StringEscapeUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    getIncludedLibraries(null, cql, libraryMap, bundleType, errorSeverity, accessToken);
  }

  public void getIncludedLibraries(
      final String namespacePrefix,
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
        Optional<String> includedLibNamespacePrefix =
            Optional.ofNullable(libraryParts[0])
                .filter(StringUtils::isNotBlank)
                .or(() -> Optional.ofNullable(namespacePrefix).filter(StringUtils::isNotBlank));

        log.info(
            "Looking for library namespace prefix: [{}]", includedLibNamespacePrefix.orElse("N/A"));

        CqlLibraryDto cqlLibrary =
            cqlLibraryService.getLibrary(
                libraryParts[1], // name
                libraryNameValuePair.getRight(), // version
                includedLibNamespacePrefix,
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
            includedLibNamespacePrefix.orElse(null),
            new String(attachment.getData()),
            libraryMap,
            bundleType,
            errorSeverity,
            accessToken);
      }
    }
  }

  private String normalizeIdentifier(String identifierText) {
    String trimmedIdentifier = identifierText.trim();
    boolean isDelimited = trimmedIdentifier.startsWith("\"") || trimmedIdentifier.startsWith("`");
    if (isDelimited) {
      trimmedIdentifier = trimmedIdentifier.substring(1, trimmedIdentifier.length() - 1);
    }

    return StringEscapeUtils.unescapeCql(trimmedIdentifier);
  }

  private List<String> getQualifiedIdentifiers(String qualifiedIdentifier) {
    List<String> identifiers = new ArrayList<>();
    StringBuilder identifier = new StringBuilder();
    char enclosingCharacter = 0;
    boolean escaped = false;

    for (char character : qualifiedIdentifier.toCharArray()) {
      if (escaped) {
        identifier.append(character);
        escaped = false;
      } else if (character == '\\') {
        identifier.append(character);
        escaped = true;
      } else if (enclosingCharacter != 0) {
        identifier.append(character);
        if (character == enclosingCharacter) {
          enclosingCharacter = 0;
        }
      } else if (character == '"' || character == '`') {
        enclosingCharacter = character;
        identifier.append(character);
      } else if (character == '.') {
        identifiers.add(normalizeIdentifier(identifier.toString()));
        identifier.setLength(0);
      } else {
        identifier.append(character);
      }
    }

    identifiers.add(normalizeIdentifier(identifier.toString()));
    return identifiers;
  }

  /**
   * Parses a library identifier into namespace and library name parts.
   *
   * <p>Each qualified identifier component is normalized. The last component is the library name,
   * while preceding components form the namespace prefix:
   *
   * <ul>
   *   <li>If {@code fullLibraryString} is {@code null} or blank, returns {@code {"", ""}}.
   *   <li>If one identifier is present, returns {@code {"", normalizedIdentifier}}.
   *   <li>If multiple identifiers are present, returns the joined namespace and library name.
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

    List<String> identifiers = getQualifiedIdentifiers(fullLibraryString);
    String libraryName = identifiers.remove(identifiers.size() - 1);
    return new String[] {String.join(".", identifiers), libraryName};
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

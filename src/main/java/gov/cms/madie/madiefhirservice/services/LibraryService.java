package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitor;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.exceptions.LibraryAttachmentNotFoundException;
import gov.cms.madie.madiefhirservice.exceptions.MissingCqlException;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.models.library.CqlLibrary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryService {

  private final CqlLibraryService cqlLibraryService;
  private final LibraryTranslatorService libraryTranslatorService;
  private final LibraryCqlVisitorFactory libCqlVisitorFactory;
  private final HumanReadableService humanReadableService;
  private final ElmTranslatorClient elmTranslatorClient;

  public String getLibraryCql(String name, String version, final String accessToken) {
    CqlLibrary library = cqlLibraryService.getLibrary(name, version, accessToken);
    if (StringUtils.isBlank(library.getCql())) {
      throw new MissingCqlException(library);
    }
    return cqlLibraryService.getLibrary(name, version, accessToken).getCql();
  }

  public Library cqlLibraryToFhirLibrary(
      CqlLibrary cqlLibrary, final String bundleType, String accessToken) {
    // Use the DataRequirementsProcessor to typecast Profile types
    // (e.g. "QICore Simple Observation") to FHIR types (e.g. "Observation").
    List<DataRequirement> fhirTypedDataRequirements =
        retrieveLibraryDataRequirements(
            CqlLibraryDetails.builder()
                .libraryName(cqlLibrary.getCqlLibraryName())
                .cql(cqlLibrary.getCql())
                .build(),
            accessToken);
    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary);
    library.setDataRequirement(fhirTypedDataRequirements);
    if (BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT.equals(bundleType)) {
      library.setText(createLibraryNarrativeText(library));
    }
    return library;
  }

  private List<DataRequirement> retrieveLibraryDataRequirements(
      CqlLibraryDetails cqlLibraryDetails, String accessToken) {
    org.hl7.fhir.r5.model.Library r5moduleDefinition =
        elmTranslatorClient.getModuleDefinitionLibrary(cqlLibraryDetails, false, accessToken);
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r4.model.Library r4moduleDefinitionLibrary =
        (org.hl7.fhir.r4.model.Library) versionConvertor_40_50.convertResource(r5moduleDefinition);
    return r4moduleDefinitionLibrary.getDataRequirement();
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

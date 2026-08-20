package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.constants.LibraryContentTypeConstants;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.TranslatorConfigUtil;
import gov.cms.madie.models.dto.CqlLibraryDto;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.models.library.CqlLibrary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class LibraryTranslatorService {
  public static final String SYSTEM_CODE = "logic-library";
  public static final String UNKNOWN_VALUE = "UNKNOWN";

  private final LibraryCqlVisitorFactory libCqlVisitorFactory;
  private final ElmTranslatorClient elmTranslatorClient;
  private final ExternalLibraryResourceMapper externalLibraryResourceMapper;

  public LibraryTranslatorService(
      LibraryCqlVisitorFactory libCqlVisitorFactory,
      ElmTranslatorClient elmTranslatorClient,
      ExternalLibraryResourceMapper externalLibraryResourceMapper) {
    this.libCqlVisitorFactory = libCqlVisitorFactory;
    this.elmTranslatorClient = elmTranslatorClient;
    this.externalLibraryResourceMapper = externalLibraryResourceMapper;
  }

  public Library convertToFhirLibrary(
      CqlLibraryDto cqlLibraryDto, Set<String> expressions, String bundleType, String accessToken) {
    Library library;
    if (cqlLibraryDto.isExternal() && StringUtils.isNotBlank(cqlLibraryDto.getFhirResource())) {
      library = externalLibraryResourceMapper.toFhirLibrary(cqlLibraryDto);
    } else {
      library = convertToFhirLibrary(LibrarySource.from(cqlLibraryDto), expressions, accessToken);
    }
    // Add the CQL Options extension and parameters to the library if this is an export bundle.
    if (BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT.equals(bundleType)) {
      addCqlOptionExtensionIfMissing(library);
      Parameters cqlOptionParameters =
          TranslatorConfigUtil.getCqlOptionParameters(cqlLibraryDto.getElmJson());
      // remove any existing "options" parameters to avoid duplicates, then add the new one.
      library
          .getContained()
          .removeIf(resource -> "options".equals(resource.getIdElement().getIdPart()));
      library.getContained().add(cqlOptionParameters);
    }
    return library;
  }

  private Library convertToFhirLibrary(
      LibrarySource cqlLibrary, Set<String> expressions, String accessToken) {
    var visitor = libCqlVisitorFactory.visit(cqlLibrary.cql());
    Library library = new Library();
    library.setId(cqlLibrary.name());
    library.setLanguage("en");
    library.setName(cqlLibrary.name());
    library.setVersion(cqlLibrary.version());
    library.setDate(new Date());
    library.setStatus(Enumerations.PublicationStatus.ACTIVE);
    library.setPublisher(
        cqlLibrary.publisher() != null && StringUtils.isNotBlank(cqlLibrary.publisher())
            ? cqlLibrary.publisher()
            : UNKNOWN_VALUE);
    library.setDescription(Objects.toString(cqlLibrary.description(), UNKNOWN_VALUE));
    library.setExperimental(cqlLibrary.experimental());
    library.setContent(createContent(cqlLibrary.cql(), cqlLibrary.elmJson(), cqlLibrary.elmXml()));
    library.setType(createType(UriConstants.CodeSystem.LIBRARY_SYSTEM_TYPE_URI, SYSTEM_CODE));
    library.setUrl(FhirResourceHelpers.buildResourceFullUrl("Library", cqlLibrary.name()));
    library.getExtension().addAll(visitor.getDrcExtensions());
    library.setMeta(createLibraryMeta());
    library.setTitle(cqlLibrary.name());
    library.setPublisher(cqlLibrary.publisher());
    Identifier identifier = new Identifier();
    identifier.setUse(IdentifierUse.OFFICIAL);
    identifier.setSystem("https://madie.cms.gov/login");
    identifier.setValue(cqlLibrary.id());
    library.setIdentifier(List.of(identifier));
    // Use the DataRequirementsProcessor to construct data requirements and related artifacts.
    Library libraryModuleDefinition =
        retrieveLibraryModuleDefinition(
            CqlLibraryDetails.builder()
                .libraryName(cqlLibrary.name())
                .cql(cqlLibrary.cql())
                .expressions(expressions)
                .build(),
            accessToken);
    library.setRelatedArtifact(libraryModuleDefinition.getRelatedArtifact());
    library.setDataRequirement(libraryModuleDefinition.getDataRequirement());

    return library;
  }

  private record LibrarySource(
      String id,
      String name,
      String version,
      String cql,
      String elmJson,
      String elmXml,
      String publisher,
      String description,
      boolean experimental) {

    private static LibrarySource from(CqlLibrary cqlLibrary) {
      return new LibrarySource(
          cqlLibrary.getId(),
          cqlLibrary.getCqlLibraryName(),
          cqlLibrary.getVersion().toString(),
          cqlLibrary.getCql(),
          cqlLibrary.getElmJson(),
          cqlLibrary.getElmXml(),
          cqlLibrary.getPublisher(),
          cqlLibrary.getDescription(),
          cqlLibrary.isExperimental());
    }

    private static LibrarySource from(CqlLibraryDto cqlLibrary) {
      return new LibrarySource(
          cqlLibrary.getId(),
          cqlLibrary.getCqlLibraryName(),
          cqlLibrary.getVersion(),
          cqlLibrary.getCql(),
          cqlLibrary.getElmJson(),
          cqlLibrary.getElmXml(),
          cqlLibrary.getPublisher(),
          cqlLibrary.getDescription(),
          cqlLibrary.isExperimental());
    }
  }

  private Library retrieveLibraryModuleDefinition(
      CqlLibraryDetails cqlLibraryDetails, String accessToken) {
    org.hl7.fhir.r5.model.Library r5moduleDefinition =
        elmTranslatorClient.getModuleDefinitionLibrary(
            cqlLibraryDetails, false, accessToken, CqlCompilerException.ErrorSeverity.Info);
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    return (Library) versionConvertor_40_50.convertResource(r5moduleDefinition);
  }

  private Meta createLibraryMeta() {
    // Currently, only one profile is allowed, but Bryn is under the impression multiples should
    // work.
    // For now, it is just computable until we resolve this.
    return new Meta()
        .addProfile(UriConstants.Library.SHAREABLE_LIBRARY_URI)
        .addProfile(UriConstants.Library.COMPUTABLE_LIBRARY_URI)
        .addProfile(UriConstants.Library.PUBLISHABLE_LIBRARY_URI)
        .addProfile(UriConstants.Library.EXECUTABLE_LIBRARY_URI)
        .addProfile(UriConstants.Library.CQL_LIBRARY_URI)
        .addProfile(UriConstants.Library.ELM_JSON_LIBRARY_URI)
        .addProfile(UriConstants.Library.ELM_XML_LIBRARY_URI);
  }

  /**
   * @param elmJson elmJson String
   * @param cql cql String
   * @param elmXml elmXml String
   * @return The content element.
   */
  private List<Attachment> createContent(String cql, String elmJson, String elmXml) {
    List<Attachment> attachments = new ArrayList<>(3);
    if (cql != null) {
      attachments.add(createAttachment(LibraryContentTypeConstants.CQL, cql.getBytes()));
    }
    if (elmXml != null) {
      attachments.add(createAttachment(LibraryContentTypeConstants.ELM_XML, elmXml.getBytes()));
    }
    if (elmJson != null) {
      attachments.add(createAttachment(LibraryContentTypeConstants.ELM_JSON, elmJson.getBytes()));
    }
    return attachments;
  }

  private CodeableConcept createType(String type, String code) {
    return new CodeableConcept().setCoding(Collections.singletonList(new Coding(type, code, null)));
  }

  private void addCqlOptionExtensionIfMissing(Library library) {
    boolean cqlOptionExtensionPresent =
        library.getExtension().stream()
            .anyMatch(extension -> UriConstants.Library.CQL_OPTIONS_URL.equals(extension.getUrl()));
    if (!cqlOptionExtensionPresent) {
      library.addExtension(TranslatorConfigUtil.getCqlOptionExtension());
    }
  }

  /* rawData are bytes that are NOT base64 encoded */
  private Attachment createAttachment(String contentType, byte[] rawData) {
    return new Attachment().setContentType(contentType).setData(rawData == null ? null : rawData);
  }
}

package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
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
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class LibraryTranslatorService {
  public static final String CQL_CONTENT_TYPE = "text/cql";
  public static final String JSON_ELM_CONTENT_TYPE = "application/elm+json";
  public static final String XML_ELM_CONTENT_TYPE = "application/elm+xml";
  public static final String SYSTEM_CODE = "logic-library";
  public static final String UNKNOWN_VALUE = "UNKNOWN";

  private final LibraryCqlVisitorFactory libCqlVisitorFactory;
  private final ElmTranslatorClient elmTranslatorClient;
  private final FhirContext qicoreFhirContext;

  public LibraryTranslatorService(
      LibraryCqlVisitorFactory libCqlVisitorFactory,
      ElmTranslatorClient elmTranslatorClient,
      @Qualifier("qicoreFhirContext") FhirContext qicoreFhirContext) {
    this.libCqlVisitorFactory = libCqlVisitorFactory;
    this.elmTranslatorClient = elmTranslatorClient;
    this.qicoreFhirContext = qicoreFhirContext;
  }

  public Library convertToFhirLibrary(
      CqlLibrary cqlLibrary, Set<String> expressions, String accessToken) {
    return convertToFhirLibrary(LibrarySource.from(cqlLibrary), expressions, accessToken);
  }

  public Library convertToFhirLibrary(
      CqlLibraryDto cqlLibrary, Set<String> expressions, String accessToken) {
    if (cqlLibrary.isExternal() && StringUtils.isNotBlank(cqlLibrary.getFhirResource())) {
      return convertExternalFhirLibrary(cqlLibrary, expressions, accessToken);
    }
    return convertToFhirLibrary(LibrarySource.from(cqlLibrary), expressions, accessToken);
  }

  private Library convertExternalFhirLibrary(
      CqlLibraryDto cqlLibrary, Set<String> expressions, String accessToken) {
    Library library = parseExternalFhirLibrary(cqlLibrary);
    restoreContent(library, cqlLibrary);
    enrichMissingModuleDefinition(library, cqlLibrary, expressions, accessToken);
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
    restoreAttachment(library, CQL_CONTENT_TYPE, cqlLibrary.getCql());
    restoreAttachment(library, XML_ELM_CONTENT_TYPE, cqlLibrary.getElmXml());
    restoreAttachment(library, JSON_ELM_CONTENT_TYPE, cqlLibrary.getElmJson());
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

  private void enrichMissingModuleDefinition(
      Library library, CqlLibraryDto cqlLibrary, Set<String> expressions, String accessToken) {
    boolean missingRelatedArtifacts = library.getRelatedArtifact().isEmpty();
    boolean missingDataRequirements = library.getDataRequirement().isEmpty();
    if (!missingRelatedArtifacts && !missingDataRequirements) {
      return;
    }

    Library moduleDefinition =
        retrieveLibraryModuleDefinition(
            CqlLibraryDetails.builder()
                .libraryName(cqlLibrary.getCqlLibraryName())
                .cql(cqlLibrary.getCql())
                .expressions(expressions)
                .build(),
            accessToken);
    if (missingRelatedArtifacts) {
      library.setRelatedArtifact(moduleDefinition.getRelatedArtifact());
    }
    if (missingDataRequirements) {
      library.setDataRequirement(moduleDefinition.getDataRequirement());
    }
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
      attachments.add(createAttachment(CQL_CONTENT_TYPE, cql.getBytes()));
    }
    if (elmXml != null) {
      attachments.add(createAttachment(XML_ELM_CONTENT_TYPE, elmXml.getBytes()));
    }
    if (elmJson != null) {
      attachments.add(createAttachment(JSON_ELM_CONTENT_TYPE, elmJson.getBytes()));
    }
    return attachments;
  }

  private CodeableConcept createType(String type, String code) {
    return new CodeableConcept().setCoding(Collections.singletonList(new Coding(type, code, null)));
  }

  /* rawData are bytes that are NOT base64 encoded */
  private Attachment createAttachment(String contentType, byte[] rawData) {
    return new Attachment().setContentType(contentType).setData(rawData == null ? null : rawData);
  }
}

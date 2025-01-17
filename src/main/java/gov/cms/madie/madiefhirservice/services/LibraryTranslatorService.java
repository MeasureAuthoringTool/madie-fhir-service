package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.models.library.CqlLibrary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.stereotype.Service;

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

  public LibraryTranslatorService(
      LibraryCqlVisitorFactory libCqlVisitorFactory, ElmTranslatorClient elmTranslatorClient) {
    this.libCqlVisitorFactory = libCqlVisitorFactory;
    this.elmTranslatorClient = elmTranslatorClient;
  }

  public Library convertToFhirLibrary(
      CqlLibrary cqlLibrary, Set<String> expressions, String accessToken) {
    var visitor = libCqlVisitorFactory.visit(cqlLibrary.getCql());
    Library library = new Library();
    library.setId(cqlLibrary.getCqlLibraryName());
    library.setLanguage("en");
    library.setName(cqlLibrary.getCqlLibraryName());
    library.setVersion(cqlLibrary.getVersion().toString());
    library.setDate(new Date());
    library.setStatus(Enumerations.PublicationStatus.ACTIVE);
    library.setPublisher(
        cqlLibrary.getPublisher() != null && StringUtils.isNotBlank(cqlLibrary.getPublisher())
            ? cqlLibrary.getPublisher()
            : UNKNOWN_VALUE);
    library.setDescription(Objects.toString(cqlLibrary.getDescription(), UNKNOWN_VALUE));
    library.setExperimental(cqlLibrary.isExperimental());
    library.setContent(
        createContent(cqlLibrary.getCql(), cqlLibrary.getElmJson(), cqlLibrary.getElmXml()));
    library.setType(createType(UriConstants.CodeSystem.LIBRARY_SYSTEM_TYPE_URI, SYSTEM_CODE));
    library.setUrl(
        FhirResourceHelpers.buildResourceFullUrl("Library", cqlLibrary.getCqlLibraryName()));
    library.getExtension().addAll(visitor.getDrcExtensions());
    library.setMeta(createLibraryMeta());
    library.setTitle(cqlLibrary.getCqlLibraryName());
    library.setPublisher(cqlLibrary.getPublisher());
    Identifier identifier = new Identifier();
    identifier.setUse(IdentifierUse.OFFICIAL);
    identifier.setSystem("https://madie.cms.gov/login");
    identifier.setValue(cqlLibrary.getId());
    library.setIdentifier(List.of(identifier));
    // Use the DataRequirementsProcessor to construct data requirements and related artifacts.
    Library libraryModuleDefinition =
        retrieveLibraryModuleDefinition(
            CqlLibraryDetails.builder()
                .libraryName(cqlLibrary.getCqlLibraryName())
                .cql(cqlLibrary.getCql())
                .expressions(expressions)
                .build(),
            accessToken);
    library.setRelatedArtifact(libraryModuleDefinition.getRelatedArtifact());
    library.setDataRequirement(libraryModuleDefinition.getDataRequirement());
    return library;
  }

  private Library retrieveLibraryModuleDefinition(
      CqlLibraryDetails cqlLibraryDetails, String accessToken) {
    org.hl7.fhir.r5.model.Library r5moduleDefinition =
        elmTranslatorClient.getModuleDefinitionLibrary(cqlLibraryDetails, false, accessToken);
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    return (Library) versionConvertor_40_50.convertResource(r5moduleDefinition);
  }

  private Meta createLibraryMeta() {
    // Currently, only one profile is allowed, but Bryn is under the impression multiples should
    // work.
    // For now, it is just computable until we resolve this.
    return new Meta()
        .addProfile(
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-library-cqfm");
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

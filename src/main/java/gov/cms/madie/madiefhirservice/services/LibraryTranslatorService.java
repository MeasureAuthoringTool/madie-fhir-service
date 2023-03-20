package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.common.ProgramUseContext;
import gov.cms.madie.models.common.Version;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Slf4j
@Service
public class LibraryTranslatorService {
  public static final String CQL_CONTENT_TYPE = "text/cql";
  public static final String JSON_ELM_CONTENT_TYPE = "application/elm+json";
  public static final String XML_ELM_CONTENT_TYPE = "application/elm+xml";
  public static final String SYSTEM_CODE = "logic-library";
  public static final String UNKNOWN_VALUE = "UNKNOWN";

  private final LibraryCqlVisitorFactory libCqlVisitorFactory;

  @Value("${fhir-base-url}")
  private String fhirBaseUrl;

  public LibraryTranslatorService(LibraryCqlVisitorFactory libCqlVisitorFactory) {
    this.libCqlVisitorFactory = libCqlVisitorFactory;
  }

  public Library convertToFhirLibrary(CqlLibrary cqlLibrary, ProgramUseContext programUseContext) {
    var visitor = libCqlVisitorFactory.visit(cqlLibrary.getCql());
    Library library = new Library();
    library.setId(cqlLibrary.getId());
    library.setLanguage("en");
    library.setName(cqlLibrary.getCqlLibraryName());
    library.setVersion(cqlLibrary.getVersion().toString());
    library.setDate(new Date());
    library.setStatus(Enumerations.PublicationStatus.ACTIVE);
    library.setPublisher(
        cqlLibrary.getPublisher() != null && StringUtils.isNotBlank(cqlLibrary.getPublisher())
            ? cqlLibrary.getPublisher()
            : UNKNOWN_VALUE);
    library.setDescription(StringUtils.defaultString(cqlLibrary.getDescription(), UNKNOWN_VALUE));
    library.setExperimental(cqlLibrary.isExperimental());
    library.setContent(
        createContent(cqlLibrary.getCql(), cqlLibrary.getElmJson(), cqlLibrary.getElmXml()));
    library.setType(createType(UriConstants.LIBRARY_SYSTEM_TYPE_URI, SYSTEM_CODE));
    library.setUrl(fhirBaseUrl + "/Library/" + cqlLibrary.getCqlLibraryName());
    library.setDataRequirement(distinctDataRequirements(visitor.getDataRequirements()));
    library.setRelatedArtifact(distinctArtifacts(visitor.getRelatedArtifacts()));
    library.setMeta(createLibraryMeta());
    library.setTitle(cqlLibrary.getCqlLibraryName());
    library.setPublisher(cqlLibrary.getPublisher());
    Identifier identifier = new Identifier();
    identifier.setUse(IdentifierUse.OFFICIAL);
    identifier.setSystem("https://madie.cms.gov/login");
    identifier.setValue(cqlLibrary.getId());
    library.setIdentifier(List.of(identifier));
    if (programUseContext != null) {
      library.setUseContext(List.of(convertUseContext(programUseContext)));
    }
    // TODO: probably have to revisit this. Human Readable feature is not yet ready
    // result.setText(findHumanReadable(lib.getMeasureId()));
    return library;
  }

  public UsageContext convertUseContext(ProgramUseContext programUseContext) {
    UsageContext useContext = new UsageContext();
    Coding code = new Coding();
    code.setSystem(UriConstants.UseContext.CODE_SYSTEM_URI);
    code.setCode("program");
    useContext.setCode(code);
    CodeableConcept valueCodeableConcept = new CodeableConcept();
    Coding coding = new Coding();
    coding.setSystem(UriConstants.UseContext.VALUE_CODABLE_CONTEXT_CODING_SYSTEM_URI);
    coding.setCode(programUseContext.getCode());
    coding.setDisplay(programUseContext.getDisplay());
    valueCodeableConcept.setCoding(List.of(coding));
    useContext.setValue(valueCodeableConcept);
    return useContext;
  }

  public CqlLibrary convertToCqlLibrary(Library library) {
    return CqlLibrary.builder()
        .id(library.getMeta().getId())
        .cqlLibraryName(library.getName())
        .version(Version.parse(library.getVersion()))
        .publisher(UNKNOWN_VALUE.equals(library.getPublisher()) ? null : library.getPublisher())
        .description(
            UNKNOWN_VALUE.equals(library.getDescription()) ? null : library.getDescription())
        .experimental(library.getExperimental())
        .cql(attachmentToString(findAttachmentOfContentType(library, CQL_CONTENT_TYPE)))
        .elmJson(attachmentToString(findAttachmentOfContentType(library, JSON_ELM_CONTENT_TYPE)))
        .elmXml(attachmentToString(findAttachmentOfContentType(library, XML_ELM_CONTENT_TYPE)))
        .build();
  }

  public String attachmentToString(Attachment attachment) {
    if (attachment == null) {
      return null;
    }
    return new String(attachment.getData());
  }

  public Attachment findAttachmentOfContentType(Library library, String contentType) {
    if (library == null || library.getContent() == null) {
      return null;
    }
    return library.getContent().stream()
        .filter(a -> a.getContentType().equals(contentType))
        .findFirst()
        .orElse(null);
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

  private List<RelatedArtifact> distinctArtifacts(List<RelatedArtifact> artifacts) {
    List<RelatedArtifact> result = new ArrayList<>(artifacts.size());
    // Remove duplicates.
    artifacts.forEach(
        a -> {
          if (result.stream().noneMatch(ar -> Objects.deepEquals(a, ar))) {
            result.add(a);
          }
        });
    result.sort(Comparator.comparing(RelatedArtifact::getUrl));
    return result;
  }

  private List<DataRequirement> distinctDataRequirements(List<DataRequirement> reqs) {
    List<DataRequirement> result = new ArrayList<>(reqs.size());
    // Remove duplicates.
    for (DataRequirement req : reqs) {
      if (result.stream()
          .noneMatch(r -> matchType(req.getType()).and(matchCodeFilter(req)).test(r))) {
        result.add(req);
      }
    }
    return result;
  }

  private Predicate<DataRequirement> matchType(String type) {
    return d -> StringUtils.equals(d.getType(), type);
  }

  private Predicate<DataRequirement> matchCodeFilter(DataRequirement o) {
    return d -> {
      if ((CollectionUtils.isEmpty(d.getCodeFilter())
          && CollectionUtils.isEmpty(o.getCodeFilter()))) {
        // Match when both code filters are empty
        return true;
      } else if ((CollectionUtils.isEmpty(d.getCodeFilter())
          || CollectionUtils.isEmpty(o.getCodeFilter()))) {
        // No match if either code filter is empty
        return false;
      } else {
        // Match on path AND (code or value set)
        return StringUtils.equals(
                d.getCodeFilter().get(0).getPath(), o.getCodeFilter().get(0).getPath())
            && (hasMatchingValueSet(d, o) || hasMatchingCode(o, d));
      }
    };
  }

  private boolean hasMatchingCode(DataRequirement o, DataRequirement d) {
    return (!CollectionUtils.isEmpty(d.getCodeFilter().get(0).getCode())
            && !CollectionUtils.isEmpty(o.getCodeFilter().get(0).getCode()))
        && StringUtils.equals(
            d.getCodeFilter().get(0).getCode().get(0).getCode(),
            o.getCodeFilter().get(0).getCode().get(0).getCode());
  }

  private boolean hasMatchingValueSet(DataRequirement d, DataRequirement o) {
    return (d.getCodeFilter().get(0).getValueSet() != null
            && o.getCodeFilter().get(0).getValueSet() != null)
        && StringUtils.equals(
            d.getCodeFilter().get(0).getValueSet(), o.getCodeFilter().get(0).getValueSet());
  }

  private CodeableConcept createType(String type, String code) {
    return new CodeableConcept().setCoding(Collections.singletonList(new Coding(type, code, null)));
  }

  /* rawData are bytes that are NOT base64 encoded */
  private Attachment createAttachment(String contentType, byte[] rawData) {
    return new Attachment().setContentType(contentType).setData(rawData == null ? null : rawData);
  }
}

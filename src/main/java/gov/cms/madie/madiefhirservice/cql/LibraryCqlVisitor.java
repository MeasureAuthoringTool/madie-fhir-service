package gov.cms.madie.madiefhirservice.cql;

import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.gen.cqlBaseVisitor;
import org.cqframework.cql.gen.cqlParser;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.RelatedArtifact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Useful for parsing out code systems, value sets, data requirements, related Artifacts, libraries.
 */
@Getter
@Slf4j
public class LibraryCqlVisitor extends cqlBaseVisitor<String> {
  private final List<cqlParser.IncludeDefinitionContext> includes = new ArrayList<>();
  private final List<cqlParser.ValuesetDefinitionContext> valueSets = new ArrayList<>();
  private final List<cqlParser.CodeDefinitionContext> codes = new ArrayList<>();
  private final List<cqlParser.CodesystemDefinitionContext> codeSystems = new ArrayList<>();
  private final ReadableArtifacts readableArtifacts = new ReadableArtifacts();
  private final List<DataRequirement> dataRequirements = new ArrayList<>();
  private final List<Extension> drcExtensions = new ArrayList<>();
  private final List<RelatedArtifact> relatedArtifacts = new ArrayList<>();
  private final Map<String, Pair<Library, LibraryCqlVisitor>> libMap = new HashMap<>();
  private final Map<Integer, Library> libraryCacheMap = new HashMap<>();
  private final Map<String, String> valueSetNameUri = new HashMap<>();
  private final List<Pair<String, String>> includedLibraries = new ArrayList<>();
  private String name;
  private String version;

  /**
   * Stores off lib name and version.
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitLibraryDefinition(cqlParser.LibraryDefinitionContext ctx) {
    name = ctx.qualifiedIdentifier().getText();
    version = trim1(ctx.versionSpecifier().getText());
    return null;
  }

  /**
   * Stores name/version pairs of included libraries, include context, and relatedArtifacts
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitIncludeDefinition(cqlParser.IncludeDefinitionContext ctx) {
    if (ctx.getChildCount() >= 4
        && StringUtils.equals(ctx.getChild(0).getText(), "include")
        && StringUtils.equals(ctx.getChild(2).getText(), "version")) {
      RelatedArtifact relatedArtifact = new RelatedArtifact();
      relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
      var nameVersion = getNameVersionFromInclude(ctx);
      String url =
          FhirResourceHelpers.buildResourceFullUrl("Library", nameVersion.getLeft())
              + "|"
              + nameVersion.getRight();
      relatedArtifact.setResource(url);
      relatedArtifacts.add(relatedArtifact);
      includedLibraries.add(nameVersion);
      includes.add(ctx);
    }
    return null;
  }

  /**
   * Stores off valueSets ,they are used later when looking up retrieves.
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitValuesetDefinition(cqlParser.ValuesetDefinitionContext ctx) {
    String uri = getUnquotedFullText(ctx.valuesetId());
    String vsName = getUnquotedFullText(ctx.identifier());
    valueSets.add(ctx);
    readableArtifacts
        .getTerminologyValueSetModels()
        .add(new ValuesetModel(vsName, uri, null, null));
    RelatedArtifact relatedArtifact = new RelatedArtifact();
    relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
    relatedArtifact.setResource(uri);
    relatedArtifacts.add(relatedArtifact);

    // need to be polished once we human readable file
    valueSetNameUri.put(vsName, uri);

    return null;
  }

  @Override
  public String visitCodesystemDefinition(cqlParser.CodesystemDefinitionContext ctx) {
    codeSystems.add(ctx);
    RelatedArtifact relatedArtifact = new RelatedArtifact();
    relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
    relatedArtifact.setResource(
        getUnquotedFullText(ctx.codesystemId())
            + (ctx.versionSpecifier() != null
                ? "|" + getUnquotedFullText(ctx.versionSpecifier())
                : ""));
    relatedArtifacts.add(relatedArtifact);
    return null;
  }

  @Override
  public String visitCodeDefinition(cqlParser.CodeDefinitionContext ctx) {

    String codeSystemName = getUnquotedFullText(ctx.codesystemIdentifier());
    String code = getUnquotedFullText(ctx.codeId());
    String codeName = getUnquotedFullText(ctx.identifier());
    codeSystems.stream()
        .filter(cs -> StringUtils.equals(getUnquotedFullText(cs.identifier()), codeSystemName))
        .findFirst()
        .ifPresentOrElse(
            cs -> {
              String csName = getUnquotedFullText(cs.identifier());
              String csUri = getUnquotedFullText(cs.codesystemId());
              String csVersionUri = getUnquotedFullText(cs.versionSpecifier());

              Extension ext = new Extension(UriConstants.CqfMeasures.DIRECT_REFERENCE_CODE_URI);

              ext.setValue(buildCoding(code, csUri, codeName));

              this.getDrcExtensions().add(ext);
              readableArtifacts
                  .getTerminologyCodeModels()
                  .add(
                      CodeModel.builder()
                          .name(codeName)
                          .oid(code)
                          .codeSystemOid(csUri)
                          .codesystemName(csName)
                          .codesystemVersion(csVersionUri)
                          .isCodesystemVersionIncluded(csVersionUri != null)
                          .datatype(null)
                          .build());
            },
            () ->
                log.error(
                    "Invalid code "
                        + ctx.getText()
                        + ". Could not find code system name "
                        + codeSystemName));

    codes.add(ctx);
    return null;
  }

  /**
   * Handles building the dataRequirements from retrieves.
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitRetrieve(cqlParser.RetrieveContext ctx) {
    if (matchesPathRetrieve(ctx)) {
      handleDataRequirement(
          trimQuotes(ctx.getChild(1).getText()),
          ctx.getChild(3).getText(),
          trimQuotes(ctx.getChild(5).getText()));
    } else if (matchesNonPathRetrieve(ctx)) {
      handleDataRequirement(
          trimQuotes(ctx.getChild(1).getText()), "code", trimQuotes(ctx.getChild(3).getText()));
    } else if (matchesTypeRetrieve(ctx)) {
      handleTypeDataRequirement(trimQuotes(ctx.getChild(1).getText()));
    }
    return null;
  }

  /**
   * @return Has to be something so always null.
   */
  @Override
  protected String defaultResult() {
    return null;
  }

  /**
   * @param ctx the retrieve.
   * @return Returns true if the retrieve is a valueset retrieve.
   */
  private boolean matchesTypeRetrieve(cqlParser.RetrieveContext ctx) {
    // define FirstInpatientEncounter:
    // First([Encounter] E where E.class = 'inpatient' sort by period.start desc)
    return ctx.getChildCount() == 3
        && ctx.getChild(0).getText().equals("[")
        && ctx.getChild(2).getText().equals("]");
  }

  /**
   * @param ctx the retrieve.
   * @return Returns true if the retrieve is a valueset retrieve.
   */
  private boolean matchesPathRetrieve(cqlParser.RetrieveContext ctx) {
    return ctx.getChildCount() == 7
        && ctx.getChild(0).getText().equals("[")
        && ctx.getChild(2).getText().equals(":")
        && ctx.getChild(4).getText().equals("in")
        && ctx.getChild(6).getText().equals("]");
  }

  /**
   * @param ctx the retrieve.
   * @return Returns true if the retrieve is a valueset retrieve.
   */
  private boolean matchesNonPathRetrieve(cqlParser.RetrieveContext ctx) {
    return ctx.getChildCount() == 5
        && ctx.getChild(0).getText().equals("[")
        && ctx.getChild(2).getText().equals(":")
        && ctx.getChild(4).getText().equals("]");
  }

  private void handleTypeDataRequirement(String type) {
    // define FirstInpatientEncounter:
    // First([Encounter] E where E.class = 'inpatient' sort by period.start desc)
    var result = new DataRequirement();
    result.setType(type);
    dataRequirements.add(result);
    readableArtifacts.getDataReqTypes().add(TypeModel.builder().type(type).build());
  }

  private void handleDataRequirement(String type, String path, String valueSetOrCodeName) {

    var result = new DataRequirement();
    result.setType(type);
    var filter = new DataRequirement.DataRequirementCodeFilterComponent();
    filter.setPath(path);
    result.setCodeFilter(Collections.singletonList(filter));
    var hrVs = getValueSetUrl(valueSetOrCodeName);

    filter.setValueSet(valueSetNameUri.get(valueSetOrCodeName));

    if (hrVs != null) {
      filter.setValueSet(hrVs.getOid());
      readableArtifacts
          .getDataReqValueSets()
          .add(new ValuesetModel(valueSetOrCodeName, hrVs.getOid(), hrVs.getVersion(), type));
    } else {

      CodeModel hrCode = getCode(valueSetOrCodeName);
      if (hrCode != null) {
        filter.setCode(
            Collections.singletonList(
                new Coding(hrCode.getCodeSystemOid(), hrCode.getOid(), hrCode.getName())));

        readableArtifacts
            .getDataReqCodes()
            .add(
                CodeModel.builder()
                    .name(hrCode.getName())
                    .oid(hrCode.getOid())
                    .codeSystemOid(hrCode.getCodeSystemOid())
                    .codesystemName(hrCode.getCodesystemName())
                    .codesystemVersion(hrCode.getCodesystemVersion())
                    .isCodesystemVersionIncluded(hrCode.isCodesystemVersionIncluded())
                    .datatype(type)
                    .build());
      } else {
        log.info(
            "Could not find a value set or code matching name "
                + valueSetOrCodeName
                + ". "
                + "This is likely a FHIR type with attribute and we don't know how to handle those yet :(.");
      }
    }

    dataRequirements.add(result);
  }

  private CodeableConcept buildCodeableConcept(String code, String system, String display) {
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setCoding(new ArrayList<>());
    codeableConcept.getCoding().add(buildCoding(code, system, display));
    return codeableConcept;
  }

  private Coding buildCoding(String code, String system, String display) {
    return new Coding().setCode(code).setSystem(system).setDisplay(display);
  }

  public ValuesetModel getValueSetUrl(String valueSetName) {
    ValuesetModel result = null;
    if (isInIncludeLib(valueSetName)) {
      var optionalVs =
          readableArtifacts.getTerminologyValueSetModels().stream()
              .filter(vs -> StringUtils.equals(vs.getName(), valueSetName))
              .findFirst();
      if (optionalVs.isPresent()) {
        result = optionalVs.get();
      } else {
        log.debug("Could not find valueset with name " + valueSetName);
      }
    } /*
       * else { // Check to see if its a lib reference like TJC."value set id" int
       * periodIndex = valueSetName.indexOf("."); if (periodIndex != -1) { String
       * alias = valueSetName.substring(0, periodIndex); String remaining =
       * trimQuotes(valueSetName.substring(periodIndex + 1)); var childLib =
       * libMap.get(alias); if (childLib != null) { result =
       * childLib.getRight().getValueSetUrl(remaining); } } }
       */
    return result;
  }

  public CodeModel getCode(String codeName) {
    CodeModel result = null;
    if (isInIncludeLib(codeName)) {
      var optionalCm =
          readableArtifacts.getTerminologyCodeModels().stream()
              .filter(cm -> StringUtils.equals(cm.getName(), codeName))
              .findFirst();
      if (optionalCm.isPresent()) {
        result = optionalCm.get();
      } else {
        log.error("Could not find code with code name " + codeName);
      }
    } /*
       * else { // Check to see if its a lib reference like TJC."value set id" int
       * periodIndex = codeName.indexOf("."); if (periodIndex != -1) { String alias =
       * codeName.substring(0, periodIndex); String remaining =
       * codeName.substring(periodIndex + 1); var childLib = libMap.get(alias); if
       * (childLib != null) { result = childLib.getRight().getCode(remaining); } } }
       */
    return result;
  }

  private String trimQuotes(String s) {
    if (StringUtils.startsWith(s, "\"") && StringUtils.endsWith(s, "\"")) {
      return trim1(s);
    } else {
      return s;
    }
  }

  private String trim1(String s) {
    if (StringUtils.isNotBlank(s) && s.length() > 2) {
      return s.substring(1, s.length() - 1);
    } else {
      return s;
    }
  }

  private boolean isInIncludeLib(String name) {
    return !name.contains(".");
  }

  private String getUnquotedFullText(ParserRuleContext context) {
    return CqlUtils.unquote(getFullText(context));
  }

  private String getFullText(ParserRuleContext context) {
    return context == null ? null : context.getText();
  }

  private Pair<String, String> getNameVersionFromInclude(cqlParser.IncludeDefinitionContext ctx) {
    return Pair.of(ctx.getChild(1).getText(), trim1(ctx.getChild(3).getText()));
  }
}

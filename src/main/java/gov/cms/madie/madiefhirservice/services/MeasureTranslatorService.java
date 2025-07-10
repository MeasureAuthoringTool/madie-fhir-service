package gov.cms.madie.madiefhirservice.services;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import gov.cms.madie.madiefhirservice.constants.IdentifierType;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureTranslatorService {
  public static final String UNKNOWN = "UNKNOWN";
  private static final Safelist RICH_TEXT_SAFE_LIST =
      Safelist.basic()
          .addTags(
              "p",
              "strong",
              "em",
              "u",
              "s",
              "ul",
              "ol",
              "li",
              "br",
              "table",
              "tbody",
              "td",
              "tfoot",
              "th",
              "thead",
              "tr",
              "col",
              "colgroup")
          .addAttributes("table", "style")
          .addAttributes("th", "rowspan", "colspan", "style", "colwidth")
          .addAttributes("td", "rowspan", "colspan", "style", "colwidth")
          .addAttributes("col", "style");

  private String sanitizeText(String val) {
    if (StringUtils.isBlank(val)) {
      return val;
    }
    String safeHtml = Jsoup.clean(val, RICH_TEXT_SAFE_LIST);
    // col tags are not self-closing in html, so we need to make them wel-formed
    return safeHtml.replaceAll("<col ([^/>]*)>", "<col $1 />");
  }

  public org.hl7.fhir.r4.model.Measure createFhirMeasureForMadieMeasure(Measure madieMeasure) {
    Organization steward = madieMeasure.getMeasureMetaData().getSteward();

    org.hl7.fhir.r4.model.Measure measure = new org.hl7.fhir.r4.model.Measure();
    measure
        .setName(madieMeasure.getCqlLibraryName())
        .setTitle(sanitizeText(madieMeasure.getMeasureName()))
        .setIdentifier(buildMeasureIdentifiers(madieMeasure))
        .setExperimental(madieMeasure.getMeasureMetaData().getExperimental())
        .setUrl(
            FhirResourceHelpers.buildResourceFullUrl("Measure", madieMeasure.getCqlLibraryName()))
        .setVersion(getVersion(madieMeasure))
        .setEffectivePeriod(
            getPeriodFromDates(
                madieMeasure.getMeasurementPeriodStart(), madieMeasure.getMeasurementPeriodEnd()))
        .setApprovalDate(getApprovalDate(madieMeasure))
        .setLastReviewDate(getLastReviewDate(madieMeasure))
        .setPublisher(sanitizeText(getStewardName(steward)))
        .setCopyright(sanitizeText(getCopyright(madieMeasure)))
        .setDisclaimer(sanitizeText(getDisclaimer(madieMeasure)))
        .setRationale(sanitizeText(madieMeasure.getMeasureMetaData().getRationale()))
        .setPurpose(sanitizeText(madieMeasure.getMeasureMetaData().getPurpose()))
        .setLibrary(
            Collections.singletonList(
                new CanonicalType(
                    FhirResourceHelpers.buildResourceFullUrl(
                        "Library", madieMeasure.getCqlLibraryName()))))
        .setContact(buildContactDetail(madieMeasure.getMeasureMetaData().getSteward(), false))
        .setGroup(buildGroups(madieMeasure.getGroups()))
        .setSupplementalData(buildSupplementalData(madieMeasure))
        .setStatus(getStatus(madieMeasure))
        .setDescription(sanitizeText(madieMeasure.getMeasureMetaData().getDescription()))
        .setUsage(sanitizeText(madieMeasure.getMeasureMetaData().getGuidance()))
        .setAuthor(buildContactDetail(madieMeasure.getMeasureMetaData().getDevelopers(), true))
        .setClinicalRecommendationStatement(
            sanitizeText(madieMeasure.getMeasureMetaData().getClinicalRecommendation()))
        .setDate(Date.from(madieMeasure.getLastModifiedAt()))
        .setMeta(buildMeasureMeta(madieMeasure.getGroups()))
        .setId(madieMeasure.getCqlLibraryName());
    setExtensions(madieMeasure, measure);
    setUseContext(madieMeasure, measure);
    setRelatedArtifact(madieMeasure, measure);
    return measure;
  }

  private String getVersion(Measure madieMeasure) {
    String version = madieMeasure.getVersion().toString();
    if (madieMeasure.getMeasureMetaData() != null && madieMeasure.getMeasureMetaData().isDraft()) {
      version = "Draft based on " + version;
    }
    return version;
  }

  private Date getApprovalDate(Measure madieMeasure) {
    Instant approvalDate = madieMeasure.getReviewMetaData().getApprovalDate();
    return approvalDate != null ? Date.from(approvalDate) : null;
  }

  private Date getLastReviewDate(Measure madieMeasure) {
    Instant lastReviewDate = madieMeasure.getReviewMetaData().getLastReviewDate();
    return lastReviewDate != null ? Date.from(lastReviewDate) : null;
  }

  private String getCopyright(Measure madieMeasure) {
    String copyright = madieMeasure.getMeasureMetaData().getCopyright();
    return StringUtils.isBlank(copyright) ? UNKNOWN : copyright;
  }

  private String getDisclaimer(Measure madieMeasure) {
    String disclaimer = madieMeasure.getMeasureMetaData().getDisclaimer();
    return StringUtils.isBlank(disclaimer) ? UNKNOWN : disclaimer;
  }

  private Enumerations.PublicationStatus getStatus(Measure madieMeasure) {
    return madieMeasure.getMeasureMetaData().isDraft()
        ? PublicationStatus.DRAFT
        : PublicationStatus.ACTIVE;
  }

  private void setExtensions(Measure madieMeasure, org.hl7.fhir.r4.model.Measure measure) {
    for (Extension ext : buildExtensions(madieMeasure)) {
      measure.addExtension(ext);
    }
  }

  private void setUseContext(Measure madieMeasure, org.hl7.fhir.r4.model.Measure measure) {
    if (madieMeasure.getMeasureMetaData().getIntendedVenue() != null) {
      measure.addUseContext(
          buildIntendedVenue(madieMeasure.getMeasureMetaData().getIntendedVenue()));
    }
  }

  private void setRelatedArtifact(Measure madieMeasure, org.hl7.fhir.r4.model.Measure measure) {
    if (CollectionUtils.isNotEmpty(madieMeasure.getMeasureMetaData().getReferences())) {
      measure.setRelatedArtifact(
          buildRelatedArtifacts(madieMeasure.getMeasureMetaData().getReferences()));
    }
  }

  /**
   * Build extensions to add to the top level measure
   *
   * @param madieMeasure
   */
  protected List<Extension> buildExtensions(Measure madieMeasure) {
    List<Extension> extensions = new ArrayList<>();
    Extension supplementalDataGuidanceExt = buildSupplementalDataGuidanceExt(madieMeasure);
    if (supplementalDataGuidanceExt != null) {
      extensions.add(supplementalDataGuidanceExt);
    }
    Extension riskAdjustmentVariableGuidanceExt = buildRiskAdjustmentGuidanceExt(madieMeasure);
    if (riskAdjustmentVariableGuidanceExt != null) {
      extensions.add(riskAdjustmentVariableGuidanceExt);
    }
    List<Extension> measureDefinitionsExts = buildMeasureDefinitionExts(madieMeasure);
    if (isNotEmpty(measureDefinitionsExts)) {
      extensions.addAll(measureDefinitionsExts);
    }
    return extensions;
  }

  public List<Identifier> buildMeasureIdentifiers(Measure madieMeasure) {
    List<Identifier> identifiers = new ArrayList<>();
    if (madieMeasure != null) {
      identifiers.add(
          buildIdentifier(
              IdentifierUse.USUAL,
              UriConstants.MadieMeasure.SHORT_NAME,
              sanitizeText(madieMeasure.getEcqmTitle()),
              IdentifierType.CODE_SHORT_NAME));
      identifiers.add(
          buildIdentifier(
              IdentifierUse.OFFICIAL,
              UriConstants.URN_IETF_RFC_3986,
              buildUrnUuid(madieMeasure.getMeasureSetId()),
              IdentifierType.CODE_VERSION_INDEPENDENT));
      identifiers.add(
          buildIdentifier(
              IdentifierUse.OFFICIAL,
              UriConstants.URN_IETF_RFC_3986,
              buildUrnUuid(madieMeasure.getVersionId()),
              IdentifierType.CODE_VERSION_SPECIFIC));
      if (madieMeasure.getMeasureMetaData() != null
          && madieMeasure.getMeasureMetaData().getEndorsements() != null
          && CollectionUtils.isNotEmpty(madieMeasure.getMeasureMetaData().getEndorsements())
          && StringUtils.isNotBlank(
              madieMeasure.getMeasureMetaData().getEndorsements().get(0).getEndorser())) {
        Endorsement endorsement = madieMeasure.getMeasureMetaData().getEndorsements().get(0);
        identifiers.add(
            buildIdentifier(
                    IdentifierUse.OFFICIAL,
                    UriConstants.MadieMeasure.CBE_ID,
                    sanitizeText(endorsement.getEndorsementId()),
                    IdentifierType.CODE_ENDORSER)
                .setAssigner(
                    buildDisplayReference(
                        madieMeasure.getMeasureMetaData().getEndorsements().get(0).getEndorser())));
      }
      MeasureSet measureSet = madieMeasure.getMeasureSet();
      if (measureSet != null && measureSet.getCmsId() != null) {
        identifiers.add(
            buildIdentifier(
                    IdentifierUse.OFFICIAL,
                    UriConstants.MadieMeasure.CMS_ID,
                    madieMeasure.getMeasureSet().getCmsId() + "FHIR",
                    IdentifierType.CODE_PUBLISHER)
                .setAssigner(buildDisplayReference("CMS")));
      }
    }
    return identifiers;
  }

  private UsageContext buildIntendedVenue(CodeConcept intendedVenue) {
    Coding coding =
        new Coding()
            .setSystem("http://hl7.org/fhir/us/cqfmeasures/CodeSystem/intended-venue-codes")
            .setCode(intendedVenue.getCode())
            .setDisplay(intendedVenue.getDisplay());

    UsageContext usageContext = new UsageContext();
    usageContext.setCode(
        new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/usage-context-type")
            .setCode("venue")
            .setDisplay("Venue"));
    usageContext.setValue(new CodeableConcept(coding));

    return usageContext;
  }

  private String getStewardName(Organization steward) {
    return (steward == null || StringUtils.isBlank(steward.getName()))
        ? UNKNOWN
        : steward.getName();
  }

  private String buildUrnUuid(String value) {
    return UriConstants.URN_UUID_PREFIX + (value == null ? "null" : value);
  }

  private Reference buildDisplayReference(String display) {
    Reference ref = new Reference();
    ref.setDisplay(display);
    return ref;
  }

  public Identifier buildIdentifier(
      IdentifierUse use, String system, String value, IdentifierType identifierType) {
    Identifier identifier = new Identifier();
    identifier.setUse(use);
    identifier.setSystem(system);
    identifier.setValue(value);
    identifier.setType(
        buildCodeableConcept(
            identifierType.getCode(),
            UriConstants.CodeSystem.CODE_SYSTEM_IDENTIFIER_TYPE_URI,
            identifierType.getDisplay()));
    log.debug("\nbuildIdentifier: identifier = {}", identifier.getValue());
    return identifier;
  }

  public Meta buildMeasureMeta(List<Group> madieGroups) {
    final Meta meta = new Meta();
    meta.addProfile(UriConstants.CqfMeasures.SHAREABLE_MEASURE_PROFILE_URI)
        .addProfile(UriConstants.CqfMeasures.COMPUTABLE_MEASURE_PROFILE_URI)
        .addProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI)
        .addProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI)
        .addProfile(UriConstants.CqfMeasures.CQL_MEASURE_PROFILE_URI)
        .addProfile(UriConstants.CqfMeasures.ELM_MEASURE_PROFILE_URI);

    if (!madieGroups.isEmpty()) {
      String score = madieGroups.get(0).getScoring();
      if (madieGroups.stream().map(Group::getScoring).allMatch(s -> s.equals(score))) {
        switch (score) {
          case "Cohort":
            meta.addProfile(UriConstants.CqfMeasures.COHORT_PROFILE_URI);
            break;
          case "Proportion":
            meta.addProfile(UriConstants.CqfMeasures.PROPORTION_PROFILE_URI);
            break;
          case "Ratio":
            meta.addProfile(UriConstants.CqfMeasures.RATIO_PROFILE_URI);
            break;
          case "Continuous Variable":
            meta.addProfile(UriConstants.CqfMeasures.CV_PROFILE_URI);
            break;
          default:
            break;
            // do nothing
        }
      }
    }

    return meta;
  }

  public List<MeasureGroupComponent> buildGroups(List<Group> madieGroups) {
    if (CollectionUtils.isNotEmpty(madieGroups)) {
      return madieGroups.stream().map(this::buildGroup).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private CodeableConcept getScoringUnitCode(Object scoringUnit) {
    if (scoringUnit == null) {
      return null;
    } else if (scoringUnit instanceof String) {
      String scoringUnitStr = (String) scoringUnit;
      return scoringUnitStr.trim().isEmpty()
          ? null
          : new CodeableConcept(new Coding(null, scoringUnitStr, scoringUnitStr));
    } else if (scoringUnit instanceof Map) {
      Map<String, Object> scoringUnitObj = (Map) scoringUnit;
      Map<String, Object> valueObj = (Map) scoringUnitObj.get("value");
      return new CodeableConcept(
          new Coding(
              (String) valueObj.get("system"),
              (String) valueObj.get("code"),
              (String) scoringUnitObj.get("label")));
    } else {
      return null;
    }
  }

  public MeasureGroupComponent buildGroup(Group madieGroup) {
    List<MeasureGroupPopulationComponent> measurePopulations = buildPopulations(madieGroup);
    measurePopulations.addAll(buildObservations(madieGroup));
    List<MeasureGroupStratifierComponent> measureStratifications = buildStratifications(madieGroup);
    // seems FHIR spec and fqm-execution want lowercase 'boolean', while other popBasis have
    // capitalized first letter
    final String popBasisValue =
        StringUtils.equalsIgnoreCase("boolean", madieGroup.getPopulationBasis())
            ? "boolean"
            : madieGroup.getPopulationBasis();
    final CodeableConcept scoringUnit = getScoringUnitCode(madieGroup.getScoringUnit());
    final List<CodeableConcept> types = getMeasureTypes(madieGroup.getMeasureGroupTypes());
    Element element =
        buildMeasureGroupComponent(
            madieGroup, measurePopulations, measureStratifications, popBasisValue);
    if (scoringUnit != null) {
      element.addExtension(new Extension(UriConstants.CqfMeasures.SCORING_UNIT_URI, scoringUnit));
    }
    if (types != null) {
      types.forEach(
          type -> element.addExtension(new Extension(UriConstants.CqfMeasures.CQFM_TYPE, type)));
    }
    if (StringUtils.isNotBlank(madieGroup.getRateAggregation())) {
      element.addExtension(
          new Extension(
              UriConstants.CqfMeasures.RATE_AGGREGATION_URI,
              new CodeType(sanitizeText(madieGroup.getRateAggregation()))));
    }
    if (madieGroup.getScoringPrecision() != null) {
      element.addExtension(
          new Extension(
              UriConstants.CqfMeasures.SCORING_PRECISION_URI,
              new PositiveIntType(madieGroup.getScoringPrecision())));
    }
    if (StringUtils.isNotBlank(madieGroup.getImprovementNotation())) {
      element.addExtension(
          new Extension(
              UriConstants.CqfMeasures.IMPROVEMENT_NOTATION_URI,
              buildImprovementNotation(madieGroup.getImprovementNotation())));
      if (StringUtils.isNotBlank(madieGroup.getImprovementNotationDescription())) {
        element.addExtension(
            UriConstants.CqfMeasures.IMPROVEMENT_NOTATION_GUIDANCE_URI,
            new MarkdownType(sanitizeText(madieGroup.getImprovementNotationDescription())));
      }
    }
    return (MeasureGroupComponent) element;
  }

  private Element buildMeasureGroupComponent(
      Group madieGroup,
      List<MeasureGroupPopulationComponent> measurePopulations,
      List<MeasureGroupStratifierComponent> measureStratifications,
      String popBasisValue) {
    return new MeasureGroupComponent()
        .setDescription(sanitizeText(madieGroup.getGroupDescription()))
        .setPopulation(measurePopulations)
        .setStratifier(measureStratifications)
        .setId(
            StringUtils.isNotBlank(madieGroup.getDisplayId())
                ? madieGroup.getDisplayId()
                : madieGroup.getId())
        .addExtension(
            new Extension(
                UriConstants.CqfMeasures.SCORING_URI, buildScoringConcept(madieGroup.getScoring())))
        .addExtension(
            new Extension(UriConstants.CqfMeasures.POPULATION_BASIS, new CodeType(popBasisValue)));
  }

  private CodeableConcept buildImprovementNotation(String improvementNotation) {
    if ("Increased score indicates improvement".equalsIgnoreCase(improvementNotation)) {
      return buildCodeableConcept(
          "increase",
          UriConstants.CodeSystem.IMPROVEMENT_NOTATION_CODE_SYSTEM_URI,
          improvementNotation);
    } else {
      return buildCodeableConcept(
          "decrease",
          UriConstants.CodeSystem.IMPROVEMENT_NOTATION_CODE_SYSTEM_URI,
          improvementNotation);
    }
  }

  private List<CodeableConcept> getMeasureTypes(List<MeasureGroupTypes> measureGroupTypes) {
    if (CollectionUtils.isEmpty(measureGroupTypes)) {
      return null;
    }
    List<CodeableConcept> types = new ArrayList<>();
    measureGroupTypes.forEach(
        type -> {
          types.add(
              new CodeableConcept()
                  .addCoding(
                      new Coding(
                          "http://terminology.hl7.org/CodeSystem/measure-type",
                          type.getValue().replaceAll("\\s+", "-").toLowerCase(),
                          type.toString())));
        });
    return types;
  }

  private List<MeasureGroupPopulationComponent> buildPopulations(Group madieGroup) {
    return madieGroup.getPopulations().stream()
        .filter(population -> StringUtils.isNotBlank(population.getDefinition()))
        .map(
            population -> {
              String populationCode = population.getName().toCode();
              String populationDisplay = population.getName().getDisplay();
              return (MeasureGroupPopulationComponent)
                  (new MeasureGroupPopulationComponent()
                          .setDescription(sanitizeText(population.getDescription()))
                          .setCode(
                              buildCodeableConcept(
                                  populationCode,
                                  UriConstants.CodeSystem.POPULATION_SYSTEM_URI,
                                  populationDisplay))
                          .setCriteria(
                              buildExpression("text/cql-identifier", population.getDefinition()))
                          .setId(
                              StringUtils.isNotBlank(population.getDisplayId())
                                  ? population.getDisplayId()
                                  : population.getId()))
                      .addExtension(buildPopulationTypeExtension(population, madieGroup));
              // TODO: Add an extension for measure observations
            })
        .collect(Collectors.toList());
  }

  private List<MeasureGroupPopulationComponent> buildObservations(Group madieGroup) {
    if (madieGroup.getMeasureObservations() == null
        || madieGroup.getMeasureObservations().isEmpty()) {
      return List.of();
    }
    return madieGroup.getMeasureObservations().stream()
        .map(
            measureObservation -> {
              MeasureGroupPopulationComponent observationPopulation =
                  (MeasureGroupPopulationComponent)
                      (new MeasureGroupPopulationComponent()
                          .setDescription(sanitizeText(measureObservation.getDescription()))
                          .setCode(
                              buildCodeableConcept(
                                  PopulationType.MEASURE_OBSERVATION.toCode(),
                                  UriConstants.CodeSystem.POPULATION_SYSTEM_URI,
                                  PopulationType.MEASURE_OBSERVATION.getDisplay()))
                          .setCriteria(
                              buildExpression(
                                  "text/cql-identifier", measureObservation.getDefinition()))
                          .addExtension(
                              new Extension(
                                  UriConstants.CqfMeasures.AGGREGATE_METHOD_URI,
                                  new StringType(measureObservation.getAggregateMethod())))
                          .setId(
                              StringUtils.isNotBlank(measureObservation.getDisplayId())
                                  ? measureObservation.getDisplayId()
                                  : measureObservation.getId()));
              if (measureObservation.getCriteriaReference() != null
                  && StringUtils.isNotBlank(measureObservation.getCriteriaReference())) {
                observationPopulation.addExtension(
                    new Extension(
                        UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI,
                        new StringType(
                            findReferencedPopulationDisplayIdByCriteriaReference(
                                measureObservation.getCriteriaReference(), madieGroup))));
              }
              return observationPopulation;
            })
        .toList();
  }

  private List<MeasureGroupStratifierComponent> buildStratifications(Group madieGroup) {
    List<MeasureGroupStratifierComponent> measureStratifications = null;
    if (madieGroup.getStratifications() != null && !madieGroup.getStratifications().isEmpty()) {
      AtomicReference<Integer> i = new AtomicReference<>();
      i.set(Integer.valueOf(0));
      measureStratifications =
          madieGroup.getStratifications().stream()
              .map(
                  strat -> {
                    List<PopulationType> associations = strat.getAssociations();
                    MeasureGroupStratifierComponent stratComponent = null;
                    if (isNotEmpty(associations)) {
                      List<Extension> extensionList =
                          associations.stream()
                              .map(
                                  associationPopulation -> {
                                    AtomicReference<Extension> extension = new AtomicReference<>();
                                    extension.set(
                                        new Extension(
                                            UriConstants.CqfMeasures.APPLIES_TO_URI,
                                            buildCodeableConcept(
                                                associationPopulation.toCode(),
                                                UriConstants.CodeSystem.POPULATION_SYSTEM_URI,
                                                associationPopulation.getDisplay())));
                                    return extension.get();
                                  })
                              .collect(Collectors.toList());
                      i.set(Integer.valueOf(i.get().intValue() + 1));
                      stratComponent =
                          (MeasureGroupStratifierComponent)
                              new MeasureGroupStratifierComponent()
                                  .setDescription(sanitizeText(strat.getDescription()))
                                  .setCriteria(
                                      buildExpression(
                                          "text/cql-identifier", strat.getCqlDefinition()))
                                  .setId(
                                      StringUtils.isNotBlank(strat.getDisplayId())
                                          ? strat.getDisplayId()
                                          : i.get().toString());
                      for (Extension extension : extensionList) {
                        stratComponent.addExtension(extension);
                      }
                    }
                    return stratComponent;
                  })
              .collect(Collectors.toList());
    }
    return measureStratifications;
  }

  private Extension buildPopulationTypeExtension(Population population, Group madieGroup) {
    // TODO: I feel like this should be in a QICore specific module
    AtomicReference<Extension> extension = new AtomicReference<Extension>();
    AtomicReference<String> id = new AtomicReference<String>();
    if (population.getName().equals(PopulationType.DENOMINATOR)
        || population.getName().equals(PopulationType.NUMERATOR)) {
      madieGroup
          .getPopulations()
          .forEach(
              pop -> {
                // find the pop that has initial_populatino, associationType = population.getName()
                // and then set the extension
                if (pop.getName().equals(PopulationType.INITIAL_POPULATION)
                    && (pop.getAssociationType() != null
                        && pop.getAssociationType()
                            .toString()
                            .equalsIgnoreCase(population.getName().toCode()))) {
                  IBaseDatatype theValue = new StringType(pop.getDisplayId());
                  extension.set(
                      new Extension(UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI, theValue));
                }
              });
    }

    // value is a codeable concept
    return extension.get();
  }

  private Expression buildExpression(String language, String expression) {
    return new Expression().setLanguage(language).setExpression(sanitizeText(expression));
  }

  private Period getPeriodFromDates(Date startDate, Date endDate) {
    return new Period()
        .setStart(startDate, TemporalPrecisionEnum.DAY)
        .setEnd(endDate, TemporalPrecisionEnum.DAY);
  }

  public CodeableConcept buildScoringConcept(String scoring) {
    if (StringUtils.isEmpty(scoring)) {
      return null;
    }
    String code = scoring.toLowerCase();
    if ("continuous variable".equals(code)) {
      code = "continuous-variable";
    }
    return buildCodeableConcept(code, UriConstants.CodeSystem.SCORING_SYSTEM_URI, scoring);
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

  private List<ContactDetail> buildContactDetail(Organization organization, boolean includeName) {
    if (organization == null) {
      return List.of();
    }
    return buildContactDetail(List.of(organization), includeName);
  }

  private List<ContactDetail> buildContactDetail(
      List<Organization> organizations, boolean includeName) {
    if (CollectionUtils.isEmpty(organizations)) {
      return List.of();
    }

    List<ContactDetail> contactDetails = new ArrayList<>();
    for (Organization organization : organizations) {
      contactDetails.add(buildContact(organization, includeName));
    }

    return contactDetails;
  }

  private ContactDetail buildContact(Organization organization, boolean includeName) {
    if (organization == null) {
      return null;
    }

    ContactDetail contactDetail = new ContactDetail();
    if (includeName) {
      contactDetail.setName(sanitizeText(organization.getName()));
    }
    contactDetail.setTelecom(new ArrayList<>());
    contactDetail.getTelecom().add(buildContactPoint(organization.getUrl()));
    return contactDetail;
  }

  private ContactPoint buildContactPoint(String url) {
    return new ContactPoint().setValue(url).setSystem(ContactPoint.ContactPointSystem.URL);
  }

  private List<MeasureSupplementalDataComponent> buildSupplementalData(Measure madieMeasure) {
    List<MeasureSupplementalDataComponent> measureSupplementalDataComponents = new ArrayList<>();
    measureSupplementalDataComponents.addAll(buildSupplementalDataElements(madieMeasure));
    measureSupplementalDataComponents.addAll(buildRiskAdjustmentFactors(madieMeasure));
    return measureSupplementalDataComponents;
  }

  /**
   * Collect descriptions for all supplemental data elements and combine into a single extension for
   * Supplemental Data Guidance
   *
   * @param madieMeasure
   * @return
   */
  public Extension buildSupplementalDataGuidanceExt(Measure madieMeasure) {
    if (CollectionUtils.isEmpty(madieMeasure.getSupplementalData())) {
      return null;
    }

    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setCoding(new ArrayList<>());
    codeableConcept
        .getCoding()
        .add(
            buildCoding(
                "supplemental-data",
                UriConstants.CodeSystem.CODE_SYSTEM_MEASURE_DATA_USAGE_URI,
                "Supplemental Data"));
    codeableConcept.setText("Supplemental Data Guidance");
    Extension ext = new Extension(UriConstants.CqfMeasures.SUPPLEMENTAL_DATA_GUIDANCE_URI);
    ext.setId("supplementalDataGuidance");
    ext.addExtension(
        new Extension(
            "guidance",
            new StringType(sanitizeText(madieMeasure.getSupplementalDataDescription()))));
    ext.addExtension(new Extension("usage", codeableConcept));

    return ext;
  }

  /**
   * Collect descriptions for all risk adjustment variables and combine into a single extension for
   * Risk Adjustment Variable Guidance
   *
   * @param madieMeasure
   * @return
   */
  public Extension buildRiskAdjustmentGuidanceExt(Measure madieMeasure) {
    if (CollectionUtils.isEmpty(madieMeasure.getRiskAdjustments())) {
      return null;
    }

    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setCoding(new ArrayList<>());
    codeableConcept
        .getCoding()
        .add(
            buildCoding(
                "risk-adjustment-factor",
                UriConstants.CodeSystem.CODE_SYSTEM_MEASURE_DATA_USAGE_URI,
                "Risk Adjustment Factor"));
    codeableConcept.setText("Risk Adjustment Variable Guidance");
    Extension ext = new Extension(UriConstants.CqfMeasures.SUPPLEMENTAL_DATA_GUIDANCE_URI);
    ext.setId("riskAdjustmentVariableGuidance");
    ext.addExtension(
        new Extension(
            "guidance", new StringType(sanitizeText(madieMeasure.getRiskAdjustmentDescription()))));
    ext.addExtension(new Extension("usage", codeableConcept));

    return ext;
  }

  /**
   * Build extension for each measure definition. Assumes that each measure definition contains a
   * Term and Definition of that Term.
   *
   * @param madieMeasure
   * @return List of Extensions where each Extension represents a measure definition.
   */
  private List<Extension> buildMeasureDefinitionExts(Measure madieMeasure) {
    if (CollectionUtils.isEmpty(madieMeasure.getMeasureMetaData().getMeasureDefinitions())) {
      return Collections.emptyList();
    }

    return madieMeasure.getMeasureMetaData().getMeasureDefinitions().stream()
        .map(
            msrDef -> {
              Extension ext = new Extension(UriConstants.CqfMeasures.MEASURE_DEFINITION_EXT_URI);
              ext.addExtension(new Extension("term", new StringType(msrDef.getTerm())));
              ext.addExtension(
                  new Extension(
                      "definition", new MarkdownType(sanitizeText(msrDef.getDefinition()))));
              return ext;
            })
        .collect(Collectors.toList());
  }

  private List<MeasureSupplementalDataComponent> buildSupplementalDataElements(
      Measure madieMeasure) {
    if (madieMeasure.getSupplementalData() == null) {
      return Collections.emptyList();
    }
    return madieMeasure.getSupplementalData().stream()
        .map(
            supplementalData -> {
              var measureSupplementalDataComponent = new MeasureSupplementalDataComponent();
              measureSupplementalDataComponent.setId(
                  supplementalData.getDefinition().toLowerCase().replace(" ", "-"));
              measureSupplementalDataComponent.setCriteria(
                  buildExpression("text/cql-identifier", supplementalData.getDefinition()));
              measureSupplementalDataComponent.setDescription(
                  sanitizeText(supplementalData.getDefinition()));
              measureSupplementalDataComponent.setUsage(
                  List.of(
                      buildCodeableConcept(
                          "supplemental-data",
                          "http://terminology.hl7.org/CodeSystem/measure-data-usage",
                          null)));
              if (isNotEmpty(supplementalData.getIncludeInReportType())) {
                supplementalData
                    .getIncludeInReportType()
                    .forEach(
                        measureReportType ->
                            measureSupplementalDataComponent.addExtension(
                                buildIncludeInReportTypeExtension(measureReportType)));
              }
              return measureSupplementalDataComponent;
            })
        .collect(Collectors.toList());
  }

  private List<MeasureSupplementalDataComponent> buildRiskAdjustmentFactors(Measure madieMeasure) {
    if (madieMeasure.getRiskAdjustments() == null) {
      return Collections.emptyList();
    }
    return madieMeasure.getRiskAdjustments().stream()
        .map(
            riskAdjustment -> {
              var measureSupplementalDataComponent = new MeasureSupplementalDataComponent();
              measureSupplementalDataComponent.setId(
                  riskAdjustment.getDefinition().toLowerCase().replace(" ", "-"));
              measureSupplementalDataComponent.setCriteria(
                  buildExpression("text/cql-identifier", riskAdjustment.getDefinition()));
              measureSupplementalDataComponent.setUsage(
                  List.of(
                      buildCodeableConcept(
                          "risk-adjustment-factor",
                          "http://terminology.hl7.org/CodeSystem/measure-data-usage",
                          null)));
              measureSupplementalDataComponent.setDescription(
                  sanitizeText(riskAdjustment.getDefinition()));
              if (isNotEmpty(riskAdjustment.getIncludeInReportType())) {
                riskAdjustment
                    .getIncludeInReportType()
                    .forEach(
                        measureReportType ->
                            measureSupplementalDataComponent.addExtension(
                                buildIncludeInReportTypeExtension(measureReportType)));
              }
              return measureSupplementalDataComponent;
            })
        .collect(Collectors.toList());
  }

  private Extension buildIncludeInReportTypeExtension(MeasureReportType measureReportType) {
    return new Extension(
        UriConstants.CqfMeasures.INCLUDE_IN_REPORT_TYPE_URI,
        new CodeType(measureReportType.toCode()));
  }

  private List<RelatedArtifact> buildRelatedArtifacts(
      List<gov.cms.madie.models.measure.Reference> references) {
    return references.stream()
        .map(
            reference ->
                new RelatedArtifact()
                    .setType(
                        RelatedArtifactType.fromCode(
                            !"unknown".equalsIgnoreCase(reference.getReferenceType())
                                ? reference.getReferenceType().toLowerCase()
                                : ""))
                    .setCitation(sanitizeText(reference.getReferenceText())))
        .collect(Collectors.toList());
  }

  private String findReferencedPopulationDisplayIdByCriteriaReference(
      String reference, Group group) {
    Optional<Population> findPopulation =
        group.getPopulations().stream()
            .filter(population -> population.getId().equals(reference))
            .findFirst();
    return findPopulation.isPresent() ? findPopulation.get().getDisplayId() : null;
  }
}

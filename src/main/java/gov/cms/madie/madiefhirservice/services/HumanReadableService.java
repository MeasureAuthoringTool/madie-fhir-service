package gov.cms.madie.madiefhirservice.services;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.ParameterDefinition;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.springframework.stereotype.Service;

import gov.cms.madie.madiefhirservice.constants.UriConstants.CqfMeasures;
import gov.cms.madie.madiefhirservice.exceptions.HumanReadableGenerationException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanReadableService extends ResourceUtils {

  private final LiquidEngine liquidEngine;

  private String escapeStr(String val) {
    if (val != null && !val.isEmpty()) {
      return htmlEscape(val);
    }
    return val;
  }

  private void escapeTopLevelProperties(org.hl7.fhir.r5.model.Measure measure) {
    measure.setPublisher(escapeStr(measure.getPublisher()));
    measure.setDescription(escapeStr(measure.getDescription()));
    measure.setPurpose(escapeStr(measure.getPurpose()));
    measure.setUsage(escapeStr(measure.getUsage()));
    measure.setCopyright(escapeStr(measure.getCopyright()));
    measure.setDisclaimer(escapeStr(measure.getDisclaimer()));
    measure.setGuidance(escapeStr(measure.getGuidance()));
    measure.setClinicalRecommendationStatement(
        escapeStr(measure.getClinicalRecommendationStatement()));
  }

  private void escapeSupplementalProperties(org.hl7.fhir.r5.model.Measure measure) {
    // supplemental data Elements
    measure
        .getSupplementalData()
        .forEach(
            supplementalData -> {
              supplementalData.setDescription(escapeStr(supplementalData.getDescription()));
              Expression criteria = supplementalData.getCriteria();
              criteria.setExpression(escapeStr(criteria.getExpression()));
              criteria.setDescription(escapeStr(criteria.getDescription()));
            });
  }

  public void escapeContainedProperties(org.hl7.fhir.r5.model.Measure measure) {
    measure
        .getContained()
        .forEach(
            contained -> {
              org.hl7.fhir.r5.model.Library lib = (org.hl7.fhir.r5.model.Library) contained;
              List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
              lib.getExtension()
                  .forEach(
                      extension -> {
                        extension
                            .getExtension()
                            .forEach(
                                innerExtension -> {
                                  innerExtension.setValue(
                                      new StringType(
                                          escapeStr(innerExtension.getValue().primitiveValue())));
                                });
                      });
              // population criteria
              relatedArtifacts.forEach(
                  relatedArtifact -> {
                    relatedArtifact.setLabel(escapeStr(relatedArtifact.getLabel()));
                    relatedArtifact.setCitation(escapeStr(relatedArtifact.getCitation()));
                    relatedArtifact.setDisplay(escapeStr(relatedArtifact.getDisplay()));
                    relatedArtifact.setResource(escapeStr(relatedArtifact.getResource()));
                  });
            });
  }

  public org.hl7.fhir.r5.model.Measure escapeMeasure(org.hl7.fhir.r5.model.Measure measure) {
    escapeTopLevelProperties(measure);
    escapeSupplementalProperties(measure);
    escapeContainedProperties(measure);
    // logic definitions, effective data requirements
    // risk factors and supplemental data guidance
    measure
        .getExtension()
        .forEach(
            topLevelExtension -> {
              topLevelExtension
                  .getExtension()
                  .forEach(
                      secondLevelExtension -> {
                        if (secondLevelExtension.getValue() instanceof StringType) {
                          secondLevelExtension.setValue(
                              new StringType(
                                  escapeStr(secondLevelExtension.getValue().primitiveValue())));
                        }
                      });
            });

    // population criteria descriptions
    measure
        .getGroup()
        .forEach(
            group -> {
              // top level description for population criteria
              group.setDescription(escapeStr(group.getDescription()));
              group
                  .getPopulation()
                  .forEach(
                      population -> {
                        // update each population description
                        population.setDescription(escapeStr(population.getDescription()));
                        Expression criteria = population.getCriteria();
                        criteria.setExpression(escapeStr(criteria.getExpression()));
                      });
            });
    return measure;
  }

  public String generateMeasureHumanReadable(
      Measure madieMeasure,
      Bundle bundleResource,
      org.hl7.fhir.r5.model.Library effectiveDataRequirements) {
    log.info("Generating human readable for measure: {}", madieMeasure.getId());
    if (bundleResource == null) {
      log.error("Unable to find a bundleResource for measure {}", madieMeasure.getId());
      throw new ResourceNotFoundException("bundle", madieMeasure.getId());
    }

    try {
      Resource measureResource = getResource(bundleResource, "Measure");
      if (measureResource == null) {
        log.error("Unable to find measure resource for measure {}", madieMeasure.getId());
        throw new ResourceNotFoundException("measure resource", madieMeasure.getId());
      }
      // converting measure resource from R4 to R5 as we are using r5 liquid engine.
      var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
      org.hl7.fhir.r5.model.Measure r5Measure =
          (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measureResource);
      // sort effectiveDataRequirements.parameters
      sortParameters(madieMeasure, effectiveDataRequirements);
      r5Measure.addContained(effectiveDataRequirements);

      r5Measure.getExtension().add(createEffectiveDataRequirementExtension());
      // escape html
      org.hl7.fhir.r5.model.Measure escapedR5Measure = escapeMeasure(r5Measure);

      String measureTemplate = getData("/templates/Measure.liquid");
      LiquidEngine.LiquidDocument doc = liquidEngine.parse(measureTemplate, "hr-script");
      return liquidEngine.evaluate(doc, escapedR5Measure, null);
    } catch (FHIRException fhirException) {
      log.error(
          "Unable to generate Human readable for measure {} Reason => {}",
          madieMeasure.getId(),
          fhirException);
      throw new HumanReadableGenerationException("measure", madieMeasure.getId());
    }
  }

  private void sortParameters(
      Measure madieMeasure, org.hl7.fhir.r5.model.Library effectiveDataRequirements) {
    List<String> suppDataDefs =
        madieMeasure.getSupplementalData().stream()
            .map((s) -> s.getDefinition())
            .collect(Collectors.toList());
    List<String> riskAdjDefs =
        madieMeasure.getRiskAdjustments().stream()
            .map((s) -> s.getDefinition())
            .collect(Collectors.toList());
    List<String> strats = new ArrayList<String>();
    if (madieMeasure.getGroups() != null) {
      madieMeasure.getGroups().stream()
          .forEach(
              (g) ->
              {
                  if (CollectionUtils.isNotEmpty(g.getStratifications())) {
                      strats.addAll(
                              g.getStratifications().stream()
                                      .map(s -> s.getCqlDefinition())
                                      .collect(Collectors.toList()));
                  }
              });
    }

    Collections.sort(
        effectiveDataRequirements.getParameter(),
        new Comparator<ParameterDefinition>() {

          @Override
          public int compare(ParameterDefinition o1, ParameterDefinition o2) {

            int ord1 = determineOrd(o1, suppDataDefs, riskAdjDefs, strats);
            int ord2 = determineOrd(o2, suppDataDefs, riskAdjDefs, strats);

            int result = ord1 - ord2;
            return result;
          }
        });
  }

  private int determineOrd(
      ParameterDefinition paramDef,
      List<String> suppDataDefs,
      List<String> riskAdjDefs,
      List<String> strats) {
    int result = 1; // default = 1

    // if paramDef is a period, then ord = 0
    if (paramDef != null
        && paramDef.getType() != null
        && paramDef.getType().toCode().equals(FHIRTypes.PERIOD.toCode())) {
      result = 0;
    }
    // if paramDef is a supp data then ord = 2
    if (paramDef != null && suppDataDefs.contains(paramDef.getName())) {
      result = 2;
    }

    // if paramDef is a risk adjustment data then ord = 3
    if (paramDef != null && riskAdjDefs.contains(paramDef.getName())) {
      result = 3;
    }

    // if paramDef is a stratification data then ord = 4
    if (paramDef != null && strats.contains(paramDef.getName())) {
      result = 4;
    }

    // if paramDef is anything else then ord = 1
    return result;
  }

  /**
   * Generate human-readable for a library
   *
   * @param library fhir r4 Library
   * @return human-readable string
   */
  public String generateLibraryHumanReadable(Library library) {
    if (library == null) {
      return "<div></div>";
    }
    log.info("Generating human readable for library {}", library.getName());
    // convert r4 libray to R5 library as we are using r5 liquid engine
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r5.model.Library r5Library =
        (org.hl7.fhir.r5.model.Library) versionConvertor_40_50.convertResource(library);
    String template = getData("/templates/Library.liquid");
    try {
      LiquidEngine.LiquidDocument doc = liquidEngine.parse(template, "libray-hr");
      return liquidEngine.evaluate(doc, r5Library, "madie");
    } catch (FHIRException ex) {
      log.error("Error occurred while generating human readable for library:", ex);
      throw new HumanReadableGenerationException(
          "Error occurred while generating human readable for library: " + library.getName());
    }
  }

  private Extension createEffectiveDataRequirementExtension() {
    var extension = new Extension();
    extension.setUrl(CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL);
    extension.getValueReference().setReference("#effective-data-requirements");
    return extension;
  }

  protected String addCssToHumanReadable(String measureHr) {
    // Wrapper template for Measure.liquid o/p
    String humanReadable = getData("/templates/HumanReadable.liquid");
    return humanReadable.replace("human_readable_content_holder", measureHr);
  }
}

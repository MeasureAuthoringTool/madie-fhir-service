package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.exceptions.HumanReadableGenerationException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.liquid.LiquidEngine;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

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

  public void escapeContainedProperties(org.hl7.fhir.r5.model.Measure measure) {
    measure
        .getContained()
        .forEach(
            contained -> {
              org.hl7.fhir.r5.model.Library lib = (org.hl7.fhir.r5.model.Library) contained;
              List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
              if (lib.hasParameter()) {
                lib.setParameter(
                    lib.getParameter().stream()
                        .map(
                            parameterDefinition ->
                                parameterDefinition.setName(
                                    escapeStr(parameterDefinition.getName())))
                        .collect(Collectors.toList()));
              }
              lib.getExtension()
                  .forEach(
                      extension -> {
                        extension
                            .getExtension()
                            .forEach(
                                innerExtension -> {
                                  if (innerExtension.getValue() instanceof StringType) {
                                    innerExtension.setValue(
                                        new StringType(
                                            escapeStr(innerExtension.getValue().primitiveValue())));
                                  }
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

  public String generateMeasureHumanReadable(Resource measure, String id) {
    log.info("Generating human readable for measure: {}", id);
    if (measure == null) {
      log.error("Null measure resource for {}", id);
      throw new ResourceNotFoundException("Measure", id);
    }

    try {
      // converting measure resource from R4 to R5 as we are using r5 liquid engine.
      var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
      Measure r5Measure = (Measure) versionConvertor_40_50.convertResource(measure);

      // escape measure.contained properties
      escapeContainedProperties(r5Measure);

      String measureTemplate = getData("/templates/Measure.liquid");
      LiquidEngine.LiquidDocument doc = liquidEngine.parse(measureTemplate, "hr-script");
      return liquidEngine.evaluate(doc, r5Measure, null);
    } catch (FHIRException fhirException) {
      log.error("Unable to generate Human readable for measure {} Reason: ", id, fhirException);
      throw new HumanReadableGenerationException("measure", id);
    }
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
    // escape html
    escapeLibrary(r5Library);
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

  private void escapeLibrary(org.hl7.fhir.r5.model.Library r5Library) {
    r5Library.setTitle(escapeStr(r5Library.getTitle()));
    r5Library.setSubtitle(escapeStr(r5Library.getSubtitle()));
    r5Library.setPublisher(escapeStr(r5Library.getPublisher()));
    r5Library.setDescription(escapeStr(r5Library.getDescription()));
    r5Library.setPurpose(escapeStr(r5Library.getPurpose()));
    r5Library.setUsage(escapeStr(r5Library.getUsage()));
    r5Library.setCopyright(escapeStr(r5Library.getCopyright()));

    r5Library
        .getRelatedArtifact()
        .forEach(
            relatedArtifact -> relatedArtifact.setDisplay(escapeStr(relatedArtifact.getDisplay())));
    r5Library
        .getDataRequirement()
        .forEach(
            dataRequirement ->
                dataRequirement
                    .getCodeFilter()
                    .forEach(
                        cf ->
                            cf.getCode()
                                .forEach(
                                    coding -> coding.setDisplay(escapeStr(coding.getDisplay())))));

    r5Library.setContent(
        r5Library.getContent().stream()
            .filter(content -> content.getContentType().equalsIgnoreCase("text/cql"))
            .map(content -> content.setData(escapeStr(new String(content.getData())).getBytes()))
            .collect(Collectors.toList()));
  }
}

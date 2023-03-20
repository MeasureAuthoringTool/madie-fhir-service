package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql.CqlFormatter;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.api.MethodOutcome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureBundleService {
  private final MeasureTranslatorService measureTranslatorService;
  private final LibraryTranslatorService libraryTranslatorService;
  private final LibraryService libraryService;
  private final HapiFhirServer hapiFhirServer;
  private final ElmTranslatorClient elmTranslatorClient;
  private final HumanReadableService humanReadableService;

  /**
   * Creates measure bundle that contains measure, main library, and included libraries resources
   */
  public Bundle createMeasureBundle(
      Measure madieMeasure, Principal principal, String bundleType, String accessToken) {
    log.info("Generating measure bundle for measure {}", madieMeasure.getId());
    madieMeasure.setCql(CqlFormatter.formatCql(madieMeasure.getCql(), principal));

    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure);

    // Bundle entry for Measure resource
    Bundle.BundleEntryComponent measureEntryComponent = getBundleEntryComponent(measure);
    Bundle bundle =
        new Bundle().setType(Bundle.BundleType.TRANSACTION).addEntry(measureEntryComponent);
    // Bundle entries for all the library resources of a MADiE Measure
    List<Bundle.BundleEntryComponent> libraryEntryComponents =
        createBundleComponentsForLibrariesOfMadieMeasure(madieMeasure);
    libraryEntryComponents.forEach(bundle::addEntry);

    if (BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT.equals(bundleType)) {
      // get effective DataRequirements
      org.hl7.fhir.r5.model.Library effectiveDataRequirements =
          elmTranslatorClient.getEffectiveDataRequirements(
              bundle, madieMeasure.getCqlLibraryName(), madieMeasure.getId(), accessToken);
      // get human-readable for measure
      String humanReadable =
          humanReadableService.generateMeasureHumanReadable(
              madieMeasure, bundle, effectiveDataRequirements);
      // set narrative and effective DataRequirements to measure
      setNarrativeText(measure, humanReadable);
      addEffectiveDataRequirementsToMeasure(measure, effectiveDataRequirements);

      // set narrative to measure library
      var measureLibrary =
          (org.hl7.fhir.r4.model.Library) ResourceUtils.getResource(bundle, "Library");
      String libraryHr = humanReadableService.generateLibraryHumanReadable(measureLibrary);
      setNarrativeText(measureLibrary, libraryHr);
    }
    return bundle;
  }

  /**
   * Collects BundleEntryComponents for main measure library and included libraries
   *
   * @param madieMeasure instance of MADiE Measure
   * @return list of Library BundleEntryComponents
   */
  public List<Bundle.BundleEntryComponent> createBundleComponentsForLibrariesOfMadieMeasure(
      Measure madieMeasure) {
    Library library = getMeasureLibraryResourceForMadieMeasure(madieMeasure);
    Bundle.BundleEntryComponent mainLibraryBundleComponent = getBundleEntryComponent(library);
    Map<String, Library> includedLibraryMap = new HashMap<>();
    libraryService.getIncludedLibraries(madieMeasure.getCql(), includedLibraryMap);
    List<Bundle.BundleEntryComponent> libraryBundleComponents =
        includedLibraryMap.values().stream()
            .map(this::getBundleEntryComponent)
            .collect(Collectors.toList());
    // add main library first in the list
    libraryBundleComponents.add(0, mainLibraryBundleComponent);
    return libraryBundleComponents;
  }

  /** Creates BundleEntryComponent for given resource */
  public Bundle.BundleEntryComponent getBundleEntryComponent(Resource resource) {
    return new Bundle.BundleEntryComponent().setResource(resource);
  }

  /**
   * Creates Library resource for main library of MADiE Measure
   *
   * @param madieMeasure instance of MADiE Measure
   * @return Library
   */
  public Library getMeasureLibraryResourceForMadieMeasure(Measure madieMeasure) {
    CqlLibrary cqlLibrary = createCqlLibraryForMadieMeasure(madieMeasure);
    return libraryTranslatorService.convertToFhirLibrary(
        cqlLibrary, madieMeasure.getProgramUseContext());
  }

  /**
   * Creates CqlLibrary resource for main library of MADiE Measure this will most likely go in
   * measure service once we have main library for measure
   *
   * @param madieMeasure instance of MADiE Measure
   * @return CqlLibrary
   */
  public CqlLibrary createCqlLibraryForMadieMeasure(Measure madieMeasure) {
    return CqlLibrary.builder()
        .id(madieMeasure.getCqlLibraryName())
        .cqlLibraryName(madieMeasure.getCqlLibraryName())
        .version(madieMeasure.getVersion())
        .description(madieMeasure.getCqlLibraryName())
        .cql(madieMeasure.getCql())
        .elmJson(madieMeasure.getElmJson())
        .elmXml(madieMeasure.getElmXml())
        .build();
  }

  public MethodOutcome saveMeasureBundle(String measureBundle) {
    return hapiFhirServer.createResourceAsString(measureBundle);
  }

  private void setNarrativeText(DomainResource resource, String humanReadable) {
    Narrative narrative = new Narrative();
    narrative.setStatus(Narrative.NarrativeStatus.EXTENSIONS);
    narrative.setDivAsString(humanReadable);
    resource.setText(narrative);
  }

  private void addEffectiveDataRequirementsToMeasure(
      org.hl7.fhir.r4.model.Measure measure,
      org.hl7.fhir.r5.model.Library effectiveDataRequirements) {
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r4.model.Library r4EffectiveDataRequirements =
        (org.hl7.fhir.r4.model.Library)
            versionConvertor_40_50.convertResource(effectiveDataRequirements);
    // TODO: verify effective data requirement profile compliance:
    // http://hl7.org/fhir/us/cqfmeasures/StructureDefinition-module-definition-library-cqfm.html
    measure.addContained(r4EffectiveDataRequirements);
    Extension extension =
        new Extension()
            .setUrl(UriConstants.CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL)
            .setValue(new Reference().setReference("#effective-data-requirements"));
    measure.getExtension().add(extension);
  }
}

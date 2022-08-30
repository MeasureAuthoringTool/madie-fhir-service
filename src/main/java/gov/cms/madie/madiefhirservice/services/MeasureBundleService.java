package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitor;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.library.Version;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureBundleService {
  private final LibraryCqlVisitorFactory libCqlVisitorFactory;
  private final MeasureTranslatorService measureTranslatorService;
  private final LibraryTranslatorService libraryTranslatorService;
  private final HapiFhirServer hapiFhirServer;

  /**
   * Creates measure bundle that contains measure, main library, and included libraries resources
   */
  public Bundle createMeasureBundle(Measure madieMeasure) {
    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure);
    // Bundle entry for Measure resource
    Bundle.BundleEntryComponent measureEntryComponent = getBundleEntryComponent(measure);
    Bundle bundle =
        new Bundle().setType(Bundle.BundleType.TRANSACTION).addEntry(measureEntryComponent);
    // Bundle entries for all the library resources of a MADiE Measure
    List<Bundle.BundleEntryComponent> libraryEntryComponents =
        createBundleComponentsForLibrariesOfMadieMeasure(madieMeasure);
    libraryEntryComponents.forEach(entryComponent -> bundle.addEntry(entryComponent));
    return bundle;
  }

  /**
   * Collects BundleEntryComponents for main measure library and included libraries
   *
   * @param madieMeasure
   * @return list of Library BundleEntryComponents
   */
  public List<Bundle.BundleEntryComponent> createBundleComponentsForLibrariesOfMadieMeasure(
      Measure madieMeasure) {
    Library library = getMeasureLibraryResourceForMadieMeasure(madieMeasure);
    Bundle.BundleEntryComponent mainLibraryBundleComponent = getBundleEntryComponent(library);
    LibraryCqlVisitor visitor = libCqlVisitorFactory.visit(madieMeasure.getCql());
    List<Bundle.BundleEntryComponent> libraryBundleComponents =
        visitor.getIncludedLibraries().stream()
            .map(
                libraryNameValuePair -> {
                  Optional<Library> optionalLibrary =
                      hapiFhirServer.fetchHapiLibrary(
                          libraryNameValuePair.getLeft(), libraryNameValuePair.getRight());
                  return optionalLibrary
                      .map(this::getBundleEntryComponent)
                      .orElseThrow(
                          () ->
                              new HapiLibraryNotFoundException(
                                  libraryNameValuePair.getLeft(), libraryNameValuePair.getRight()));
                })
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
   * @param madieMeasure
   * @return Library
   */
  public Library getMeasureLibraryResourceForMadieMeasure(Measure madieMeasure) {
    CqlLibrary cqlLibrary = createCqlLibraryForMadieMeasure(madieMeasure);
    return libraryTranslatorService.convertToFhirLibrary(cqlLibrary);
  }

  /**
   * Creates CqlLibrary resource for main library of MADiE Measure this will most likely go in
   * measure service once we have main library for measure
   *
   * @param madieMeasure
   * @return CqlLibrary
   */
  public CqlLibrary createCqlLibraryForMadieMeasure(Measure madieMeasure) {
    return CqlLibrary.builder()
        .id(madieMeasure.getCqlLibraryName())
        .cqlLibraryName(madieMeasure.getCqlLibraryName())
        .version(Version.parse(madieMeasure.getVersion()))
        .description(madieMeasure.getCqlLibraryName())
        .cql(madieMeasure.getCql())
        .elmJson(madieMeasure.getElmJson())
        .elmXml(madieMeasure.getElmXml())
        .build();
  }
}

package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.exceptions.HumanReadableGenerationException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import org.hl7.fhir.MeasureGroup;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.ParameterDefinition;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest implements ResourceFileUtil {

  @Mock LiquidEngine liquidEngine;

  @InjectMocks HumanReadableService humanReadableService;

  private Measure madieMeasure;

  private org.hl7.fhir.r4.model.Measure measure;

  private Library library;

  private org.hl7.fhir.r5.model.Library effectiveDataRequirements;

  private String humanReadable;

  @BeforeEach
  void setUp() {
    Group measureGroup1 = Group.builder()
            .id("GroupId1")
            .groupDescription("some random group")
            .measureGroupTypes(List.of(MeasureGroupTypes.OUTCOME))
            .scoring(MeasureScoring.COHORT.toString())
            .populations(List.of(
                    Population.builder()
                            .id("PopId1")
                            .name(PopulationType.INITIAL_POPULATION)
                            .definition("Initial Population")
                            .description(null)
                            .build()
            ))
            .build();
    measureGroup1.setStratifications(null);

    madieMeasure =
        Measure.builder()
            .id("madie-test-id")
            .measureName("test_measure_name")
            .cqlLibraryName("test_cql_library_name")
            .version(new Version(1, 0, 0))
            .measurementPeriodStart(new Date())
            .measurementPeriodEnd(new Date())
            .measureMetaData(
                new MeasureMetaData()
                    .toBuilder()
                    .copyright("test_copyright")
                    .disclaimer("test_disclaimer")
                    .build())
            .groups(List.of(measureGroup1))
            .build();

    measure =
        new org.hl7.fhir.r4.model.Measure()
            .setName(madieMeasure.getCqlLibraryName())
            .setTitle(madieMeasure.getMeasureName())
            .setExperimental(true)
            .setUrl("fhirBaseUrl/Measure/" + madieMeasure.getCqlLibraryName())
            .setVersion(madieMeasure.getVersion().toString())
            .setEffectivePeriod(
                getPeriodFromDates(
                    madieMeasure.getMeasurementPeriodStart(),
                    madieMeasure.getMeasurementPeriodEnd()))
            .setCopyright(madieMeasure.getMeasureMetaData().getCopyright())
            .setDisclaimer(madieMeasure.getMeasureMetaData().getDisclaimer());

    String cqlData = ResourceUtils.getData("/test-cql/cv_populations.cql");
    library =
        new Library()
            .addContent(new Attachment().setData(cqlData.getBytes()).setContentType("text/cql"));

    library.setId(madieMeasure.getCqlLibraryName());

    effectiveDataRequirements =
        convertToFhirR5Resource(
            org.hl7.fhir.r5.model.Library.class,
            getStringFromTestResource("/humanReadable/effective-data-requirements.json"));

    humanReadable = getStringFromTestResource("/humanReadable/humanReadable_test");
  }

  public Bundle.BundleEntryComponent getBundleEntryComponent(Resource resource) {
    return new Bundle.BundleEntryComponent().setResource(resource);
  }

  private Period getPeriodFromDates(Date startDate, Date endDate) {
    return new Period()
        .setStart(startDate, TemporalPrecisionEnum.DAY)
        .setEnd(endDate, TemporalPrecisionEnum.DAY);
  }

  @Test
  public void generateMeasureHumanReadable() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    String hrText = "<div>Human Readable for Measure: " + madieMeasure.getMeasureName() + "</div>";

    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            any(org.hl7.fhir.r5.model.Measure.class),
            any()))
        .thenReturn(hrText);

    String generatedHumanReadable =
        humanReadableService.generateMeasureHumanReadable(
            madieMeasure, bundle, effectiveDataRequirements);
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains(hrText));
  }

  @Test
  public void generateMeasureHumanReadableOrdered() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    String hrText = "<div>Human Readable for Measure: " + madieMeasure.getMeasureName() + "</div>";

    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            argThat(
                (measure) -> {
                  org.hl7.fhir.r5.model.Measure m = (org.hl7.fhir.r5.model.Measure) measure;

                  org.hl7.fhir.r5.model.Library library =
                      (org.hl7.fhir.r5.model.Library) m.getContained().get(0);
                  ParameterDefinition paramDef = library.getParameter().get(0);

                  return "Period".equals(paramDef.getType().toCode());
                }),
            any()))
        .thenReturn(hrText);

    String generatedHumanReadable =
        humanReadableService.generateMeasureHumanReadable(
            madieMeasure, bundle, effectiveDataRequirements);
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains(hrText));
  }

  @Test
  public void generateHumanReadableThrowsResourceNotFoundExceptionForNoBundle() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> humanReadableService.generateMeasureHumanReadable(madieMeasure, null, null));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoMeasureResource() {
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(getBundleEntryComponent(new Library()));

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            humanReadableService.generateMeasureHumanReadable(
                madieMeasure, bundle, effectiveDataRequirements));
  }

  @Test
  public void generateHumanReadableThrowsFHIRException() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            any(org.hl7.fhir.r5.model.Measure.class),
            any()))
        .thenThrow(new FHIRException());

    assertThrows(
        HumanReadableGenerationException.class,
        () ->
            humanReadableService.generateMeasureHumanReadable(
                madieMeasure, bundle, effectiveDataRequirements));
  }

  @Test
  public void testGetHumanReadableForLibrary() {
    String hrText = "<div>test hr text for library</div>";
    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            any(org.hl7.fhir.r5.model.Library.class),
            anyString()))
        .thenReturn(hrText);
    String hr = humanReadableService.generateLibraryHumanReadable(library);
    assertEquals(hr, hrText);
  }

  @Test
  public void testGetHumanReadableForLibraryWhenLibraryIsnull() {
    assertEquals(humanReadableService.generateLibraryHumanReadable(null), "<div></div>");
  }

  @Test
  public void testGetHumanReadableForLibraryWhenTemplateEvaluationFailed() {
    library.setName(madieMeasure.getCqlLibraryName());
    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            any(org.hl7.fhir.r5.model.Library.class),
            anyString()))
        .thenThrow(new FHIRException());
    Exception ex =
        assertThrows(
            HumanReadableGenerationException.class,
            () -> humanReadableService.generateLibraryHumanReadable(library));
    assertEquals(
        ex.getMessage(),
        "Error occurred while generating human readable for library: " + library.getName());
  }

  @Test
  public void testAddCssToHumanReadable() {
    String humanReadableWithCSS = humanReadableService.addCssToHumanReadable(humanReadable);
    assertTrue(humanReadableWithCSS.contains("<style>"));
  }
}

package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.constants.UriConstants.CqfMeasures;
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
import gov.cms.madie.models.measure.Stratification;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.model.ParameterDefinition;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
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
class HumanReadableServiceTest
    implements ResourceFileUtil, LiquidEngine.ILiquidEngineIncludeResolver {

  @Mock LiquidEngine liquidEngine;

  @InjectMocks HumanReadableService humanReadableService;

  private Measure madieMeasure;

  private org.hl7.fhir.r4.model.Measure measure;

  private Library library;

  private org.hl7.fhir.r5.model.Library effectiveDataRequirements;

  List<MarkdownType> terms = new ArrayList<>();
  MarkdownType term1 = new MarkdownType("Term1 - Definition1");
  MarkdownType term2 = new MarkdownType("Term2 - Definition2");

  @BeforeEach
  void setUp() {
    Group measureGroup1 =
        Group.builder()
            .id("GroupId1")
            .groupDescription("some random group")
            .measureGroupTypes(List.of(MeasureGroupTypes.OUTCOME))
            .scoring(MeasureScoring.COHORT.toString())
            .populations(
                List.of(
                    Population.builder()
                        .id("PopId1")
                        .name(PopulationType.INITIAL_POPULATION)
                        .definition("Initial Population")
                        .description(null)
                        .build()))
            .build();
    Stratification stratification =
        Stratification.builder().cqlDefinition("Stratification 1").build();
    measureGroup1.setStratifications(List.of(stratification));

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
                    .toBuilder().copyright("test_copyright").disclaimer("test_disclaimer").build())
            .groups(List.of(measureGroup1))
            .build();

    terms.add(term1);
    terms.add(term2);
    measure =
        new org.hl7.fhir.r4.model.Measure()
            .setName(madieMeasure.getCqlLibraryName())
            .setTitle(madieMeasure.getMeasureName())
            .setExperimental(true)
            .setUrl("baseUrl/Measure/" + madieMeasure.getCqlLibraryName())
            .setVersion(madieMeasure.getVersion().toString())
            .setEffectivePeriod(
                getPeriodFromDates(
                    madieMeasure.getMeasurementPeriodStart(),
                    madieMeasure.getMeasurementPeriodEnd()))
            .setCopyright(madieMeasure.getMeasureMetaData().getCopyright())
            .setDisclaimer(madieMeasure.getMeasureMetaData().getDisclaimer())
            .setDefinition(terms);

    String cqlData = ResourceUtils.getData("/test-cql/cv_populations.cql");
    library =
        new Library()
            .addContent(new Attachment().setData(cqlData.getBytes()).setContentType("text/cql"));

    library.setId(madieMeasure.getCqlLibraryName());

    effectiveDataRequirements =
        convertToFhirR5Resource(
            org.hl7.fhir.r5.model.Library.class,
            getStringFromTestResource("/humanReadable/effective-data-requirements.json"));
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
    RelatedArtifact relatedArtifact = new RelatedArtifact();
    relatedArtifact.setType(RelatedArtifactType.CITATION);
    relatedArtifact.setCitation("test reference text");
    measure.setRelatedArtifact(List.of(relatedArtifact));
    measure.addAuthor().setName("test contact details");
    Reference reference = new Reference();
    reference.setDisplay("test display reference");
    Identifier identifier = new Identifier();
    identifier.setAssigner(reference);
    measure.setIdentifier(List.of(identifier));

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
  public void generateMeasureHumanReadableUsingIncludes() throws IOException {
    var measureBundleEntryComponent = getBundleEntryComponent(measure);
    var libraryBundleEntryComponent = getBundleEntryComponent(library);
    var bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    var le = new LiquidEngine(new SimpleWorkerContext.SimpleWorkerContextBuilder().build(), null);
    // Set the include resolver
    le.setIncludeResolver(this);
    var hr = new HumanReadableService(le);

    var generatedHumanReadable =
        hr.generateMeasureHumanReadable(madieMeasure, bundle, effectiveDataRequirements);
    assertNotNull(generatedHumanReadable);
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
    library.addRelatedArtifact();
    org.hl7.fhir.r4.model.DataRequirement dataRequirement =
        new org.hl7.fhir.r4.model.DataRequirement();
    org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent codeFilter =
        new org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent();
    org.hl7.fhir.r4.model.Coding coding = new org.hl7.fhir.r4.model.Coding();
    coding.setDisplay("test display");
    codeFilter.addCode(coding);
    dataRequirement.setCodeFilter(List.of(codeFilter));
    library.addDataRequirement(dataRequirement);
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

  @Override
  public String fetchInclude(LiquidEngine engine, String name) {
    return gov.cms.madie.madiefhirservice.utils.ResourceUtils.getData("/templates/" + name);
  }

  @Test
  public void testEscapeMeasure() {
    org.hl7.fhir.r5.model.Measure r5Measure = new org.hl7.fhir.r5.model.Measure();

    org.hl7.fhir.r5.model.Expression expression = new org.hl7.fhir.r5.model.Expression();
    expression.setDescription("test description");
    org.hl7.fhir.r5.model.Measure.MeasureSupplementalDataComponent supplementalData =
        new org.hl7.fhir.r5.model.Measure.MeasureSupplementalDataComponent();
    supplementalData.setCriteria(expression);
    supplementalData.setDescription("test description");
    r5Measure.addSupplementalData().setCriteria(expression);

    org.hl7.fhir.r5.model.Library lib = new org.hl7.fhir.r5.model.Library();
    ParameterDefinition parameter = new ParameterDefinition();
    parameter.setName("test name");
    lib.addParameter(parameter);

    org.hl7.fhir.r5.model.Extension topLevelExtension = new org.hl7.fhir.r5.model.Extension();
    org.hl7.fhir.r5.model.Extension secondLevelExtension = new org.hl7.fhir.r5.model.Extension();
    secondLevelExtension.setValue(new org.hl7.fhir.r5.model.StringType("test string"));
    topLevelExtension.addExtension(secondLevelExtension);
    lib.addExtension(topLevelExtension);

    org.hl7.fhir.r5.model.RelatedArtifact r5RelatedArtifact =
        new org.hl7.fhir.r5.model.RelatedArtifact();
    r5RelatedArtifact.setCitation("test reference text");
    r5RelatedArtifact.setLabel("test label");
    r5RelatedArtifact.setDisplay("test display &");
    r5RelatedArtifact.setResource("test resource");
    lib.addRelatedArtifact(r5RelatedArtifact);

    r5Measure.addContained(lib);

    org.hl7.fhir.r5.model.Measure.MeasureTermComponent term =
        new org.hl7.fhir.r5.model.Measure.MeasureTermComponent();
    term.setDefinition("test definition");
    r5Measure.addTerm(term);

    r5Measure.addExtension(topLevelExtension);

    org.hl7.fhir.r5.model.Measure.MeasureGroupComponent group =
        new org.hl7.fhir.r5.model.Measure.MeasureGroupComponent();
    group.setDescription("test description");
    org.hl7.fhir.r5.model.Measure.MeasureGroupPopulationComponent population =
        new org.hl7.fhir.r5.model.Measure.MeasureGroupPopulationComponent();
    population.setDescription("test population description");
    org.hl7.fhir.r5.model.Expression criteria = new org.hl7.fhir.r5.model.Expression();
    criteria.setExpression("test expression");
    population.setCriteria(criteria);
    group.addPopulation(population);
    org.hl7.fhir.r5.model.Measure.MeasureGroupStratifierComponent stratifier =
        new org.hl7.fhir.r5.model.Measure.MeasureGroupStratifierComponent();
    stratifier.setCriteria(criteria);
    stratifier.setDescription("test stratifier description");
    group.setStratifier(List.of(stratifier));
    org.hl7.fhir.r5.model.Extension extension = new org.hl7.fhir.r5.model.Extension();
    extension.setUrl(CqfMeasures.RATE_AGGREGATION_URI);
    extension.setValue((new org.hl7.fhir.r5.model.StringType(CqfMeasures.RATE_AGGREGATION_URI)));
    group.addExtension(extension);
    r5Measure.addGroup(group);

    r5Measure.addRelatedArtifact(r5RelatedArtifact);

    org.hl7.fhir.r5.model.Measure result = humanReadableService.escapeMeasure(r5Measure);
    assertNotNull(result);
    assertNotNull(result.getRelatedArtifact());
    assertEquals(1, result.getRelatedArtifact().size());
    assertEquals("test display &amp;", result.getRelatedArtifact().get(0).getDisplay());
  }
}

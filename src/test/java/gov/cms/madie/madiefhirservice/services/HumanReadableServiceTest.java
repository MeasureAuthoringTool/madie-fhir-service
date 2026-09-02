package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
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
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.liquid.LiquidEngine;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest
    implements ResourceFileUtil, LiquidEngine.ILiquidEngineIncludeResolver {

  @Mock LiquidEngine liquidEngine;

  @InjectMocks HumanReadableService humanReadableService;

  private Measure madieMeasure;

  private org.hl7.fhir.r4.model.Measure measure;

  private Library library;

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

    Library r4EffectiveDataRequirements =
        convertToFhirR4Resource(
            getStringFromTestResource("/humanReadable/effective" + "-data-requirements.json"),
            Library.class);

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

    measure.addContained(r4EffectiveDataRequirements);

    String cqlData = ResourceUtils.getData("/test-cql/cv_populations.cql");
    library =
        new Library()
            .addContent(new Attachment().setData(cqlData.getBytes()).setContentType("text/cql"));

    library.setId(madieMeasure.getCqlLibraryName());
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

    String hrText = "<div>Human Readable for Measure: " + madieMeasure.getMeasureName() + "</div>";

    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            any(org.hl7.fhir.r5.model.Measure.class),
            any()))
        .thenReturn(hrText);

    String generatedHumanReadable =
        humanReadableService.generateMeasureHumanReadable(measure, madieMeasure.getId());
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains(hrText));
  }

  @Test
  public void generateMeasureHumanReadableUsingIncludes() throws IOException {
    measure.addUseContext(
        new UsageContext()
            .setCode(
                new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/usage-context-type")
                    .setCode("venue")
                    .setDisplay("Clinical Venue"))
            .setValue(
                new CodeableConcept(
                    new Coding()
                        .setSystem("http://cms.gov/CodeSystem/measure-eligibility")
                        .setCode("tele-health-eligible")
                        .setDisplay("Telehealth Eligible"))));

    var le = new LiquidEngine(new SimpleWorkerContext.SimpleWorkerContextBuilder().build(), null);
    // Set include resolver
    le.setIncludeResolver(this);
    var hr = new HumanReadableService(le);

    var generatedHumanReadable = hr.generateMeasureHumanReadable(measure, madieMeasure.getId());
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains("Clinical Venue"));
    assertTrue(generatedHumanReadable.contains("Telehealth Eligible"));
  }

  /** Pins the sample-content-ig template release; update this test on each version upgrade. */
  @Test
  public void humanReadableReportsPinnedLiquidTemplateVersion() throws IOException {
    var realEngine =
        new LiquidEngine(new SimpleWorkerContext.SimpleWorkerContextBuilder().build(), null);
    realEngine.setIncludeResolver(this);

    var measureHtml =
        new HumanReadableService(realEngine)
            .generateMeasureHumanReadable(measure, "template-version-check");

    assertTrue(
        measureHtml.contains("version 0.5.6 of the sample-content-ig Liquid templates"),
        "Human Readable should report the pinned Liquid template version");
  }

  /**
   * Since 0.5.6 the templates escape CQL via FHIRPath {@code .escape('html')}, so {@link
   * HumanReadableService} must not escape it as well.
   */
  @Test
  public void humanReadableEscapesCqlExactlyOnce() throws IOException {
    // Reference the contained effective-data-requirements so logic definitions render.
    measure
        .getExtension()
        .add(
            new org.hl7.fhir.r4.model.Extension()
                .setUrl(UriConstants.CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL)
                .setValue(new Reference().setReference("#effective-data-requirements")));

    ((Library) measure.getContained().get(0))
        .getExtension().stream()
            .filter(extension -> extension.getUrl().contains("logicDefinition"))
            .findFirst()
            .flatMap(
                logicDefinition ->
                    logicDefinition.getExtension().stream()
                        .filter(subExtension -> "statement".equals(subExtension.getUrl()))
                        .findFirst())
            .ifPresent(
                statement ->
                    statement.setValue(
                        new org.hl7.fhir.r4.model.StringType("define \"X\": 1 < 2 and 3 > 2 & 4")));

    var realEngine =
        new LiquidEngine(new SimpleWorkerContext.SimpleWorkerContextBuilder().build(), null);
    realEngine.setIncludeResolver(this);
    var realTemplateService = new HumanReadableService(realEngine);

    var measureHtml =
        realTemplateService.generateMeasureHumanReadable(measure, "cql-escaping-check");
    assertTrue(
        measureHtml.contains("1 &lt; 2 and 3 &gt; 2 &amp; 4"),
        "Measure CQL statements should be escaped exactly once");
    assertNoDoubleEscaping(measureHtml);

    var libraryHtml = realTemplateService.generateLibraryHumanReadable(library);
    assertTrue(
        libraryHtml.contains("Interval&lt;DateTime&gt;"),
        "Library CQL content should be escaped exactly once");
    assertNoDoubleEscaping(libraryHtml);
  }

  private void assertNoDoubleEscaping(String html) {
    for (String doubleEscaped : List.of("&amp;lt;", "&amp;gt;", "&amp;amp;", "&amp;quot;")) {
      assertFalse(
          html.contains(doubleEscaped),
          "Human Readable contains double-escaped entity: " + doubleEscaped);
    }
  }

  @Test
  public void generateMeasureHumanReadableOrdered() {
    String hrText = "<div>Human Readable for Measure: " + madieMeasure.getMeasureName() + "</div>";

    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            any(org.hl7.fhir.r5.model.Measure.class),
            any()))
        .thenReturn(hrText);

    String generatedHumanReadable =
        humanReadableService.generateMeasureHumanReadable(measure, madieMeasure.getId());
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains(hrText));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoMeasureResource() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> humanReadableService.generateMeasureHumanReadable(null, madieMeasure.getId()));
  }

  @Test
  public void generateHumanReadableThrowsFHIRException() {
    when(liquidEngine.parse(anyString(), anyString()))
        .thenReturn(new LiquidEngine.LiquidDocument());

    when(liquidEngine.evaluate(
            any(LiquidEngine.LiquidDocument.class),
            any(org.hl7.fhir.r5.model.Measure.class),
            any()))
        .thenThrow(new FHIRException());

    assertThrows(
        HumanReadableGenerationException.class,
        () -> humanReadableService.generateMeasureHumanReadable(measure, madieMeasure.getId()));
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
}

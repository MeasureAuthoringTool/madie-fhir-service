package gov.cms.madie.madiefhirservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Measure;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeasureBundleServiceTest implements ResourceFileUtil {
  @InjectMocks private MeasureBundleService measureBundleService;

  @Mock private MeasureTranslatorService measureTranslatorService;

  @Mock private LibraryTranslatorService libraryTranslatorService;

  @Mock private LibraryCqlVisitorFactory libCqlVisitorFactory;

  @Mock private HapiFhirServer hapiFhirServer;

  @Mock MethodOutcome methodOutcome;

  @Mock IIdType iidType;

  private Measure madieMeasure;
  private Library library;
  private org.hl7.fhir.r4.model.Measure measure;

  @BeforeEach
  public void setup() throws JsonProcessingException {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);

    String fhirMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/fhir_measure.json");
    measure =
        MeasureTestHelper.createFhirResourceFromJson(
            fhirMeasureJson, org.hl7.fhir.r4.model.Measure.class);

    String fhirLibraryJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/fhir_measure_library.json");
    library = MeasureTestHelper.createFhirResourceFromJson(fhirLibraryJson, Library.class);
  }

  @Test
  public void testCreateMeasureBundle() {
    when(measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure))
        .thenReturn(measure);
    when(hapiFhirServer.fetchHapiLibrary(anyString(), anyString()))
        .thenReturn(Optional.of((new Library())));
    when(libraryTranslatorService.convertToFhirLibrary(any(CqlLibrary.class))).thenReturn(library);
    var visitor = new LibraryCqlVisitorFactory().visit(madieMeasure.getCql());
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);

    Bundle bundle = measureBundleService.createMeasureBundle(madieMeasure);

    assertThat(bundle.getEntry().size(), is(3));
    assertThat(bundle.getType(), is(equalTo(Bundle.BundleType.TRANSACTION)));

    org.hl7.fhir.r4.model.Measure measureResource =
        (org.hl7.fhir.r4.model.Measure) bundle.getEntry().get(0).getResource();
    assertThat(madieMeasure.getCqlLibraryName(), is(equalTo(measureResource.getName())));
    assertThat(
        madieMeasure.getMeasureMetaData().getSteward(), is(equalTo(measureResource.getGuidance())));

    Library measureLibrary = (Library) bundle.getEntry().get(1).getResource();
    assertThat(measureLibrary.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertThat(measureLibrary.getContent(), is(notNullValue()));
    assertThat(measureLibrary.getContent().size(), is(equalTo(2)));
  }

  @Test
  public void testCreateMeasureBundleWhenIncludedLibraryNotFoundInHapi() {
    when(measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure))
        .thenReturn(measure);
    when(libraryTranslatorService.convertToFhirLibrary(any(CqlLibrary.class))).thenReturn(library);
    var visitor = new LibraryCqlVisitorFactory().visit(madieMeasure.getCql());
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);

    Exception exception =
        Assertions.assertThrows(
            HapiLibraryNotFoundException.class,
            () -> measureBundleService.createMeasureBundle(madieMeasure));

    assertThat(
        exception.getMessage(),
        is(equalTo("Cannot find a Hapi Fhir Library with name: FHIRHelpers, version: 4.0.001")));
  }

  @Test
  public void testSaveMeasure() {
    when(methodOutcome.getCreated()).thenReturn(true);
    when(methodOutcome.getId()).thenReturn(iidType);
    when(iidType.toString()).thenReturn("testId");
    when(hapiFhirServer.createResourceAsString(anyString())).thenReturn(methodOutcome);

    MethodOutcome outcome = measureBundleService.saveMeasureBundle("test");

    assertTrue(outcome.getCreated());
    assertEquals("testId", outcome.getId().toString());
  }
}

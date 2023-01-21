package gov.cms.madie.madiefhirservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Measure;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeasureBundleServiceTest implements ResourceFileUtil {
  @InjectMocks private MeasureBundleService measureBundleService;

  @Mock private MeasureTranslatorService measureTranslatorService;

  @Mock private LibraryTranslatorService libraryTranslatorService;

  @Mock private CqlElmTranslatorClient cqlElmTranslatorClient;

  @Mock private LibraryService libraryService;

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
    when(cqlElmTranslatorClient.getFormattedCql(anyString(), anyString()))
        .thenReturn(madieMeasure.getCql());
    when(measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure))
        .thenReturn(measure);
    when(libraryTranslatorService.convertToFhirLibrary(any(CqlLibrary.class))).thenReturn(library);

    doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              Map<String, Library> includedLibraries = (Map<String, Library>) args[1];
              includedLibraries.put("test", new Library());
              return null;
            })
        .when(libraryService)
        .getIncludedLibraries(anyString(), anyMap());

    Bundle bundle = measureBundleService.createMeasureBundle(madieMeasure, "testUser");

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
    when(cqlElmTranslatorClient.getFormattedCql(anyString(), anyString()))
        .thenReturn(madieMeasure.getCql());
    when(measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure))
        .thenReturn(measure);
    when(libraryTranslatorService.convertToFhirLibrary(any(CqlLibrary.class))).thenReturn(library);
    doThrow(new HapiLibraryNotFoundException("FHIRHelpers", "4.0.001"))
        .when(libraryService)
        .getIncludedLibraries(anyString(), anyMap());
    Exception exception =
        Assertions.assertThrows(
            HapiLibraryNotFoundException.class,
            () -> measureBundleService.createMeasureBundle(madieMeasure, "testUser"));

    assertThat(
        exception.getMessage(),
        is(equalTo("Cannot find a Hapi Fhir Library with name: FHIRHelpers, version: 4.0.001")));
  }
}

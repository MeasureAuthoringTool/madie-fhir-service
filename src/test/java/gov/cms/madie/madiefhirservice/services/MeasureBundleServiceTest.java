package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.dto.CqlLibraryDetails;
import gov.cms.madie.madiefhirservice.exceptions.CqlLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
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

import java.security.Principal;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeasureBundleServiceTest implements ResourceFileUtil {
  @InjectMocks private MeasureBundleService measureBundleService;

  @Mock private MeasureTranslatorService measureTranslatorService;
  @Mock private LibraryTranslatorService libraryTranslatorService;
  @Mock private LibraryService libraryService;
  @Mock private HumanReadableService humanReadableService;
  @Mock private ElmTranslatorClient elmTranslatorClient;
  @Mock MethodOutcome methodOutcome;
  @Mock IIdType iidType;

  private Measure madieMeasure;
  private Library library;
  private String humanReadable;
  private org.hl7.fhir.r4.model.Measure measure;
  private org.hl7.fhir.r5.model.Library effectiveDataRequirements;

  @BeforeEach
  public void setup() throws JsonProcessingException {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);

    String fhirMeasureJson = getStringFromTestResource("/measures/fhir_measure.json");
    measure =
        MeasureTestHelper.createFhirResourceFromJson(
            fhirMeasureJson, org.hl7.fhir.r4.model.Measure.class);

    String fhirLibraryJson = getStringFromTestResource("/measures/fhir_measure_library.json");
    library = MeasureTestHelper.createFhirResourceFromJson(fhirLibraryJson, Library.class);
    effectiveDataRequirements =
        convertToFhirR5Resource(
            org.hl7.fhir.r5.model.Library.class,
            getStringFromTestResource("/humanReadable/effective-data-requirements.json"));
    effectiveDataRequirements.setId("effective-data-requirements");
    humanReadable = getStringFromTestResource("/humanReadable/humanReadable_test");
  }

  @Test
  public void testCreateMeasureBundle() {
    when(measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure))
        .thenReturn(measure);

    when(libraryTranslatorService.convertToFhirLibrary(any(CqlLibrary.class))).thenReturn(library);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class), anyBoolean(), anyString()))
        .thenReturn(effectiveDataRequirements);
    doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              Map<String, Library> includedLibraries = (Map<String, Library>) args[1];
              includedLibraries.put("test", new Library());
              return null;
            })
        .when(libraryService)
        .getIncludedLibraries(anyString(), anyMap(), anyString(), anyString());

    Bundle bundle =
        measureBundleService.createMeasureBundle(
            madieMeasure,
            mock(Principal.class),
            BundleUtil.MEASURE_BUNDLE_TYPE_CALCULATION,
            "token");

    assertThat(bundle.getEntry().size(), is(3));
    assertThat(bundle.getType(), is(equalTo(Bundle.BundleType.TRANSACTION)));

    org.hl7.fhir.r4.model.Measure measureResource =
        (org.hl7.fhir.r4.model.Measure) bundle.getEntry().get(0).getResource();
    assertThat(madieMeasure.getCqlLibraryName(), is(equalTo(measureResource.getName())));
    assertThat(
        madieMeasure.getMeasureMetaData().getGuidance(),
        is(equalTo(measureResource.getGuidance())));

    Library measureLibrary = (Library) bundle.getEntry().get(1).getResource();
    assertThat(measureLibrary.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertThat(measureLibrary.getContent(), is(notNullValue()));
    assertThat(measureLibrary.getContent().size(), is(equalTo(2)));
    assertThat(
        measureLibrary.getPublisher(),
        is(equalTo(madieMeasure.getMeasureMetaData().getSteward().getName())));
    Bundle.BundleEntryRequestComponent measureEntryRequest = bundle.getEntry().get(0).getRequest();
    assertThat(
        measureEntryRequest.getUrl(), is(equalTo("Measure/" + madieMeasure.getCqlLibraryName())));
    assertThat(measureEntryRequest.getMethod(), is(equalTo(Bundle.HTTPVerb.POST)));

    Bundle.BundleEntryRequestComponent libraryEntryRequest = bundle.getEntry().get(1).getRequest();
    assertThat(
        libraryEntryRequest.getUrl(), is(equalTo("Library/" + madieMeasure.getCqlLibraryName())));
    assertThat(libraryEntryRequest.getMethod(), is(equalTo(Bundle.HTTPVerb.POST)));
  }

  @Test
  public void testCreateMeasureBundleWhenIncludedLibraryNotFound() {
    when(measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure))
        .thenReturn(measure);

    when(libraryTranslatorService.convertToFhirLibrary(any(CqlLibrary.class))).thenReturn(library);

    doThrow(new CqlLibraryNotFoundException("FHIRHelpers", "4.0.001"))
        .when(libraryService)
        .getIncludedLibraries(anyString(), any(), anyString(), anyString());
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class), anyBoolean(), anyString()))
        .thenReturn(effectiveDataRequirements);
    Exception exception =
        Assertions.assertThrows(
            CqlLibraryNotFoundException.class,
            () ->
                measureBundleService.createMeasureBundle(
                    madieMeasure,
                    mock(Principal.class),
                    BundleUtil.MEASURE_BUNDLE_TYPE_CALCULATION,
                    "token"));

    assertThat(
        exception.getMessage(),
        is(equalTo("Cannot find a CQL Library with name: FHIRHelpers, version: 4.0.001")));
  }

  @Test
  public void testCreateMeasureBundleForExport() {
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
        .getIncludedLibraries(anyString(), anyMap(), anyString(), anyString());

    when(elmTranslatorClient.getEffectiveDataRequirements(
            any(CqlLibraryDetails.class), anyBoolean(), anyString()))
        .thenReturn(effectiveDataRequirements);
    when(elmTranslatorClient.getModuleDefinitionLibrary(
            any(CqlLibraryDetails.class), anyBoolean(), anyString()))
        .thenReturn(effectiveDataRequirements);

    when(humanReadableService.generateMeasureHumanReadable(
            any(Measure.class), any(Bundle.class), any(org.hl7.fhir.r5.model.Library.class)))
        .thenReturn(humanReadable);
    when(humanReadableService.generateLibraryHumanReadable(
            any(org.hl7.fhir.r4.model.Library.class)))
        .thenReturn("<div>test narrative</div>");

    Bundle bundle =
        measureBundleService.createMeasureBundle(
            madieMeasure, mock(Principal.class), BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT, "token");

    assertThat(bundle.getEntry().size(), is(3));
    assertThat(bundle.getType(), is(equalTo(Bundle.BundleType.TRANSACTION)));

    org.hl7.fhir.r4.model.Measure measureResource =
        (org.hl7.fhir.r4.model.Measure) bundle.getEntry().get(0).getResource();
    assertThat(madieMeasure.getCqlLibraryName(), is(equalTo(measureResource.getName())));
    assertThat(
        madieMeasure.getMeasureMetaData().getGuidance(),
        is(equalTo(measureResource.getGuidance())));

    var r4Measure = (org.hl7.fhir.r4.model.Measure) bundle.getEntry().get(0).getResource();
    Library r4MeasureLibrary = (Library) bundle.getEntry().get(1).getResource();

    assertThat(r4MeasureLibrary.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    // assert effective DR
    assertThat(r4Measure.getContained().size(), is(equalTo(1)));
    assertThat(r4Measure.getContained().get(0).getId(), is(equalTo("effective-data-requirements")));

    // assert narrative text
    assertThat(r4Measure.getText().getStatus().getDisplay(), is(equalTo("Extensions")));
    assertThat(r4Measure.getText().getDivAsString(), is(notNullValue()));
    assertThat(
        r4Measure.getExtension().get(0).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL)));
    assertThat(r4MeasureLibrary.getText().getStatus().getDisplay(), is(equalTo("Extensions")));
    assertThat(
        r4MeasureLibrary.getText().getDivAsString(),
        is(equalTo("<div xmlns=\"http://www.w3.org/1999/xhtml\">test narrative</div>")));
    assertThat(
        r4MeasureLibrary.getPublisher(),
        is(equalTo(madieMeasure.getMeasureMetaData().getSteward().getName())));
  }
}

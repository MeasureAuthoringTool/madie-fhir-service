package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.madie.madiefhirservice.services.ExportService;
import gov.cms.madie.madiefhirservice.services.MeasureBundleService;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.Principal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MeasureBundleController.class})
public class MeasureBundleControllerMvcTest implements ResourceFileUtil {
  private static final String TEST_USER_ID = "john_doe";

  @MockBean private MeasureBundleService measureBundleService;

  @MockBean private ExportService exportService;

  @MockBean private FhirContext fhirContext;

  @Autowired private MockMvc mockMvc;

  @Mock MethodOutcome methodOutcome;
  @Mock IIdType iidType;

  @Test
  public void testGetMeasureBundle() throws Exception {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(
            any(Measure.class), any(Principal.class), anyString(), anyString()))
        .thenReturn(testBundle);
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/measures/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resourceType").value("Bundle"))
        .andExpect(jsonPath("$.entry[0].resource.resourceType").value("Measure"))
        .andExpect(jsonPath("$.entry[0].resource.name").value("TestCMS0001"))
        .andExpect(jsonPath("$.entry[0].resource.version").value("0.0.001"));
    verify(measureBundleService, times(1))
        .createMeasureBundle(any(Measure.class), any(Principal.class), anyString(), anyString());
  }

  @Test
  public void testGetMeasureBundleXml() throws Exception {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(
            any(Measure.class), any(Principal.class), anyString(), anyString()))
        .thenReturn(testBundle);
    when(fhirContext.newXmlParser()).thenReturn(FhirContext.forR4().newXmlParser());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/measures/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .accept(MediaType.APPLICATION_XML)
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_XML));
    verify(measureBundleService, times(1))
        .createMeasureBundle(any(Measure.class), any(Principal.class), anyString(), anyString());
  }

  @Test
  public void testExportMeasure() throws Exception {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/fhir/measures/export")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "test-okta")
                    .content(madieMeasureJson)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andReturn();

    assertThat(result.getResponse().getContentType(), is(equalTo("application/octet-stream")));
    assertThat(
        result.getResponse().getHeader("Content-Disposition"),
        is(equalTo("attachment;filename=\"title-v0.0.000-FHIR.zip\"")));
  }
}

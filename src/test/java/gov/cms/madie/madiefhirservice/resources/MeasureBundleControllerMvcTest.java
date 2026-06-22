package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.madie.madiefhirservice.clients.UserRoleConverter;
import gov.cms.madie.madiefhirservice.config.SecurityConfig;
import gov.cms.madie.madiefhirservice.services.ExportService;
import gov.cms.madie.madiefhirservice.clients.UserServiceClient;
import gov.cms.madie.madiefhirservice.services.MeasureBundleService;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.Principal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
@Import({SecurityConfig.class, UserRoleConverter.class})
public class MeasureBundleControllerMvcTest implements ResourceFileUtil {
  private static final String TEST_USER_ID = "john_doe";

  @MockitoBean private UserServiceClient userServiceClient;
  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private MeasureBundleService measureBundleService;

  @MockitoBean private ExportService exportService;

  @MockitoBean private FhirContext fhirContext;

  @Autowired private MockMvc mockMvc;

  @Mock MethodOutcome methodOutcome;
  @Mock IIdType iidType;

  @Test
  public void testGetMeasureBundle() throws Exception {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(
            any(Measure.class),
            any(Principal.class),
            anyString(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
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
        .createMeasureBundle(
            any(Measure.class),
            any(Principal.class),
            anyString(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info));
  }

  @Test
  public void testGetMeasureBundleXml() throws Exception {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(
            any(Measure.class),
            any(Principal.class),
            anyString(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
        .thenReturn(testBundle);
    when(fhirContext.newXmlParser()).thenReturn(FhirContext.forR4().newXmlParser());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/measures/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .queryParam(
                    "errorSeverity", String.valueOf(CqlCompilerException.ErrorSeverity.Info))
                .accept(MediaType.APPLICATION_XML)
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_XML));
    verify(measureBundleService, times(1))
        .createMeasureBundle(
            any(Measure.class),
            any(Principal.class),
            anyString(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info));
  }

  @Test
  public void testExportMeasure() throws Exception {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");

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
        is(equalTo("attachment;filename=\"title-v1.2.003-FHIR.zip\"")));
  }
}

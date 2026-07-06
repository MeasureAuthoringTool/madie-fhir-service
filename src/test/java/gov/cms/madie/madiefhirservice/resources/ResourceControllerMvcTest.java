package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.clients.UserServiceClient;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.ModelType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ResourceController.class})
public class ResourceControllerMvcTest implements ResourceFileUtil {
  private static final String TEST_USER_ID = "john_doe";

  @MockitoBean private UserServiceClient userServiceClient;
  @MockitoBean private StructureDefinitionService structureDefinitionService;
  @Autowired private MockMvc mockMvc;

  // --- Canonical path tests ---

  @Test
  void testCanonicalPathGetAllResourcesForQiCore6() throws Exception {
    when(structureDefinitionService.getAllResources(eq(ModelType.QI_CORE_6_0_0)))
        .thenReturn(
            List.of(
                ResourceIdentifier.builder().id("qicore-careplan").title("QICore CarePlan").build(),
                ResourceIdentifier.builder().id("qicore-device").title("QICore Device").build()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/fhir/models/qicore6/resources")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[0].id").value("qicore-careplan"))
        .andExpect(jsonPath("$.[0].title").value("QICore CarePlan"));

    verify(structureDefinitionService, times(1)).getAllResources(eq(ModelType.QI_CORE_6_0_0));
  }

  @Test
  void testCanonicalPathGetAllResourcesForUsCore6() throws Exception {
    when(structureDefinitionService.getAllResources(eq(ModelType.US_CORE_6_0_1)))
        .thenReturn(
            List.of(
                ResourceIdentifier.builder()
                    .id("us-core-patient")
                    .title("US Core Patient Profile")
                    .build()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/fhir/models/uscore6/resources")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[0].id").value("us-core-patient"));

    verify(structureDefinitionService, times(1)).getAllResources(eq(ModelType.US_CORE_6_0_1));
  }

  @Test
  void testCanonicalPathGetAllResourcesForUsQualityCore() throws Exception {
    when(structureDefinitionService.getAllResources(eq(ModelType.US_QUALITY_CORE_0_5_0)))
        .thenReturn(List.of());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/fhir/models/usqualitycore05/resources")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    verify(structureDefinitionService, times(1))
        .getAllResources(eq(ModelType.US_QUALITY_CORE_0_5_0));
  }

  @Test
  void testCanonicalPathReturns400ForUnknownModel() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/fhir/models/unknownmodel/resources")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testCanonicalPathGetStructureDefinitionForQiCore() throws Exception {
    StructureDefinitionDto dto =
        StructureDefinitionDto.builder()
            .definition(
                "{\n"
                    + "        \"resourceType\": \"StructureDefinition\",\n"
                    + "        \"id\": \"qicore-patient\",\n"
                    + "        \"title\": \"QICore Patient\",\n"
                    + "        \"kind\": \"resource\"\n"
                    + "}")
            .build();
    when(structureDefinitionService.getStructureDefinitionById(eq(ModelType.QI_CORE), anyString()))
        .thenReturn(dto);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                    "/fhir/models/qicore/resources/structure-definitions/qicore-patient")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.definition.id").value("qicore-patient"));

    verify(structureDefinitionService, times(1))
        .getStructureDefinitionById(eq(ModelType.QI_CORE), eq("qicore-patient"));
  }

  // --- Legacy alias tests (must continue to resolve to qicore6) ---

  @Test
  void testLegacyAliasGetAllResourcesQiCore600PathResolvesToQiCore6() throws Exception {
    when(structureDefinitionService.getAllResources(eq(ModelType.QI_CORE_6_0_0)))
        .thenReturn(
            List.of(
                ResourceIdentifier.builder().id("qicore-careplan").title("QICore CarePlan").build(),
                ResourceIdentifier.builder().id("qicore-device").title("QICore Device").build(),
                ResourceIdentifier.builder()
                    .id("qicore-practitioner")
                    .title("QICore Practitioner")
                    .build()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/6_0_0/resources")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[0].id").value("qicore-careplan"))
        .andExpect(jsonPath("$.[0].title").value("QICore CarePlan"));

    verify(structureDefinitionService, times(1)).getAllResources(eq(ModelType.QI_CORE_6_0_0));
  }

  @Test
  void testLegacyAliasGetAllResourcesQiCorePathResolvesToQiCore6() throws Exception {
    when(structureDefinitionService.getAllResources(eq(ModelType.QI_CORE_6_0_0)))
        .thenReturn(List.of());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/resources")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk());

    verify(structureDefinitionService, times(1)).getAllResources(eq(ModelType.QI_CORE_6_0_0));
  }

  @Test
  void testLegacyAliasGetStructureDefinitionReturns404NotFound() throws Exception {
    when(structureDefinitionService.getStructureDefinitionById(any(ModelType.class), anyString()))
        .thenThrow(new ResourceNotFoundException("StructureDefinition", "fake"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/6_0_0/resources/structure-definitions/qicore-fake")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isNotFound());

    verify(structureDefinitionService, times(1))
        .getStructureDefinitionById(eq(ModelType.QI_CORE_6_0_0), eq("qicore-fake"));
  }

  @Test
  void testLegacyAliasGetStructureDefinitionReturnsDefinitionDto() throws Exception {
    StructureDefinitionDto dto =
        StructureDefinitionDto.builder()
            .definition(
                "{\n"
                    + "        \"resourceType\": \"StructureDefinition\",\n"
                    + "        \"id\": \"qicore-patient\",\n"
                    + "        \"title\": \"QICore Patient\",\n"
                    + "        \"kind\": \"resource\"\n"
                    + "}")
            .build();
    when(structureDefinitionService.getStructureDefinitionById(any(ModelType.class), anyString()))
        .thenReturn(dto);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                    "/qicore/6_0_0/resources/structure-definitions/qicore-patient")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.definition.resourceType").value("StructureDefinition"))
        .andExpect(jsonPath("$.definition.id").value("qicore-patient"))
        .andExpect(jsonPath("$.definition.kind").value("resource"));

    verify(structureDefinitionService, times(1))
        .getStructureDefinitionById(eq(ModelType.QI_CORE_6_0_0), eq("qicore-patient"));
  }

  @Test
  void testLegacyAliasGetValueSetDefinition() throws Exception {
    String url = "test";
    String valueSetDefinition =
        "{\"resourceType\": \"ValueSet\", \"id\": \"omb-ethnicity-category\",\"url\": \"http://hl7.org/fhir/us/core/ValueSet/omb-ethnicity-category\"}";
    when(structureDefinitionService.getValueSetDefinition(any(ModelType.class), anyString()))
        .thenReturn(valueSetDefinition);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                        "/qicore/6_0_0/resources/value-set-definition?url=" + url)
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "test-okta"))
            .andExpect(status().isOk())
            .andReturn();

    verify(structureDefinitionService, times(1))
        .getValueSetDefinition(eq(ModelType.QI_CORE_6_0_0), eq(url));
    Assertions.assertThat(result.getResponse().getContentAsString()).isEqualTo(valueSetDefinition);
  }

  @Test
  void testLegacyAliasGetExtensionsForTargetPathReturnsSuccessfully() throws Exception {
    List<StructureDefinitionDto> extensions =
        List.of(
            StructureDefinitionDto.builder()
                .definition(
                    "{\"resourceType\": \"StructureDefinition\", \"id\": \"ext-1\", \"type\": \"Extension\"}")
                .build(),
            StructureDefinitionDto.builder()
                .definition(
                    "{\"resourceType\": \"StructureDefinition\", \"id\": \"ext-2\", \"type\": \"Extension\"}")
                .build());
    when(structureDefinitionService.getExtensionsForTargetPath(
            any(ModelType.class), anyString(), anyString()))
        .thenReturn(extensions);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/resources/extensions")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .param("targetPath", "Observation.code")
                .param("kind", "Element")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].definition.id").value("ext-1"))
        .andExpect(jsonPath("$[1].definition.id").value("ext-2"));

    verify(structureDefinitionService, times(1))
        .getExtensionsForTargetPath(eq(ModelType.QI_CORE_6_0_0), anyString(), anyString());
  }

  @Test
  void testLegacyAliasGetExtensionsForTargetPathReturnsEmptyList() throws Exception {
    when(structureDefinitionService.getExtensionsForTargetPath(
            any(ModelType.class), anyString(), anyString()))
        .thenReturn(List.of());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/resources/extensions")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .param("targetPath", "InvalidPath")
                .param("kind", "Invalid")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    verify(structureDefinitionService, times(1))
        .getExtensionsForTargetPath(eq(ModelType.QI_CORE_6_0_0), anyString(), anyString());
  }

  @Test
  void testGetExtensionsForTargetPathWithMissingParameters() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/resources/extensions")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest());

    verify(structureDefinitionService, times(0))
        .getExtensionsForTargetPath(any(ModelType.class), anyString(), anyString());
  }
}

package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
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

  @MockitoBean private StructureDefinitionService structureDefinitionService;
  @Autowired private MockMvc mockMvc;

  @Test
  void testThatGetAllResourcesReturnsListOfResourceIdentifiers() throws Exception {
    // given
    when(structureDefinitionService.getAllResources())
        .thenReturn(
            List.of(
                ResourceIdentifier.builder().id("qicore-careplan").title("QICore CarePlan").build(),
                ResourceIdentifier.builder().id("qicore-device").title("QICore Device").build(),
                ResourceIdentifier.builder()
                    .id("qicore-practitioner")
                    .title("QICore Practitioner")
                    .build()));

    // when
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/6_0_0/resources")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[0].id").value("qicore-careplan"))
        .andExpect(jsonPath("$.[0].title").value("QICore CarePlan"));

    // then
    verify(structureDefinitionService, times(1)).getAllResources();
  }

  @Test
  void testThatGetStructureDefinitionReturns404NotFound() throws Exception {
    // given
    when(structureDefinitionService.getStructureDefinitionById(anyString()))
        .thenThrow(new ResourceNotFoundException("StructureDefinition", "fake"));

    // when
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/qicore/6_0_0/resources/structure-definitions/qicore-fake")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isNotFound());

    // then
    verify(structureDefinitionService, times(1)).getStructureDefinitionById(eq("qicore-fake"));
  }

  @Test
  void testThatGetStructureDefinitionReturnsDefinitionDto() throws Exception {
    // given
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
    when(structureDefinitionService.getStructureDefinitionById(anyString())).thenReturn(dto);

    // when
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

    // then
    verify(structureDefinitionService, times(1)).getStructureDefinitionById(eq("qicore-patient"));
  }

  @Test
  void testGetValueSetDefinition() throws Exception {
    // given
    String url = "test";
    String valueSetDefinition =
        "{\"resourceType\": \"ValueSet\", \"id\": \"omb-ethnicity-category\",\"url\": \"http://hl7.org/fhir/us/core/ValueSet/omb-ethnicity-category\"}";
    ;
    when(structureDefinitionService.getValueSetDefinition(anyString()))
        .thenReturn(valueSetDefinition);

    // when
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

    // then
    verify(structureDefinitionService, times(1)).getValueSetDefinition(eq(url));
    Assertions.assertThat(result.getResponse().getContentAsString()).isEqualTo(valueSetDefinition);
  }

  @Test
  void testGetExtensionsForTargetPathReturnsSuccessfully() throws Exception {
    // given
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
    when(structureDefinitionService.getExtensionsForTargetPath("Observation.code", "Element"))
        .thenReturn(extensions);

    // when
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

    // then
    verify(structureDefinitionService, times(1))
        .getExtensionsForTargetPath(anyString(), anyString());
  }

  @Test
  void testGetExtensionsForTargetPathReturnsEmptyList() throws Exception {
    // given
    when(structureDefinitionService.getExtensionsForTargetPath("InvalidPath", "Invalid"))
        .thenReturn(List.of());

    // when
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

    // then
    verify(structureDefinitionService, times(1))
        .getExtensionsForTargetPath(anyString(), anyString());
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

    // then
    verify(structureDefinitionService, times(0))
        .getExtensionsForTargetPath(anyString(), anyString());
  }
}

package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.BuilderResourceMetadata;
import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.UnsupportedTypeException;
import gov.cms.madie.madiefhirservice.clients.UserRoleConverter;
import gov.cms.madie.madiefhirservice.clients.UserServiceClient;
import gov.cms.madie.madiefhirservice.config.SecurityConfig;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.madiefhirservice.utils.ModelTypeResolver;
import gov.cms.madie.models.common.ModelType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ResourceController.class})
@Import({SecurityConfig.class, UserRoleConverter.class})
public class ResourceControllerMvcTest implements ResourceFileUtil {
  private static final String TEST_USER_ID = "john_doe";

  @MockitoBean private UserServiceClient userServiceClient;
  @MockitoBean private StructureDefinitionService structureDefinitionService;
  @MockitoBean private JwtDecoder jwtDecoder;
  @Autowired private MockMvc mockMvc;
  private MockedStatic<ModelTypeResolver> modelTypeResolverMock;

  @BeforeEach
  void setUpModelTypeResolver() {
    modelTypeResolverMock = mockStatic(ModelTypeResolver.class);
    modelTypeResolverMock
        .when(() -> ModelTypeResolver.resolve("qicore6"))
        .thenReturn(ModelType.QI_CORE_6_0_0);
    modelTypeResolverMock
        .when(() -> ModelTypeResolver.resolve("qicore"))
        .thenReturn(ModelType.QI_CORE);
    modelTypeResolverMock
        .when(() -> ModelTypeResolver.resolve("uscore6"))
        .thenReturn(ModelType.US_CORE_6_1_0);
    modelTypeResolverMock
        .when(() -> ModelTypeResolver.resolve("usqualitycore05"))
        .thenReturn(ModelType.US_QUALITY_CORE_0_5_0);
    modelTypeResolverMock
        .when(() -> ModelTypeResolver.resolve(null))
        .thenReturn(ModelType.QI_CORE_6_0_0);
    modelTypeResolverMock
        .when(() -> ModelTypeResolver.resolve("unknownmodel"))
        .thenThrow(new UnsupportedTypeException(ModelTypeResolver.class.getName(), "unknownmodel"));
  }

  @AfterEach
  void tearDownModelTypeResolver() {
    modelTypeResolverMock.close();
  }

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
  void testCanonicalPathGetBuilderResourceMetadataForUsQualityCore() throws Exception {
    when(structureDefinitionService.getBuilderResourceMetadata(eq(ModelType.US_QUALITY_CORE_0_5_0)))
        .thenReturn(
            BuilderResourceMetadata.builder()
                .resourcePaths(List.of("/guides/onc/us-quality-core", "/us/core"))
                .primaryPatientProfile(
                    ResourceIdentifier.builder()
                        .id("us-quality-core-patient")
                        .title("US Quality Core Patient")
                        .build())
                .build());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/fhir/models/usqualitycore05/resources/builder-metadata")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resourcePaths[0]").value("/guides/onc/us-quality-core"))
        .andExpect(jsonPath("$.resourcePaths[1]").value("/us/core"))
        .andExpect(jsonPath("$.primaryPatientProfile.id").value("us-quality-core-patient"));

    verify(structureDefinitionService, times(1))
        .getBuilderResourceMetadata(eq(ModelType.US_QUALITY_CORE_0_5_0));
  }

  @Test
  void testCanonicalPathGetAllResourcesForUsCore6() throws Exception {
    when(structureDefinitionService.getAllResources(eq(ModelType.US_CORE_6_1_0)))
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

    verify(structureDefinitionService, times(1)).getAllResources(eq(ModelType.US_CORE_6_1_0));
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

    verifyNoInteractions(structureDefinitionService);
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

  @Test
  void testGetExtensionsForTargetPathWithMissingParameters() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/fhir/models/qicore6/resources/extensions")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest());

    verify(structureDefinitionService, times(0))
        .getExtensionsForTargetPath(any(ModelType.class), anyString(), anyString());
  }
}

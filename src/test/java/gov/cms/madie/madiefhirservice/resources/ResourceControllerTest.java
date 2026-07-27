package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import gov.cms.madie.madiefhirservice.utils.ModelTypeResolver;
import gov.cms.madie.models.common.ModelType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

  @Mock private StructureDefinitionService structureDefinitionService;
  @InjectMocks private ResourceController resourceController;
  private MockedStatic<ModelTypeResolver> modelTypeResolverMock;

  @BeforeEach
  void setUp() {
    modelTypeResolverMock = mockStatic(ModelTypeResolver.class);
    modelTypeResolverMock
        .when(() -> ModelTypeResolver.resolve(null))
        .thenReturn(ModelType.QI_CORE_6_0_0);
  }

  @AfterEach
  void tearDown() {
    modelTypeResolverMock.close();
  }

  @Test
  void testThatGetAllResourcesReturnsListOfResourceIdentifiers() {
    // given
    when(structureDefinitionService.getAllResources(any(ModelType.class)))
        .thenReturn(
            List.of(
                ResourceIdentifier.builder().id("qicore-careplan").title("QICore CarePlan").build(),
                ResourceIdentifier.builder().id("qicore-device").title("QICore Device").build(),
                ResourceIdentifier.builder()
                    .id("qicore-practitioner")
                    .title("QICore Practitioner")
                    .build()));

    // when
    List<ResourceIdentifier> output = resourceController.getAllResources(null);

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(3)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getId(), is(equalTo("qicore-careplan")));
    assertThat(output.get(1).getId(), is(equalTo("qicore-device")));
    assertThat(output.get(2).getId(), is(equalTo("qicore-practitioner")));
  }

  @Test
  void testThatGetStructureDefinitionThrowsNotFound() {
    // given
    when(structureDefinitionService.getStructureDefinitionById(any(ModelType.class), anyString()))
        .thenThrow(new ResourceNotFoundException("StructureDefinition", "fake"));

    // when / then
    assertThrows(
        ResourceNotFoundException.class,
        () -> resourceController.getStructureDefinition(null, "fake"));
  }

  @Test
  void testThatGetStructureDefinitionReturnsDefinitionDto() {
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
    when(structureDefinitionService.getStructureDefinitionById(any(ModelType.class), anyString()))
        .thenReturn(dto);

    // when
    StructureDefinitionDto output =
        resourceController.getStructureDefinition(null, "qicore-patient");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getDefinition(), is(notNullValue()));
    assertThat(output.getDefinition().contains("\"id\": \"qicore-patient\""), is(true));
  }

  @Test
  void testGetValueSetDefinition() {
    // given
    String valueSetDefinition =
        "{\"resourceType\": \"ValueSet\", \"id\": \"omb-ethnicity-category\",\"url\": \"http://hl7.org/fhir/us/core/ValueSet/omb-ethnicity-category\"}";
    when(structureDefinitionService.getValueSetDefinition(any(ModelType.class), anyString()))
        .thenReturn(valueSetDefinition);

    // when
    String output = resourceController.getValueSetDefinition(null, "url");

    // then
    assertThat(output, is(equalTo(valueSetDefinition)));
  }

  @Test
  void testGetExtensionsForTargetPathReturnsListOfExtensions() {
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
    when(structureDefinitionService.getExtensionsForTargetPath(
            any(ModelType.class), anyString(), anyString()))
        .thenReturn(extensions);

    // when
    List<StructureDefinitionDto> output =
        resourceController.getExtensionsForTargetPath(null, "Observation.code", "Element");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(2)));
    assertThat(output.get(0).getDefinition(), is(notNullValue()));
    assertThat(output.get(0).getDefinition().contains("\"id\": \"ext-1\""), is(true));
    assertThat(output.get(1).getDefinition().contains("\"id\": \"ext-2\""), is(true));
  }

  @Test
  void testGetExtensionsForTargetPathReturnsEmptyList() {
    // given
    when(structureDefinitionService.getExtensionsForTargetPath(
            any(ModelType.class), anyString(), anyString()))
        .thenReturn(List.of());

    // when
    List<StructureDefinitionDto> output =
        resourceController.getExtensionsForTargetPath(null, "InvalidPath", "Invalid");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.isEmpty(), is(true));
  }
}

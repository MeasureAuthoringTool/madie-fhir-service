package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

  @Mock private StructureDefinitionService structureDefinitionService;
  @InjectMocks private ResourceController resourceController;

  @Test
  void testThatGetAllResourcesReturnsListOfResourceIdentifiers() {
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
    List<ResourceIdentifier> output = resourceController.getAllResources();

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
    when(structureDefinitionService.getStructureDefinitionById(anyString()))
        .thenThrow(new ResourceNotFoundException("StructureDefinition", "fake"));

    // when / then
    assertThrows(
        ResourceNotFoundException.class, () -> resourceController.getStructureDefinition("fake"));
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
    when(structureDefinitionService.getStructureDefinitionById(anyString())).thenReturn(dto);

    // when
    StructureDefinitionDto output = resourceController.getStructureDefinition("qicore-patient");

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
    when(structureDefinitionService.getValueSetDefinition(anyString()))
        .thenReturn(valueSetDefinition);

    // when
    String output = resourceController.getValueSetDefinition("url");

    // then
    assertThat(output, is(equalTo(valueSetDefinition)));
  }
}

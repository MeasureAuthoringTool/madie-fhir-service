package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructureDefinitionServiceTest {

  @Spy private FhirContext fhirContextQiCoreStu600;
  @Mock private IValidationSupport validationSupportChainQiCore600;

  @InjectMocks private StructureDefinitionService structureDefinitionService;

  @Test
  void testGetStructureDefinitionByIdThrowsNotFoundForNoDefinitions() {
    // given
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions()).thenReturn(List.of());

    // when / then
    assertThrows(
        ResourceNotFoundException.class,
        () -> structureDefinitionService.getStructureDefinitionById("qicore-practitioner"));
  }

  @Test
  void testGetStructureDefinitionByIdThrowsNotFoundForNoMatchingDefinitions() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def3));

    // when / then
    assertThrows(
        ResourceNotFoundException.class,
        () -> structureDefinitionService.getStructureDefinitionById("qicore-practitioner"));
  }

  @Test
  void testGetStructureDefinitionByIdReturnsQiCoreResourceStructureDefinitionDto() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setId("qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3));
    when(validationSupportChainQiCore600.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    StructureDefinitionDto output =
        structureDefinitionService.getStructureDefinitionById("qicore-patient");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getDefinition(), is(notNullValue()));
    assertThat(output.getDefinition().contains("\"id\": \"qicore-patient\""), is(true));
    assertThat(output.getDefinition().contains("\"kind\": \"resource\""), is(true));
    assertThat(
        output.getDefinition().contains("\"resourceType\": \"StructureDefinition\""), is(true));
  }

  @Test
  void testGetStructureDefinitionByIdReturnsComplexTypeStructureDefinitionDto() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setId("qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3));
    when(validationSupportChainQiCore600.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    StructureDefinitionDto output =
        structureDefinitionService.getStructureDefinitionById("qicore-keyelement");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getDefinition(), is(notNullValue()));
    assertThat(output.getDefinition().contains("\"id\": \"qicore-keyelement\""), is(true));
    assertThat(output.getDefinition().contains("\"kind\": \"complex-type\""), is(true));
    assertThat(
        output.getDefinition().contains("\"resourceType\": \"StructureDefinition\""), is(true));
  }

  @Test
  void testGetAllResourcesReturnsOnlyQiCoreResources() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setType("Patient");
    def1.setId("qicore-patient");
    def1.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setType("Extension");
    def2.setId("qicore-keyelement");
    def2.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setType("Practitioner");
    def3.setId("us-core-practitioner");
    def3.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner");
    StructureDefinition def4 = new StructureDefinition();
    def4.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def4.setTitle(null);
    def4.setType("Patient");
    def4.setId("Patient");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    def4.setExtension(
        List.of(
            new Extension(
                UriConstants.FhirStructureDefinitions.CATEGORY_URI,
                new StringType("Base.Individuals"))));
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3, def4));

    // when
    List<ResourceIdentifier> output = structureDefinitionService.getAllResources();

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(1)));
    assertThat(output.get(0).getId(), is(equalTo("qicore-patient")));
    assertThat(output.get(0).getTitle(), is(equalTo("QICore Patient")));
    assertThat(output.get(0).getType(), is(equalTo("Patient")));
    assertThat(output.get(0).getCategory(), is(equalTo("Base.Individuals")));
    assertThat(
        output.get(0).getProfile(),
        is(equalTo("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient")));
  }

  @Test
  void testGetCategoryByTypeHandlesUnknownType() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setType("Patient");
    def1.setId("qicore-patient");
    def1.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setType("Extension");
    def2.setId("qicore-keyelement");
    def2.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setType("Practitioner");
    def3.setId("us-core-practitioner");
    def3.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner");
    StructureDefinition def4 = new StructureDefinition();
    def4.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def4.setTitle(null);
    def4.setType("Patient");
    def4.setId("Patient");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    def4.setExtension(
        List.of(
            new Extension(
                UriConstants.FhirStructureDefinitions.CATEGORY_URI,
                new StringType("Base.Individuals"))));
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3, def4));

    // when
    String output = structureDefinitionService.getCategoryByType("BLAHBLAH");

    // then
    assertThat(output, is(nullValue()));
  }

  @Test
  void testGetCategoryByTypeHandlesTypeWithNoExtensions() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setType("Patient");
    def1.setId("qicore-patient");
    def1.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setType("Extension");
    def2.setId("qicore-keyelement");
    def2.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setType("Practitioner");
    def3.setId("us-core-practitioner");
    def3.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner");
    StructureDefinition def4 = new StructureDefinition();
    def4.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def4.setTitle(null);
    def4.setType("Patient");
    def4.setId("Patient");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3, def4));

    // when
    String output = structureDefinitionService.getCategoryByType("Patient");

    // then
    assertThat(output, is(nullValue()));
  }

  @Test
  void testGetCategoryByTypeHandlesTypeWithNoCategoryExtension() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setType("Patient");
    def1.setId("qicore-patient");
    def1.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setType("Extension");
    def2.setId("qicore-keyelement");
    def2.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setType("Practitioner");
    def3.setId("us-core-practitioner");
    def3.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner");
    StructureDefinition def4 = new StructureDefinition();
    def4.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def4.setTitle(null);
    def4.setType("Patient");
    def4.setId("Patient");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    def4.setExtension(List.of(new Extension("RANDOM.URL", new StringType("NOT_A_CATEGORY"))));
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3, def4));

    // when
    String output = structureDefinitionService.getCategoryByType("Patient");

    // then
    assertThat(output, is(nullValue()));
  }

  @Test
  void testGetCategoryByTypeReturnsCategoryFromExtension() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setType("Patient");
    def1.setId("qicore-patient");
    def1.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setType("Extension");
    def2.setId("qicore-keyelement");
    def2.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setType("Practitioner");
    def3.setId("us-core-practitioner");
    def3.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner");
    StructureDefinition def4 = new StructureDefinition();
    def4.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def4.setTitle(null);
    def4.setType("Patient");
    def4.setId("Patient");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    def4.setExtension(
        List.of(
            new Extension(
                UriConstants.FhirStructureDefinitions.CATEGORY_URI,
                new StringType("Base.Individuals"))));
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3, def4));

    // when
    String output = structureDefinitionService.getCategoryByType("Patient");

    // then
    assertThat(output, is(equalTo("Base.Individuals")));
  }
}

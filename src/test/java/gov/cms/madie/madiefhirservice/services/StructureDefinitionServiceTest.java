package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.factories.ModelAwareFhirFactory;
import gov.cms.madie.models.common.ModelType;

import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructureDefinitionServiceTest {

  @Spy private FhirContext fhirContextQiCoreStu600;
  @Mock private IValidationSupport mockChain;
  @Mock private ModelAwareFhirFactory modelAwareFhirFactory;

  @InjectMocks private StructureDefinitionService structureDefinitionService;

  @BeforeEach
  void setUp() {
    lenient()
        .when(modelAwareFhirFactory.getValidationSupportForModel(any(ModelType.class)))
        .thenReturn(mockChain);
  }

  @Test
  void testGetStructureDefinitionByIdThrowsNotFoundForNoDefinitions() {
    // given
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of());

    // when / then
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            structureDefinitionService.getStructureDefinitionById(
                ModelType.QI_CORE_6_0_0, "qicore-practitioner"));
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
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def3));

    // when / then
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            structureDefinitionService.getStructureDefinitionById(
                ModelType.QI_CORE_6_0_0, "qicore-practitioner"));
  }

  @Test
  void testGetExtensionsForTargetPath() {
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    StructureDefinition def2 = new StructureDefinition();
    def2.setType("Extension");
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setId("qicore-keyelement");
    def2.setStatus(PublicationStatus.ACTIVE);
    def2.setExperimental(false);

    StructureDefinition.StructureDefinitionContextComponent context =
        new StructureDefinition.StructureDefinitionContextComponent();
    context.setExpressionElement(new StringType("Element"));

    context.setType(StructureDefinition.ExtensionContextType.ELEMENT);
    List<StructureDefinition.StructureDefinitionContextComponent> contexts =
        new ArrayList<StructureDefinition.StructureDefinitionContextComponent>() {
          {
            this.add(context);
          }
        };
    def2.setContext(contexts);

    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def2, def3));
    when(mockChain.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    List<StructureDefinitionDto> extension =
        structureDefinitionService.getExtensionsForTargetPath(
            ModelType.QI_CORE_6_0_0, "test", "element");
    assertNotNull(extension);
    assertEquals(1, extension.size());
  }

  @Test
  void testGetExtensionsForTargetPathExperiementalStatus() {
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    StructureDefinition def2 = new StructureDefinition();
    def2.setType("Extension");
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setId("qicore-keyelement");
    def2.setStatus(PublicationStatus.ACTIVE);
    def2.setExperimental(true);

    StructureDefinition.StructureDefinitionContextComponent context =
        new StructureDefinition.StructureDefinitionContextComponent();
    context.setExpressionElement(new StringType("Element"));

    context.setType(StructureDefinition.ExtensionContextType.ELEMENT);
    List<StructureDefinition.StructureDefinitionContextComponent> contexts =
        new ArrayList<StructureDefinition.StructureDefinitionContextComponent>() {
          {
            this.add(context);
          }
        };
    def2.setContext(contexts);

    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def2, def3));
    when(mockChain.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    List<StructureDefinitionDto> extension =
        structureDefinitionService.getExtensionsForTargetPath(
            ModelType.QI_CORE_6_0_0, "test", "element");
    assertNotNull(extension);
    assertEquals(0, extension.size());
  }

  @Test
  void testGetExtensionsForTargetPathNotValidStatus() {
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");

    StructureDefinition def2 = new StructureDefinition();
    def2.setType("Extension");
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setId("qicore-keyelement");
    def2.setStatus(PublicationStatus.RETIRED);
    def2.setExperimental(true);

    StructureDefinition.StructureDefinitionContextComponent context =
        new StructureDefinition.StructureDefinitionContextComponent();
    context.setExpressionElement(new StringType("Element"));

    context.setType(StructureDefinition.ExtensionContextType.ELEMENT);
    List<StructureDefinition.StructureDefinitionContextComponent> contexts =
        new ArrayList<StructureDefinition.StructureDefinitionContextComponent>() {
          {
            this.add(context);
          }
        };
    def2.setContext(contexts);

    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def2, def3));
    when(mockChain.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    List<StructureDefinitionDto> extension =
        structureDefinitionService.getExtensionsForTargetPath(
            ModelType.QI_CORE_6_0_0, "test", "element");
    assertNotNull(extension);
    assertEquals(0, extension.size());
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
    def2.setStatus(PublicationStatus.ACTIVE);
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def2, def3));
    when(mockChain.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    StructureDefinitionDto output =
        structureDefinitionService.getStructureDefinitionById(
            ModelType.QI_CORE_6_0_0, "qicore-patient");

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
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def2, def3));
    when(mockChain.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    StructureDefinitionDto output =
        structureDefinitionService.getStructureDefinitionById(
            ModelType.QI_CORE_6_0_0, "qicore-keyelement");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getDefinition(), is(notNullValue()));
    assertThat(output.getDefinition().contains("\"id\": \"qicore-keyelement\""), is(true));
    assertThat(output.getDefinition().contains("\"kind\": \"complex-type\""), is(true));
    assertThat(
        output.getDefinition().contains("\"resourceType\": \"StructureDefinition\""), is(true));
  }

  @Test
  void testGetAllResourcesReturnsAllResourcesWithTitleOrIdPart() {
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
    def4.setTitle(null); // Should fallback to id
    def4.setType("Patient");
    def4.setId("patient-null-title");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/patient-null-title");
    StructureDefinition def5 = new StructureDefinition();
    def5.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def5.setTitle(""); // Should fallback to id
    def5.setType("Patient");
    def5.setId("patient-empty-title");
    def5.setUrl("http://hl7.org/fhir/StructureDefinition/patient-empty-title");
    StructureDefinition def6 = new StructureDefinition();
    def6.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def6.setTitle("Valid Resource");
    def6.setType("Observation");
    def6.setId(""); // Should be included because title is present
    def6.setUrl("http://hl7.org/fhir/StructureDefinition/observation-empty-id");
    StructureDefinition def7 = new StructureDefinition();
    def7.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def7.setTitle("Valid Resource");
    def7.setType("Observation");
    def7.setId((String) null); // Should be included because title is present
    def7.setUrl("http://hl7.org/fhir/StructureDefinition/observation-null-id");
    StructureDefinition def8 = new StructureDefinition();
    def8.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def8.setTitle(null); // Should be excluded (no title, no id)
    def8.setType("Observation");
    def8.setId((String) null);
    def8.setUrl("http://hl7.org/fhir/StructureDefinition/observation-null-id2");
    StructureDefinition def9 = new StructureDefinition();
    def9.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def9.setTitle(""); // Should be excluded (no title, no id)
    def9.setType("Observation");
    def9.setId("");
    def9.setUrl("http://hl7.org/fhir/StructureDefinition/observation-empty-id2");
    StructureDefinition def10 = new StructureDefinition();
    def10.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def10.setTitle(null); // Should fallback to id
    def10.setType("Observation");
    def10.setId("valid-observation"); // Should be included
    def10.setUrl("http://hl7.org/fhir/StructureDefinition/valid-observation");
    when(mockChain.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3, def4, def5, def6, def7, def8, def9, def10));

    // when
    List<ResourceIdentifier> output =
        structureDefinitionService.getAllResources(ModelType.QI_CORE_6_0_0);

    // then
    assertThat(output, is(notNullValue()));
    // Only resources with either title or idPart should be included (def1, def3, def4, def5, def6,
    // def7, def10)
    assertThat(output.size(), is(equalTo(7)));
    assertThat(
        output.stream()
            .anyMatch(
                r -> "qicore-patient".equals(r.getId()) && "QICore Patient".equals(r.getTitle())),
        is(true));
    assertThat(
        output.stream()
            .anyMatch(
                r ->
                    "us-core-practitioner".equals(r.getId())
                        && "US Core Practitioner Profile".equals(r.getTitle())),
        is(true));
    assertThat(
        output.stream()
            .anyMatch(
                r ->
                    "patient-null-title".equals(r.getId())
                        && "patient-null-title".equals(r.getTitle())),
        is(true));
    assertThat(
        output.stream()
            .anyMatch(
                r ->
                    "patient-empty-title".equals(r.getId())
                        && "patient-empty-title".equals(r.getTitle())),
        is(true));
    assertThat(
        output.stream().anyMatch(r -> r.getId() == null && "Valid Resource".equals(r.getTitle())),
        is(true));
    assertThat(
        output.stream()
            .anyMatch(
                r ->
                    "valid-observation".equals(r.getId())
                        && "valid-observation".equals(r.getTitle())),
        is(true));
    assertThat(
        output.stream().anyMatch(r -> r.getId() == null && "Valid Resource".equals(r.getTitle())),
        is(true));
  }

  @Test
  void testGetAllResourcesExcludesNonResourceKind() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def1.setTitle("QI-Core Key Element Extension");
    def1.setType("Extension");
    def1.setId("qicore-keyelement");
    def1.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def2.setTitle("QICore Patient");
    def2.setType("Patient");
    def2.setId("qicore-patient");
    def2.setUrl("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient");
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def2));

    // when
    List<ResourceIdentifier> output =
        structureDefinitionService.getAllResources(ModelType.QI_CORE_6_0_0);

    // then
    assertThat(output.size(), is(equalTo(1)));
    assertThat(output.get(0).getId(), is(equalTo("qicore-patient")));
  }

  @Test
  void testGetCategoryByTypeHandlesUnknownType() {
    // given
    StructureDefinition def1 = buildDef("qicore-patient", "Patient", "QICore Patient");
    StructureDefinition def2 =
        buildDef("us-core-practitioner", "Practitioner", "US Core Practitioner Profile");
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
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def2, def4));

    // when
    String output = structureDefinitionService.getCategoryByType(mockChain, "BLAHBLAH");

    // then
    assertThat(output, is(nullValue()));
  }

  @Test
  void testGetCategoryByTypeHandlesTypeWithNoExtensions() {
    // given
    StructureDefinition def1 = buildDef("qicore-patient", "Patient", "QICore Patient");
    StructureDefinition def4 = new StructureDefinition();
    def4.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def4.setTitle(null);
    def4.setType("Patient");
    def4.setId("Patient");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def4));

    // when
    String output = structureDefinitionService.getCategoryByType(mockChain, "Patient");

    // then
    assertThat(output, is(nullValue()));
  }

  @Test
  void testGetCategoryByTypeHandlesTypeWithNoCategoryExtension() {
    // given
    StructureDefinition def1 = buildDef("qicore-patient", "Patient", "QICore Patient");
    StructureDefinition def4 = new StructureDefinition();
    def4.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def4.setTitle(null);
    def4.setType("Patient");
    def4.setId("Patient");
    def4.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    def4.setExtension(List.of(new Extension("RANDOM.URL", new StringType("NOT_A_CATEGORY"))));
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def4));

    // when
    String output = structureDefinitionService.getCategoryByType(mockChain, "Patient");

    // then
    assertThat(output, is(nullValue()));
  }

  @Test
  void testGetCategoryByTypeReturnsCategoryFromExtension() {
    // given
    StructureDefinition def1 = buildDef("qicore-patient", "Patient", "QICore Patient");
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
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1, def4));

    // when
    String output = structureDefinitionService.getCategoryByType(mockChain, "Patient");

    // then
    assertThat(output, is(equalTo("Base.Individuals")));
  }

  @Test
  void getValueSetDefinition() {
    // given
    ValueSet valueSet = new ValueSet().setUrl("test").setName("omb ethnicity category");
    when(mockChain.fetchValueSet(valueSet.getUrl())).thenReturn(valueSet);
    when(mockChain.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    String output =
        structureDefinitionService.getValueSetDefinition(
            ModelType.QI_CORE_6_0_0, valueSet.getUrl());

    // then
    assertThat(
        output,
        is(
            equalTo(
                "{\n  \"resourceType\": \"ValueSet\",\n  \"url\": \"test\",\n  \"name\": \"omb ethnicity category\"\n}")));
  }

  @Test
  void testGetStructureDefinitionByIdWorksForUsCoreModel() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("US Core Patient Profile");
    def1.setId("us-core-patient");
    when(mockChain.fetchAllStructureDefinitions()).thenReturn(List.of(def1));
    when(mockChain.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    StructureDefinitionDto output =
        structureDefinitionService.getStructureDefinitionById(
            ModelType.US_CORE_6_0_1, "us-core-patient");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getDefinition().contains("\"id\": \"us-core-patient\""), is(true));
  }

  private StructureDefinition buildDef(String id, String type, String title) {
    StructureDefinition def = new StructureDefinition();
    def.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def.setTitle(title);
    def.setType(type);
    def.setId(id);
    return def;
  }
}

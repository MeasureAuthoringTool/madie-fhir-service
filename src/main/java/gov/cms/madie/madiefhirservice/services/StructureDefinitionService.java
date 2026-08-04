package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.dto.BuilderResourceMetadata;
import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.factories.ModelAwareFhirFactory;
import gov.cms.madie.models.common.ModelType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.net.URI;

@Slf4j
@Service
@AllArgsConstructor
public class StructureDefinitionService {

  private ModelAwareFhirFactory modelAwareFhirFactory;

  /**
   * Fetches the structure definition for the given resource
   *
   * @param modelType the model to fetch the structure definition for
   * @param structureDefinitionId ID of the structure definition, as found in the
   *     StructureDefinitions based on model and version. e.g. Patient, us-core-patient,
   *     qicore-patient
   */
  public StructureDefinitionDto getStructureDefinitionById(
      ModelType modelType, String structureDefinitionId) {
    IValidationSupport chain = modelAwareFhirFactory.getValidationSupportForModel(modelType);

    IBaseResource structureDefinition =
        Objects.requireNonNull(chain.fetchAllStructureDefinitions()).stream()
            .filter(resource -> structureDefinitionId.equals(resource.getIdElement().getIdPart()))
            .findFirst()
            .orElseThrow(
                () -> new ResourceNotFoundException("StructureDefinition", structureDefinitionId));

    IParser parser =
        chain
            .getFhirContext()
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);
    return StructureDefinitionDto.builder()
        .definition(parser.encodeResourceToString(structureDefinition))
        .build();
  }

  public List<StructureDefinitionDto> getExtensionsForTargetPath(
      ModelType modelType, String targetPath, String targetKind) {
    IValidationSupport chain = modelAwareFhirFactory.getValidationSupportForModel(modelType);

    List<StructureDefinition> collect =
        Objects.requireNonNull(chain.fetchAllStructureDefinitions()).stream()
            .map(resource -> (StructureDefinition) resource)
            .filter(
                resource -> {
                  // if Extension resource is Active & non-experimental
                  return "Extension".equals((resource).getType())
                      && PublicationStatus.ACTIVE.equals(resource.getStatus())
                      && !resource.getExperimental();
                })
            .filter(structureDef -> contextApplies(structureDef, targetPath, targetKind))
            .toList();

    IParser parser =
        chain
            .getFhirContext()
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);

    return collect.stream()
        .map(
            c ->
                StructureDefinitionDto.builder()
                    .definition(parser.encodeResourceToString(c))
                    .build())
        .collect(Collectors.toList());
  }

  private boolean contextApplies(StructureDefinition structDefs, String target, String targetKind) {
    List<StructureDefinition.StructureDefinitionContextComponent> contexts =
        structDefs.getContext();
    return CollectionUtils.isNotEmpty(contexts)
        && contexts.stream()
            .anyMatch(
                (ctx) -> {
                  boolean matched =
                      ctx.hasExpression()
                          && (target.equals(ctx.getExpressionElement().getValueAsString())
                              || targetKind.equalsIgnoreCase(ctx.getType().name()));
                  ctx.getModifierExtension().stream().anyMatch(ext -> false);
                  return matched;
                });
  }

  /**
   * Fetches the value set definition for the given value set url
   *
   * @param modelType the model to fetch the value set for
   * @param url of the value set definition
   */
  public String getValueSetDefinition(ModelType modelType, String url) {
    IValidationSupport chain = modelAwareFhirFactory.getValidationSupportForModel(modelType);
    IBaseResource structureDefinition = chain.fetchValueSet(url);
    IParser parser =
        chain
            .getFhirContext()
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);
    return parser.encodeResourceToString(structureDefinition);
  }

  /**
   * Return the ID, title, profile, category and type of all structure definitions that have a kind
   * of "resource"
   *
   * @param modelType the model to fetch resources for
   * @return list of ResourceIdentifier, comprised of ID and title of the structure definitions
   */
  public List<ResourceIdentifier> getAllResources(ModelType modelType) {
    IValidationSupport chain = modelAwareFhirFactory.getValidationSupportForModel(modelType);
    return Objects.requireNonNull(chain.fetchAllStructureDefinitions()).stream()
        .filter(
            resource -> {
              StructureDefinition sd = (StructureDefinition) resource;
              String title = sd.getTitle();
              String idPart =
                  resource.getIdElement() != null ? resource.getIdElement().getIdPart() : null;
              return "resource".equals(sd.getKind().toCode())
                  && (title != null && !title.isEmpty() || idPart != null && !idPart.isEmpty());
            })
        .map(
            resource -> {
              StructureDefinition structureDefinition = (StructureDefinition) resource;
              String idPart =
                  resource.getIdElement() != null ? resource.getIdElement().getIdPart() : null;
              String title = structureDefinition.getTitle();
              String resultTitle = (title != null && !title.isEmpty()) ? title : idPart;
              return ResourceIdentifier.builder()
                  .id(idPart)
                  .title(resultTitle)
                  .type(structureDefinition.getType())
                  .category(getCategoryByType(chain, structureDefinition.getType()))
                  .profile(structureDefinition.getUrl())
                  .build();
            })
        .toList();
  }

  public BuilderResourceMetadata getBuilderResourceMetadata(ModelType modelType) {
    IValidationSupport chain = modelAwareFhirFactory.getValidationSupportForModel(modelType);
    List<StructureDefinition> structureDefinitions =
        Objects.requireNonNull(chain.fetchAllStructureDefinitions()).stream()
            .map(resource -> (StructureDefinition) resource)
            .toList();
    List<String> resourcePaths = getResourcePaths(structureDefinitions);
    return BuilderResourceMetadata.builder()
        .resourcePaths(resourcePaths)
        .primaryPatientProfile(getPrimaryPatientProfile(structureDefinitions, resourcePaths))
        .build();
  }

  private List<String> getResourcePaths(List<StructureDefinition> structureDefinitions) {
    return structureDefinitions.stream()
        .filter(
            structureDefinition ->
                "resource".equals(structureDefinition.getKind().toCode())
                    && structureDefinition.getUrl() != null)
        .map(StructureDefinition::getUrl)
        .map(this::getResourcePath)
        .filter(path -> path != null && !"/fhir".equals(path))
        .distinct()
        .toList();
  }

  private ResourceIdentifier getPrimaryPatientProfile(
      List<StructureDefinition> structureDefinitions, List<String> resourcePaths) {
    List<StructureDefinition> patientProfiles =
        structureDefinitions.stream()
            .filter(
                structureDefinition ->
                    "resource".equals(structureDefinition.getKind().toCode())
                        && "Patient".equals(structureDefinition.getType())
                        && structureDefinition.getUrl() != null)
            .filter(
                structureDefinition ->
                    resourcePaths.stream()
                        .anyMatch(path -> structureDefinition.getUrl().contains(path)))
            .toList();

    return patientProfiles.stream()
        .filter(
            candidate ->
                patientProfiles.stream()
                    .noneMatch(
                        profile ->
                            candidate
                                .getUrl()
                                .equals(getBaseDefinitionUrl(profile.getBaseDefinition()))))
        .findFirst()
        .map(
            structureDefinition ->
                ResourceIdentifier.builder()
                    .id(structureDefinition.getIdElement().getIdPart())
                    .title(structureDefinition.getTitle())
                    .type(structureDefinition.getType())
                    .profile(structureDefinition.getUrl())
                    .build())
        .orElseThrow(() -> new ResourceNotFoundException("Patient StructureDefinition", null));
  }

  private String getBaseDefinitionUrl(String baseDefinition) {
    if (baseDefinition == null) {
      return null;
    }
    return baseDefinition.split("\\|", 2)[0];
  }

  private String getResourcePath(String profileUrl) {
    int structureDefinitionIndex = profileUrl.indexOf("/StructureDefinition/");
    if (structureDefinitionIndex < 0) {
      return null;
    }
    return URI.create(profileUrl.substring(0, structureDefinitionIndex)).getPath();
  }

  /**
   * Returns the FHIR categorization of the provided type by loading the StructureDefinition with an
   * ID matching the provided type, and inspecting the Category extension.
   *
   * @param chain the resolved validation support chain for the model
   * @param type base Type of the resource
   * @return FHIR categorization, including top-level and sub-category, of the provided
   */
  /** Package-private for testing. */
  String getCategoryByType(IValidationSupport chain, String type) {
    Extension extension =
        Objects.requireNonNull(chain.fetchAllStructureDefinitions()).stream()
            .filter(resource -> type.equals(resource.getIdElement().getIdPart()))
            .map(resource -> (StructureDefinition) resource)
            .findFirst()
            .map(
                structureDefinition ->
                    structureDefinition.getExtensionByUrl(
                        UriConstants.FhirStructureDefinitions.CATEGORY_URI))
            .orElse(null);
    return extension == null ? null : extension.getValueAsPrimitive().getValueAsString();
  }
}

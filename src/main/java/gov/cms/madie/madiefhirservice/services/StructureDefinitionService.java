package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class StructureDefinitionService {

  private IValidationSupport validationSupportChainQiCore600;

  /**
   * Fetches the structure definition for the given resource
   *
   * @param structureDefinitionId ID of the structure definition, as found in the
   *     StructureDefinitions based on model and version. e.g. Patient, us-core-patient,
   *     qicore-patient
   */
  public StructureDefinitionDto getStructureDefinitionById(String structureDefinitionId) {

    IBaseResource structureDefinition =
        Objects.requireNonNull(validationSupportChainQiCore600.fetchAllStructureDefinitions())
            .stream()
            .filter(resource -> structureDefinitionId.equals(resource.getIdElement().getIdPart()))
            .findFirst()
            .orElseThrow(
                () -> new ResourceNotFoundException("StructureDefinition", structureDefinitionId));

    // Todo: enhance with model-info, or at least primary code path

    IParser parser =
        validationSupportChainQiCore600
            .getFhirContext()
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);
    return StructureDefinitionDto.builder()
        .definition(parser.encodeResourceToString(structureDefinition))
        .build();
  }

  public List<StructureDefinitionDto> getExtensionsForTargetPath(
      String targetPath, String targetKind) {
    List<org.hl7.fhir.r4.model.StructureDefinition> collect =
        Objects.requireNonNull(validationSupportChainQiCore600.fetchAllStructureDefinitions())
            .stream()
            .filter(
                resource -> {
                  boolean match = "Extension".equals(((StructureDefinition) resource).getType());
                  return match;
                })
            .map(
                resource -> {
                  return (org.hl7.fhir.r4.model.StructureDefinition) resource;
                })
            .filter(
                structureDef -> {
                  return contextApplies(structureDef, targetPath, targetKind);
                })
            .toList();

    // Todo: enhance with model-info, or at least primary code path
    IParser parser =
        validationSupportChainQiCore600
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
                  ctx.getModifierExtension().stream()
                      .anyMatch(
                          ext -> {
                            log.info("extension.getValue() {}", ext.getExtensionString(targetKind));
                            return false;
                          });
                  return matched;
                });
  }

  /**
   * Fetches the value set definition for the given value set url
   *
   * @param url of the value set definition
   */
  public String getValueSetDefinition(String url) {
    IBaseResource structureDefinition = validationSupportChainQiCore600.fetchValueSet(url);
    IParser parser =
        validationSupportChainQiCore600
            .getFhirContext()
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);
    return parser.encodeResourceToString(structureDefinition);
  }

  /**
   * Return the ID, title, profile, category and type of all structure definitions that start with
   * QICore and have a kind of "resource"
   *
   * @return list of ResourceIdentifier, comprised of ID and title of the structure definitions
   */
  public List<ResourceIdentifier> getAllResources() {
    return Objects.requireNonNull(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .stream()
        .filter(
            resource ->
                "resource".equals(((StructureDefinition) resource).getKind().toCode())
                    && resource.getIdElement().getIdPart().startsWith("qicore"))
        .map(
            (resource) -> {
              StructureDefinition structureDefinition = (StructureDefinition) resource;
              return ResourceIdentifier.builder()
                  .id(resource.getIdElement().getIdPart())
                  .title(structureDefinition.getTitle())
                  .type(structureDefinition.getType())
                  .category(getCategoryByType(structureDefinition.getType()))
                  // Todo: update profile URL if this method changes to return more than just
                  // QI-Core resources
                  .profile(structureDefinition.getUrl())
                  .build();
            })
        .toList();
  }

  /**
   * Returns the FHIR categorization of the provided type by loading the StructureDefinition with an
   * ID matching the provided type, and inspecting the Category extension.
   *
   * @param type base Type of the resource
   * @return FHIR categorization, including top-level and sub-category, of the provided
   */
  public String getCategoryByType(String type) {
    Extension extension =
        Objects.requireNonNull(validationSupportChainQiCore600.fetchAllStructureDefinitions())
            .stream()
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

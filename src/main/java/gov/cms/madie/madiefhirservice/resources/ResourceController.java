package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.UnsupportedTypeException;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import gov.cms.madie.models.common.ModelType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(
    path = {
      "/fhir/models/{model}/resources", // canonical model-agnostic path
      "/qicore/6_0_0/resources", // deprecated alias → qicore6
      "/qicore/resources" // deprecated alias → qicore6
    })
@AllArgsConstructor
public class ResourceController {

  private StructureDefinitionService structureDefinitionService;

  /**
   * Resolves a {model} path variable (ModelType.shortValue) to a ModelType. Falls back to
   * QI_CORE_6_0_0 for legacy aliases that carry no path variable.
   */
  private ModelType resolveModel(String model) {
    if (model == null) {
      return ModelType.QI_CORE_6_0_0;
    }
    ModelType modelType = ModelType.byShortValue(model);
    if (modelType == null) {
      throw new UnsupportedTypeException(getClass().getName(), model);
    }
    return modelType;
  }

  @GetMapping(
      value = "/structure-definitions/{structureDefinitionId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public StructureDefinitionDto getStructureDefinition(
      @PathVariable(required = false) String model, @PathVariable String structureDefinitionId) {
    return structureDefinitionService.getStructureDefinitionById(
        resolveModel(model), structureDefinitionId);
  }

  @GetMapping(value = "/value-set-definition", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getValueSetDefinition(
      @PathVariable(required = false) String model, @RequestParam String url) {
    return structureDefinitionService.getValueSetDefinition(resolveModel(model), url);
  }

  @GetMapping(value = "/extensions", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<StructureDefinitionDto> getExtensionsForTargetPath(
      @PathVariable(required = false) String model,
      @RequestParam(name = "targetPath") String targetPath,
      @RequestParam(name = "kind") String targetKind) {
    return structureDefinitionService.getExtensionsForTargetPath(
        resolveModel(model), targetPath, targetKind);
  }

  @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ResourceIdentifier> getAllResources(@PathVariable(required = false) String model) {
    return structureDefinitionService.getAllResources(resolveModel(model));
  }
}

package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import gov.cms.madie.madiefhirservice.utils.ModelTypeResolver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/fhir/models/{model}/resources")
@AllArgsConstructor
public class ResourceController {

  private StructureDefinitionService structureDefinitionService;

  @GetMapping(
      value = "/structure-definitions/{structureDefinitionId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public StructureDefinitionDto getStructureDefinition(
      @PathVariable(required = false) String model, @PathVariable String structureDefinitionId) {
    return structureDefinitionService.getStructureDefinitionById(
        ModelTypeResolver.resolve(model), structureDefinitionId);
  }

  @GetMapping(value = "/value-set-definition", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getValueSetDefinition(
      @PathVariable(required = false) String model, @RequestParam String url) {
    return structureDefinitionService.getValueSetDefinition(ModelTypeResolver.resolve(model), url);
  }

  @GetMapping(value = "/extensions", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<StructureDefinitionDto> getExtensionsForTargetPath(
      @PathVariable(required = false) String model,
      @RequestParam(name = "targetPath") String targetPath,
      @RequestParam(name = "kind") String targetKind) {
    return structureDefinitionService.getExtensionsForTargetPath(
        ModelTypeResolver.resolve(model), targetPath, targetKind);
  }

  @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ResourceIdentifier> getAllResources(@PathVariable(required = false) String model) {
    return structureDefinitionService.getAllResources(ModelTypeResolver.resolve(model));
  }
}

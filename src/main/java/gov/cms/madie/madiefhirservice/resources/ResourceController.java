package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = {"/qicore/6_0_0/resources", "/qicore/resources"})
@AllArgsConstructor
public class ResourceController {

  private StructureDefinitionService structureDefinitionService;

  @GetMapping(
      value = "/structure-definitions/{structureDefinitionId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public StructureDefinitionDto getStructureDefinition(@PathVariable String structureDefinitionId) {
    return structureDefinitionService.getStructureDefinitionById(structureDefinitionId);
  }

  @GetMapping(value = "/value-set-definition", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getValueSetDefinition(@RequestParam String url) {
    return structureDefinitionService.getValueSetDefinition(url);
  }

  @GetMapping(
      value="/extensions/{targetPath}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<StructureDefinitionDto> getExtensionsForTargetPath(@PathVariable String targetPath) {
    log.info("targetPath: [{}]", targetPath);
    return structureDefinitionService.getExtensionsForTargetPath(targetPath);
  }

  @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ResourceIdentifier> getAllResources() {
    return structureDefinitionService.getAllResources();
  }
}

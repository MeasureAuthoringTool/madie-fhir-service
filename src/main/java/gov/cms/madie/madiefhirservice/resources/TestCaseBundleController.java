package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import gov.cms.madie.madiefhirservice.dto.ExecutionBundleDTO;
import gov.cms.madie.madiefhirservice.exceptions.BundleOperationException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.factories.ModelAwareFhirFactory;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import gov.cms.madie.madiefhirservice.services.TestCaseBundleService;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.dto.ExportDTO;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.madie.madiefhirservice.utils.ModelEndpointMap.QICORE_VERSION_MODELTYPE_MAP;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/test-cases")
@Tag(
    name = "TestCase-Bundle-Controller",
    description = "API for generating test case bundle for export and execution")
@AllArgsConstructor
public class TestCaseBundleController {

  private final TestCaseBundleService testCaseBundleService;
  private final ModelAwareFhirFactory fhirModelFactory;
  private final ResourceValidationService validationService;

  @PutMapping("/export-all")
  public ResponseEntity<byte[]> getTestCaseExportBundle(
      Principal principal, @RequestBody ExportDTO exportDTO) {
    Measure measure = exportDTO.getMeasure();

    List<String> testCaseIds = exportDTO.getTestCaseIds();
    final String username = principal.getName();
    log.info(
        "User [{}] is attempting to export all test cases from Measure [{}]",
        username,
        measure.getId());
    if (testCaseIds == null || testCaseIds.isEmpty()) {
      throw new ResourceNotFoundException("test cases", "measure", measure.getId());
    }

    List<TestCase> testCases =
        Optional.ofNullable(measure.getTestCases())
            .orElseThrow(
                () -> new ResourceNotFoundException("test cases", "measure", measure.getId()))
            .stream()
            .filter(tc -> testCaseIds.stream().anyMatch(id -> id.equals(tc.getId())))
            .collect(Collectors.toList());

    Map<String, Bundle> exportableTestCaseBundle =
        testCaseBundleService.getTestCaseExportBundle(measure, testCases, exportDTO);
    if (testCases.size() != exportableTestCaseBundle.size()) {
      // remove the test cases that couldn't be parsed
      List<TestCase> missingTestCases = getMissingTestCases(testCases, exportableTestCaseBundle);
      log.info(
          "Some test cases couldn't be parsed: {}",
          missingTestCases.stream().map(TestCase::getId).collect(Collectors.joining(", ")));
      testCases =
          testCases.stream()
              .filter(
                  testCase ->
                      exportableTestCaseBundle.keySet().stream()
                          .anyMatch(s -> s.contains(testCase.getPatientId().toString())))
              .collect(Collectors.toList());
      return ResponseEntity.status(206)
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment;filename=\""
                  + ExportFileNamesUtil.getTestCaseExportZipName(measure)
                  + ".zip\"")
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(
              testCaseBundleService.zipTestCaseContents(
                  measure, exportableTestCaseBundle, testCases, missingTestCases));
    }
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment;filename=\""
                + ExportFileNamesUtil.getTestCaseExportZipName(measure)
                + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(
            testCaseBundleService.zipTestCaseContents(
                measure, exportableTestCaseBundle, testCases, null));
  }

  private static List<TestCase> getMissingTestCases(
      List<TestCase> testCases, Map<String, Bundle> exportableTestCaseBundle) {
    Set<String> exportablePatientIds =
        exportableTestCaseBundle.keySet().stream()
            .map(key -> key.split("/")[0])
            .collect(Collectors.toSet());
    List<TestCase> missingTestCases =
        testCases.stream()
            .filter(testCase -> !exportablePatientIds.contains(testCase.getPatientId().toString()))
            .collect(Collectors.toList());
    return missingTestCases;
  }

  @PostMapping(
      path = "/qicore/{model}/execution-bundles",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<ExecutionBundleDTO>> getTestCaseExecutionBundle(
      @PathVariable("model") String modelVersion,
      @RequestBody List<TestCase> testCases,
      HttpEntity<String> request) {
    final ModelType modelType = QICORE_VERSION_MODELTYPE_MAP.get(modelVersion);
    IParser parser = fhirModelFactory.getJsonParserForModel(modelType);
    FhirContext fhirContext = fhirModelFactory.getContextForModel(modelType);

    List<ExecutionBundleDTO> executionBundles = new ArrayList<>();
    for (TestCase testCase : testCases) {
      IBaseBundle bundle;
      try {
        bundle = fhirModelFactory.parseForModel(modelType, testCase.getJson());
      } catch (DataFormatException | ClassCastException ex) {
        throw new BundleOperationException("Test Case", testCase.getId(), ex);
      }

      // only operate on bundles
      if (!"BUNDLE".equalsIgnoreCase(bundle.fhirType())) {
        throw new BundleOperationException(
            "Test Case",
            testCase.getId(),
            new IllegalArgumentException("Resource must have resourceType of 'Bundle'"));
      }

      // find any resources with invalid references.
      Set<IBaseResource> resourcesWithInvalidReferences =
          validationService.findResourcesWithInvalidReferences(fhirContext, bundle);
      List<Bundle.BundleEntryComponent> validResources =
          ((Bundle) bundle)
              .getEntry().stream()
                  .filter(entry -> !resourcesWithInvalidReferences.contains(entry.getResource()))
                  .toList();

      // if there are invalid references, create a new bundle with only the valid resources.
      if (validResources.size() != ((Bundle) bundle).getEntry().size()) {
        ExecutionBundleDTO executionBundleDTO =
            ExecutionBundleDTO.builder()
                .bundle(
                    parser.encodeResourceToString(
                        ((Bundle) bundle).copy().setEntry(validResources)))
                .testCaseId(testCase.getId())
                .build();
        executionBundles.add(executionBundleDTO);
      }
    }
    // return modified bundles with their test case IDs.
    return ResponseEntity.ok(executionBundles);
  }
}

package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.exceptions.BundleOperationException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.services.TestCaseBundleService;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.dto.ExportDTO;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.packaging.utils.PackagingUtility;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/test-cases")
@Tag(
    name = "TestCase-Bundle-Controller",
    description = "API for generating test case bundle for export")
@RequiredArgsConstructor
public class TestCaseBundleController {

  private final TestCaseBundleService testCaseBundleService;

  @PutMapping("/{testCaseId}/exports")
  public ResponseEntity<byte[]> getTestCaseExportBundle(
      Principal principal, @RequestBody Measure measure, @PathVariable String testCaseId) {

    final String username = principal.getName();
    log.info(
        "User [{}] is attempting to export test case with id [{}] from Measure [{}]",
        username,
        testCaseId,
        measure.getId());

    TestCase testCase =
        Optional.ofNullable(measure.getTestCases())
            .orElseThrow(
                () -> new ResourceNotFoundException("test cases", "measure", measure.getId()))
            .stream()
            .filter(tc -> tc.getId().equals(testCaseId))
            .findFirst()
            .orElseThrow(
                () -> new ResourceNotFoundException("test case resource", "test case", testCaseId));

    Bundle exportableTestCaseBundle =
        testCaseBundleService.getTestCaseExportBundle(measure, testCase);

    try {
      String exportFileName = ExportFileNamesUtil.getTestCaseExportFileName(measure, testCase);
      PackagingUtility utility = PackagingUtilityFactory.getInstance(measure.getModel());
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment;filename=\""
                  + ExportFileNamesUtil.getTestCaseExportZipName(measure)
                  + ".zip\"")
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(utility.getZipBundle(exportableTestCaseBundle, exportFileName));
    } catch (RestClientException
        | IllegalArgumentException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException ex) {
      log.error("An error occurred while bundling measure {}", measure.getId(), ex);
      throw new BundleOperationException("Measure", measure.getId(), ex);
    }
  }

  @PutMapping("/export-all")
  public ResponseEntity<byte[]> getTestCaseExportBundle(
      Principal principal, @RequestBody ExportDTO exportDTO) {
    Measure measure = exportDTO.getMeasure();
    List<String> testCaseId = exportDTO.getTestCaseIds();

    final String username = principal.getName();
    log.info(
        "User [{}] is attempting to export all test cases from Measure [{}]",
        username,
        measure.getId());

    if (testCaseId == null || testCaseId.isEmpty()) {
      throw new ResourceNotFoundException("test cases", "measure", measure.getId());
    }

    List<TestCase> testCases =
        Optional.ofNullable(measure.getTestCases())
            .orElseThrow(
                () -> new ResourceNotFoundException("test cases", "measure", measure.getId()))
            .stream()
            .filter(tc -> testCaseId.stream().anyMatch(id -> id.equals(tc.getId())))
            .collect(Collectors.toList());

    Map<String, Bundle> exportableTestCaseBundle =
        testCaseBundleService.getTestCaseExportBundle(measure, testCases);

    try {
      PackagingUtility utility = PackagingUtilityFactory.getInstance(measure.getModel());
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment;filename=\""
                  + ExportFileNamesUtil.getTestCaseExportZipName(measure)
                  + ".zip\"")
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(utility.getZipBundle(exportableTestCaseBundle, null));
    } catch (RestClientException
        | IllegalArgumentException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException ex) {
      log.error("An error occurred while bundling measure {}", measure.getId(), ex);
      throw new BundleOperationException("Measure", measure.getId(), ex);
    }
  }
}

package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.services.TestCaseBundleService;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.dto.ExportDTO;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/test-cases")
@Tag(
    name = "TestCase-Bundle-Controller",
    description = "API for generating test case bundle for export")
@RequiredArgsConstructor
public class TestCaseBundleController {

  private final TestCaseBundleService testCaseBundleService;

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
    if (testCases.size() != exportableTestCaseBundle.size()) {

      // remove the test cases that couldn't be parsed
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
                  measure, exportableTestCaseBundle, testCases));
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
                measure, exportableTestCaseBundle, testCases));
  }
}

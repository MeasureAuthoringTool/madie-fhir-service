package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.services.TestCaseDateShifterService;
import gov.cms.madie.models.measure.TestCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import static java.util.stream.Collectors.joining;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/")
@Tag(name = "Test-Case-Controller", description = "API for manipulating FHIR Test Cases")
@RequiredArgsConstructor
public class TestCaseController {
  private final TestCaseDateShifterService testCaseDateShifterService;

  @PutMapping("/test-cases/shift-dates")
  public ResponseEntity<List<TestCase>> shiftTestCasesDates(
      Principal principal,
      @RequestBody List<TestCase> testCases,
      @RequestParam(name = "shifted", defaultValue = "0") int shifted) {
    log.info(
        "User [{}] requested date shift for test cases [{}] of [{}] years",
        principal.getName(),
        testCases.stream().map(TestCase::getId).collect(joining(", ")),
        shifted);
    return ResponseEntity.ok(testCaseDateShifterService.shiftDates(testCases, shifted));
  }

  @PutMapping("/test-case/shift-dates")
  public ResponseEntity<TestCase> shiftTestCaseDates(
      Principal principal,
      @RequestBody TestCase testCase,
      @RequestParam(name = "shifted", defaultValue = "0") int shifted) {
    log.info(
        "User [{}] requested date shift for test case [{}] of [{}] years",
        principal.getName(),
        testCase.getId(),
        shifted);
    return ResponseEntity.ok(testCaseDateShifterService.shiftDates(testCase, shifted));
  }
}

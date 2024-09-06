package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class TestCaseDateShifterServiceTest implements ResourceFileUtil {

  @Autowired FhirContext fhirContext;

  @Autowired TestCaseDateShifterService testCaseDateShifterService;
  private Measure measure;
  private Bundle testCaseBundle;
  private IParser fhirParser;

  @BeforeEach
  void setUpMeasure() throws JsonProcessingException {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");
    measure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);

    fhirParser = testCaseDateShifterService.getIParser();
    // Canned measure has 2 test cases, which are duplicates except for FHIR Bundle type
    // (transaction vs collection)
    testCaseBundle = (Bundle) fhirParser.parseResource(measure.getTestCases().get(0).getJson());
  }

  @Test
  void testPositiveDateShiftBirthdate() {
    Patient patient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient");
    Date originalBirthDate = (Date) patient.getBirthDate().clone();
    int shiftBy = 2; // years
    testCaseDateShifterService.shiftDates(patient, shiftBy);
    assertEquals(DateUtils.addYears(originalBirthDate, shiftBy), patient.getBirthDate());
  }

  @Test
  void testNegativeDateShiftBirthdate() {
    Patient patient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient");
    Date originalBirthDate = (Date) patient.getBirthDate().clone();
    int shiftBy = -2; // years
    testCaseDateShifterService.shiftDates(patient, shiftBy);
    assertEquals(DateUtils.addYears(originalBirthDate, shiftBy), patient.getBirthDate());
  }

  @Test
  void testDateShiftEncounter() {
    Encounter encounter = (Encounter) ResourceUtils.getResource(testCaseBundle, "Encounter");
    Date orgPeriodStart = (Date) encounter.getPeriod().getStart().clone();
    Date orgPeriodEnd = (Date) encounter.getPeriod().getEnd().clone();
    int shiftBy = -10; // years
    testCaseDateShifterService.shiftDates(encounter, shiftBy);
    assertEquals(DateUtils.addYears(orgPeriodStart, shiftBy), encounter.getPeriod().getStart());
    assertEquals(DateUtils.addYears(orgPeriodEnd, shiftBy), encounter.getPeriod().getEnd());
  }

  @Test
  void testDateShiftMedicationRequest() {
    MedicationRequest medicationRequest =
        (MedicationRequest) ResourceUtils.getResource(testCaseBundle, "MedicationRequest");
    DateTimeType orgAuthoredOn = medicationRequest.getAuthoredOnElement().copy();
    Date orgPeriodStart =
        (Date)
            medicationRequest
                .getDosageInstruction()
                .get(0)
                .getTiming()
                .getRepeat()
                .getBoundsPeriod()
                .getStart()
                .clone();
    Date orgPeriodEnd =
        (Date)
            medicationRequest
                .getDosageInstruction()
                .get(0)
                .getTiming()
                .getRepeat()
                .getBoundsPeriod()
                .getEnd()
                .clone();
    int shiftBy = 1; // years
    testCaseDateShifterService.shiftDates(medicationRequest, shiftBy);
    assertEquals(
        DateUtils.addYears(orgAuthoredOn.getValue(), shiftBy),
        medicationRequest.getAuthoredOnElement().getValue());
    assertEquals(
        DateUtils.addYears(orgPeriodStart, shiftBy),
        medicationRequest
            .getDosageInstruction()
            .get(0)
            .getTiming()
            .getRepeat()
            .getBoundsPeriod()
            .getStart());
    assertEquals(
        DateUtils.addYears(orgPeriodEnd, shiftBy),
        medicationRequest
            .getDosageInstruction()
            .get(0)
            .getTiming()
            .getRepeat()
            .getBoundsPeriod()
            .getEnd());
  }

  @Test
  void testDateShiftLeapYear() {
    Date leapYearBirthDate = new Date(Instant.parse("1992-02-29T10:15:30.00Z").toEpochMilli());
    Patient patient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient");
    patient.setBirthDate(leapYearBirthDate);
    int shiftBy = -2; // years
    testCaseDateShifterService.shiftDates(patient, shiftBy);
    assertEquals(DateUtils.addYears(leapYearBirthDate, shiftBy), patient.getBirthDate());
    assertEquals(
        new Date(Instant.parse("1990-02-28T10:15:30.00Z").toEpochMilli()), patient.getBirthDate());
  }

  @Test
  void testSingleTestCaseDateShift() {
    Patient orgPatient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient").copy();
    Encounter orgEncounter =
        (Encounter) ResourceUtils.getResource(testCaseBundle, "Encounter").copy();
    MedicationRequest orgMedicationRequest =
        (MedicationRequest) ResourceUtils.getResource(testCaseBundle, "MedicationRequest").copy();
    Condition orgCondition =
        (Condition) ResourceUtils.getResource(testCaseBundle, "Condition").copy();

    int shiftBy = 2;
    TestCase shiftedTestCase =
        testCaseDateShifterService.shiftDates(measure.getTestCases().get(0), shiftBy);

    Bundle shiftedDatesBundle =
        (Bundle) testCaseDateShifterService.getIParser().parseResource(shiftedTestCase.getJson());
    Patient shiftedPatient = (Patient) ResourceUtils.getResource(shiftedDatesBundle, "Patient");
    Encounter shiftedEncounter =
        (Encounter) ResourceUtils.getResource(shiftedDatesBundle, "Encounter");
    MedicationRequest shiftedMedRequest =
        (MedicationRequest) ResourceUtils.getResource(shiftedDatesBundle, "MedicationRequest");
    Condition shiftedCondition =
        (Condition) ResourceUtils.getResource(shiftedDatesBundle, "Condition");

    assertEquals(
        DateUtils.addYears(orgPatient.getBirthDate(), shiftBy), shiftedPatient.getBirthDate());

    assertEquals(
        DateUtils.addYears(orgEncounter.getPeriod().getStart(), shiftBy),
        shiftedEncounter.getPeriod().getStart());
    assertEquals(
        DateUtils.addYears(orgEncounter.getPeriod().getEnd(), shiftBy),
        shiftedEncounter.getPeriod().getEnd());

    assertEquals(
        DateUtils.addYears(orgMedicationRequest.getAuthoredOnElement().getValue(), shiftBy),
        shiftedMedRequest.getAuthoredOnElement().getValue());

    assertEquals(
        DateUtils.addYears(orgCondition.getOnsetDateTimeType().getValue(), shiftBy),
        shiftedCondition.getOnsetDateTimeType().getValue());
  }

  @Test
  void testMultipleTestCaseDateShift() {
    Patient orgPatient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient").copy();
    int shiftBy = 500;
    List<TestCase> shiftedTestCases =
        testCaseDateShifterService.shiftDates(measure.getTestCases(), shiftBy);
    assertEquals(measure.getTestCases().size(), shiftedTestCases.size());
    shiftedTestCases.forEach(
        testCase -> {
          Bundle bundle =
              (Bundle) testCaseDateShifterService.getIParser().parseResource(testCase.getJson());
          assertEquals(
              DateUtils.addYears(orgPatient.getBirthDate(), shiftBy),
              ((Patient) ResourceUtils.getResource(bundle, "Patient")).getBirthDate());
        });
  }

  @Test
  void handlesNullTestCase() {
    assertNull(testCaseDateShifterService.shiftDates((TestCase) null, 2));
  }

  @Test
  void handlesEmptyTestCaseList() {
    List<TestCase> testCases = testCaseDateShifterService.shiftDates(new ArrayList<>(), 2);
    assertEquals(Collections.emptyList(), testCases);
  }

  @Test
  void handlesEmptyTestCaseJson() {
    List<TestCase> emptyTestCase = List.of(TestCase.builder().id("1234").json("").build());
    List<TestCase> shiftedTestCases = testCaseDateShifterService.shiftDates(emptyTestCase, 2);
    assertEquals(Collections.emptyList(), shiftedTestCases);
  }

  @Test
  void skipsUnparsableTestCase() {
    TestCase badTestCase = TestCase.builder().id("1234").json("").build();
    measure.getTestCases().add(badTestCase);

    List<TestCase> shiftedTestCases =
        testCaseDateShifterService.shiftDates(measure.getTestCases(), 1);
    assertEquals(measure.getTestCases().size() - 1, shiftedTestCases.size());
    assertFalse(shiftedTestCases.contains(badTestCase));
  }

  @Test
  void verifiesPartialDateTypeHasDateValue() {
    // Parse a Procedure with partial Performed element that only contains an extension and no date
    // values.
    Procedure procedure =
        fhirParser.parseResource(
            Procedure.class,
            "{\n"
                + "        \"resourceType\": \"Procedure\",\n"
                + "        \"id\": \"device-application-2c38\",\n"
                + "        \"meta\": {\n"
                + "          \"profile\": [\n"
                + "            \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-procedurenotdone\"\n"
                + "          ]\n"
                + "        },\n"
                + "        \"extension\": [\n"
                + "          {\n"
                + "            \"url\": \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-recorded\",\n"
                + "            \"valueDateTime\": \"2025-10-31T17:35:00-04:00\"\n"
                + "          }\n"
                + "        ],\n"
                + "        \"status\": \"not-done\",\n"
                + "        \"_performedDateTime\": {\n"
                + "          \"extension\": [\n"
                + "            {\n"
                + "              \"url\": \"http://hl7.org/fhir/StructureDefinition/data-absent-reason\",\n"
                + "              \"valueCode\": \"not-performed\"\n"
                + "            }\n"
                + "          ]\n"
                + "        }\n"
                + "      }");
    Procedure copy = procedure.copy();
    testCaseDateShifterService.shiftDates(procedure, 1);
    assertTrue(copy.getPerformed().equalsDeep(procedure.getPerformed()));
  }

  @Test
  void limitsUpperYearRangeTo9999() {
    Patient patient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient");
    Date originalBirthDate = (Date) patient.getBirthDate().clone();
    int shiftBy = 1000000; // years
    testCaseDateShifterService.shiftDates(patient, shiftBy);
    assertEquals(DateUtils.setYears(originalBirthDate, 9999), patient.getBirthDate());
  }

  @Test
  void limitsLowerYearRangeTo0() {
    Patient patient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient");
    Date originalBirthDate = (Date) patient.getBirthDate().clone();
    int shiftBy = -1000000; // years
    testCaseDateShifterService.shiftDates(patient, shiftBy);
    assertEquals(DateUtils.setYears(originalBirthDate, 1900), patient.getBirthDate());
  }
}

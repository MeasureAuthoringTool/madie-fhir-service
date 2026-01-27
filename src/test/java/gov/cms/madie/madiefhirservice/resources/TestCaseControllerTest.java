package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.services.TestCaseDateShifterService;
import gov.cms.madie.models.measure.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseControllerTest {

  @Mock private TestCaseDateShifterService testCaseDateShifterService;

  @Mock private Principal principal;

  @InjectMocks private TestCaseController testCaseController;

  private TestCase testCase1;
  private TestCase testCase2;

  @BeforeEach
  void setUp() {
    testCase1 = TestCase.builder().id("test-case-1").title("Test Case 1").build();
    testCase2 = TestCase.builder().id("test-case-2").title("Test Case 2").build();
    when(principal.getName()).thenReturn("test-user");
  }

  @Test
  void testShiftTestCasesDatesReturnsListOfTestCases() {
    List<TestCase> testCases = Arrays.asList(testCase1, testCase2);
    List<TestCase> shiftedTestCases =
        Arrays.asList(
            TestCase.builder().id("test-case-1").title("Test Case 1 Shifted").build(),
            TestCase.builder().id("test-case-2").title("Test Case 2 Shifted").build());

    when(testCaseDateShifterService.shiftDates(anyList(), anyInt())).thenReturn(shiftedTestCases);

    ResponseEntity<List<TestCase>> response =
        testCaseController.shiftTestCasesDates(principal, testCases, 1);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(response.getBody(), is(notNullValue()));
    assertThat(response.getBody().size(), is(equalTo(2)));
    assertThat(response.getBody().get(0).getTitle(), is(equalTo("Test Case 1 Shifted")));
    assertThat(response.getBody().get(1).getTitle(), is(equalTo("Test Case 2 Shifted")));
  }

  @Test
  void testShiftTestCasesDatesWithZeroYears() {
    List<TestCase> testCases = Arrays.asList(testCase1, testCase2);

    when(testCaseDateShifterService.shiftDates(anyList(), anyInt())).thenReturn(testCases);

    ResponseEntity<List<TestCase>> response =
        testCaseController.shiftTestCasesDates(principal, testCases, 0);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(response.getBody(), is(notNullValue()));
    assertThat(response.getBody().size(), is(equalTo(2)));
  }

  @Test
  void testShiftTestCaseDatesReturnsTestCase() {
    TestCase shiftedTestCase =
        TestCase.builder().id("test-case-1").title("Test Case 1 Shifted").build();

    when(testCaseDateShifterService.shiftDates(any(TestCase.class), anyInt()))
        .thenReturn(shiftedTestCase);

    ResponseEntity<TestCase> response =
        testCaseController.shiftTestCaseDates(principal, testCase1, 1);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(response.getBody(), is(notNullValue()));
    assertThat(response.getBody().getId(), is(equalTo("test-case-1")));
    assertThat(response.getBody().getTitle(), is(equalTo("Test Case 1 Shifted")));
  }

  @Test
  void testShiftTestCaseDatesWithNegativeYears() {
    TestCase shiftedTestCase =
        TestCase.builder().id("test-case-1").title("Test Case 1 Shifted Back").build();

    when(testCaseDateShifterService.shiftDates(any(TestCase.class), anyInt()))
        .thenReturn(shiftedTestCase);

    ResponseEntity<TestCase> response =
        testCaseController.shiftTestCaseDates(principal, testCase1, -1);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(response.getBody(), is(notNullValue()));
    assertThat(response.getBody().getTitle(), is(equalTo("Test Case 1 Shifted Back")));
  }

  @Test
  void testShiftTestCaseDatesWithZeroYears() {
    when(testCaseDateShifterService.shiftDates(any(TestCase.class), anyInt()))
        .thenReturn(testCase1);

    ResponseEntity<TestCase> response =
        testCaseController.shiftTestCaseDates(principal, testCase1, 0);

    assertThat(response, is(notNullValue()));
    assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(response.getBody(), is(notNullValue()));
    assertThat(response.getBody().getId(), is(equalTo("test-case-1")));
  }
}

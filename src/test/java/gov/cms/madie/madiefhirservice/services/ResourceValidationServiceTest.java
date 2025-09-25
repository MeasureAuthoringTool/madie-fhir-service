package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ResourceValidationServiceTest {

  @Spy static FhirContext fhirContext;
  @Spy static FhirContext r5FhirContext;

  @InjectMocks ResourceValidationService validationService;

  @BeforeAll
  public static void setup() {
    fhirContext = FhirContext.forR4();
    r5FhirContext = FhirContext.forR5();
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsNoIssuesForEmptyBundle() {
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of());
      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesProfiles(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(false));
    }
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsIssueForMissingProfile() {
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(new Patient(), new Encounter()));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesProfiles(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      // empty profiles for Patient and Encounter has 2 issues, required profile missing adds
      // another one
      assertThat(output.getIssue().size(), is(equalTo(2)));
    }
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsIssueForInvalidProfile() {
    Patient p = new Patient();
    p.getMeta().addProfile(UriConstants.QiCore.PATIENT_PROFILE_URI);
    Encounter encounter = new Encounter();
    encounter.getMeta().addProfile("invalidURL");
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, encounter));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesProfiles(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      assertThat(output.getIssue().size(), is(equalTo(1)));
    }
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsIssueForInvalidProfileWithURISyntaxException() {
    Patient p = new Patient();
    p.getMeta().addProfile(UriConstants.QiCore.PATIENT_PROFILE_URI);
    Encounter encounter = new Encounter();
    encounter.getMeta().addProfile("http://localhost:8080/measures/id?s=^IXIC");
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, encounter));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesProfiles(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      assertThat(output.getIssue().size(), is(equalTo(1)));
    }
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsValidForPresentRequiredProfile() {
    Patient p = new Patient();
    p.getMeta().addProfile(UriConstants.QiCore.PATIENT_PROFILE_URI);
    Encounter encounter = new Encounter();
    encounter.getMeta().addProfile("http://test.profile.com");
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, encounter));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesProfiles(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(false));
    }
  }

  @Test
  void testValidateBundleResourcesIdUniquenessCatchesDuplicateIds() {
    Patient p = new Patient();
    p.setId("1111");
    Encounter e1 = new Encounter();
    e1.setId("1234");
    Encounter e2 = new Encounter();
    e2.setId("1234");
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, e1, e2));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesIdValid(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      assertThat(output.getIssueFirstRep().getDiagnostics().contains("1234"), is(true));
    }
  }

  @Test
  void testValidateBundleResourcesIdUniquenessLooksAcrossResourceTypes() {
    Patient p = new Patient();
    p.setId("1234");
    Encounter e1 = new Encounter();
    e1.setId("1234");
    Procedure p1 = new Procedure();
    p1.setId("1234");
    Encounter e2 = new Encounter();
    e2.setId("3456");
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, e1, p1, e2));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesIdValid(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      assertThat(output.getIssueFirstRep().getDiagnostics().contains("1234"), is(true));
      assertThat(output.getIssue().size(), is(equalTo(1)));
    }
  }

  @Test
  void testValidateBundleResourcesIdUniquenessReturnsNoErrorsForAllUniqueIds() {
    Patient p = new Patient();
    p.setId("1111");
    Encounter e1 = new Encounter();
    e1.setId("2222");
    Encounter e2 = new Encounter();
    e2.setId("3333");
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, e1, e2));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesIdValid(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(false));
    }
  }

  @Test
  void testValidateBundleResourcesIdValidReturnsErrorForNoId() {
    Patient p = new Patient();
    Encounter e1 = new Encounter();
    e1.setId("2222");
    Encounter e2 = new Encounter();
    e2.setId("3333");
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, e1, e2));

      Bundle bundle = new Bundle();
      OperationOutcome output =
          (OperationOutcome) validationService.validateBundleResourcesIdValid(fhirContext, bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      assertEquals(output.getIssueFirstRep().getDiagnostics(), "All resources must have an Id");
    }
  }

  @Test
  void testR4OperationOutcomeIsSuccessfulWithWarningIssue() {
    // given
    OperationOutcome operationOutcome = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "information", "test-error", null, "invalid");

    // when
    boolean output = validationService.isSuccessful(fhirContext, operationOutcome);

    // then
    assertThat(output, is(true));
  }

  @Test
  void testR4OperationOutcomeIsSuccessfulWithInfoIssue() {
    // given
    OperationOutcome operationOutcome = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "information", "test-info", null, "business-rule");

    // when
    boolean output = validationService.isSuccessful(fhirContext, operationOutcome);

    // then
    assertThat(output, is(true));
  }

  @Test
  void testR4OperationOutcomeIsNotSuccessfulWithInfoIssueAndErrorIssue() {
    // given
    OperationOutcome operationOutcome = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "information", "test-info", null, "business-rule");
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "error", "test-error", null, "invalid");

    // when
    boolean output = validationService.isSuccessful(fhirContext, operationOutcome);

    // then
    assertThat(output, is(false));
  }

  @Test
  void testR4OperationOutcomeIsNotSuccessfulWithWarningIssueAndErrorIssue() {
    // given
    OperationOutcome operationOutcome = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "warning", "test-warning", null, "business-rule");
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "error", "test-error", null, "invalid");

    // when
    boolean output = validationService.isSuccessful(fhirContext, operationOutcome);

    // then
    assertThat(output, is(false));
  }

  @Test
  void testR4OperationOutcomeIsNotSuccessfulWithInfoIssueAndFatalIssue() {
    // given
    OperationOutcome operationOutcome = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "information", "test-info", null, "business-rule");
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "fatal", "test-fatal", null, "invalid");

    // when
    boolean output = validationService.isSuccessful(fhirContext, operationOutcome);

    // then
    assertThat(output, is(false));
  }

  @Test
  void testR5OperationOutcomeIsSuccessfulWithWarningIssue() {
    // given
    org.hl7.fhir.r5.model.OperationOutcome operationOutcome =
        new org.hl7.fhir.r5.model.OperationOutcome();
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome, "information", "test-error", null, "invalid");

    // when
    boolean output = validationService.isSuccessful(r5FhirContext, operationOutcome);

    // then
    assertThat(output, is(true));
  }

  @Test
  void testR5OperationOutcomeIsSuccessfulWithInfoIssue() {
    // given
    org.hl7.fhir.r5.model.OperationOutcome operationOutcome =
        new org.hl7.fhir.r5.model.OperationOutcome();
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome, "information", "test-info", null, "business-rule");

    // when
    boolean output = validationService.isSuccessful(r5FhirContext, operationOutcome);

    // then
    assertThat(output, is(true));
  }

  @Test
  void testR5OperationOutcomeIsNotSuccessfulWithInfoIssueAndErrorIssue() {
    // given
    org.hl7.fhir.r5.model.OperationOutcome operationOutcome =
        new org.hl7.fhir.r5.model.OperationOutcome();
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome, "information", "test-info", null, "business-rule");
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome, "error", "test-error", null, "invalid");

    // when
    boolean output = validationService.isSuccessful(r5FhirContext, operationOutcome);

    // then
    assertThat(output, is(false));
  }

  @Test
  void testCombineOutcomesHandlesR4NoIssues() {
    // given
    OperationOutcome operationOutcome1 = new OperationOutcome();
    OperationOutcome operationOutcome2 = new OperationOutcome();

    // when
    IBaseOperationOutcome output =
        validationService.combineOutcomes(fhirContext, operationOutcome1, operationOutcome2);

    // then
    assertThat(output, is(notNullValue()));

    assertThat(((OperationOutcome) output).getIssue(), is(notNullValue()));
    assertThat(((OperationOutcome) output).getIssue().isEmpty(), is(true));
  }

  @Test
  void testCombineOutcomesHandlesR4OneIssueEach() {
    // given
    OperationOutcome operationOutcome1 = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome1, "warning", "test-warning", null, "invalid");
    OperationOutcome operationOutcome2 = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome2, "information", "test-info", null, "business-rule");

    // when
    IBaseOperationOutcome output =
        validationService.combineOutcomes(fhirContext, operationOutcome1, operationOutcome2);

    // then
    assertThat(output, is(notNullValue()));

    assertThat(((OperationOutcome) output).getIssue(), is(notNullValue()));
    assertThat(((OperationOutcome) output).getIssue().size(), is(equalTo(2)));
    assertThat(((OperationOutcome) output).getIssue().get(0), is(notNullValue()));
    assertThat(
        ((OperationOutcome) output).getIssue().get(0).getSeverity().toCode(),
        is(equalTo("warning")));
    assertThat(((OperationOutcome) output).getIssue().get(1), is(notNullValue()));
    assertThat(
        ((OperationOutcome) output).getIssue().get(1).getSeverity().toCode(),
        is(equalTo("information")));
  }

  @Test
  void testCombineOutcomesHandlesR4MultipleOutcomes() {
    // given
    OperationOutcome operationOutcome1 = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome1, "information", "test-info", null, "business-rule");
    OperationOutcome operationOutcome2 = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome2, "warning", "test-warning", null, "invalid");
    OperationOutcome operationOutcome3 = new OperationOutcome();
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome3, "error", "test-error", null, "invalid");
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome3, "error", "test-error2", null, "invalid");

    // when
    IBaseOperationOutcome output =
        validationService.combineOutcomes(
            fhirContext, operationOutcome1, operationOutcome2, operationOutcome3);

    // then
    assertThat(output, is(notNullValue()));

    assertThat(((OperationOutcome) output).getIssue(), is(notNullValue()));
    assertThat(((OperationOutcome) output).getIssue().size(), is(equalTo(4)));
    assertThat(((OperationOutcome) output).getIssue().get(0), is(notNullValue()));
    assertThat(
        ((OperationOutcome) output).getIssue().get(0).getSeverity().toCode(),
        is(equalTo("information")));
    assertThat(((OperationOutcome) output).getIssue().get(1), is(notNullValue()));
    assertThat(
        ((OperationOutcome) output).getIssue().get(1).getSeverity().toCode(),
        is(equalTo("warning")));
    assertThat(((OperationOutcome) output).getIssue().get(2), is(notNullValue()));
    assertThat(
        ((OperationOutcome) output).getIssue().get(2).getDiagnostics(), is(equalTo("test-error")));
    assertThat(((OperationOutcome) output).getIssue().get(3), is(notNullValue()));
    assertThat(
        ((OperationOutcome) output).getIssue().get(3).getDiagnostics(), is(equalTo("test-error2")));
  }

  @Test
  void testCombineOutcomesHandlesR5MultipleOutcomes() {
    // given
    org.hl7.fhir.r5.model.OperationOutcome operationOutcome1 =
        new org.hl7.fhir.r5.model.OperationOutcome();
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome1, "information", "test-info", null, "business-rule");
    org.hl7.fhir.r5.model.OperationOutcome operationOutcome2 =
        new org.hl7.fhir.r5.model.OperationOutcome();
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome2, "warning", "test-warning", null, "invalid");
    org.hl7.fhir.r5.model.OperationOutcome operationOutcome3 =
        new org.hl7.fhir.r5.model.OperationOutcome();
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome3, "error", "test-error", null, "invalid");
    OperationOutcomeUtil.addIssue(
        r5FhirContext, operationOutcome3, "error", "test-error2", null, "invalid");

    // when
    IBaseOperationOutcome output =
        validationService.combineOutcomes(
            r5FhirContext, operationOutcome1, operationOutcome2, operationOutcome3);

    // then
    assertThat(output, is(notNullValue()));

    assertThat(((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue(), is(notNullValue()));
    assertThat(((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().size(), is(equalTo(4)));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(0), is(notNullValue()));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(0).getSeverity().toCode(),
        is(equalTo("information")));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(1), is(notNullValue()));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(1).getSeverity().toCode(),
        is(equalTo("warning")));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(2), is(notNullValue()));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(2).getDiagnostics(),
        is(equalTo("test-error")));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(3), is(notNullValue()));
    assertThat(
        ((org.hl7.fhir.r5.model.OperationOutcome) output).getIssue().get(3).getDiagnostics(),
        is(equalTo("test-error2")));
  }
}

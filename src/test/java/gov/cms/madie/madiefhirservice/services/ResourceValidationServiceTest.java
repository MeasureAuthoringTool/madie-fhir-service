package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceValidationServiceTest {

  @Spy FhirContext fhirContext;

  @InjectMocks ResourceValidationService validationService;

  @Test
  void testValidateBundleResourcesProfilesReturnsNoIssuesForEmptyBundle() {
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of());
      Bundle bundle = new Bundle();
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
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
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
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
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
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
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
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
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
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
      OperationOutcome output = validationService.validateBundleResourcesIdUniqueness(bundle);
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
      OperationOutcome output = validationService.validateBundleResourcesIdUniqueness(bundle);
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
      OperationOutcome output = validationService.validateBundleResourcesIdUniqueness(bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(false));
    }
  }
}

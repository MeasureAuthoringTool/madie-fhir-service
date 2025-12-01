package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.madiefhirservice.exceptions.HapiJsonException;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class ResourceValidationService {

  private ObjectMapper mapper;

  public IBaseOperationOutcome validateBundleResourcesProfiles(
      FhirContext fhirContext, IBaseBundle bundleResource) {
    List<IBaseResource> resources = BundleUtil.toListOfResources(fhirContext, bundleResource);
    IBaseOperationOutcome operationOutcome = OperationOutcomeUtil.newInstance(fhirContext);
    for (IBaseResource resource : resources) {
      if (resource.getMeta().getProfile().isEmpty()) {
        OperationOutcomeUtil.addIssue(
            fhirContext,
            operationOutcome,
            "warning",
            formatMissingMetaProfileMessage(resource),
            null,
            "invalid");
      } else {
        resource
            .getMeta()
            .getProfile()
            .forEach(
                (p) -> {
                  if (!isValidURL(p.getValueAsString())) {
                    OperationOutcomeUtil.addIssue(
                        fhirContext,
                        operationOutcome,
                        "warning",
                        formatInvalidProfileMessage(resource),
                        null,
                        "invalid");
                  }
                });
      }
    }
    return operationOutcome;
  }

  public IBaseOperationOutcome validateBundleResourcesIdValid(
      FhirContext fhirContext, IBaseBundle bundleResource) {
    List<IBaseResource> resources = BundleUtil.toListOfResources(fhirContext, bundleResource);
    Set<String> existingIds = new HashSet<>();
    Set<String> duplicateIds = new HashSet<>();
    IBaseOperationOutcome operationOutcome = OperationOutcomeUtil.newInstance(fhirContext);
    for (IBaseResource resource : resources) {
      final String resourceId = resource.getIdElement().getIdPart();
      if (StringUtils.isBlank(resourceId)) {
        OperationOutcomeUtil.addIssue(
            fhirContext,
            operationOutcome,
            "error",
            "All resources must have an Id",
            null,
            "invalid");
      } else {
        if (existingIds.contains(resourceId) && !duplicateIds.contains(resourceId)) {
          OperationOutcomeUtil.addIssue(
              fhirContext,
              operationOutcome,
              "error",
              formatUniqueIdViolationMessage(resourceId),
              null,
              "invalid");
          duplicateIds.add(resourceId);
        } else {
          existingIds.add(resourceId);
        }
      }
    }
    return operationOutcome;
  }

  public boolean isSuccessful(FhirContext fhirContext, IBaseOperationOutcome outcome) {
    return outcome == null
        || (!OperationOutcomeUtil.hasIssuesOfSeverity(fhirContext, outcome, "error")
            && !OperationOutcomeUtil.hasIssuesOfSeverity(fhirContext, outcome, "fatal"));
  }

  public IBaseOperationOutcome combineOutcomes(
      FhirContext fhirContext, IBaseOperationOutcome... outcomes) {

    IBaseOperationOutcome finalOo = OperationOutcomeUtil.newInstance(fhirContext);
    RuntimeResourceDefinition finalOoDef = fhirContext.getResourceDefinition(finalOo);
    BaseRuntimeChildDefinition finalOoIssueChild = finalOoDef.getChildByName("issue");
    BaseRuntimeChildDefinition.IMutator mutator = finalOoIssueChild.getMutator();
    Arrays.stream(outcomes)
        .map(
            iBaseOperationOutcome -> {
              // This is the way that HAPI handles managing child data without needing the specific
              // FHIR version (R4 vs R5, etc)
              RuntimeResourceDefinition ooDef =
                  fhirContext.getResourceDefinition(iBaseOperationOutcome);
              BaseRuntimeChildDefinition issueChild = ooDef.getChildByName("issue");
              return issueChild.getAccessor().getValues(iBaseOperationOutcome);
            })
        .flatMap(Collection::stream)
        .forEach(iBase -> mutator.addValue(finalOo, iBase));

    return finalOo;
  }

  private String formatMissingRequiredProfileMessage(IBaseResource resource, final String profile) {
    return String.format(
        "Resource of type [%s] must declare conformance to profile [%s].",
        resource.fhirType(), profile);
  }

  private String formatUniqueIdViolationMessage(final String resourceId) {
    return String.format(
        "All resources in bundle must have unique ID regardless of type. Multiple resources detected with ID [%s]",
        resourceId);
  }

  private String formatMissingMetaProfileMessage(IBaseResource resource) {
    return String.format(
        "Resource of type [%s] is missing the Meta.profile. Resource Id: [%s]. "
            + "Resources missing Meta.profile may cause incorrect results while executing this test case.",
        resource.fhirType(), resource.getIdElement().getIdPart());
  }

  private boolean isValidURL(String url) {
    try {
      new URL(url).toURI();
      return true;
    } catch (MalformedURLException | URISyntaxException e) {
      return false;
    }
  }

  private String formatInvalidProfileMessage(IBaseResource resource) {
    return String.format(
        "Resource of type [%s] has invalid profile. Resource Id: [%s]",
        resource.fhirType(), resource.getIdElement().getIdPart());
  }

  public HapiOperationOutcome invalidErrorOutcome(
      FhirContext fhirContext, IParser parser, String message, String exceptionMessage) {
    IBaseOperationOutcome operationOutcome = OperationOutcomeUtil.newInstance(fhirContext);
    OperationOutcomeUtil.addIssue(
        fhirContext, operationOutcome, "error", exceptionMessage, null, "invalid");
    return encodeOutcome(parser, HttpStatus.BAD_REQUEST.value(), false, message, operationOutcome);
  }

  protected HapiOperationOutcome encodeOutcome(
      IParser parser, int code, boolean successful, String message, IBaseOperationOutcome outcome) {
    try {
      String outcomeString = parser.encodeResourceToString(outcome);
      return HapiOperationOutcome.builder()
          .code(code)
          .message(message)
          .successful(successful)
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (Exception ex) {
      throw new HapiJsonException("An error occurred processing the validation results", ex);
    }
  }
}

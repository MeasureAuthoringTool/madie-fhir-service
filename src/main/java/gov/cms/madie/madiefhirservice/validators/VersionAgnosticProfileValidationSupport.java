package gov.cms.madie.madiefhirservice.validators;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StructureDefinition;

import java.util.Arrays;
import java.util.List;

/** Supports versioned profile canonical resolution by falling back to the unversioned canonical. */
public class VersionAgnosticProfileValidationSupport extends BaseValidationSupport {

  private final List<IValidationSupport> delegates;

  public VersionAgnosticProfileValidationSupport(
      FhirContext fhirContext, IValidationSupport... delegates) {
    super(fhirContext);
    this.delegates = Arrays.asList(delegates);
  }

  @Override
  public StructureDefinition fetchStructureDefinition(String canonicalUrl) {
    if (StringUtils.isBlank(canonicalUrl)) {
      return null;
    }

    StructureDefinition structureDefinition = fetchFromDelegates(canonicalUrl);
    if (structureDefinition != null) {
      return structureDefinition;
    }

    String unversionedCanonicalUrl = StringUtils.substringBefore(canonicalUrl, "|");
    return fetchFromDelegates(unversionedCanonicalUrl);
  }

  @Override
  public String getName() {
    return "Version-Agnostic Custom Profile Validation Support";
  }

  private StructureDefinition fetchFromDelegates(String canonicalUrl) {
    for (IValidationSupport delegate : delegates) {
      IBaseResource resource = delegate.fetchStructureDefinition(canonicalUrl);
      if (resource instanceof StructureDefinition structureDefinition) {
        return structureDefinition;
      }
    }
    return null;
  }
}

package gov.cms.madie.madiefhirservice.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HapiFhirServerTest implements LibraryHelper, ResourceFileUtil {

    @InjectMocks
    private HapiFhirServer hapiFhirServer;

    @Mock
    private FhirContext fhirContext;

    IGenericClient hapiClient;

    Bundle bundle = new Bundle();
    private Library globalCommonsFunctionsLibrary;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(hapiFhirServer, "hapiFhirUrl", "https://hapiFhirTestUrl.com");
        hapiClient = mock(IGenericClient.class, new ReturnsDeepStubs());
        ReflectionTestUtils.setField(hapiFhirServer, "hapiClient", hapiClient);
    }

    @BeforeEach
    void buildLibraryBundle() {

        String fhirHelpersCql = getStringFromTestResource("/includes/FHIRHelpers.cql");
        Library fhirHelpersLibrary = createLibrary(fhirHelpersCql);

        String globalCommonsFunctionsCql = getStringFromTestResource("/includes/GlobalCommonFunctions.cql");
        globalCommonsFunctionsLibrary = createLibrary(globalCommonsFunctionsCql);

        Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
        bundleEntryComponent.setResource(fhirHelpersLibrary);
    }

    @Test
    void getCtx() {
        assertEquals(fhirContext, hapiFhirServer.getFhirContext());
    }

    @Test
    void getHapiClient() {
        assertEquals(hapiClient, hapiFhirServer.getHapiClient());
    }

    @Test
    void testSuccessfullyFindingLibraryResourceInBundle() {
        Optional<Library> optionalLibrary = hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class);
        assertTrue(optionalLibrary.isPresent());
        assertEquals("FHIRHelpers", optionalLibrary.get().getName());
    }

    @Test
    void testMoreThanOneResourceFound() {
        Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
        bundleEntryComponent.setResource(globalCommonsFunctionsLibrary);

        assertEquals(2, bundle.getEntry().size());

        Optional<Library> optionalLibrary = hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class);
        assertTrue(optionalLibrary.isEmpty());
    }

    @Test
    void testResourceFoundIsNotAnInstanceOfLibrary() {
        Bundle bundle = new Bundle();
        Measure measureResource = new Measure();
        Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
        bundleEntryComponent.setResource(measureResource);

        // providing measure instance instead of library
        Optional<Library> optionalLibrary = hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class);
        assertTrue(optionalLibrary.isEmpty());
    }

    @Test
    void createResource() {
        MethodOutcome outcome = new MethodOutcome();

        Object when = hapiClient
          .create()
          .resource(any(Library.class))
          .execute();

        when((Object) when)
          .thenReturn(outcome);

        MethodOutcome methodOutcome = hapiFhirServer.createResource(new Library());
        assertSame(outcome, methodOutcome);
    }
}
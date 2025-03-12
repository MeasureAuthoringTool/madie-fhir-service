package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import gov.cms.madie.packaging.utils.qicore411.PackagingUtilityImpl;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest implements ResourceFileUtil {

  @Mock private FhirContext fhirContext;
  @Mock private HumanReadableService humanReadableService;
  @Mock private MeasureBundleService measureBundleService;

  @InjectMocks private ExportService exportService;

  private Measure madieMeasure;

  private Principal principal;
  private static MockedStatic<PackagingUtilityFactory> factory;

  @BeforeAll
  public static void staticSetup() {
    factory = Mockito.mockStatic(PackagingUtilityFactory.class);
  }

  @AfterAll
  public static void close() {
    factory.close();
  }

  @BeforeEach
  public void setUp() {

    madieMeasure =
        Measure.builder()
            .active(true)
            .ecqmTitle("ExportTest")
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .cqlErrors(false)
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version(new Version(1, 0, 0))
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .model("QI-Core v4.1.1")
            .cqlLibraryName("testCqlLibraryName")
            .build();

    principal = mock(Principal.class);
  }

  @Test
  void testCreateExportsForMeasure() throws IOException {
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(
            any(Measure.class),
            any(Principal.class),
            anyString(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
        .thenReturn(testBundle);
    PackagingUtilityImpl utility = Mockito.mock(PackagingUtilityImpl.class);

    factory.when(() -> PackagingUtilityFactory.getInstance("QI-Core v4.1.1")).thenReturn(utility);
    doReturn("THis is a test".getBytes())
        .when(utility)
        .getZipBundle(any(Bundle.class), any(String.class));

    byte[] result = exportService.createExport(madieMeasure, principal, "Bearer TOKEN");

    assertThat(result, is(equalTo("THis is a test".getBytes())));
  }

  @Test
  void testGenerateExportsWhenWritingFileToZipFailed() throws IOException {

    factory
        .when(() -> PackagingUtilityFactory.getInstance("QI-Core v4.1.1"))
        .thenThrow(
            new InternalServerException(
                "Unexpected error while generating exports for measureID: xyz-p13r-13ert"));

    Exception ex =
        assertThrows(
            RuntimeException.class,
            () -> exportService.createExport(madieMeasure, principal, "Bearer TOKEN"));
    assertThat(
        ex.getMessage(),
        is(equalTo("Unexpected error while generating exports for measureID: xyz-p13r-13ert")));
  }
}

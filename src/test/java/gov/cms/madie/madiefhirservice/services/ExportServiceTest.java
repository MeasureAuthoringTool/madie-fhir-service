package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.packaging.utils.PackagingUtility;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;

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
    PackagingUtility utility = Mockito.mock(PackagingUtility.class);

    factory.when(() -> PackagingUtilityFactory.getInstance("QI-Core v4.1.1")).thenReturn(utility);
    doReturn("This is a test".getBytes())
        .when(utility)
        .getZipBundle(any(Bundle.class), any(String.class));

    byte[] result =
        exportService.createExport(
            madieMeasure,
            principal,
            BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT_PUBLISH,
            CqlCompilerException.ErrorSeverity.Info,
            "******");

    assertThat(result, is(equalTo("This is a test".getBytes())));
    Mockito.verify(measureBundleService)
        .createMeasureBundle(
            madieMeasure,
            principal,
            BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT_PUBLISH,
            "******",
            CqlCompilerException.ErrorSeverity.Info);
  }

  /**
   * Ballot version fix: an included CQL library with a version like "2.0.0-ballot" previously
   * caused export generation to fail, because packaging-utility attempted to convert every FHIR
   * Library resource in the bundle into a MADiE CqlLibrary object, whose version field enforced
   * MADiE's major.minor.patch version format. Now packaging-utility uses a plain-string-version
   * DTO, so the ballot-versioned library should package successfully.
   */
  @Test
  void testCreateExportSucceedsForIncludedLibraryWithBallotVersion() throws IOException {
    // given
    Bundle testBundle =
        MeasureTestHelper.createTestMeasureBundleWithLibrary(
            "IncludedTestLibrary",
            "2.0.0-ballot",
            "library IncludedTestLibrary version '2.0.0-ballot'\n\ncontext Patient");

    when(measureBundleService.createMeasureBundle(
            any(Measure.class),
            any(Principal.class),
            anyString(),
            anyString(),
            eq(CqlCompilerException.ErrorSeverity.Info)))
        .thenReturn(testBundle);

    // Use the real packaging-utility implementation (instead of a mock) so this test actually
    // exercises the ballot version fix end to end.
    factory.when(() -> PackagingUtilityFactory.getInstance("QI-Core v4.1.1")).thenCallRealMethod();

    // when
    byte[] result =
        exportService.createExport(
            madieMeasure,
            principal,
            BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT_PUBLISH,
            CqlCompilerException.ErrorSeverity.Info,
            "******");

    // then
    assertThat(result, is(notNullValue()));
    boolean foundBallotCqlEntry = false;
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(result))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if ("cql/IncludedTestLibrary-2.0.0-ballot.cql".equals(entry.getName())) {
          foundBallotCqlEntry = true;
          break;
        }
      }
    }
    assertThat(foundBallotCqlEntry, is(true));
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
            () ->
                exportService.createExport(
                    madieMeasure,
                    principal,
                    BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT,
                    CqlCompilerException.ErrorSeverity.Info,
                    "******"));
    assertThat(
        ex.getMessage(),
        is(equalTo("Unexpected error while generating exports for measureID: xyz-p13r-13ert")));
  }
}

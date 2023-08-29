package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExportFileNamesUtilTest {

  private Measure measure;
  private TestCase testCase;
  private UUID uuid = UUID.randomUUID();

  @BeforeEach
  void setup() {
    measure = Measure.builder().ecqmTitle("ecqm").version(Version.parse("1.0.000")).build();
    testCase = TestCase.builder().patientId(uuid).series("group").title("test").build();
  }

  @Test
  void getTestCaseExportFileNameTest() {
    assertEquals(
        uuid + "/ecqm-v1.0.000-group-test",
        ExportFileNamesUtil.getTestCaseExportFileName(measure, testCase));
  }

  @Test
  void getTestCaseExportFileNameTestNoGroup() {
    testCase.setSeries(null);
    assertEquals(
        uuid + "/ecqm-v1.0.000-test",
        ExportFileNamesUtil.getTestCaseExportFileName(measure, testCase));
  }

  @Test
  void getTestCaseExportFileNameTestRemoveSpaces() {
    testCase.setTitle(" te st  ");
    testCase.setSeries("group  ");
    assertEquals(
        uuid + "/ecqm-v1.0.000-group-test",
        ExportFileNamesUtil.getTestCaseExportFileName(measure, testCase));
  }

  @Test
  void getTestCaseExportFileNameTestRemoveSpacesNoGroup() {
    testCase.setTitle("test  ");
    testCase.setSeries(null);
    assertEquals(
        uuid + "/ecqm-v1.0.000-test",
        ExportFileNamesUtil.getTestCaseExportFileName(measure, testCase));
  }
}

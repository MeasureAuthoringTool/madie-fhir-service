package gov.cms.madie.madiefhirservice.services;

import java.lang.reflect.InvocationTargetException;
import java.security.Principal;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.packaging.utils.PackagingUtility;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ExportService {

  private final MeasureBundleService measureBundleService;

  public byte[] createExport(Measure madieMeasure, Principal principal, String accessToken) {
    String exportFileName = ExportFileNamesUtil.getExportFileName(madieMeasure);

    Bundle bundle =
        measureBundleService.createMeasureBundle(
            madieMeasure, principal, BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT, accessToken, CqlCompilerException.ErrorSeverity.Info);

    PackagingUtility utility;
    try {
      utility = PackagingUtilityFactory.getInstance(madieMeasure.getModel());
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException e) {
      throw new InternalServerException(
          "Unexpected error while generating exports for measureID: " + madieMeasure.getId());
    }
    return utility.getZipBundle(bundle, exportFileName);
  }
}

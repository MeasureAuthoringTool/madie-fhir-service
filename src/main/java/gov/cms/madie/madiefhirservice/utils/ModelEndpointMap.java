package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.models.common.ModelType;

import java.util.Map;

public class ModelEndpointMap {

  public static final String QICORE_4_1_1 = "4-1-1";
  public static final String QICORE_6_0_0 = "6-0-0";

  public static final Map<String, ModelType> QICORE_VERSION_MODELTYPE_MAP;

  static {
    QICORE_VERSION_MODELTYPE_MAP =
        Map.of(QICORE_4_1_1, ModelType.QI_CORE, QICORE_6_0_0, ModelType.QI_CORE_6_0_0);
  }
}

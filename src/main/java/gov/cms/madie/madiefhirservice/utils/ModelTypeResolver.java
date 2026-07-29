package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.madiefhirservice.exceptions.UnsupportedTypeException;
import gov.cms.madie.models.common.ModelType;

public final class ModelTypeResolver {

  private ModelTypeResolver() {}

  public static ModelType resolve(String model) {
    if (model == null) {
      return ModelType.QI_CORE_6_0_0;
    }

    ModelType modelType = ModelType.byShortValue(model);
    if (modelType == null) {
      modelType = ModelEndpointMap.QICORE_VERSION_MODELTYPE_MAP.get(model);
    }
    if (modelType == null) {
      throw new UnsupportedTypeException(ModelTypeResolver.class.getName(), model);
    }
    return modelType;
  }
}

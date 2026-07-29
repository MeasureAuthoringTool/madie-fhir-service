package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.madiefhirservice.exceptions.UnsupportedTypeException;
import gov.cms.madie.models.common.ModelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelTypeResolverTest {

  @Test
  void testResolveReturnsDefaultModelWhenModelIsNull() {
    // given
    String model = null;

    // when
    ModelType resolvedModel = ModelTypeResolver.resolve(model);

    // then
    assertEquals(ModelType.QI_CORE_6_0_0, resolvedModel);
  }

  @Test
  void testResolveReturnsModelForShortValue() {
    // given
    String model = ModelType.QI_CORE.getShortValue();

    // when
    ModelType resolvedModel = ModelTypeResolver.resolve(model);

    // then
    assertEquals(ModelType.QI_CORE, resolvedModel);
  }

  @Test
  void testResolveReturnsModelForQiCoreVersion() {
    // given
    String model =
        ModelEndpointMap.QICORE_VERSION_MODELTYPE_MAP.keySet().stream()
            .filter(
                version ->
                    ModelEndpointMap.QICORE_VERSION_MODELTYPE_MAP.get(version) == ModelType.QI_CORE)
            .findFirst()
            .orElseThrow();

    // when
    ModelType resolvedModel = ModelTypeResolver.resolve(model);

    // then
    assertEquals(ModelType.QI_CORE, resolvedModel);
  }

  @Test
  void testResolveThrowsUnsupportedTypeExceptionForUnknownModel() {
    // given
    String model = "unknown-model";

    // when / then
    assertThrows(UnsupportedTypeException.class, () -> ModelTypeResolver.resolve(model));
  }
}

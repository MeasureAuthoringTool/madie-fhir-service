package gov.cms.madie.madiefhirservice.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class TranslatorConfigUtilTest {

  @Test
  void testGetTranslationConfigs() {
    String elmJson =
        "{\"library\":{\"annotation\":[{\"type\":\"CqlToElmInfo\",\"translatorVersion\":\"5.0.0\",\"translatorOptions\":\"EnableLocators,DisableListPromotion,EnableResultTypes\",\"signatureLevel\":\"Overloads\"},{\"type\":\"Annotation\"}]}}";

    TranslatorConfigUtil.TranslatorConfig translatorConfig =
        TranslatorConfigUtil.getTranslationConfigs(elmJson);

    assertThat(translatorConfig.translatorVersion(), is(equalTo("5.0.0")));
    assertThat(
        translatorConfig.translatorOptions(),
        is(equalTo(List.of("EnableLocators", "DisableListPromotion", "EnableResultTypes"))));
    assertThat(translatorConfig.signatureLevel(), is(equalTo("Overloads")));
  }

  @Test
  void testGetTranslationConfigsWhenNoCqlToElmInfo() {
    String elmJson = "{\"library\":{\"annotation\":[{\"type\":\"Annotation\"}]}}";

    TranslatorConfigUtil.TranslatorConfig translatorConfig =
        TranslatorConfigUtil.getTranslationConfigs(elmJson);

    assertThat(translatorConfig.translatorVersion(), is(nullValue()));
    assertThat(translatorConfig.translatorOptions(), is(equalTo(List.of())));
    assertThat(translatorConfig.signatureLevel(), is(nullValue()));
  }
}

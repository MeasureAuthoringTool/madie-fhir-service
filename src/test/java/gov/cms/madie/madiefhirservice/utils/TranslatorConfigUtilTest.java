package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.madiefhirservice.constants.UriConstants;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
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

  @Test
  void testGetTranslationConfigsWhenBlankJson() {
    TranslatorConfigUtil.TranslatorConfig translatorConfig =
        TranslatorConfigUtil.getTranslationConfigs("   ");

    assertThat(translatorConfig.translatorVersion(), is(nullValue()));
    assertThat(translatorConfig.translatorOptions(), is(equalTo(List.of())));
    assertThat(translatorConfig.signatureLevel(), is(nullValue()));
  }

  @Test
  void testGetTranslationConfigsWhenAnnotationIsNotArray() {
    String elmJson = "{\"library\":{\"annotation\":{\"type\":\"CqlToElmInfo\"}}}";

    TranslatorConfigUtil.TranslatorConfig translatorConfig =
        TranslatorConfigUtil.getTranslationConfigs(elmJson);

    assertThat(translatorConfig.translatorVersion(), is(nullValue()));
    assertThat(translatorConfig.translatorOptions(), is(equalTo(List.of())));
    assertThat(translatorConfig.signatureLevel(), is(nullValue()));
  }

  @Test
  void testGetTranslationConfigsWhenInvalidJson() {
    TranslatorConfigUtil.TranslatorConfig translatorConfig =
        TranslatorConfigUtil.getTranslationConfigs("{not valid json}");

    assertThat(translatorConfig.translatorVersion(), is(nullValue()));
    assertThat(translatorConfig.translatorOptions(), is(equalTo(List.of())));
    assertThat(translatorConfig.signatureLevel(), is(nullValue()));
  }

  @Test
  void testGetTranslationConfigsTrimAndParseOptions() {
    String elmJson =
        "{\"library\":{\"annotation\":[{\"type\":\"CqlToElmInfo\","
            + "\"translatorVersion\":\" 6.1.0 \","
            + "\"translatorOptions\":\" EnableLocators, ,DisableListDemotion ,, \","
            + "\"signatureLevel\":\"  Differing \"}]}}";

    TranslatorConfigUtil.TranslatorConfig translatorConfig =
        TranslatorConfigUtil.getTranslationConfigs(elmJson);

    assertThat(translatorConfig.translatorVersion(), is(equalTo("6.1.0")));
    assertThat(
        translatorConfig.translatorOptions(),
        is(equalTo(List.of("EnableLocators", "DisableListDemotion"))));
    assertThat(translatorConfig.signatureLevel(), is(equalTo("Differing")));
  }

  @Test
  void testGetCqlOptionExtension() {
    Extension extension = TranslatorConfigUtil.getCqlOptionExtension();

    assertThat(extension.getUrl(), is(equalTo(UriConstants.Library.CQL_OPTIONS_URL)));
    assertThat(extension.getValue(), is(instanceOf(Reference.class)));
    assertThat(((Reference) extension.getValue()).getReference(), is(equalTo("#options")));
  }

  @Test
  void testGetCqlOptionParametersWithTranslatorInfo() {
    String elmJson =
        "{\"library\":{\"annotation\":[{\"type\":\"CqlToElmInfo\","
            + "\"translatorVersion\":\"5.0.0\","
            + "\"translatorOptions\":\"EnableLocators,DisableListPromotion\","
            + "\"signatureLevel\":\"Overloads\"}]}}";

    Parameters parameters = TranslatorConfigUtil.getCqlOptionParameters(elmJson);

    assertThat(parameters.getId(), is(equalTo("options")));
    assertThat(parameters.getParameter().size(), is(equalTo(4)));
    assertThat(parameters.getParameter().get(0).getName(), is(equalTo("translatorVersion")));
    assertThat(parameters.getParameter().get(0).getValue().primitiveValue(), is(equalTo("5.0.0")));
    assertThat(parameters.getParameter().get(1).getName(), is(equalTo("option")));
    assertThat(
        parameters.getParameter().get(1).getValue().primitiveValue(),
        is(equalTo("EnableLocators")));
    assertThat(parameters.getParameter().get(2).getName(), is(equalTo("option")));
    assertThat(
        parameters.getParameter().get(2).getValue().primitiveValue(),
        is(equalTo("DisableListPromotion")));
    assertThat(parameters.getParameter().get(3).getName(), is(equalTo("signatureLevel")));
    assertThat(
        parameters.getParameter().get(3).getValue().primitiveValue(), is(equalTo("Overloads")));
  }

  @Test
  void testGetCqlOptionParametersWhenNullElmJson() {
    Parameters parameters = TranslatorConfigUtil.getCqlOptionParameters(null);

    assertThat(parameters.getId(), is(equalTo("options")));
    assertThat(parameters.getParameter().size(), is(equalTo(0)));
  }
}

package gov.cms.madie.madiefhirservice.factories;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.madie.madiefhirservice.exceptions.UnsupportedTypeException;
import gov.cms.madie.models.common.ModelType;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelAwareFhirFactoryTest {

  @Mock FhirValidator qicoreNpmFhirValidator;
  @Mock FhirValidator qicore6NpmFhirValidator;

  FhirContext qicoreFhirContext;
  FhirContext qicore6FhirContext;

  @Mock private Map<String, FhirContext> fhirContextMap;

  @Mock private Map<String, FhirValidator> fhirValidatorMap;

  // Not using @InjectMocks because Mockito seems to have issues injecting two maps
  // especially when running unit tests with coverage
  private ModelAwareFhirFactory modelAwareFhirFactory;

  @BeforeEach
  public void setUp() {
    qicoreFhirContext = FhirContext.forR4();
    qicore6FhirContext = FhirContext.forR4();

    // manually instantiating because test fail when running with coverage
    modelAwareFhirFactory =
        Mockito.spy(new ModelAwareFhirFactory(fhirValidatorMap, fhirContextMap));
  }

  @Test
  public void testGetValidatorForModelReturnsValidator() {
    // given
    ModelType modelType = ModelType.QI_CORE;
    String lookup = modelType.getShortValue() + "NpmFhirValidator";
    when(fhirValidatorMap.get(anyString())).thenReturn(qicoreNpmFhirValidator);

    // when
    FhirValidator output = modelAwareFhirFactory.getValidatorForModel(modelType);

    // then
    assertThat(output, is(equalTo(qicoreNpmFhirValidator)));
    verify(fhirValidatorMap).get(lookup);
  }

  @Test
  public void testGetValidatorForModelThrowsUnsupportedTypeException() {
    ModelType modelType = ModelType.QDM_5_6;
    String lookup = modelType.getShortValue() + "NpmFhirValidator";
    when(fhirValidatorMap.get(anyString())).thenReturn(null);

    // when
    assertThrows(
        UnsupportedTypeException.class,
        () -> modelAwareFhirFactory.getValidatorForModel(modelType));

    // then
    verify(fhirValidatorMap).get(lookup);
  }

  @Test
  public void testGetContextForModelReturnsContext() {
    // given
    ModelType modelType = ModelType.QI_CORE;
    String lookup = modelType.getShortValue() + "FhirContext";
    when(fhirContextMap.get(anyString())).thenReturn(qicoreFhirContext);

    // when
    FhirContext output = modelAwareFhirFactory.getContextForModel(modelType);

    // then
    assertThat(output, is(equalTo(qicoreFhirContext)));
    verify(fhirContextMap).get(lookup);
  }

  @Test
  public void testGetContextForModelThrowsUnsupportedTypeException() {
    // given
    ModelType modelType = ModelType.QDM_5_6;
    String lookup = modelType.getShortValue() + "FhirContext";
    when(fhirContextMap.get(anyString())).thenReturn(null);

    // when
    assertThrows(
        UnsupportedTypeException.class, () -> modelAwareFhirFactory.getContextForModel(modelType));

    // then
    verify(fhirContextMap).get(lookup);
  }

  @Test
  public void testGetJsonParserForModelReturnsParser() {
    // given
    ModelType modelType = ModelType.QI_CORE;
    IParser mockParser = Mockito.mock(IParser.class);
    String lookup = modelType.getShortValue() + "FhirContext";
    FhirContext mockContext = Mockito.mock(FhirContext.class);

    when(fhirContextMap.get(anyString())).thenReturn(mockContext);
    when(mockContext.newJsonParser()).thenReturn(mockParser);
    when(mockParser.setParserErrorHandler(any(StrictErrorHandler.class))).thenReturn(mockParser);
    when(mockParser.setPrettyPrint(true)).thenReturn(mockParser);

    // when
    IParser output = modelAwareFhirFactory.getJsonParserForModel(modelType);

    assertThat(output, is(equalTo(mockParser)));
    verify(fhirContextMap).get(lookup);
    verify(mockContext).newJsonParser();
    verify(mockParser).setParserErrorHandler(any(StrictErrorHandler.class));
    verify(mockParser).setPrettyPrint(true);
  }

  @Test
  public void testGetJsonParserForModelThrowsUnsupportedTypeException() {
    // given
    ModelType modelType = ModelType.QDM_5_6;
    String lookup = modelType.getShortValue() + "FhirContext";

    when(fhirContextMap.get(anyString())).thenReturn(null);

    // when
    assertThrows(
        UnsupportedTypeException.class,
        () -> {
          modelAwareFhirFactory.getJsonParserForModel(modelType);
        });

    // then
    verify(fhirContextMap).get(lookup);
  }

  @Test
  public void testParseForModelQICoreReturnsBundle() {
    // given
    ModelType modelType = ModelType.QI_CORE;
    String bundleString = "{ \"resourceType\" : \"Bundle\", \"entry\": []}";
    IParser mockParser = Mockito.mock(IParser.class);
    Bundle mockBundle = Mockito.mock(Bundle.class);

    when(fhirContextMap.get(anyString())).thenReturn(qicoreFhirContext);
    when(modelAwareFhirFactory.getJsonParserForModel(modelType)).thenReturn(mockParser);
    when(mockParser.parseResource(Bundle.class, bundleString)).thenReturn(mockBundle);

    // when
    IBaseBundle output = modelAwareFhirFactory.parseForModel(modelType, bundleString);

    // then
    assertThat(output, is(equalTo(mockBundle)));
    verify(mockParser).parseResource(Bundle.class, bundleString);
  }

  @Test
  public void testParseForModelQDMThrowsUnsupportedTypeException() {
    // given
    ModelType modelType = ModelType.QDM_5_6;
    String bundleString = "{ \"resourceType\" : \"Bundle\", \"entry\": []}";
    IParser mockParser = Mockito.mock(IParser.class);

    when(fhirContextMap.get(anyString())).thenReturn(qicoreFhirContext);
    when(modelAwareFhirFactory.getJsonParserForModel(modelType)).thenReturn(mockParser);

    // when
    assertThrows(
        UnsupportedTypeException.class,
        () -> modelAwareFhirFactory.parseForModel(modelType, bundleString));
  }
}

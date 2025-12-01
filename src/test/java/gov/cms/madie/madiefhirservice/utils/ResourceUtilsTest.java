package gov.cms.madie.madiefhirservice.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;

@ExtendWith(MockitoExtension.class)
public class ResourceUtilsTest {

  @Test
  public void testReadDataThrowsInternalServerException() {
    assertThrows(InternalServerException.class, () -> ResourceUtils.getData(null));
  }
}

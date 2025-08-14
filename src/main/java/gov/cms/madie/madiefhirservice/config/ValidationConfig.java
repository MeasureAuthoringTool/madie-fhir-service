package gov.cms.madie.madiefhirservice.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
public class ValidationConfig {

  @Value("${validation.terminology.validateDisplay:false}")
  private boolean validateDisplay;
}

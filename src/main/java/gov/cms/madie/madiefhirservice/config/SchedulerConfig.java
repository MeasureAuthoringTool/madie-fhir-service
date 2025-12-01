package gov.cms.madie.madiefhirservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
public class SchedulerConfig {

  @CacheEvict(value = "libraries", allEntries = true)
  @Scheduled(fixedRateString = "${caching.spring.libraries.ttlMillis}")
  public void emptyLibrariesCache() {
    log.info("emptying libraries cache");
  }
}

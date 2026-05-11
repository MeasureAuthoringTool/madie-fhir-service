package gov.cms.madie.madiefhirservice.resources;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final CacheManager cacheManager;

  @DeleteMapping("/cache/evict")
  @PreAuthorize("hasRole('MADIE-ADMIN')")
  public ResponseEntity<List<String>> evictAllCaches(Principal principal) {
    List<String> evictedCaches = new ArrayList<>(cacheManager.getCacheNames());
    log.info("Admin user [{}] is evicting all caches: {}", principal.getName(), evictedCaches);
    evictedCaches.forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    return ResponseEntity.ok(evictedCaches);
  }
}

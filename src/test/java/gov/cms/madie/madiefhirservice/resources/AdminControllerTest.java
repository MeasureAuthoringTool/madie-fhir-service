package gov.cms.madie.madiefhirservice.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock private CacheManager cacheManager;
  @Mock private Cache cache;
  @InjectMocks private AdminController adminController;

  private static final String TEST_USER = "test.admin.user";

  @Test
  void testEvictAllCachesReturnsOkWithCacheNames() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER);
    when(cacheManager.getCacheNames()).thenReturn(Set.of("libraries"));
    when(cacheManager.getCache(anyString())).thenReturn(cache);

    ResponseEntity<List<String>> response = adminController.evictAllCaches(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertTrue(response.getBody().contains("libraries"));
    verify(cacheManager, times(1)).getCache(anyString());
    verify(cache, times(1)).clear();
  }
}

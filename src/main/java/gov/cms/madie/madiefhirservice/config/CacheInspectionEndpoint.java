package gov.cms.madie.madiefhirservice.config;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom actuator endpoint to inspect cache details including entry count, estimated memory usage,
 * and hit/miss statistics. Supports ConcurrentMapCache, CaffeineCache, and any unknown cache type
 * with a generic fallback. Accessible at /actuator/cache-inspection
 */
@Slf4j
@Component
@Endpoint(id = "cache-inspection")
@RequiredArgsConstructor
public class CacheInspectionEndpoint {

  private final CacheManager cacheManager;

  @ReadOperation
  public Map<String, Object> inspect() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("cacheManagerType", cacheManager.getClass().getSimpleName());

    for (String cacheName : cacheManager.getCacheNames()) {
      Cache springCache = cacheManager.getCache(cacheName);
      if (springCache == null) {
        result.put(cacheName, Map.of("type", "N/A", "message", "Cache not found"));
        continue;
      }

      Map<String, Object> cacheInfo = new LinkedHashMap<>();

      if (springCache instanceof CaffeineCache caffeineCache) {
        inspectCaffeineCache(caffeineCache, cacheInfo);
      } else if (springCache instanceof ConcurrentMapCache concurrentMapCache) {
        inspectConcurrentMapCache(concurrentMapCache, cacheInfo);
      } else {
        inspectGenericCache(springCache, cacheInfo);
      }

      result.put(cacheName, cacheInfo);
    }

    return result;
  }

  private void inspectCaffeineCache(CaffeineCache caffeineCache, Map<String, Object> cacheInfo) {
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
        caffeineCache.getNativeCache();
    Map<Object, Object> cacheEntries = nativeCache.asMap();

    cacheInfo.put("type", "CaffeineCache");
    cacheInfo.put("entryCount", nativeCache.estimatedSize());

    // Stats are only available if Caffeine was built with recordStats()
    try {
      CacheStats stats = nativeCache.stats();
      cacheInfo.put("hitCount", stats.hitCount());
      cacheInfo.put("missCount", stats.missCount());
      long totalRequests = stats.hitCount() + stats.missCount();
      cacheInfo.put(
          "hitRate", totalRequests > 0 ? String.format("%.2f%%", stats.hitRate() * 100) : "N/A");
      cacheInfo.put("evictionCount", stats.evictionCount());
      cacheInfo.put("loadCount", stats.loadCount());
      cacheInfo.put(
          "averageLoadPenaltyMs", String.format("%.2f", stats.averageLoadPenalty() / 1_000_000.0));
    } catch (Exception e) {
      cacheInfo.put("stats", "Stats recording not enabled for this cache");
    }

    addMemoryEstimate(cacheEntries, cacheInfo);
    addKeys(cacheEntries, cacheInfo);
  }

  private void inspectConcurrentMapCache(
      ConcurrentMapCache concurrentMapCache, Map<String, Object> cacheInfo) {
    ConcurrentMap<Object, Object> nativeCache = concurrentMapCache.getNativeCache();

    cacheInfo.put("type", "ConcurrentMapCache");
    cacheInfo.put("entryCount", nativeCache.size());
    cacheInfo.put(
        "stats",
        "ConcurrentMapCache does not track hit/miss statistics. "
            + "Consider switching to CaffeineCacheManager for detailed metrics.");

    addMemoryEstimate(nativeCache, cacheInfo);
    addKeys(nativeCache, cacheInfo);
  }

  private void inspectGenericCache(Cache springCache, Map<String, Object> cacheInfo) {
    cacheInfo.put("type", springCache.getClass().getSimpleName());

    Object nativeCache = springCache.getNativeCache();
    cacheInfo.put("nativeCacheType", nativeCache.getClass().getName());

    // Attempt to extract entries if the native cache is a Map
    if (nativeCache instanceof Map<?, ?> mapCache) {
      cacheInfo.put("entryCount", mapCache.size());
      Map<Object, Object> entries = new LinkedHashMap<>(mapCache);
      addMemoryEstimate(entries, cacheInfo);
      addKeys(entries, cacheInfo);
    } else {
      cacheInfo.put("entryCount", "Unknown (native cache is not a Map)");
      cacheInfo.put(
          "message",
          "This cache type is not fully supported for inspection. "
              + "Entry count and memory estimation are unavailable.");
    }
  }

  private void addMemoryEstimate(Map<?, ?> entries, Map<String, Object> cacheInfo) {
    long estimatedBytes = 0;
    for (Map.Entry<?, ?> entry : entries.entrySet()) {
      estimatedBytes += estimateObjectSize(entry.getKey());
      estimatedBytes += estimateObjectSize(entry.getValue());
    }
    cacheInfo.put("estimatedMemoryBytes", estimatedBytes);
    cacheInfo.put("estimatedMemoryKB", estimatedBytes / 1024);
    cacheInfo.put("estimatedMemoryMB", estimatedBytes / (1024 * 1024));
  }

  private void addKeys(Map<?, ?> entries, Map<String, Object> cacheInfo) {
    List<String> keys = entries.keySet().stream().map(Object::toString).toList();
    cacheInfo.put("keys", keys);
  }

  /**
   * Estimates object size via Java serialization. This is an approximation — actual heap usage may
   * differ due to object headers, padding, and references. If the object is not serializable, falls
   * back to toString() length as a rough estimate.
   */
  private long estimateObjectSize(Object obj) {
    if (obj == null) {
      return 0;
    }
    if (obj instanceof Serializable) {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(obj);
        oos.flush();
        return baos.size();
      } catch (Exception e) {
        log.debug("Cannot serialize for size estimation: {}", e.getMessage());
      }
    }
    // Rough fallback: toString length * 2 (chars are 2 bytes in Java)
    return (long) obj.toString().length() * 2;
  }
}

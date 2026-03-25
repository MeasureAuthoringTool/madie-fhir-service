package gov.cms.madie.madiefhirservice.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheInspectionEndpointTest {

  private CacheInspectionEndpoint cacheInspectionEndpoint;

  @Mock private CacheManager mockCacheManager;

  @BeforeEach
  void setUp() {
    cacheInspectionEndpoint = new CacheInspectionEndpoint(mockCacheManager);
  }

  // ==================== inspect() method tests ====================

  @Test
  void inspectShouldReturnCacheManagerType() {
    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList());

    Map<String, Object> result = cacheInspectionEndpoint.inspect();

    assertThat(result).isNotNull();
    assertThat(result.get("cacheManagerType")).isNotNull();
    assertThat(result).containsKey("cacheManagerType");
  }

  @Test
  void inspectShouldHandleNoCaches() {
    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList());

    Map<String, Object> result = cacheInspectionEndpoint.inspect();

    assertThat(result).isNotNull();
    assertThat(result).hasSize(1); // Only cacheManagerType
  }

  @Test
  void inspectShouldHandleNullCache() {
    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("nullCache"));
    when(mockCacheManager.getCache("nullCache")).thenReturn(null);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();

    assertThat(result).containsKey("nullCache");
    Map<String, Object> nullCacheInfo = (Map<String, Object>) result.get("nullCache");
    assertThat(nullCacheInfo)
        .containsEntry("type", "N/A")
        .containsEntry("message", "Cache not found");
  }

  @Test
  void inspectShouldProcessMultipleCaches() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    CaffeineCache caffeineCache = new CaffeineCache("caffeineCache", nativeCaffeine);
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("concurrentCache");

    when(mockCacheManager.getCacheNames())
        .thenReturn(Arrays.asList("caffeineCache", "concurrentCache"));
    when(mockCacheManager.getCache("caffeineCache")).thenReturn(caffeineCache);
    when(mockCacheManager.getCache("concurrentCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();

    assertThat(result).containsKeys("cacheManagerType", "caffeineCache", "concurrentCache");
  }

  // ==================== inspectCaffeineCache() tests ====================

  @Test
  void inspectCaffeineCacheShouldReturnCorrectTypeAndEntryCount() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key1", "value1");
    nativeCaffeine.put("key2", "value2");
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    assertThat(cacheInfo).containsEntry("type", "CaffeineCache");
    assertThat(cacheInfo.get("entryCount")).isEqualTo(2L);
  }

  @Test
  void inspectCaffeineCacheShouldIncludeStats() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key1", "value1");
    nativeCaffeine.getIfPresent("key1"); // Hit
    nativeCaffeine.getIfPresent("key2"); // Miss
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    assertThat(cacheInfo)
        .containsKeys(
            "hitCount",
            "missCount",
            "hitRate",
            "evictionCount",
            "loadCount",
            "averageLoadPenaltyMs");
    assertThat(cacheInfo.get("hitCount")).isEqualTo(1L);
    assertThat(cacheInfo.get("missCount")).isEqualTo(1L);
    assertThat(cacheInfo.get("hitRate")).asString().matches("\\d+\\.\\d+%");
  }

  @Test
  void inspectCaffeineCacheShouldEstimateMemory() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key", "value");
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    assertThat(cacheInfo)
        .containsKeys("estimatedMemoryBytes", "estimatedMemoryKB", "estimatedMemoryMB");
    assertThat((Long) cacheInfo.get("estimatedMemoryBytes")).isGreaterThan(0);
  }

  @Test
  void inspectCaffeineCacheShouldIncludeKeys() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key1", "value1");
    nativeCaffeine.put("key2", "value2");
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).hasSize(2).contains("key1", "key2");
  }

  @Test
  void inspectCaffeineCacheShouldAlwaysProvideStats() {
    Cache<Object, Object> nativeCaffeine =
        Caffeine.newBuilder().build(); // Stats are always available
    nativeCaffeine.put("key1", "value1");
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    // Caffeine provides stats by default
    assertThat(cacheInfo).containsKeys("hitCount", "missCount", "hitRate", "evictionCount");
  }

  @Test
  void inspectCaffeineCacheWithEmptyCacheShouldReturnZeroEntries() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    CaffeineCache caffeineCache = new CaffeineCache("emptyCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("emptyCache"));
    when(mockCacheManager.getCache("emptyCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("emptyCache");

    assertThat(cacheInfo.get("entryCount")).isEqualTo(0L);
    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).isEmpty();
  }

  // ==================== inspectConcurrentMapCache() tests ====================

  @Test
  void inspectConcurrentMapCacheShouldReturnCorrectType() {
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("testCache");
    concurrentMapCache.put("key1", "value1");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    assertThat(cacheInfo).containsEntry("type", "ConcurrentMapCache");
  }

  @Test
  void inspectConcurrentMapCacheShouldReturnEntryCount() {
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("testCache");
    concurrentMapCache.put("key1", "value1");
    concurrentMapCache.put("key2", "value2");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    assertThat(cacheInfo.get("entryCount")).isEqualTo(2);
  }

  @Test
  void inspectConcurrentMapCacheShouldIndicateNoStatsAvailable() {
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("testCache");
    concurrentMapCache.put("key1", "value1");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    assertThat(cacheInfo.get("stats"))
        .asString()
        .contains("ConcurrentMapCache does not track hit/miss statistics");
  }

  @Test
  void inspectConcurrentMapCacheShouldEstimateMemory() {
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("testCache");
    concurrentMapCache.put("key", "value");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    assertThat(cacheInfo)
        .containsKeys("estimatedMemoryBytes", "estimatedMemoryKB", "estimatedMemoryMB");
    assertThat((Long) cacheInfo.get("estimatedMemoryBytes")).isGreaterThan(0);
  }

  @Test
  void inspectConcurrentMapCacheShouldIncludeKeys() {
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("testCache");
    concurrentMapCache.put("key1", "value1");
    concurrentMapCache.put("key2", "value2");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).hasSize(2).contains("key1", "key2");
  }

  @Test
  void inspectConcurrentMapCacheWithEmptyCacheShouldReturnZeroEntries() {
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("emptyCache");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("emptyCache"));
    when(mockCacheManager.getCache("emptyCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("emptyCache");

    assertThat(cacheInfo.get("entryCount")).isEqualTo(0);
    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).isEmpty();
  }

  // ==================== inspectGenericCache() tests ====================

  @Test
  void inspectGenericCacheShouldReturnCacheType() {
    org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
    Map<Object, Object> nativeMap = new HashMap<>();
    nativeMap.put("key1", "value1");

    when(mockCache.getNativeCache()).thenReturn(nativeMap);
    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("genericCache"));
    when(mockCacheManager.getCache("genericCache")).thenReturn(mockCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("genericCache");

    assertThat(cacheInfo).containsKey("type");
    assertThat(cacheInfo).containsKey("nativeCacheType");
  }

  @Test
  void inspectGenericCacheWithMapNativeTypeShouldReturnEntryCount() {
    org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
    Map<Object, Object> nativeMap = new HashMap<>();
    nativeMap.put("key1", "value1");
    nativeMap.put("key2", "value2");

    when(mockCache.getNativeCache()).thenReturn(nativeMap);
    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("genericCache"));
    when(mockCacheManager.getCache("genericCache")).thenReturn(mockCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("genericCache");

    assertThat(cacheInfo.get("entryCount")).isEqualTo(2);
  }

  @Test
  void inspectGenericCacheWithMapNativeTypeShouldEstimateMemory() {
    org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
    Map<Object, Object> nativeMap = new HashMap<>();
    nativeMap.put("key", "value");

    when(mockCache.getNativeCache()).thenReturn(nativeMap);
    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("genericCache"));
    when(mockCacheManager.getCache("genericCache")).thenReturn(mockCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("genericCache");

    assertThat(cacheInfo)
        .containsKeys("estimatedMemoryBytes", "estimatedMemoryKB", "estimatedMemoryMB");
  }

  @Test
  void inspectGenericCacheWithNonMapNativeTypeShouldHandleGracefully() {
    org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
    Object nonMapNativeCache = new Object();

    when(mockCache.getNativeCache()).thenReturn(nonMapNativeCache);
    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("genericCache"));
    when(mockCacheManager.getCache("genericCache")).thenReturn(mockCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("genericCache");

    assertThat(cacheInfo.get("entryCount")).isEqualTo("Unknown (native cache is not a Map)");
    assertThat(cacheInfo.get("message"))
        .asString()
        .contains("This cache type is not fully supported for inspection");
  }

  // ==================== addMemoryEstimate() tests ====================

  @Test
  void addMemoryEstimateShouldCalculateCorrectly() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key1", new SerializableTestObject("value1"));
    nativeCaffeine.put("key2", new SerializableTestObject("value2"));
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    long bytes = (Long) cacheInfo.get("estimatedMemoryBytes");
    long kb = (Long) cacheInfo.get("estimatedMemoryKB");
    long mb = (Long) cacheInfo.get("estimatedMemoryMB");

    assertThat(bytes).isGreaterThan(0);
    assertThat(kb).isEqualTo(bytes / 1024);
    assertThat(mb).isEqualTo(bytes / (1024 * 1024));
  }

  @Test
  void addMemoryEstimateShouldHandleNullValues() {
    // ConcurrentMapCache supports null values
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("testCache");
    concurrentMapCache.put("nullKey", null);
    concurrentMapCache.put("key", "value");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    // Should not throw exception and should calculate memory
    assertThat(cacheInfo).containsKeys("estimatedMemoryBytes");
  }

  @Test
  void addMemoryEstimateForEmptyCacheShouldReturnZero() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    CaffeineCache caffeineCache = new CaffeineCache("emptyCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("emptyCache"));
    when(mockCacheManager.getCache("emptyCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("emptyCache");

    assertThat(cacheInfo.get("estimatedMemoryBytes")).isEqualTo(0L);
    assertThat(cacheInfo.get("estimatedMemoryKB")).isEqualTo(0L);
    assertThat(cacheInfo.get("estimatedMemoryMB")).isEqualTo(0L);
  }

  // ==================== addKeys() tests ====================

  @Test
  void addKeysShouldExtractAllKeys() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key1", "value1");
    nativeCaffeine.put("key2", "value2");
    nativeCaffeine.put("key3", "value3");
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).hasSize(3).contains("key1", "key2", "key3");
  }

  @Test
  void addKeysForEmptyCacheShouldReturnEmptyList() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    CaffeineCache caffeineCache = new CaffeineCache("emptyCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("emptyCache"));
    when(mockCacheManager.getCache("emptyCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("emptyCache");

    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).isEmpty();
  }

  @Test
  void addKeysShouldConvertKeysToString() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put(123, "value1");
    nativeCaffeine.put(456, "value2");
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).hasSize(2).contains("123", "456");
  }

  // ==================== estimateObjectSize() tests ====================

  @Test
  void estimateObjectSizeShouldHandleNullObject() {
    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("testCache");
    concurrentMapCache.put("nullKey", null);
    concurrentMapCache.put("key", "value");

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    // Should not throw exception and should calculate memory
    assertThat(cacheInfo).containsKeys("estimatedMemoryBytes");
  }

  @Test
  void estimateObjectSizeShouldHandleSerializableObject() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key", new SerializableTestObject("test"));
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    long bytes = (Long) cacheInfo.get("estimatedMemoryBytes");
    assertThat(bytes).isGreaterThan(0);
  }

  @Test
  void estimateObjectSizeShouldFallbackToStringLengthForNonSerializable() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key", new NonSerializableTestObject("test"));
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    long bytes = (Long) cacheInfo.get("estimatedMemoryBytes");
    // Should use fallback (toString.length() * 2)
    assertThat(bytes).isGreaterThan(0);
  }

  @Test
  void estimateObjectSizeShouldHandleStringObject() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    String testString = "This is a test string";
    nativeCaffeine.put("key", testString);
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    long bytes = (Long) cacheInfo.get("estimatedMemoryBytes");
    assertThat(bytes).isGreaterThan(0);
  }

  // ==================== Edge cases and integration tests ====================

  @Test
  void inspectShouldHandleMultipleCachesOfDifferentTypes() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("cf_key", "cf_value");
    CaffeineCache caffeineCache = new CaffeineCache("caffeineCache", nativeCaffeine);

    ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache("concurrentCache");
    concurrentMapCache.put("cm_key", "cm_value");

    when(mockCacheManager.getCacheNames())
        .thenReturn(Arrays.asList("caffeineCache", "concurrentCache"));
    when(mockCacheManager.getCache("caffeineCache")).thenReturn(caffeineCache);
    when(mockCacheManager.getCache("concurrentCache")).thenReturn(concurrentMapCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();

    assertThat(result).containsKeys("cacheManagerType", "caffeineCache", "concurrentCache");
    Map<String, Object> caffeineInfo = (Map<String, Object>) result.get("caffeineCache");
    Map<String, Object> concurrentInfo = (Map<String, Object>) result.get("concurrentCache");

    assertThat(caffeineInfo.get("type")).isEqualTo("CaffeineCache");
    assertThat(concurrentInfo.get("type")).isEqualTo("ConcurrentMapCache");
  }

  @Test
  void inspectShouldMaintainInsertionOrder() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("z_key", "z_value");
    nativeCaffeine.put("a_key", "a_value");
    nativeCaffeine.put("m_key", "m_value");
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    // Verify result is LinkedHashMap (maintains order)
    assertThat(result).isInstanceOf(java.util.LinkedHashMap.class);
    assertThat(cacheInfo).isInstanceOf(java.util.LinkedHashMap.class);
  }

  @Test
  void inspectCaffeineCacheShouldHandleLargeNumberOfEntries() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    for (int i = 0; i < 1000; i++) {
      nativeCaffeine.put("key" + i, "value" + i);
    }
    CaffeineCache caffeineCache = new CaffeineCache("largeCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("largeCache"));
    when(mockCacheManager.getCache("largeCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("largeCache");

    assertThat(cacheInfo.get("entryCount")).isEqualTo(1000L);
    List<String> keys = (List<String>) cacheInfo.get("keys");
    assertThat(keys).hasSize(1000);
  }

  @Test
  void inspectShouldComputeHitRateCorrectly() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    nativeCaffeine.put("key1", "value1");
    // 3 hits
    nativeCaffeine.getIfPresent("key1");
    nativeCaffeine.getIfPresent("key1");
    nativeCaffeine.getIfPresent("key1");
    // 2 misses
    nativeCaffeine.getIfPresent("key2");
    nativeCaffeine.getIfPresent("key2");
    // Total: 3 hits + 2 misses = 5 requests, 60% hit rate
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    String hitRate = (String) cacheInfo.get("hitRate");
    assertThat(hitRate).isEqualTo("60.00%");
  }

  @Test
  void inspectShouldShowZeroHitRateWhenNoRequests() {
    Cache<Object, Object> nativeCaffeine = Caffeine.newBuilder().recordStats().build();
    CaffeineCache caffeineCache = new CaffeineCache("testCache", nativeCaffeine);

    when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("testCache"));
    when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

    Map<String, Object> result = cacheInspectionEndpoint.inspect();
    Map<String, Object> cacheInfo = (Map<String, Object>) result.get("testCache");

    String hitRate = (String) cacheInfo.get("hitRate");
    assertThat(hitRate).isEqualTo("N/A");
  }

  // ==================== Test helper classes ====================

  /** Serializable test object for testing memory estimation with serializable objects */
  static class SerializableTestObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String value;

    SerializableTestObject(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "SerializableTestObject{" + "value='" + value + '\'' + '}';
    }
  }

  /** Non-serializable test object for testing fallback memory estimation */
  static class NonSerializableTestObject {
    private final String value;

    NonSerializableTestObject(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "NonSerializableTestObject{" + "value='" + value + '\'' + '}';
    }
  }
}

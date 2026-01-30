package com.example.booklog.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 캐시 설정
 *
 * ✅ Redis 연결 성공 시: Redis 캐싱 사용
 * ⚠️ Redis 연결 실패 시: 인메모리 캐시로 자동 전환 (서비스는 정상 작동)
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    /**
     * Redis 기반 CacheManager
     * Redis가 사용 가능할 때만 생성
     */
    @Bean
    @Primary
    @ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            // Redis 연결 테스트
            connectionFactory.getConnection().close();
            log.info("✅ Redis 연결 성공 - Redis 캐싱 활성화");

            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .serializeKeysWith(
                            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                    )
                    .serializeValuesWith(
                            RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                    )
                    .entryTtl(Duration.ofHours(24)) // 기본 TTL 24시간
                    .disableCachingNullValues(); // null 값은 캐싱하지 않음

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withCacheConfiguration("homeBooks",
                            defaultConfig.entryTtl(Duration.ofHours(6))) // 홈 화면: 6시간
                    .withCacheConfiguration("bookMetadata",
                            defaultConfig.entryTtl(Duration.ofDays(7)))  // 도서 메타데이터: 7일
                    .transactionAware() // 트랜잭션 인식
                    .build();

        } catch (Exception e) {
            log.warn("⚠️ Redis 연결 실패 - 인메모리 캐시로 전환: {}", e.getMessage());
            return inMemoryCacheManager();
        }
    }

    /**
     * Fallback: 인메모리 CacheManager
     * Redis 연결 실패 시 사용
     */
    @Bean
    public CacheManager inMemoryCacheManager() {
        log.info("📦 인메모리 캐시 활성화 (ConcurrentMap) - Redis 미사용");
        return new ConcurrentMapCacheManager("homeBooks", "bookMetadata");
    }

    /**
     * 캐시 에러 핸들러
     * Redis 장애 시에도 애플리케이션이 정상 동작하도록 처리
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("캐시 조회 실패 (캐시 미사용으로 처리): cache={}, key={}, error={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("캐시 저장 실패 (무시): cache={}, key={}, error={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("캐시 삭제 실패 (무시): cache={}, key={}, error={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("캐시 초기화 실패 (무시): cache={}, error={}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}


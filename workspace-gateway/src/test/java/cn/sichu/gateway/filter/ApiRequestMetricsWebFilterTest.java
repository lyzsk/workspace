package cn.sichu.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class ApiRequestMetricsWebFilterTest {

    @Test
    void countsApiRequestsByHour() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(Mono.just(1L));
        when(redis.expire(anyString(), eq(Duration.ofDays(35)))).thenReturn(Mono.just(true));
        ApiRequestMetricsWebFilter filter = new ApiRequestMetricsWebFilter(redis);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/system/dashboard"));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        verify(values).increment(anyString());
        verify(redis).expire(anyString(), eq(Duration.ofDays(35)));
    }

    @Test
    void ignoresNonApiRequests() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        ApiRequestMetricsWebFilter filter = new ApiRequestMetricsWebFilter(redis);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/dashboard"));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        verify(redis, never()).opsForValue();
    }
}

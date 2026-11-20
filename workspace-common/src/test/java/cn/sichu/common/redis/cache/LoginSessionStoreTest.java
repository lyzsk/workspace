package cn.sichu.common.redis.cache;

import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.model.LoginUserSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class LoginSessionStoreTest {

    @Test
    void savesCompleteUserWithoutPasswordAndReadsItBack() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        LoginSessionStore store = new LoginSessionStore(redis, mapper);
        LoginUser user = new LoginUser(source(), new java.util.ArrayList<>(List.of("ADMIN")),
            new java.util.ArrayList<>(List.of("app:list")));
        LoginSession session = new LoginSession("session-1", user, LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now().plusHours(12), false, null,
            "127.0.0.1", "JUnit");
        final String[] json = new String[1];
        doAnswer(invocation -> {
            json[0] = invocation.getArgument(1);
            return null;
        }).when(operations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        store.save(session);

        assertFalse(json[0].contains("secret-hash"));
        assertTrue(json[0].contains("app:list"));
        when(operations.get(CacheKeys.loginUser("session-1"))).thenReturn(json[0]);
        LoginSession restored = store.find("session-1").orElseThrow();
        assertEquals(1L, restored.loginUser().getUserId());
        assertEquals("ADMIN", restored.loginUser().getRoles().iterator().next());
        assertNull(restored.loginUser().getPassword());
    }

    private LoginUserSource source() {
        return new LoginUserSource() {
            @Override
            public Long getId() { return 1L; }
            @Override
            public String getUsername() { return "admin"; }
            @Override
            public String getPassword() { return "secret-hash"; }
            @Override
            public String getRealName() { return "管理员"; }
            @Override
            public String getEmail() { return null; }
            @Override
            public String getMobile() { return null; }
            @Override
            public String getAvatar() { return null; }
            @Override
            public Long getTenantId() { return 0L; }
            @Override
            public Long getDeptId() { return 7L; }
            @Override
            public Integer getGender() { return 1; }
            @Override
            public Integer getStatus() { return 1; }
            @Override
            public Long getTokenVersion() { return 0L; }
            @Override
            public LocalDateTime getLockedUntil() { return null; }
        };
    }
}

package cn.sichu.common.security.token;

import cn.sichu.common.security.model.JwtSubject;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.model.LoginUserSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class JwtTokenServiceTest {

    private final JwtTokenService service = new JwtTokenService(
        "0123456789abcdef0123456789abcdef", "workspace", java.time.Duration.ofMinutes(30));

    @Test
    void createsAndParsesSubject() {
        LoginUser user = new LoginUser(source(), List.of("ADMIN"), List.of("app:list"));
        String token = service.createToken(user, "session-1");

        JwtSubject subject = service.parse(token);

        assertEquals(1L, subject.userId());
        assertEquals("session-1", subject.sessionId());
        assertEquals(2L, subject.tokenVersion());
    }

    @Test
    void rejectsDifferentToken() {
        assertThrows(TokenException.class, () -> service.parse("not-a-jwt"));
    }

    private LoginUserSource source() {
        return new LoginUserSource() {
            @Override
            public Long getId() { return 1L; }
            @Override
            public String getUsername() { return "admin"; }
            @Override
            public String getPassword() { return "hash"; }
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
            public Long getTokenVersion() { return 2L; }
            @Override
            public LocalDateTime getLockedUntil() { return null; }
        };
    }
}

package cn.sichu.system.online.service.impl;

import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.online.model.vo.SysOnlineUserVO;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class OnlineUserServiceImplTest {

    private final LoginSessionStore sessionStore = mock(LoginSessionStore.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final OnlineUserServiceImpl service = new OnlineUserServiceImpl(sessionStore,
        userMapper, mock(AuthTokenStore.class));

    @Test
    void returnsClientIpAndUserAgentForActiveSessions() {
        LoginUser loginUser = mock(LoginUser.class);
        when(loginUser.getUserId()).thenReturn(1L);
        LocalDateTime now = LocalDateTime.now();
        when(sessionStore.listAll()).thenReturn(List.of(new LoginSession("s1", loginUser, now, now,
            now.plusHours(12), false, null, "172.18.20.190", "Mozilla/5.0 Chrome/157")));
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setRealName("管理员");
        when(userMapper.selectById(1L)).thenReturn(user);

        List<SysOnlineUserVO> result = service.listOnline();

        assertEquals(1, result.size());
        assertEquals("172.18.20.190", result.get(0).getIp());
        assertEquals("Mozilla/5.0 Chrome/157", result.get(0).getUserAgent());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void filtersExpiredSessions() {
        LoginUser loginUser = mock(LoginUser.class);
        LocalDateTime now = LocalDateTime.now();
        when(sessionStore.listAll()).thenReturn(List.of(new LoginSession("s1", loginUser,
            now.minusHours(13), now.minusHours(1), now.minusMinutes(1), false, null, null,
            null)));

        assertEquals(0, service.listOnline().size());
    }
}

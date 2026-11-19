package cn.sichu.system.security;

import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.service.LoginUserDetailsService;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
class LoginUserDetailsServiceTest {

    private SysUserMapper userMapper;
    private AuthQueryMapper authQueryMapper;
    private LoginUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        authQueryMapper = mock(AuthQueryMapper.class);
        userDetailsService = new LoginUserDetailsService(userMapper, authQueryMapper);
    }

    @Test
    void loadsUserWithRolesAndPermissions() {
        when(userMapper.selectOne(any())).thenReturn(user());
        when(authQueryMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        when(authQueryMapper.selectPermissions(1L)).thenReturn(List.of("sys:user:list"));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        LoginUser loginUser = assertInstanceOf(LoginUser.class, details);
        assertEquals(1L, loginUser.getUserId());
        assertEquals("admin", loginUser.getUsername());
        assertTrue(loginUser.getRoles().contains("ADMIN"));
        assertTrue(loginUser.getPermissions().contains("sys:user:list"));
    }

    @Test
    void loadsUserByIdForOpaqueAuthentication() {
        when(userMapper.selectById(1L)).thenReturn(user());
        when(authQueryMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        when(authQueryMapper.selectPermissions(1L)).thenReturn(List.of("sys:user:list"));

        LoginUser details = userDetailsService.loadUserById(1L);

        assertEquals(1L, details.getUserId());
        assertEquals("admin", details.getUsername());
    }

    @Test
    void rejectsUnknownUser() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("missing"));
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(0L);
        user.setUsername("admin");
        user.setPassword("hash");
        user.setRealName("管理员");
        user.setGender(1);
        user.setStatus(1);
        return user;
    }
}

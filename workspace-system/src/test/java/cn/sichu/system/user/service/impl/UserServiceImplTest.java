package cn.sichu.system.user.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.redis.cache.AuthorizationStore;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.mapper.SysUserAppMapper;
import cn.sichu.system.app.grant.service.IAppGrantService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.security.model.DataScopeFilter;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.security.service.IDataScopeService;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import cn.sichu.system.user.model.form.SysUserForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class UserServiceImplTest {

    private SysUserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthTokenStore authTokenStore = mock(AuthTokenStore.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        IDataScopeService dataScopeService = mock(IDataScopeService.class);
        IAppGrantService appGrantService = mock(IAppGrantService.class);
        SysAppMapper appMapper = mock(SysAppMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserAppMapper userAppMapper = mock(SysUserAppMapper.class);
        AuthorizationStore authorizationStore = mock(AuthorizationStore.class);
        SysApp workspace = new SysApp();
        workspace.setId(1000L);
        workspace.setCode("workspace");
        when(appMapper.selectOne(any())).thenReturn(workspace);
        userService = new UserServiceImpl(passwordEncoder, authTokenStore, userRoleMapper,
            dataScopeService, appGrantService, appMapper, roleMapper, userAppMapper,
            mock(cn.sichu.system.security.mapper.AuthQueryMapper.class), authorizationStore);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        when(dataScopeService.current()).thenReturn(new DataScopeFilter(true, java.util.Set.of(), false, null));
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void updateRejectsProtectedAdmin() {
        when(userMapper.selectById(1L)).thenReturn(admin());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.update(1L, new SysUserForm()));

        assertEquals(SystemCode.USER_PROTECTED, exception.getResultCode());
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void deleteRejectsProtectedAdmin() {
        when(userMapper.selectById(1L)).thenReturn(admin());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.delete(1L));

        assertEquals(SystemCode.USER_PROTECTED, exception.getResultCode());
    }

    @Test
    void getAllowsReadOnlyAdmin() {
        when(userMapper.selectById(1L)).thenReturn(admin());

        assertDoesNotThrow(() -> userService.get(1L));
    }

    private SysUser admin() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setStatus(1);
        return user;
    }
}

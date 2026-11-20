package cn.sichu.system.app.grant.service.impl;

import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.entity.SysUserApp;
import cn.sichu.system.app.grant.entity.SysUserAppDept;
import cn.sichu.system.app.grant.mapper.AppAuthorizationMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppDeptMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppMapper;
import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.mapper.SysRoleDeptMapper;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppAuthorizationServiceImplTest {

    private SysUserMapper userMapper;
    private SysAppMapper appMapper;
    private SysUserAppMapper userAppMapper;
    private SysUserAppDeptMapper userAppDeptMapper;
    private AppAuthorizationMapper authorizationMapper;
    private AppAuthorizationServiceImpl service;
    private SysUserApp grant;

    @BeforeAll
    static void initTables() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysApp.class);
        TableInfoHelper.initTableInfo(assistant, SysUserApp.class);
    }

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        appMapper = mock(SysAppMapper.class);
        userAppMapper = mock(SysUserAppMapper.class);
        userAppDeptMapper = mock(SysUserAppDeptMapper.class);
        authorizationMapper = mock(AppAuthorizationMapper.class);
        service = new AppAuthorizationServiceImpl(userMapper, appMapper, userAppMapper,
            userAppDeptMapper, authorizationMapper, mock(SysRoleDeptMapper.class), mock(SysDeptMapper.class),
            mock(cn.sichu.common.redis.cache.AuthorizationStore.class), new com.fasterxml.jackson.databind.ObjectMapper());
        when(userMapper.selectById(1L)).thenReturn(user());
        when(appMapper.selectOne(any())).thenReturn(app());
        grant = new SysUserApp();
        grant.setId(10L);
        grant.setUserId(1L);
        grant.setAppId(2000L);
        grant.setStatus(1);
        when(userAppMapper.selectOne(any())).thenReturn(grant);
    }

    @Test
    void grantWithoutActiveRoleCannotEnterOrReceiveMenus() {
        when(authorizationMapper.selectRoles(1L, "app-a")).thenReturn(List.of());

        AppAuthorizationVO result = service.getAuthorization(1L, "app-a");

        assertFalse(result.isAccessible());
        assertTrue(result.getRoles().isEmpty());
        assertTrue(result.getMenus().isEmpty());
        assertTrue(result.isSelfOnly());
        verify(authorizationMapper, never()).selectPermissions(any(), any());
    }

    @Test
    void returnsOnlyCurrentApplicationRolePermissionsAndMenuTree() {
        when(authorizationMapper.selectRoles(1L, "app-a")).thenReturn(List.of(role(3)));
        when(authorizationMapper.selectPermissions(1L, "app-a")).thenReturn(Set.of("app-a:edit"));
        when(authorizationMapper.selectMenus(1L, "app-a")).thenReturn(List.of(menu()));

        AppAuthorizationVO result = service.getAuthorization(1L, "app-a");

        assertTrue(result.isAccessible());
        assertEquals(Set.of("APP_ADMIN"), result.getRoles());
        assertEquals(Set.of("app-a:edit"), result.getPermissions());
        assertEquals(List.of("7"), result.getDeptIds());
        assertEquals(1, result.getMenus().size());
        assertEquals("app-a:home", result.getMenus().get(0).getMenuCode());
    }

    @Test
    void platformAdminCannotEnterBusinessAppWithoutGrant() {
        when(authorizationMapper.selectRoleCodes(1L, "workspace"))
            .thenReturn(Set.of("SUPER_ADMIN"));
        when(userAppMapper.selectOne(any())).thenReturn(null);

        AppAuthorizationVO result = service.getAuthorization(1L, "app-a");

        assertFalse(result.isAccessible());
        assertTrue(result.getRoles().isEmpty());
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getMenus().isEmpty());
    }

    @Test
    void platformAdminKeepsWorkspaceBypassWithoutGrant() {
        SysApp workspace = app();
        workspace.setCode("workspace");
        when(appMapper.selectOne(any())).thenReturn(workspace);
        when(userAppMapper.selectOne(any())).thenReturn(null);
        when(authorizationMapper.selectRoleCodes(1L, "workspace"))
            .thenReturn(Set.of("SUPER_ADMIN"));
        when(authorizationMapper.selectAllPermissions("workspace"))
            .thenReturn(Set.of("sys:user:list"));
        when(authorizationMapper.selectAllMenus("workspace")).thenReturn(List.of(menu()));

        AppAuthorizationVO result = service.getAuthorization(1L, "workspace");

        assertTrue(result.isAccessible());
        assertTrue(result.getRoles().contains("SUPER_ADMIN"));
        assertTrue(result.isAll());
        assertEquals(Set.of("sys:user:list"), result.getPermissions());
    }

    @Test
    void userGrantDataScopeOverridesRoleScope() {
        grant.setDataScope(2);
        when(authorizationMapper.selectRoles(1L, "app-a")).thenReturn(List.of(role(1)));
        SysUserAppDept relation = new SysUserAppDept();
        relation.setDeptId(9L);
        when(userAppDeptMapper.selectList(any())).thenReturn(List.of(relation));

        AppAuthorizationVO result = service.getAuthorization(1L, "app-a");

        assertFalse(result.isAll());
        assertEquals(List.of("9"), result.getDeptIds());
        assertFalse(result.isSelfOnly());
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(1);
        user.setDeptId(7L);
        user.setTokenVersion(0L);
        return user;
    }

    private SysApp app() {
        SysApp app = new SysApp();
        app.setId(2000L);
        app.setCode("app-a");
        app.setStatus(1);
        return app;
    }

    private SysRole role(int dataScope) {
        SysRole role = new SysRole();
        role.setId(20L);
        role.setAppId(2000L);
        role.setRoleCode("APP_ADMIN");
        role.setDataScope(dataScope);
        role.setStatus(1);
        return role;
    }

    private SysMenu menu() {
        SysMenu menu = new SysMenu();
        menu.setId(30L);
        menu.setParentId(0L);
        menu.setAppCode("app-a");
        menu.setMenuCode("app-a:home");
        menu.setMenuType("MENU");
        menu.setStatus(1);
        menu.setHidden(1);
        return menu;
    }
}

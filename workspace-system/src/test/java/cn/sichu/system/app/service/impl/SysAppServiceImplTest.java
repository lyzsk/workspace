package cn.sichu.system.app.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.app.model.form.SysAppForm;
import cn.sichu.system.app.model.vo.SysAppVO;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.mapper.SysMenuMapper;
import cn.sichu.system.role.entity.SysRoleMenu;
import cn.sichu.system.role.mapper.SysRoleMenuMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.user.entity.SysUser;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

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
class SysAppServiceImplTest {

    private SysAppMapper appMapper;
    private SysMenuMapper menuMapper;
    private SysRoleMenuMapper roleMenuMapper;
    private SysAppServiceImpl appService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
            new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysApp.class);
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
    }

    @BeforeEach
    void setUp() {
        appMapper = mock(SysAppMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        roleMenuMapper = mock(SysRoleMenuMapper.class);
        appService = new SysAppServiceImpl(appMapper, menuMapper, roleMenuMapper);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void runtimeReturnsAppWhenUserHasAccessPermission() {
        when(appMapper.selectOne(any())).thenReturn(app());
        authenticate(List.of("ies:access"));

        SysAppVO result = appService.getRuntime("/apps/ies");

        assertEquals("/ies/micro.js", result.getModuleEntry());
        assertEquals("ies:access", result.getAccessPermission());
    }

    @Test
    void runtimeRejectsUserWithoutAccessPermission() {
        when(appMapper.selectOne(any())).thenReturn(app());
        authenticate(List.of());

        assertThrows(AccessDeniedException.class, () -> appService.getRuntime("/apps/ies"));
    }

    @Test
    void createRejectsBlankModuleEntry() {
        SysAppForm form = form();
        form.setModuleEntry(" ");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> appService.create(form));

        assertEquals(SystemCode.APP_ENTRY_URL_REQUIRED, exception.getResultCode());
        verify(appMapper, never()).insert(any(SysApp.class));
    }

    @Test
    void createBuildsCanonicalAccessPermissionAndHiddenMenu() {
        when(menuMapper.selectOne(any())).thenReturn(appsRoot());

        appService.create(form());

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuMapper).insert(captor.capture());
        SysMenu menu = captor.getValue();
        assertEquals("apps:ies:access", menu.getMenuCode());
        assertEquals("ies:access", menu.getPermission());
        assertEquals("BUTTON", menu.getMenuType());
        assertEquals(0, menu.getHidden());
        assertEquals(201L, menu.getParentId());
    }

    @Test
    void deleteRemovesAccessMenuAndRoleReferences() {
        when(appMapper.selectById(1L)).thenReturn(app());
        when(menuMapper.selectCount(any())).thenReturn(0L);
        SysMenu accessMenu = new SysMenu();
        accessMenu.setId(900L);
        when(menuMapper.selectOne(any())).thenReturn(accessMenu);

        appService.delete(1L);

        verify(roleMenuMapper).delete(any());
        verify(menuMapper).deleteById(900L);
        verify(appMapper).deleteById(1L);
    }

    private SysAppForm form() {
        SysAppForm form = new SysAppForm();
        form.setCode("ies");
        form.setName("IES");
        form.setBasePath("/apps/ies");
        form.setModuleEntry("/ies/micro.js");
        form.setAccessPermission("ignored");
        return form;
    }

    private SysMenu appsRoot() {
        SysMenu root = new SysMenu();
        root.setId(201L);
        root.setAppCode("workspace");
        root.setMenuCode("apps");
        return root;
    }

    private SysApp app() {
        SysApp app = new SysApp();
        app.setId(1L);
        app.setCode("ies");
        app.setName("IES");
        app.setBasePath("/apps/ies");
        app.setModuleEntry("/ies/micro.js");
        app.setAccessPermission("ies:access");
        app.setStatus(1);
        return app;
    }

    private void authenticate(List<String> permissions) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(0L);
        user.setUsername("abc");
        user.setStatus(1);
        LoginUser loginUser = new LoginUser(user, List.of(), permissions);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }
}

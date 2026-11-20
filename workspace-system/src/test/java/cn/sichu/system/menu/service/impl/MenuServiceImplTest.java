package cn.sichu.system.menu.service.impl;

import cn.sichu.common.redis.cache.AuthorizationStore;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.mapper.SysMenuMapper;
import cn.sichu.system.menu.model.vo.SysMenuTreeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class MenuServiceImplTest {

    private final SysMenuMapper menuMapper = mock(SysMenuMapper.class);
    private final IAppAuthorizationService authorizationService = mock(IAppAuthorizationService.class);
    private final MenuServiceImpl service = new MenuServiceImpl(menuMapper, authorizationService,
        mock(SysAppMapper.class), mock(AuthorizationStore.class));

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAllApplicationsWithAppCodeForManagementTree() {
        SysMenu workspace = menu(1L, "workspace", "workspace-menu");
        SysMenu child = menu(2L, "app-a", "child-menu");
        when(menuMapper.selectList(any())).thenReturn(List.of(workspace, child));

        List<SysMenuTreeVO> result = service.tree(true);

        assertEquals(2, result.size());
        assertEquals("workspace", result.get(0).getAppCode());
        assertEquals("app-a", result.get(1).getAppCode());
    }

    @Test
    void appendsAuthorizedApplicationMenusAsExternalChildren() {
        LoginUser user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        when(user.getRoles()).thenReturn(Set.of("SUPER_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(menuMapper.selectList(any())).thenReturn(List.of());
        SysApp app = new SysApp();
        app.setCode("app-a");
        app.setName("应用 A");
        app.setBasePath("/apps/app-a");
        when(authorizationService.listAccessibleApps(1L)).thenReturn(List.of(app));
        SysMenuTreeVO page = new SysMenuTreeVO();
        page.setId("page-a");
        page.setAppCode("app-a");
        page.setMenuCode("page-a");
        page.setMenuName("页面 A");
        page.setMenuType("MENU");
        page.setPath("/apps/app-a/page-a");
        AppAuthorizationVO authorization = new AppAuthorizationVO();
        authorization.setMenus(List.of(page));
        when(authorizationService.getAuthorization(1L, "app-a")).thenReturn(authorization);

        List<SysMenuTreeVO> result = service.currentUserTree();

        assertEquals(1, result.size());
        SysMenuTreeVO appNode = result.get(0);
        assertEquals("0", appNode.getParentId());
        assertEquals("DIR", appNode.getMenuType());
        assertEquals("apps/detail/MicroFrontendHost", appNode.getComponent());
        assertEquals(List.of(page), appNode.getChildren());
    }

    private SysMenu menu(Long id, String appCode, String code) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(0L);
        menu.setAppCode(appCode);
        menu.setMenuCode(code);
        menu.setMenuName(code);
        menu.setMenuType("MENU");
        menu.setSortNo(0);
        menu.setHidden(1);
        menu.setStatus(1);
        return menu;
    }
}

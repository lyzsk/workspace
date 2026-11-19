package cn.sichu.system.security;

import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.security.service.PermissionService;
import cn.sichu.system.user.entity.SysUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
class PermissionServiceTest {

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        IAppAuthorizationService authorizationService = mock(IAppAuthorizationService.class);
        AppAuthorizationVO authorization = new AppAuthorizationVO();
        authorization.setAccessible(true);
        when(authorizationService.getAuthorization(any(), eq("workspace"))).thenReturn(authorization);
        permissionService = new PermissionService(authorizationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void superAdminBypassesPermissionCheck() {
        authenticate("SUPER_ADMIN", List.of());

        assertTrue(permissionService.hasPerm("sys:user:create"));
    }

    @Test
    void normalUserNeedsExactPermission() {
        authenticate("USER", List.of("sys:user:list"));

        assertTrue(permissionService.hasPerm("sys:user:list"));
        assertFalse(permissionService.hasPerm("sys:user:delete"));
    }

    @Test
    void unauthenticatedHasNoPermission() {
        assertFalse(permissionService.hasPerm("sys:user:list"));
    }

    private void authenticate(String role, List<String> permissions) {
        LoginUser loginUser = new LoginUser(user(), List.of(role), permissions);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
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

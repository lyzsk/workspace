package cn.sichu.system.security.service.impl;

import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.security.model.DataScopeFilter;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.user.entity.SysUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataScopeServiceImplTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformQueriesUseWorkspaceGrantDataScope() {
        IAppAuthorizationService authorizationService = mock(IAppAuthorizationService.class);
        AppAuthorizationVO authorization = new AppAuthorizationVO();
        authorization.setAll(false);
        authorization.setDeptIds(List.of("9"));
        authorization.setSelfOnly(false);
        when(authorizationService.getAuthorization(1L, "workspace")).thenReturn(authorization);
        DataScopeServiceImpl service = new DataScopeServiceImpl(authorizationService);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(1);
        LoginUser loginUser = new LoginUser(user, List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));

        DataScopeFilter result = service.current();

        assertFalse(result.all());
        assertEquals(java.util.Set.of(9L), result.deptIds());
        assertFalse(result.selfOnly());
        assertEquals(1L, result.userId());
    }
}

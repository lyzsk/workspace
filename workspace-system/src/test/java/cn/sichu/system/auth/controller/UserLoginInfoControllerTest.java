package cn.sichu.system.auth.controller;

import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.auth.service.ILoginLogService;
import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.security.service.LoginUserDetailsService;
import cn.sichu.system.security.service.ServiceTokenValidator;
import cn.sichu.system.user.service.IUserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class UserLoginInfoControllerTest {

    @Test
    void loadsCredentialsByIdInsteadOfTreatingIdAsUsername() {
        LoginUserDetailsService userDetailsService = mock(LoginUserDetailsService.class);
        ServiceTokenValidator tokenValidator = mock(ServiceTokenValidator.class);
        UserLoginInfoController controller = new UserLoginInfoController(
            userDetailsService, mock(AuthQueryMapper.class), tokenValidator,
            mock(IUserService.class), mock(ILoginLogService.class));
        LoginUser user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("admin");
        when(userDetailsService.loadUserById(1L)).thenReturn(user);

        var result = controller.loginInfoById(1L, "service-token");

        assertEquals(1L, result.getId());
        assertEquals("admin", result.getUsername());
        verify(tokenValidator).validate("service-token");
        verify(userDetailsService).loadUserById(1L);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}

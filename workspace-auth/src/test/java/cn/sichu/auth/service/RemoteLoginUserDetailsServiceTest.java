package cn.sichu.auth.service;

import cn.sichu.auth.model.dto.LoginCredentialsDTO;
import cn.sichu.common.security.model.LoginUser;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
class RemoteLoginUserDetailsServiceTest {

    @Test
    void loadsUserByIdThroughDedicatedEndpoint() {
        IRemoteUserService remoteUserService = mock(IRemoteUserService.class);
        ServiceTokenProvider tokenProvider = mock(ServiceTokenProvider.class);
        RemoteLoginUserDetailsService service =
            new RemoteLoginUserDetailsService(remoteUserService, tokenProvider);
        LoginCredentialsDTO credentials = new LoginCredentialsDTO();
        credentials.setId(1L);
        credentials.setUsername("admin");
        credentials.setStatus(1);
        credentials.setTokenVersion(0L);
        credentials.setRoles(Set.of("ADMIN"));
        credentials.setPermissions(Set.of("sys:user:list"));
        when(tokenProvider.serviceToken()).thenReturn("service-token");
        when(remoteUserService.getLoginInfoById(1L, "service-token"))
            .thenReturn(credentials);

        LoginUser result = service.loadUserById(1L);

        assertEquals(1L, result.getUserId());
        assertEquals("admin", result.getUsername());
        verify(remoteUserService).getLoginInfoById(1L, "service-token");
        verify(remoteUserService, never()).getLoginInfo(anyString(), anyString());
    }
}

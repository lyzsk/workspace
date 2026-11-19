package cn.sichu.auth.service;

import cn.sichu.auth.model.dto.LoginCredentialsDTO;
import cn.sichu.common.security.model.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Service
@RequiredArgsConstructor
public class RemoteLoginUserDetailsService implements UserDetailsService {

    private final IRemoteUserService remoteUserService;
    private final ServiceTokenProvider tokenProvider;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LoginCredentialsDTO credentials = remoteUserService.getLoginInfo(username,
            tokenProvider.serviceToken());
        if (credentials == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return credentials.toLoginUser();
    }

    public LoginUser loadUserById(Long userId) {
        LoginCredentialsDTO credentials = remoteUserService.getLoginInfo(String.valueOf(userId),
            tokenProvider.serviceToken());
        if (credentials == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return credentials.toLoginUser();
    }
}

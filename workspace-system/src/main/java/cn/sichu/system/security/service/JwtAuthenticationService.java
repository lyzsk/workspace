package cn.sichu.system.security.service;

import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.token.JwtTokenManager;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.system.security.token.TokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtTokenManager tokenManager;
    private final UserDetailsService userDetailsService;
    private final AuthTokenStore authTokenStore;
    private final SessionActivityRecorder activityRecorder;

    public LoginUser authenticate(String token) {
        TokenPayload payload = tokenManager.parseAccessToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(payload.username());
        if (userDetails instanceof LoginUser loginUser
            && loginUser.isEnabled()
            && loginUser.isAccountNonLocked()
            && Objects.equals(payload.tokenVersion(), loginUser.getTokenVersion())
            && !authTokenStore.isAccessTokenRevoked(payload.tokenId())) {
            activityRecorder.record(payload.tokenId());
            return loginUser;
        }
        throw new TokenException("登录凭证无效");
    }
}

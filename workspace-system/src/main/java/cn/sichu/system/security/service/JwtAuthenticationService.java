package cn.sichu.system.security.service;

import cn.sichu.system.auth.model.vo.IntrospectionVO;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.token.TokenException;
import lombok.RequiredArgsConstructor;
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

    private final OpaqueTokenService tokenService;
    private final LoginUserDetailsService userDetailsService;

    public LoginUser authenticate(String token) {
        IntrospectionVO introspection = tokenService.introspect(token);
        if (!introspection.active()) {
            throw new TokenException("登录凭证无效或已过期");
        }
        LoginUser loginUser =
            userDetailsService.loadUserById(introspection.userId());
        if (!loginUser.isEnabled() || !loginUser.isAccountNonLocked()
            || !Objects.equals(introspection.tokenVersion(),
            loginUser.getTokenVersion())) {
            throw new TokenException("用户状态已变化");
        }
        return loginUser;
    }
}

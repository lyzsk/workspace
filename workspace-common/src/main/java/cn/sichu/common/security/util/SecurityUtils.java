package cn.sichu.common.security.util;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.common.security.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static LoginUser getLoginUserOrNull() {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getUserIdOrNull() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getUserId();
    }
}

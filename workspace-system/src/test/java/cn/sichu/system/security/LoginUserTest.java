package cn.sichu.system.security;

import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.user.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
class LoginUserTest {

    @Test
    void mapsRolesAndPermissionsToAuthorities() {
        LoginUser loginUser = loginUser(1);

        Collection<String> authorities =
            loginUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("sys:user:list"));
        assertEquals(2, authorities.size());
    }

    @Test
    void enabledFollowsStatus() {
        assertTrue(loginUser(1).isEnabled());
        assertFalse(loginUser(0).isEnabled());
    }

    private LoginUser loginUser(int status) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(0L);
        user.setUsername("admin");
        user.setPassword("hash");
        user.setRealName("管理员");
        user.setGender(1);
        user.setStatus(status);
        return new LoginUser(user, List.of("ADMIN"), List.of("sys:user:list"));
    }
}

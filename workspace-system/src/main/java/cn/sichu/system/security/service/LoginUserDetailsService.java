package cn.sichu.system.security.service;

import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class LoginUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final AuthQueryMapper authQueryMapper;

    @Override
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername,
                username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return loginUser(user);
    }

    public LoginUser loadUserById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return loginUser(user);
    }

    private LoginUser loginUser(SysUser user) {
        return new LoginUser(user,
            authQueryMapper.selectRoleCodes(user.getId()),
            authQueryMapper.selectPermissions(user.getId()));
    }
}

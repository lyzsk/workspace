package cn.sichu.system.online.service.impl;

import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.util.SecurityUtils;
import cn.sichu.system.online.model.vo.SysOnlineUserVO;
import cn.sichu.system.online.service.IOnlineUserService;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements IOnlineUserService {

    private final LoginSessionStore sessionStore;
    private final SysUserMapper userMapper;
    private final AuthTokenStore authTokenStore;

    @Value("${workspace.security.super-admin-assignee:superadmin}")
    private String superAdminAssignee;

    @Override
    public List<SysOnlineUserVO> listOnline() {
        LocalDateTime now = LocalDateTime.now();
        return sessionStore.listAll().stream()
            .filter(session -> !session.revoked() && now.isBefore(session.expiresAt()))
            .map(session -> new SessionUser(session,
                userMapper.selectById(session.loginUser().getUserId())))
            .filter(pair -> !isHidden(pair.user()))
            .sorted(Comparator.comparing((SessionUser pair) -> pair.session().loginTime())
                .reversed())
            .map(pair -> toVO(pair.session(), pair.user())).toList();
    }

    @Override
    @Transactional
    public void kick(Long userId) {
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId).setSql("token_version = token_version + 1")
            .set(SysUser::getUpdateTime, LocalDateTime.now());
        userMapper.update(null, update);
        authTokenStore.revokeAllSessions(userId);
    }

    private boolean isHidden(SysUser user) {
        return user != null && user.getUsername() != null
            && user.getUsername().equals(superAdminAssignee)
            && !isCurrentSuperAdminAssignee();
    }

    private boolean isCurrentSuperAdminAssignee() {
        LoginUser current = SecurityUtils.getLoginUserOrNull();
        return current != null && current.getRoles().contains("SUPER_ADMIN")
            && current.getUsername() != null
            && current.getUsername().equals(superAdminAssignee);
    }

    private SysOnlineUserVO toVO(LoginSession session, SysUser user) {
        SysOnlineUserVO vo = new SysOnlineUserVO();
        vo.setId(session.sessionId());
        vo.setUserId(session.loginUser().getUserId() == null ? null
            : session.loginUser().getUserId().toString());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
        }
        vo.setIp(session.ip());
        vo.setUserAgent(session.userAgent());
        vo.setLoginTime(session.loginTime());
        vo.setLastSeenAt(session.lastSeenAt());
        vo.setExpiresAt(session.expiresAt());
        return vo;
    }

    private record SessionUser(LoginSession session, SysUser user) {
    }
}

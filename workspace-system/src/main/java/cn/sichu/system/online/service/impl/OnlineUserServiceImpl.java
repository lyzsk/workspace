package cn.sichu.system.online.service.impl;

import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.system.online.model.vo.SysOnlineUserVO;
import cn.sichu.system.online.service.IOnlineUserService;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
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

    @Override
    public List<SysOnlineUserVO> listOnline() {
        LocalDateTime now = LocalDateTime.now();
        return sessionStore.listAll().stream()
            .filter(session -> !session.revoked() && now.isBefore(session.expiresAt()))
            .sorted(Comparator.comparing(LoginSession::loginTime).reversed())
            .map(this::toVO).toList();
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

    private SysOnlineUserVO toVO(LoginSession session) {
        SysOnlineUserVO vo = new SysOnlineUserVO();
        vo.setId(session.sessionId());
        vo.setUserId(session.loginUser().getUserId() == null ? null
            : session.loginUser().getUserId().toString());
        SysUser user = userMapper.selectById(session.loginUser().getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
        }
        vo.setLoginTime(session.loginTime());
        vo.setLastSeenAt(session.lastSeenAt());
        vo.setExpiresAt(session.expiresAt());
        return vo;
    }
}

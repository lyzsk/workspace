package cn.sichu.system.online.service.impl;

import cn.sichu.system.online.model.vo.SysOnlineUserVO;
import cn.sichu.system.online.service.IOnlineUserService;
import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements IOnlineUserService {

    private final SysUserSessionMapper sessionMapper;
    private final SysUserMapper userMapper;
    private final AuthTokenStore authTokenStore;

    @Override
    public List<SysOnlineUserVO> listOnline() {
        return sessionMapper.selectList(
            new LambdaQueryWrapper<SysUserSession>().eq(SysUserSession::getRevoked, 0)
                .gt(SysUserSession::getExpiresAt, LocalDateTime.now())
                .orderByDesc(SysUserSession::getLoginTime)).stream().map(this::toVO).toList();
    }

    @Override
    public void kick(Long userId) {
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId).setSql("token_version = token_version + 1");
        userMapper.update(null, update);
        authTokenStore.revokeAllSessions(userId);
    }

    private SysOnlineUserVO toVO(SysUserSession session) {
        SysOnlineUserVO vo = new SysOnlineUserVO();
        vo.setId(session.getJti());
        vo.setUserId(session.getUserId() == null ? null : session.getUserId().toString());
        SysUser user = userMapper.selectById(session.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
        }
        vo.setIp(session.getIp());
        vo.setUserAgent(session.getUserAgent());
        vo.setLoginTime(session.getLoginTime());
        vo.setLastSeenAt(session.getLastSeenAt());
        vo.setExpiresAt(session.getExpiresAt());
        return vo;
    }
}

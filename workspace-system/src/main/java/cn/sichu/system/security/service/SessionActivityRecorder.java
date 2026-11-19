package cn.sichu.system.security.service;

import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionActivityRecorder {

    private static final long RECORD_INTERVAL_SECONDS = 60;

    private final SysUserSessionMapper sessionMapper;
    private final Map<String, Long> recordedAt = new ConcurrentHashMap<>();

    public void record(String jti) {
        if (jti == null) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        Long last = recordedAt.get(jti);
        if (last != null && now - last < RECORD_INTERVAL_SECONDS) {
            return;
        }
        recordedAt.put(jti, now);
        try {
            LambdaUpdateWrapper<SysUserSession> update =
                new LambdaUpdateWrapper<>();
            update.eq(SysUserSession::getJti, jti)
                .eq(SysUserSession::getRevoked, 0)
                .set(SysUserSession::getLastSeenAt, LocalDateTime.now());
            sessionMapper.update(null, update);
        } catch (DataAccessException e) {
            log.warn("记录会话最后活跃时间失败 jti={} msg={}", jti,
                e.getMessage());
        }
    }

    public void evict(String jti) {
        if (jti != null) {
            recordedAt.remove(jti);
        }
    }

    public void clear() {
        recordedAt.clear();
    }
}

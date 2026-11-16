package cn.sichu.system.security.service;

import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class SessionActivityRecorderTest {

    private SysUserSessionMapper sessionMapper;
    private SessionActivityRecorder recorder;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            SysUserSession.class);
    }

    @BeforeEach
    void setUp() {
        sessionMapper = mock(SysUserSessionMapper.class);
        recorder = new SessionActivityRecorder(sessionMapper);
    }

    @Test
    void recordSkipsDuplicateWithinInterval() {
        recorder.record("jti-1");
        recorder.record("jti-1");

        verify(sessionMapper, times(1)).update(any(), any());
    }

    @Test
    void recordDoesNotPropagateDatabaseFailure() {
        when(sessionMapper.update(any(), any())).thenThrow(
            new DataAccessResourceFailureException("database unavailable"));

        assertDoesNotThrow(() -> recorder.record("jti-2"));

        verify(sessionMapper).update(any(), any());
    }
}

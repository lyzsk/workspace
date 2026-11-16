package cn.sichu.system.notice.handler;

import cn.sichu.system.notice.entity.SysMessage;
import cn.sichu.system.notice.entity.SysNotice;
import cn.sichu.system.notice.entity.SysNotificationPreference;
import cn.sichu.system.notice.mapper.SysMessageMapper;
import cn.sichu.system.notice.mapper.SysNotificationPreferenceMapper;
import cn.sichu.system.notice.mapper.SysNoticeMapper;
import cn.sichu.system.notice.websocket.NotificationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
class NoticeRemindJobHandlerTest {

    private SysMessageMapper messageMapper;
    private SysNoticeMapper noticeMapper;
    private SysNotificationPreferenceMapper preferenceMapper;
    private NotificationEventPublisher eventPublisher;
    private NoticeRemindJobHandler handler;

    @BeforeEach
    void setUp() {
        messageMapper = mock(SysMessageMapper.class);
        noticeMapper = mock(SysNoticeMapper.class);
        preferenceMapper = mock(SysNotificationPreferenceMapper.class);
        eventPublisher = mock(NotificationEventPublisher.class);
        handler = new NoticeRemindJobHandler(messageMapper, noticeMapper, preferenceMapper,
            eventPublisher);
    }

    @Test
    void schedulesNextRepeatAfterSendingReminder() {
        SysMessage message = new SysMessage();
        message.setId(1L);
        message.setNoticeId(1L);
        message.setReceiverId(2L);
        message.setCategory("TODO");
        message.setNextRemindTime(LocalDateTime.now().minusSeconds(1));
        message.setReadFlag(0);
        message.setDoneFlag(0);
        message.setRemindCount(0);
        SysNotice notice = new SysNotice();
        notice.setId(1L);
        notice.setTitle("待办");
        notice.setStatus(1);
        notice.setRepeatIntervalSeconds(60L);
        notice.setMaxRemindCount(2);
        when(messageMapper.selectList(any())).thenReturn(List.of(message));
        when(noticeMapper.selectById(1L)).thenReturn(notice);
        when(preferenceMapper.selectOne(any())).thenReturn(null);

        handler.execute(null);

        assertEquals(1, message.getRemindCount());
        assertNotNull(message.getNextRemindTime());
        verify(messageMapper).updateById(message);
        verify(eventPublisher).publishAfterCommit(anyLong(), anyString(), any());
    }

    @Test
    void overnightQuietHoursDeferToSameDayEnd() {
        SysNotificationPreference preference = new SysNotificationPreference();
        preference.setQuietEnabled(1);
        preference.setQuietStart(LocalTime.of(22, 0));
        preference.setQuietEnd(LocalTime.of(6, 0));
        preference.setTimezone("Asia/Shanghai");
        when(preferenceMapper.selectOne(any())).thenReturn(preference);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 1, 0);

        LocalDateTime result = ReflectionTestUtils.invokeMethod(handler, "quietEnd", 2L, now);

        assertEquals(LocalDateTime.of(2026, 11, 15, 6, 0), result);
    }
}

package cn.sichu.system.notice.handler;

import cn.sichu.quartz.handler.JobHandler;
import cn.sichu.system.notice.entity.SysMessage;
import cn.sichu.system.notice.entity.SysNotice;
import cn.sichu.system.notice.entity.SysNotificationPreference;
import cn.sichu.system.notice.mapper.SysMessageMapper;
import cn.sichu.system.notice.mapper.SysNoticeMapper;
import cn.sichu.system.notice.mapper.SysNotificationPreferenceMapper;
import cn.sichu.system.notice.websocket.NotificationEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component("noticeRemindJobHandler")
@RequiredArgsConstructor
public class NoticeRemindJobHandler implements JobHandler {

    private final SysMessageMapper messageMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysNotificationPreferenceMapper preferenceMapper;
    private final NotificationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void execute(String params) {
        LocalDateTime now = LocalDateTime.now();
        List<SysMessage> dueMessages = messageMapper.selectList(
            new LambdaQueryWrapper<SysMessage>().isNotNull(SysMessage::getNextRemindTime)
                .le(SysMessage::getNextRemindTime, now).eq(SysMessage::getReadFlag, 0)
                .eq(SysMessage::getDoneFlag, 0).orderByAsc(SysMessage::getNextRemindTime)
                .last("LIMIT 200"));
        for (SysMessage message : dueMessages) {
            SysNotice notice = noticeMapper.selectById(message.getNoticeId());
            if (notice == null || notice.getStatus() == null || notice.getStatus() != 1 || (
                notice.getExpireTime() != null && !notice.getExpireTime().isAfter(now))) {
                message.setNextRemindTime(null);
                messageMapper.updateById(message);
                continue;
            }
            LocalDateTime quietEnd = quietEnd(message.getReceiverId(), now);
            if (quietEnd != null) {
                message.setNextRemindTime(quietEnd);
                messageMapper.updateById(message);
                continue;
            }

            int remindCount = (message.getRemindCount() == null ? 0 : message.getRemindCount()) + 1;
            message.setRemindCount(remindCount);
            message.setLastRemindTime(now);
            long interval =
                notice.getRepeatIntervalSeconds() == null ? 0L : notice.getRepeatIntervalSeconds();
            int maxCount = notice.getMaxRemindCount() == null ? 1 : notice.getMaxRemindCount();
            message.setNextRemindTime(
                interval > 0 && remindCount < maxCount ? now.plusSeconds(interval) : null);
            messageMapper.updateById(message);
            eventPublisher.publishAfterCommit(message.getReceiverId(), "reminder.created",
                Map.of("id", message.getId().toString(), "title", notice.getTitle(), "category",
                    message.getCategory(), "remindCount", remindCount));
        }
    }

    private LocalDateTime quietEnd(Long userId, LocalDateTime now) {
        SysNotificationPreference preference = preferenceMapper.selectOne(
            new LambdaQueryWrapper<SysNotificationPreference>().eq(
                SysNotificationPreference::getUserId, userId));
        if (preference == null || preference.getQuietEnabled() == null
            || preference.getQuietEnabled() != 1 || preference.getQuietStart() == null
            || preference.getQuietEnd() == null) {
            return null;
        }
        ZoneId zone = ZoneId.of(preference.getTimezone());
        LocalTime current = now.atZone(zone).toLocalTime();
        LocalTime start = preference.getQuietStart();
        LocalTime end = preference.getQuietEnd();
        boolean overnight = start.isAfter(end);
        boolean quiet = overnight ? !current.isBefore(start) || current.isBefore(end) :
            !current.isBefore(start) && current.isBefore(end);
        if (!quiet) {
            return null;
        }
        if (overnight && current.isBefore(end)) {
            return now.with(end);
        }
        return now.plusDays(1).with(end);
    }
}

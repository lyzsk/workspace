package cn.sichu.system.notice.service;

import cn.sichu.system.notice.entity.SysMessage;
import cn.sichu.system.notice.entity.SysNotice;
import cn.sichu.system.user.entity.SysUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
class NoticeTemplateRendererTest {

    private final NoticeTemplateRenderer renderer = new NoticeTemplateRenderer(new ObjectMapper());

    @Test
    void rendersOnlySupportedPlaceholders() {
        SysNotice notice = new SysNotice();
        notice.setTitle("${realName}：待办通知");
        notice.setContent("${param.team} ${dueTime} #{system}");
        notice.setTemplateCode("default");
        notice.setTemplateParams("{\"team\":\"研发部\"}");
        SysMessage message = new SysMessage();
        message.setDueTime(LocalDateTime.of(2026, 11, 16, 9, 0));
        SysUser user = new SysUser();
        user.setUsername("test");
        user.setRealName("测试用户");

        assertEquals("测试用户：待办通知", renderer.title(notice, message, user));
        assertEquals("研发部 2026-11-16T09:00 #{system}",
            renderer.content(notice, message, user));
    }
}

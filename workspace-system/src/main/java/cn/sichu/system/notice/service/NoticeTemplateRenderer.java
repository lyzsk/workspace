package cn.sichu.system.notice.service;

import cn.sichu.system.notice.entity.SysMessage;
import cn.sichu.system.notice.entity.SysNotice;
import cn.sichu.system.user.entity.SysUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component
@RequiredArgsConstructor
public class NoticeTemplateRenderer {

    private static final TypeReference<Map<String, String>> PARAMS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public String title(SysNotice notice, SysMessage message, SysUser user) {
        return render(notice.getTitle(), notice, message, user);
    }

    public String content(SysNotice notice, SysMessage message, SysUser user) {
        return render(notice.getContent(), notice, message, user);
    }

    private String render(String template, SysNotice notice, SysMessage message, SysUser user) {
        if (notice.getTemplateCode() == null || notice.getTemplateCode().isBlank()) {
            return template;
        }
        String result = template
            .replace("${username}", user == null ? "" : nullToEmpty(user.getUsername()))
            .replace("${realName}", user == null ? "" : nullToEmpty(user.getRealName()))
            .replace("${dueTime}", message.getDueTime() == null ? "" : message.getDueTime().toString());
        for (Map.Entry<String, String> entry : params(notice.getTemplateParams()).entrySet()) {
            result = result.replace("${param." + entry.getKey() + "}",
                nullToEmpty(entry.getValue()));
        }
        return result;
    }

    private Map<String, String> params(String value) {
        try {
            if (value == null || value.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(value, PARAMS);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

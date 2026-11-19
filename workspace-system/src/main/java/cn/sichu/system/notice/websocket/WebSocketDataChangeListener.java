package cn.sichu.system.notice.websocket;

import cn.sichu.common.core.event.DataChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketDataChangeListener {

    private final NotificationWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onDataChanged(DataChangedEvent event) {
        try {
            handler.broadcast(objectMapper.writeValueAsString(Map.of(
                "type", "data.changed",
                "data", Map.of("path", event.path(), "method", event.method(),
                    "time", event.time().toString()))));
        } catch (Exception e) {
            log.warn("数据变更广播失败 path={} msg={}", event.path(), e.getMessage());
        }
    }
}

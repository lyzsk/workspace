package cn.sichu.system.notice.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final NotificationWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    public void publishAfterCommit(Long userId, String type, Object data) {
        Runnable action = () -> publish(userId, type, data);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
        } else {
            action.run();
        }
    }

    private void publish(Long userId, String type, Object data) {
        try {
            handler.send(userId, objectMapper.writeValueAsString(
                Map.of("type", type, "data", data, "time", LocalDateTime.now().toString())));
        } catch (Exception e) {
            log.warn("通知 WebSocket 推送失败 userId={} type={} msg={}", userId, type, e.getMessage());
        }
    }
}

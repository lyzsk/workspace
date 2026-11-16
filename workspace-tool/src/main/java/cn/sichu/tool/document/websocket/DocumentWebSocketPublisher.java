package cn.sichu.tool.document.websocket;

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
public class DocumentWebSocketPublisher {

    private final DocumentWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    public void publishAfterCommit(Long documentId, String type, Object data) {
        Runnable action = () -> publish(documentId, type, data);
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

    private void publish(Long documentId, String type, Object data) {
        try {
            handler.broadcast(documentId, objectMapper.writeValueAsString(
                Map.of("type", type, "data", data, "time", LocalDateTime.now().toString())));
        } catch (Exception e) {
            log.warn("文档 WebSocket 推送失败 documentId={} type={} msg={}",
                documentId, type, e.getMessage());
        }
    }
}

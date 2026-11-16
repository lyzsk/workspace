package cn.sichu.system.notice.websocket;

import cn.sichu.common.event.DataChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class WebSocketDataChangeListenerTest {

    @Test
    void eventIsSerializedAndBroadcast() {
        NotificationWebSocketHandler handler = mock(NotificationWebSocketHandler.class);
        WebSocketDataChangeListener listener = new WebSocketDataChangeListener(handler,
            new ObjectMapper());

        listener.onDataChanged(new DataChangedEvent("/api/system/roles/1", "PUT",
            LocalDateTime.of(2026, 11, 16, 12, 0)));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(handler).broadcast(payload.capture());
        assertTrue(payload.getValue().contains("\"type\":\"data.changed\""));
        assertTrue(payload.getValue().contains("\"path\":\"/api/system/roles/1\""));
        assertTrue(payload.getValue().contains("\"method\":\"PUT\""));
    }
}

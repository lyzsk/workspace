package cn.sichu.tool.document.websocket;

import cn.sichu.tool.document.model.form.SysDocumentOperationForm;
import cn.sichu.tool.document.service.IDocumentService;
import cn.sichu.common.security.model.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component
@RequiredArgsConstructor
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ObjectProvider<IDocumentService> documentService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        rooms.computeIfAbsent(documentId(session), key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
        throws IOException {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            return;
        }
        try {
            SysDocumentOperationForm form = objectMapper.readValue(message.getPayload(),
                SysDocumentOperationForm.class);
            if ("PRESENCE".equals(form.getOpType())) {
                broadcast(documentId(session), message.getPayload());
                return;
            }
            documentService.getObject().applyOperation(documentId(session), userId(session), form);
        } catch (Exception e) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                Map.of("type", "error", "message", e.getMessage()))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long documentId = documentId(session);
        Set<WebSocketSession> room = rooms.get(documentId);
        if (room != null) {
            room.remove(session);
            if (room.isEmpty()) {
                rooms.remove(documentId);
            }
        }
    }

    public void broadcast(Long documentId, String payload) {
        Set<WebSocketSession> room = rooms.get(documentId);
        if (room == null) {
            return;
        }
        for (WebSocketSession session : room) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (IOException e) {
                room.remove(session);
            }
        }
    }

    private Long documentId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return Long.valueOf(path.substring(path.lastIndexOf('/') + 1));
    }

    private Long userId(WebSocketSession session) {
        return ((LoginUser) session.getAttributes().get("loginUser")).getUserId();
    }
}

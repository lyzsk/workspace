package cn.sichu.system.notice.websocket;

import cn.sichu.system.security.model.LoginUser;
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
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.computeIfAbsent(userId(session), key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
        throws IOException {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    public void send(Long userId, String payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        for (WebSocketSession session : userSessions) {
            sendToSession(userSessions, session, payload);
        }
    }

    public void broadcast(String payload) {
        for (Set<WebSocketSession> userSessions : sessions.values()) {
            for (WebSocketSession session : userSessions) {
                sendToSession(userSessions, session, payload);
            }
        }
    }

    private void sendToSession(Set<WebSocketSession> userSessions, WebSocketSession session,
        String payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException e) {
            userSessions.remove(session);
        }
    }

    private Long userId(WebSocketSession session) {
        return ((LoginUser) session.getAttributes().get("loginUser")).getUserId();
    }
}

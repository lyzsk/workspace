package cn.sichu.tool.document.websocket;

import cn.sichu.tool.document.service.IDocumentService;
import cn.sichu.common.security.model.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component
@RequiredArgsConstructor
public class DocumentAccessHandshakeInterceptor implements HandshakeInterceptor {

    private final IDocumentService documentService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Map<String, Object> attributes) {
        LoginUser user = (LoginUser) attributes.get("loginUser");
        String path = request.getURI().getPath();
        Long documentId = Long.valueOf(path.substring(path.lastIndexOf('/') + 1));
        documentService.assertPermission(documentId, user.getUserId(), "READ");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Exception exception) {
    }
}

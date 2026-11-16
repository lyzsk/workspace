package cn.sichu.system.security.handler;

import cn.sichu.common.result.IResultCode;
import cn.sichu.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int httpStatus, IResultCode resultCode)
        throws IOException {
        write(response, httpStatus, resultCode, resultCode.getMsg());
    }

    public void write(HttpServletResponse response, int httpStatus, IResultCode resultCode,
        String message) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.failed(resultCode, message));
    }
}

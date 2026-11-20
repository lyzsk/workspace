package cn.sichu.system.sync;

import cn.sichu.common.core.event.DataChangedEvent;
import cn.sichu.common.core.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class DataChangeAspectTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final DataChangeAspect aspect = new DataChangeAspect(eventPublisher);

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void ignoresAccessTelemetry() {
        request("POST", "/api/system/dashboard/access-events/app-a");

        aspect.afterReturning(Result.success());

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void publishesBusinessChanges() {
        request("POST", "/api/system/users");

        aspect.afterReturning(Result.success());

        verify(eventPublisher).publishEvent(any(DataChangedEvent.class));
    }

    private void request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}

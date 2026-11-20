package cn.sichu.system.log.aspect;

import cn.sichu.common.core.log.annotation.Log;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.auth.model.vo.UserOrgVO;
import cn.sichu.system.log.entity.SysOperLog;
import cn.sichu.system.log.service.ILogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class OperLogAspectTest {

    private final ILogService logService = mock(ILogService.class);
    private final AuthQueryMapper authQueryMapper = mock(AuthQueryMapper.class);
    private final OperLogAspect aspect = new OperLogAspect(logService, new ObjectMapper(),
        authQueryMapper);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void persistsStructuredRequestAndErrorDetails() throws Throwable {
        LoginUser user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("admin");
        when(user.getTenantId()).thenReturn(0L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(authQueryMapper.selectUserOrg(1L)).thenReturn(new UserOrgVO("默认公司", "数据中心"));
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/system/users/1");
        request.setQueryString("force=true");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getDeclaringTypeName()).thenReturn("cn.sichu.TestController");
        when(signature.getName()).thenReturn("update");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"body"});
        doThrow(new IllegalStateException("failed")).when(joinPoint).proceed();
        Log annotation = mock(Log.class);
        when(annotation.module()).thenReturn("用户管理");
        when(annotation.operation()).thenReturn("修改用户");

        assertThrows(IllegalStateException.class, () -> aspect.around(joinPoint, annotation));

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(logService).record(captor.capture());
        SysOperLog entry = captor.getValue();
        assertEquals("cn.sichu.TestController.update", entry.getMethod());
        assertEquals("force=true", entry.getRequestQuery());
        assertEquals("JUnit", entry.getUserAgent());
        assertEquals("数据中心", entry.getDeptName());
        assertEquals(IllegalStateException.class.getName(), entry.getErrorType());
    }
}

package cn.sichu.system.dashboard.service.impl;

import cn.sichu.common.redis.cache.CacheKeys;
import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.dashboard.entity.AppAccessLog;
import cn.sichu.system.dashboard.mapper.DashboardMapper;
import cn.sichu.system.dashboard.model.vo.DashboardVO;
import cn.sichu.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class DashboardServiceImplTest {

    private final DashboardMapper dashboardMapper = mock(DashboardMapper.class);
    private final SysAppMapper appMapper = mock(SysAppMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final LoginSessionStore sessionStore = mock(LoginSessionStore.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> redisValues = mock(ValueOperations.class);
    private final IAppAuthorizationService authorizationService = mock(IAppAuthorizationService.class);
    private final DashboardServiceImpl service = new DashboardServiceImpl(dashboardMapper,
        appMapper, userMapper, sessionStore, redisTemplate, authorizationService);

    @BeforeEach
    void setUpRedis() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAuthenticatedUserAndApplication() {
        LoginUser user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        when(user.getTenantId()).thenReturn(0L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of()));
        SysApp app = new SysApp();
        app.setId(2000L);
        app.setCode("app-a");
        when(appMapper.selectOne(any())).thenReturn(app);

        service.recordAccess("app-a");

        ArgumentCaptor<AppAccessLog> captor = ArgumentCaptor.forClass(AppAccessLog.class);
        verify(authorizationService).assertAccess(1L, "app-a");
        verify(dashboardMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(2000L, captor.getValue().getAppId());
    }

    @Test
    void fillsTrendDaysAndDeduplicatesOnlineUsers() {
        LoginUser first = mock(LoginUser.class);
        when(first.getUserId()).thenReturn(1L);
        LoginUser second = mock(LoginUser.class);
        when(second.getUserId()).thenReturn(2L);
        LocalDateTime now = LocalDateTime.now();
        when(sessionStore.listAll()).thenReturn(List.of(
            new LoginSession("s1", first, now, now, now.plusHours(1), false, null, null, null),
            new LoginSession("s2", first, now, now, now.plusHours(1), false, null, null, null),
            new LoginSession("s3", second, now, now, now.plusHours(1), false, null, null, null)));
        when(dashboardMapper.selectDailyVisits(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectDailyNewUsers(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectAppVisits(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectTopUsers(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectUserDepartments()).thenReturn(List.of());
        when(dashboardMapper.selectAppDepartments(any(), any())).thenReturn(List.of());

        DashboardVO result = service.summary();

        assertEquals(2, result.getOnlineUsers());
        assertEquals(7, result.getWeekTrend().size());
    }

    @Test
    void includesCurrentHourInWeekApiCalls() {
        LocalDateTime now = LocalDateTime.now();
        String currentHour = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        when(redisValues.get(CacheKeys.apiRequestHour(currentHour))).thenReturn("5");
        when(sessionStore.listAll()).thenReturn(List.of());
        when(dashboardMapper.selectDailyVisits(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectDailyNewUsers(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectAppVisits(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectTopUsers(any(), any())).thenReturn(List.of());
        when(dashboardMapper.selectUserDepartments()).thenReturn(List.of());
        when(dashboardMapper.selectAppDepartments(any(), any())).thenReturn(List.of());

        DashboardVO result = service.summary();

        assertEquals(5, result.getWeekApiCalls());
        assertEquals(5, result.getApiCallTrend().get(6).getValue());
    }
}

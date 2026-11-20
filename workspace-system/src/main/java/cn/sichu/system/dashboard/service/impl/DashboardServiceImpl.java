package cn.sichu.system.dashboard.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.redis.cache.CacheKeys;
import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.util.SecurityUtils;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.dashboard.entity.AppAccessLog;
import cn.sichu.system.dashboard.mapper.DashboardMapper;
import cn.sichu.system.dashboard.model.vo.DashboardVO;
import cn.sichu.system.dashboard.service.IDashboardService;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final DashboardMapper dashboardMapper;
    private final SysAppMapper appMapper;
    private final SysUserMapper userMapper;
    private final LoginSessionStore sessionStore;
    private final StringRedisTemplate redisTemplate;
    private final IAppAuthorizationService authorizationService;

    @Override
    public void recordAccess(String appCode) {
        LoginUser user = SecurityUtils.getLoginUser();
        SysApp app = appMapper.selectOne(
            new LambdaQueryWrapper<SysApp>().eq(SysApp::getCode, appCode));
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        authorizationService.assertAccess(user.getUserId(), appCode);
        AppAccessLog event = new AppAccessLog();
        event.setTenantId(user.getTenantId() == null ? 0L : user.getTenantId());
        event.setUserId(user.getUserId());
        event.setAppId(app.getId());
        event.setAccessTime(LocalDateTime.now());
        dashboardMapper.insert(event);
    }

    @Override
    public DashboardVO summary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime lastWeekStart = weekStart.minusWeeks(1);
        LocalDateTime lastWeekSameTime = now.minusWeeks(1);

        DashboardVO result = new DashboardVO();
        result.setTotalVisits(dashboardMapper.countTotalVisits());
        result.setTodayVisits(dashboardMapper.countVisits(todayStart, tomorrow));
        result.setTodayUv(dashboardMapper.countUv(todayStart, tomorrow));
        result.setOnlineUsers(sessionStore.listAll().stream()
            .filter(session -> !session.revoked() && now.isBefore(session.expiresAt()))
            .map(session -> session.loginUser().getUserId()).collect(Collectors.toSet()).size());
        result.setTotalUsers(userMapper.selectCount(null));
        result.setTotalApps(appMapper.selectCount(
            new LambdaQueryWrapper<SysApp>().ne(SysApp::getCode, "workspace")));
        result.setEnabledApps(appMapper.selectCount(new LambdaQueryWrapper<SysApp>()
            .ne(SysApp::getCode, "workspace").eq(SysApp::getStatus, 1)));

        long weekVisits = dashboardMapper.countVisits(weekStart, now);
        long lastWeekVisits = dashboardMapper.countVisits(lastWeekStart, lastWeekSameTime);
        long lastWeekTodayVisits = dashboardMapper.countVisits(todayStart.minusWeeks(1),
            lastWeekSameTime);
        long lastWeekUv = dashboardMapper.countUv(todayStart.minusWeeks(1), lastWeekSameTime);
        long weekNewUsers = dashboardMapper.countNewUsers(weekStart, now);
        long lastWeekNewUsers = dashboardMapper.countNewUsers(lastWeekStart, lastWeekSameTime);
        result.setVisitsWeekChange(percentChange(weekVisits, lastWeekVisits));
        result.setTodayVisitsWeekChange(
            percentChange(result.getTodayVisits(), lastWeekTodayVisits));
        result.setActiveUsersWeekChange(percentChange(result.getTodayUv(), lastWeekUv));
        result.setUsersWeekChange(percentChange(weekNewUsers, lastWeekNewUsers));
        LocalDateTime apiCallWeekStart = today.minusDays(6).atStartOfDay();
        LocalDateTime apiCallHourEnd = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        long apiCalls = sumApiCalls(apiCallWeekStart, apiCallHourEnd);
        long previousApiCalls = sumApiCalls(apiCallWeekStart.minusDays(7),
            apiCallHourEnd.minusDays(7));
        result.setWeekApiCalls(apiCalls);
        result.setApiCallsWeekChange(percentChange(apiCalls, previousApiCalls));

        result.setWeekTrend(fillDays(today.minusDays(6), today,
            dashboardMapper.selectDailyVisits(today.minusDays(6).atStartOfDay(), tomorrow)));
        result.setUserGrowth(fillValues(today.minusDays(6), today,
            dashboardMapper.selectDailyNewUsers(today.minusDays(6).atStartOfDay(), tomorrow)));
        result.setApiCallTrend(apiCallTrend(today));
        result.setAppRanking(dashboardMapper.selectAppVisits(weekStart, now));
        result.setTopUsers(dashboardMapper.selectTopUsers(weekStart, now));
        result.setUserDepartments(dashboardMapper.selectUserDepartments());
        result.setAppDepartments(dashboardMapper.selectAppDepartments(weekStart, now));
        return result;
    }

    private List<DashboardVO.DailyVisitVO> fillDays(LocalDate begin, LocalDate end,
        List<DashboardVO.DailyVisitVO> actual) {
        Map<String, DashboardVO.DailyVisitVO> values = actual.stream().collect(
            Collectors.toMap(DashboardVO.DailyVisitVO::getDay, Function.identity()));
        List<DashboardVO.DailyVisitVO> result = new ArrayList<>();
        for (LocalDate day = begin; !day.isAfter(end); day = day.plusDays(1)) {
            result.add(values.getOrDefault(day.toString(), emptyDay(day)));
        }
        return result;
    }

    private DashboardVO.DailyVisitVO emptyDay(LocalDate day) {
        DashboardVO.DailyVisitVO value = new DashboardVO.DailyVisitVO();
        value.setDay(day.toString());
        return value;
    }

    private List<DashboardVO.DailyValueVO> fillValues(LocalDate begin, LocalDate end,
        List<DashboardVO.DailyValueVO> actual) {
        Map<String, DashboardVO.DailyValueVO> values = actual.stream().collect(
            Collectors.toMap(DashboardVO.DailyValueVO::getDay, Function.identity()));
        List<DashboardVO.DailyValueVO> result = new ArrayList<>();
        for (LocalDate day = begin; !day.isAfter(end); day = day.plusDays(1)) {
            DashboardVO.DailyValueVO value = values.get(day.toString());
            if (value == null) {
                value = new DashboardVO.DailyValueVO();
                value.setDay(day.toString());
            }
            result.add(value);
        }
        return result;
    }

    private long sumApiCalls(LocalDateTime begin, LocalDateTime end) {
        long total = 0;
        for (LocalDateTime hour = begin; hour.isBefore(end); hour = hour.plusHours(1)) {
            String value = redisTemplate.opsForValue().get(
                CacheKeys.apiRequestHour(hour.format(HOUR_FORMAT)));
            if (value != null) {
                total += Long.parseLong(value);
            }
        }
        return total;
    }

    private List<DashboardVO.DailyValueVO> apiCallTrend(LocalDate today) {
        List<DashboardVO.DailyValueVO> result = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            DashboardVO.DailyValueVO value = new DashboardVO.DailyValueVO();
            value.setDay(day.toString());
            value.setValue(sumApiCalls(day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
            result.add(value);
        }
        return result;
    }

    private double percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        return Math.round((current - previous) * 1000.0 / previous) / 10.0;
    }
}

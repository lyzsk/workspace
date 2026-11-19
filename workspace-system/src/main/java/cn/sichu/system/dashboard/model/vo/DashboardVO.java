package cn.sichu.system.dashboard.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Data
public class DashboardVO {

    private long totalVisits;
    private long todayVisits;
    private long weekApiCalls;
    private long todayUv;
    private long onlineUsers;
    private long totalUsers;
    private long totalApps;
    private long enabledApps;
    private double visitsWeekChange;
    private double todayVisitsWeekChange;
    private double apiCallsWeekChange;
    private double activeUsersWeekChange;
    private double usersWeekChange;
    private List<DailyVisitVO> weekTrend;
    private List<DailyValueVO> userGrowth;
    private List<DailyValueVO> apiCallTrend;
    private List<AppVisitVO> appRanking;
    private List<UserVisitVO> topUsers;
    private List<DeptDistributionVO> userDepartments;
    private List<AppDepartmentVO> appDepartments;

    @Data
    public static class DailyVisitVO {
        private String day;
        private long pv;
        private long uv;
    }

    @Data
    public static class DailyValueVO {
        private String day;
        private long value;
    }

    @Data
    public static class AppVisitVO {
        private String appCode;
        private String appName;
        private long visits;
    }

    @Data
    public static class UserVisitVO {
        private String userId;
        private String username;
        private String realName;
        private String departmentName;
        private long visits;
        private LocalDateTime lastVisitTime;
    }

    @Data
    public static class DeptDistributionVO {
        private String name;
        private long value;
    }

    @Data
    public static class AppDepartmentVO {
        private String appCode;
        private String departmentName;
        private long userCount;
    }
}

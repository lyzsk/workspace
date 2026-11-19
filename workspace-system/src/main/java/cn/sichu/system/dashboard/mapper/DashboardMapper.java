package cn.sichu.system.dashboard.mapper;

import cn.sichu.system.dashboard.entity.AppAccessLog;
import cn.sichu.system.dashboard.model.vo.DashboardVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Mapper
public interface DashboardMapper extends BaseMapper<AppAccessLog> {

    long countTotalVisits();

    long countVisits(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    long countUv(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    long countNewUsers(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    List<DashboardVO.DailyVisitVO> selectDailyVisits(@Param("begin") LocalDateTime begin,
        @Param("end") LocalDateTime end);

    List<DashboardVO.DailyValueVO> selectDailyNewUsers(@Param("begin") LocalDateTime begin,
        @Param("end") LocalDateTime end);

    List<DashboardVO.AppVisitVO> selectAppVisits(@Param("begin") LocalDateTime begin,
        @Param("end") LocalDateTime end);

    List<DashboardVO.UserVisitVO> selectTopUsers(@Param("begin") LocalDateTime begin,
        @Param("end") LocalDateTime end);

    List<DashboardVO.DeptDistributionVO> selectUserDepartments();

    List<DashboardVO.AppDepartmentVO> selectAppDepartments(@Param("begin") LocalDateTime begin,
        @Param("end") LocalDateTime end);
}

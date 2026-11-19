package cn.sichu.system.dashboard.service;

import cn.sichu.system.dashboard.model.vo.DashboardVO;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
public interface IDashboardService {

    void recordAccess(String appCode);

    DashboardVO summary();
}

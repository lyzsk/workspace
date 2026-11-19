package cn.sichu.system.app.grant.service;

import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public interface IAppAuthorizationService {

    AppAuthorizationVO getAuthorization(Long userId, String appCode);

    AppAuthorizationVO currentAuthorization(String appCode);

    void assertAccess(Long userId, String appCode);

    List<SysApp> listAccessibleApps(Long userId);
}

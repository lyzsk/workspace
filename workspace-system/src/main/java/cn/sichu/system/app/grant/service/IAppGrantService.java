package cn.sichu.system.app.grant.service;

import cn.sichu.system.app.grant.model.form.AppGrantForm;
import cn.sichu.system.app.grant.model.vo.AppGrantVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public interface IAppGrantService {

    List<AppGrantVO> listUserGrants(Long userId);

    void saveGrant(Long userId, String appCode, AppGrantForm form);

    void ensureWorkspaceGrant(Long userId, Long tenantId);
}

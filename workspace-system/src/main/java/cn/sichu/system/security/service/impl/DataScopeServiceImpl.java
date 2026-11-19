package cn.sichu.system.security.service.impl;

import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.security.model.DataScopeFilter;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.system.security.service.IDataScopeService;
import cn.sichu.common.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements IDataScopeService {

    private final IAppAuthorizationService authorizationService;

    @Override
    public DataScopeFilter current() {
        return current("workspace");
    }

    @Override
    public DataScopeFilter current(String appCode) {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null) {
            return new DataScopeFilter(false, Set.of(), true, null);
        }
        AppAuthorizationVO authorization = authorizationService.getAuthorization(
            loginUser.getUserId(), appCode);
        Set<Long> deptIds = authorization.getDeptIds().stream()
            .map(Long::valueOf).collect(Collectors.toSet());
        return new DataScopeFilter(authorization.isAll(), deptIds,
            authorization.isSelfOnly(), loginUser.getUserId());
    }
}

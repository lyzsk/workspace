package cn.sichu.system.app.grant.service.impl;

import cn.sichu.common.event.DataChangedEvent;
import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.ResultCode;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.entity.SysUserApp;
import cn.sichu.system.app.grant.entity.SysUserAppDept;
import cn.sichu.system.app.grant.mapper.AppAuthorizationMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppDeptMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppMapper;
import cn.sichu.system.app.grant.model.form.AppGrantForm;
import cn.sichu.system.app.grant.model.vo.AppGrantVO;
import cn.sichu.system.app.grant.service.IAppGrantService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.dept.entity.SysDept;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.entity.SysUserRole;
import cn.sichu.system.user.mapper.SysUserMapper;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Service
@RequiredArgsConstructor
public class AppGrantServiceImpl implements IAppGrantService {

    private static final String WORKSPACE = "workspace";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysUserMapper userMapper;
    private final SysAppMapper appMapper;
    private final SysUserAppMapper userAppMapper;
    private final SysUserAppDeptMapper userAppDeptMapper;
    private final AppAuthorizationMapper authorizationMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<AppGrantVO> listUserGrants(Long userId) {
        requireUser(userId);
        List<SysApp> apps = appMapper.selectList(
            new LambdaQueryWrapper<SysApp>().orderByAsc(SysApp::getSortNo)
                .orderByAsc(SysApp::getId));
        return apps.stream().map(app -> toGrantVO(userId, app)).toList();
    }

    @Override
    @Transactional
    public void saveGrant(Long userId, String appCode, AppGrantForm form) {
        SysUser user = requireUser(userId);
        SysApp app = requireApp(appCode);
        validateForm(form);
        if (Objects.equals(userId, 1L) && WORKSPACE.equals(appCode) && form.getStatus() == 0) {
            throw new BusinessException(SystemCode.USER_PROTECTED);
        }
        SysUserApp grant = findGrant(userId, app.getId());
        if (grant == null) {
            grant = new SysUserApp();
            grant.setTenantId(
                user.getTenantId() == null ? 0L : user.getTenantId());
            grant.setUserId(userId);
            grant.setAppId(app.getId());
        }
        grant.setStatus(form.getStatus());
        grant.setDataScope(form.getDataScope());
        if (grant.getId() == null) {
            userAppMapper.insert(grant);
        } else {
            userAppMapper.updateById(grant);
        }
        saveDepartments(grant, form.getDeptIds());
        saveRoles(user, app, form.getRoleIds());
        eventPublisher.publishEvent(new DataChangedEvent(
            "/api/system/users/" + userId + "/apps/" + appCode + "/grant",
            "PUT", LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void ensureWorkspaceGrant(Long userId, Long tenantId) {
        SysApp workspace = requireApp(WORKSPACE);
        if (findGrant(userId, workspace.getId()) != null) {
            return;
        }
        SysUserApp grant = new SysUserApp();
        grant.setTenantId(tenantId == null ? 0L : tenantId);
        grant.setUserId(userId);
        grant.setAppId(workspace.getId());
        grant.setStatus(1);
        userAppMapper.insert(grant);
    }

    private AppGrantVO toGrantVO(Long userId, SysApp app) {
        SysUserApp grant = findGrant(userId, app.getId());
        List<SysRole> roles =
            authorizationMapper.selectAssignedRoles(userId, app.getCode());
        boolean hasActiveRole =
            !authorizationMapper.selectRoles(userId, app.getCode()).isEmpty();
        AppGrantVO vo = new AppGrantVO();
        vo.setAppCode(app.getCode());
        vo.setAppName(app.getName());
        vo.setAppStatus(app.getStatus());
        vo.setGranted(grant != null);
        vo.setEffective(grant != null && Objects.equals(grant.getStatus(), 1)
            && Objects.equals(app.getStatus(), 1) && hasActiveRole);
        vo.setStatus(grant == null ? 0 : grant.getStatus());
        vo.setDataScope(grant == null ? null : grant.getDataScope());
        vo.setRoleIds(
            roles.stream().map(role -> role.getId().toString()).toList());
        vo.setRoleNames(roles.stream().map(SysRole::getRoleName).toList());
        vo.setDeptIds(grant == null ? List.of() :
            customDeptIds(grant.getId()).stream().map(String::valueOf)
                .toList());
        return vo;
    }

    private void saveRoles(SysUser user, SysApp app, List<Long> roleIds) {
        List<Long> requested = roleIds.stream().distinct().toList();
        List<SysRole> roles = requested.isEmpty() ? List.of() :
            roleMapper.selectBatchIds(requested);
        if (roles.size() != requested.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "包含不存在的角色");
        }
        for (SysRole role : roles) {
            if (!Objects.equals(role.getAppId(), app.getId())) {
                throw new BusinessException(ResultCode.FORBIDDEN,
                    "不能分配其他应用的角色");
            }
            if (!Objects.equals(role.getStatus(), 1)) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                    "不能分配已停用角色");
            }
            if (SUPER_ADMIN.equals(role.getRoleCode())
                && !isCurrentSuperAdmin()) {
                throw new BusinessException(ResultCode.FORBIDDEN,
                    "无权分配超级管理员角色");
            }
        }
        if (Objects.equals(user.getId(), 1L) && WORKSPACE.equals(app.getCode())
            && roles.stream().noneMatch(role -> SUPER_ADMIN.equals(role.getRoleCode()))) {
            throw new BusinessException(SystemCode.USER_PROTECTED);
        }
        List<Long> appRoleIds = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getAppId,
                app.getId())).stream().map(SysRole::getId).toList();
        if (!appRoleIds.isEmpty()) {
            userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId,
                    user.getId()).in(SysUserRole::getRoleId, appRoleIds));
        }
        for (Long roleId : requested) {
            SysUserRole relation = new SysUserRole();
            relation.setTenantId(
                user.getTenantId() == null ? 0L : user.getTenantId());
            relation.setUserId(user.getId());
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private void saveDepartments(SysUserApp grant, List<Long> deptIds) {
        userAppDeptMapper.delete(new LambdaQueryWrapper<SysUserAppDept>().eq(
            SysUserAppDept::getUserAppId, grant.getId()));
        if (grant.getDataScope() == null || grant.getDataScope() != 2) {
            return;
        }
        for (Long deptId : deptIds.stream().distinct().toList()) {
            SysUserAppDept relation = new SysUserAppDept();
            relation.setTenantId(grant.getTenantId());
            relation.setUserAppId(grant.getId());
            relation.setDeptId(deptId);
            userAppDeptMapper.insert(relation);
        }
    }

    private void validateForm(AppGrantForm form) {
        if (form.getStatus() != 0 && form.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "应用授权状态无效");
        }
        if (form.getStatus() == 1 && form.getRoleIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "启用应用授权前必须至少分配一个角色");
        }
        if (form.getDataScope() != null && !Set.of(1, 2, 3, 4, 5)
            .contains(form.getDataScope())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "数据范围无效");
        }
        if (Objects.equals(form.getDataScope(), 2)) {
            List<Long> deptIds = form.getDeptIds().stream().distinct().toList();
            if (deptIds.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                    "指定部门不能为空");
            }
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            if (depts.size() != deptIds.size()) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                    "包含不存在的部门");
            }
        }
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        return user;
    }

    private SysApp requireApp(String appCode) {
        SysApp app = appMapper.selectOne(
            new LambdaQueryWrapper<SysApp>().eq(SysApp::getCode, appCode));
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        return app;
    }

    private SysUserApp findGrant(Long userId, Long appId) {
        return userAppMapper.selectOne(
            new LambdaQueryWrapper<SysUserApp>().eq(SysUserApp::getUserId,
                userId).eq(SysUserApp::getAppId, appId));
    }

    private List<Long> customDeptIds(Long userAppId) {
        return userAppDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserAppDept>().eq(
                    SysUserAppDept::getUserAppId, userAppId)).stream()
            .map(SysUserAppDept::getDeptId).toList();
    }

    private boolean isCurrentSuperAdmin() {
        LoginUser current = SecurityUtils.getLoginUserOrNull();
        return current != null && current.getRoles().contains(SUPER_ADMIN);
    }
}

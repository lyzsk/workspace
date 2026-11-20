package cn.sichu.system.user.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.common.core.utils.QuerySortUtils;
import cn.sichu.common.redis.cache.AuthorizationStore;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.entity.SysUserApp;
import cn.sichu.system.app.grant.mapper.SysUserAppMapper;
import cn.sichu.system.app.grant.service.IAppGrantService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.security.model.DataScopeFilter;
import cn.sichu.system.security.service.IDataScopeService;
import cn.sichu.common.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.entity.SysUserRole;
import cn.sichu.system.user.mapper.SysUserMapper;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import cn.sichu.system.auth.model.vo.UserInfoVO;
import cn.sichu.system.auth.model.vo.UserOrgVO;
import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.user.model.form.PasswordForm;
import cn.sichu.system.user.model.form.ProfileForm;
import cn.sichu.system.user.model.form.SysUserForm;
import cn.sichu.system.user.model.query.SysUserQuery;
import cn.sichu.system.user.model.vo.SysUserPageVO;
import cn.sichu.system.user.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.sichu.common.core.base.BaseImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends BaseImpl<SysUserMapper, SysUser> implements IUserService {
    private static final Long PROTECTED_USER_ID = 1L;

    private final PasswordEncoder passwordEncoder;
    private final AuthTokenStore authTokenStore;
    private final SysUserRoleMapper userRoleMapper;
    private final IDataScopeService dataScopeService;
    private final IAppGrantService appGrantService;
    private final SysAppMapper appMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserAppMapper userAppMapper;
    private final AuthQueryMapper authQueryMapper;
    private final AuthorizationStore authorizationStore;

    @Value("${workspace.security.super-admin-assignee:superadmin}")
    private String superAdminAssignee;

    @Override
    @cn.sichu.system.security.datascope.DataScope(appCode = "workspace")
    public PageResult<SysUserPageVO> page(SysUserQuery query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysUser> result = baseMapper.selectPage(page, buildWrapper(query));
        List<SysUserPageVO> records = result.getRecords().stream()
            .map(user -> SysUserPageVO.from(user, roleIdsOf(user.getId()))).toList();

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SysUserPageVO get(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user == null
            || (isSuperAdminAccount(user) && !isCurrentSuperAdminAssignee())) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        assertVisible(user);
        return SysUserPageVO.from(user, roleIdsOf(user.getId()));
    }

    @Override
    @Transactional
    public Long create(SysUserForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BusinessException(SystemCode.PASSWORD_REQUIRED);
        }
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, form.getUsername()));
        if (count > 0) {
            throw new BusinessException(SystemCode.USERNAME_EXISTS);
        }

        SysUser user = new SysUser();
        applyForm(user, form);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setTokenVersion(0L);
        baseMapper.insert(user);
        if (form.getRoleIds() != null && !form.getRoleIds().isEmpty()) {
            saveRoles(user.getId(), form.getRoleIds(), user.getTenantId());
        }
        appGrantService.ensureWorkspaceGrant(user.getId(), user.getTenantId());
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysUserForm form) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        checkUserManageable(user);
        if (isSuperAdminAccount(user) && form.getStatus() != null && form.getStatus() == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能停用超级管理员");
        }
        assertVisible(user);

        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, form.getUsername())
                .ne(SysUser::getId, id));
        if (count > 0) {
            throw new BusinessException(SystemCode.USERNAME_EXISTS);
        }

        Integer oldStatus = user.getStatus();
        boolean passwordChanged = StringUtils.hasText(form.getPassword());
        boolean disabled = form.getStatus() != null && form.getStatus() == 0
            && oldStatus != null && oldStatus == 1;

        applyForm(user, form);
        if (passwordChanged) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        if (passwordChanged || disabled) {
            user.setTokenVersion(
                (user.getTokenVersion() == null ? 0L : user.getTokenVersion()) + 1);
        }
        baseMapper.updateById(user);
        if (form.getRoleIds() != null) {
            saveRoles(id, form.getRoleIds(), user.getTenantId());
        }
        // 角色、状态或资料变化后，当前用户在所有应用的有效授权都需要重新计算。
        authorizationStore.deleteUser(id);
        if (passwordChanged || disabled) {
            authTokenStore.revokeAllSessions(id);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        checkUserDeletable(user);
        assertVisible(user);

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id).set(SysUser::getIsDeleted, 1)
            .set(SysUser::getDeletedBy, SecurityUtils.getUserIdOrNull())
            .set(SysUser::getDeletedTime, LocalDateTime.now())
            .setSql("token_version = token_version + 1");
        baseMapper.update(null, wrapper);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userAppMapper.delete(new LambdaQueryWrapper<SysUserApp>().eq(SysUserApp::getUserId, id));
        authorizationStore.deleteUser(id);
        authTokenStore.revokeAllSessions(id);
    }

    @Override
    @cn.sichu.system.security.datascope.DataScope(appCode = "workspace")
    public List<SysUserPageVO> list(SysUserQuery query) {
        return baseMapper.selectList(buildWrapper(query)).stream()
            .map(user -> SysUserPageVO.from(user, roleIdsOf(user.getId()))).toList();
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null) {
            return;
        }
        for (Long id : ids.stream().distinct().toList()) {
            delete(id);
        }
    }

    private LambdaQueryWrapper<SysUser> buildWrapper(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getUsername()),
                SysUser::getUsername, query.getUsername())
            .eq(query.getGender() != null, SysUser::getGender, query.getGender())
            .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        if (!isCurrentSuperAdminAssignee() && StringUtils.hasText(superAdminAssignee)) {
            wrapper.ne(SysUser::getUsername, superAdminAssignee);
        }
        boolean sorted = QuerySortUtils.apply(wrapper, query.getSortBy(), query.getOrder(),
            Map.of(
                "username", SysUser::getUsername,
                "realName", SysUser::getRealName,
                "status", SysUser::getStatus,
                "createTime", SysUser::getCreateTime
            ));
        if (!sorted) {
            wrapper.orderByDesc(SysUser::getCreateTime);
        }
        applyDataScope(wrapper);
        return wrapper;
    }

    private void applyDataScope(LambdaQueryWrapper<SysUser> wrapper) {
        DataScopeFilter scoped = cn.sichu.system.security.model.DataScopeContext.get();
        DataScopeFilter scope = scoped == null ? dataScopeService.current() : scoped;
        if (scope.all()) {
            return;
        }
        wrapper.and(nested -> {
            boolean hasDept = !scope.deptIds().isEmpty();
            if (hasDept) {
                nested.in(SysUser::getDeptId, scope.deptIds());
            }
            if (scope.selfOnly() && scope.userId() != null) {
                if (hasDept) {
                    nested.or();
                }
                nested.eq(SysUser::getId, scope.userId());
            }
            if (!hasDept && !scope.selfOnly() && scope.userId() != null) {
                nested.eq(SysUser::getId, scope.userId());
            }
        });
    }

    private void checkUserManageable(SysUser user) {
        if (PROTECTED_USER_ID.equals(user.getId())) {
            throw new BusinessException(SystemCode.USER_PROTECTED);
        }
        if (isSuperAdminAccount(user) && !isCurrentSuperAdminAssignee()) {
            throw new BusinessException(SystemCode.USER_PROTECTED);
        }
    }

    private void checkUserDeletable(SysUser user) {
        if (PROTECTED_USER_ID.equals(user.getId()) || isSuperAdminAccount(user)) {
            throw new BusinessException(SystemCode.USER_PROTECTED);
        }
    }

    private boolean isSuperAdminAccount(SysUser user) {
        return user.getUsername() != null && user.getUsername().equals(superAdminAssignee);
    }

    private void assertVisible(SysUser user) {
        DataScopeFilter scope = dataScopeService.current();
        if (scope.all()) {
            return;
        }
        if (scope.selfOnly() && Objects.equals(user.getId(), scope.userId())) {
            return;
        }
        if (user.getDeptId() != null && scope.deptIds().contains(user.getDeptId())) {
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    @Override
    @Transactional
    public UserInfoVO updateProfile(Long id, ProfileForm form) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        user.setRealName(form.getRealName());
        user.setGender(form.getGender() == null ? 0 : form.getGender());
        user.setEmail(form.getEmail());
        user.setMobile(form.getMobile());
        if (form.getAvatar() != null) {
            user.setAvatar(form.getAvatar());
        }
        baseMapper.updateById(user);
        return toUserInfo(user);
    }

    @Override
    @Transactional
    public void changePassword(Long id, PasswordForm form) {
        if (!Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前密码不正确");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        user.setTokenVersion((user.getTokenVersion() == null ? 0L : user.getTokenVersion()) + 1);
        baseMapper.updateById(user);
        authTokenStore.revokeAllSessions(id);
    }

    private UserInfoVO toUserInfo(SysUser user) {
        UserOrgVO org = authQueryMapper.selectUserOrg(user.getId());
        return new UserInfoVO(user.getId(), user.getUsername(), user.getRealName(),
            user.getGender(), user.getEmail(), user.getMobile(), user.getAvatar(),
            user.getTenantId(), org == null ? null : org.tenantName(), user.getDeptId(),
            org == null ? null : org.deptName(),
            new java.util.LinkedHashSet<>(authQueryMapper.selectRoleCodes(user.getId())),
            new java.util.LinkedHashSet<>(authQueryMapper.selectPermissions(user.getId())));
    }

    private List<String> roleIdsOf(Long userId) {
        List<Long> platformRoleIds = platformRoleIds();
        if (platformRoleIds.isEmpty()) {
            return List.of();
        }
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
                    .in(SysUserRole::getRoleId, platformRoleIds)).stream()
            .map(userRole -> userRole.getRoleId().toString()).toList();
    }

    private void saveRoles(Long userId, List<Long> roleIds, Long tenantId) {
        if (roleIds == null) {
            return;
        }
        checkRoleAssignmentAllowed();
        List<Long> platformRoleIds = platformRoleIds();
        if (!platformRoleIds.isEmpty()) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId).in(SysUserRole::getRoleId, platformRoleIds));
        }
        List<Long> requested = roleIds.stream().distinct().toList();
        if (requested.isEmpty()) {
            return;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(requested);
        SysApp workspace = workspaceApp();
        if (roles.size() != requested.size()) {
            throw new BusinessException(SystemCode.ROLE_NOT_FOUND);
        }
        for (SysRole role : roles) {
            if (!Objects.equals(role.getAppId(), workspace.getId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "平台用户只能分配 workspace 角色");
            }
            if (!Objects.equals(role.getStatus(), 1)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "不能分配已停用角色");
            }
            if ("SUPER_ADMIN".equals(role.getRoleCode())
                && !isCurrentSuperAdminAssignee()) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只有指定的超级管理员可以分配 SUPER_ADMIN 角色");
            }
        }
        for (Long roleId : requested) {
            SysUserRole userRole = new SysUserRole();
            userRole.setTenantId(tenantId == null ? 0L : tenantId);
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    private List<Long> platformRoleIds() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getAppId, workspaceApp().getId())).stream().map(SysRole::getId).toList();
    }

    private SysApp workspaceApp() {
        SysApp app = appMapper.selectOne(new LambdaQueryWrapper<SysApp>()
            .eq(SysApp::getCode, "workspace"));
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        return app;
    }

    private void checkRoleAssignmentAllowed() {
        var current = SecurityUtils.getLoginUserOrNull();
        if (current == null || !current.getRoles().contains("SUPER_ADMIN")
            && !current.getPermissions().contains("sys:user:assign")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权分配平台角色");
        }
    }

    private boolean isCurrentSuperAdminAssignee() {
        var current = SecurityUtils.getLoginUserOrNull();
        return current != null && current.getRoles().contains("SUPER_ADMIN")
            && current.getUsername() != null
            && current.getUsername().equals(superAdminAssignee);
    }

    private void applyForm(SysUser user, SysUserForm form) {
        user.setUsername(form.getUsername());
        user.setRealName(form.getRealName());
        user.setGender(form.getGender() == null ? 0 : form.getGender());
        user.setEmail(form.getEmail());
        user.setMobile(form.getMobile());
        user.setAvatar(form.getAvatar());
        user.setDeptId(form.getDeptId());
        user.setStatus(form.getStatus() == null ? 1 : form.getStatus());
    }
}

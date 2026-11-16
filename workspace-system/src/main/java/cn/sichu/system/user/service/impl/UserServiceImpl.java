package cn.sichu.system.user.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.PageResult;
import cn.sichu.common.result.ResultCode;
import cn.sichu.common.utils.QuerySortUtils;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.security.model.DataScopeFilter;
import cn.sichu.system.security.service.IDataScopeService;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.entity.SysUserRole;
import cn.sichu.system.user.mapper.SysUserMapper;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import cn.sichu.system.user.model.form.SysUserForm;
import cn.sichu.system.user.model.query.SysUserQuery;
import cn.sichu.system.user.model.vo.SysUserPageVO;
import cn.sichu.system.user.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.sichu.common.base.BaseImpl;
import lombok.RequiredArgsConstructor;
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

    @Override
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
        if (user == null) {
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
        saveRoles(user.getId(), form.getRoleIds(), user.getTenantId());
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysUserForm form) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        checkUserAllowed(user);
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
        checkUserAllowed(user);
        assertVisible(user);

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id).set(SysUser::getIsDeleted, 1)
            .set(SysUser::getDeletedBy, SecurityUtils.getUserIdOrNull())
            .set(SysUser::getDeletedTime, LocalDateTime.now())
            .setSql("token_version = token_version + 1");
        baseMapper.update(null, wrapper);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        authTokenStore.revokeAllSessions(id);
    }

    @Override
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
        DataScopeFilter scope = dataScopeService.current();
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

    private void checkUserAllowed(SysUser user) {
        if (PROTECTED_USER_ID.equals(user.getId())) {
            throw new BusinessException(SystemCode.USER_PROTECTED);
        }
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

    private List<String> roleIdsOf(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
            .stream().map(userRole -> userRole.getRoleId().toString()).toList();
    }

    private void saveRoles(Long userId, List<Long> roleIds, Long tenantId) {
        userRoleMapper.delete(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds.stream().distinct().toList()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setTenantId(tenantId == null ? 0L : tenantId);
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
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

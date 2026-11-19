package cn.sichu.system.app.grant.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.redis.cache.AuthorizationStore;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.entity.SysUserApp;
import cn.sichu.system.app.grant.entity.SysUserAppDept;
import cn.sichu.system.app.grant.mapper.AppAuthorizationMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppDeptMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppMapper;
import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.dept.entity.SysDept;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.model.vo.SysMenuTreeVO;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.entity.SysRoleDept;
import cn.sichu.system.role.mapper.SysRoleDeptMapper;
import cn.sichu.common.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Service
@RequiredArgsConstructor
public class AppAuthorizationServiceImpl implements IAppAuthorizationService {

    private static final String WORKSPACE = "workspace";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysUserMapper userMapper;
    private final SysAppMapper appMapper;
    private final SysUserAppMapper userAppMapper;
    private final SysUserAppDeptMapper userAppDeptMapper;
    private final AppAuthorizationMapper authorizationMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;
    private final AuthorizationStore authorizationStore;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public AppAuthorizationVO getAuthorization(Long userId, String appCode) {
        try {
            var cached = authorizationStore.find(userId, appCode);
            if (cached.isPresent()) {
                return objectMapper.convertValue(cached.get(), AppAuthorizationVO.class);
            }
        } catch (Exception ignored) {
        }
        AppAuthorizationVO result = computeAuthorization(userId, appCode);
        try {
            authorizationStore.save(userId, appCode, result);
        } catch (Exception ignored) {
        }
        return result;
    }

    private AppAuthorizationVO computeAuthorization(Long userId, String appCode) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        SysApp app = appMapper.selectOne(
            new LambdaQueryWrapper<SysApp>().eq(SysApp::getCode, appCode));
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        SysUserApp grant = findGrant(userId, app.getId());
        boolean superAdmin = isPlatformSuperAdmin(userId);
        boolean baseAccessible = isActiveUser(user)
            && Objects.equals(app.getStatus(), 1)
            && (superAdmin || grant != null && Objects.equals(grant.getStatus(), 1));
        List<SysRole> roles = baseAccessible
            ? authorizationMapper.selectRoles(userId, appCode) : List.of();
        boolean accessible = baseAccessible && (superAdmin || !roles.isEmpty());
        Set<String> roleCodes = roles.stream().map(SysRole::getRoleCode)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (superAdmin) {
            roleCodes.add(SUPER_ADMIN);
        }
        boolean allPermissions = superAdmin
            || accessible && WORKSPACE.equals(appCode) && roleCodes.contains(SUPER_ADMIN);
        Set<String> permissions = Set.of();
        if (accessible) {
            permissions = allPermissions
                ? authorizationMapper.selectAllPermissions(appCode)
                : authorizationMapper.selectPermissions(userId, appCode);
        }

        AppAuthorizationVO result = new AppAuthorizationVO();
        result.setAppCode(appCode);
        result.setAccessible(accessible);
        result.setRoles(roleCodes);
        result.setPermissions(permissions);
        result.setMenus(accessible ? buildMenuTree(allPermissions
            ? authorizationMapper.selectAllMenus(appCode)
            : authorizationMapper.selectMenus(userId, appCode)) : List.of());
        result.setUserId(userId.toString());
        result.setDataScope(grant == null ? null : grant.getDataScope());
        if (superAdmin) {
            result.setAll(true);
            result.setSelfOnly(false);
            result.setDeptIds(List.of());
        } else {
            applyDataScope(result, user, grant, roles, accessible);
        }
        return result;
    }

    @Override
    public AppAuthorizationVO currentAuthorization(String appCode) {
        return getAuthorization(SecurityUtils.getLoginUser().getUserId(),
            appCode);
    }

    @Override
    public void assertAccess(Long userId, String appCode) {
        if (!getAuthorization(userId, appCode).isAccessible()) {
            throw new AccessDeniedException("无权访问该应用");
        }
    }

    @Override
    public List<SysApp> listAccessibleApps(Long userId) {
        if (isPlatformSuperAdmin(userId)) {
            return appMapper.selectList(new LambdaQueryWrapper<SysApp>()
                .eq(SysApp::getStatus, 1)
                .ne(SysApp::getCode, WORKSPACE)
                .orderByAsc(SysApp::getSortNo)
                .orderByAsc(SysApp::getId));
        }
        return authorizationMapper.selectAccessibleApps(userId);
    }

    private boolean isPlatformSuperAdmin(Long userId) {
        return authorizationMapper.selectRoleCodes(userId, WORKSPACE)
            .contains(SUPER_ADMIN);
    }

    private SysUserApp findGrant(Long userId, Long appId) {
        return userAppMapper.selectOne(
            new LambdaQueryWrapper<SysUserApp>().eq(SysUserApp::getUserId,
                userId).eq(SysUserApp::getAppId, appId));
    }

    private boolean isActiveUser(SysUser user) {
        return Objects.equals(user.getStatus(), 1) && (
            user.getLockedUntil() == null || user.getLockedUntil()
                .isBefore(LocalDateTime.now()));
    }

    private void applyDataScope(AppAuthorizationVO result, SysUser user,
        SysUserApp grant, List<SysRole> roles, boolean accessible) {
        if (!accessible) {
            result.setSelfOnly(true);
            result.setDeptIds(List.of());
            return;
        }
        if (WORKSPACE.equals(result.getAppCode()) && result.getRoles()
            .contains(SUPER_ADMIN)) {
            result.setAll(true);
            result.setDeptIds(List.of());
            return;
        }
        Set<Long> deptIds = new HashSet<>();
        boolean selfOnly = false;
        boolean all = false;
        if (grant != null && grant.getDataScope() != null) {
            ScopeAccumulator scope = accumulate(grant.getDataScope(), user,
                customDeptIds(grant.getId()));
            all = scope.all();
            selfOnly = scope.selfOnly();
            deptIds.addAll(scope.deptIds());
        } else {
            for (SysRole role : roles) {
                ScopeAccumulator scope = accumulate(
                    role.getDataScope() == null ? 5 : role.getDataScope(), user,
                    roleDeptIds(role.getId()));
                all |= scope.all();
                selfOnly |= scope.selfOnly();
                deptIds.addAll(scope.deptIds());
            }
        }
        if (deptIds.isEmpty() && !selfOnly && !all) {
            selfOnly = true;
        }
        result.setAll(all);
        result.setSelfOnly(!all && selfOnly);
        result.setDeptIds(all ? List.of() :
            deptIds.stream().sorted().map(String::valueOf).toList());
    }

    private ScopeAccumulator accumulate(int dataScope, SysUser user,
        Set<Long> customDeptIds) {
        return switch (dataScope) {
            case 1 -> new ScopeAccumulator(true, false, Set.of());
            case 2 -> new ScopeAccumulator(false, false, customDeptIds);
            case 3 -> new ScopeAccumulator(false, false,
                user.getDeptId() == null ? Set.of() : Set.of(user.getDeptId()));
            case 4 -> new ScopeAccumulator(false, false,
                user.getDeptId() == null ? Set.of() :
                    deptWithDescendants(user.getDeptId()));
            default -> new ScopeAccumulator(false, true, Set.of());
        };
    }

    private Set<Long> customDeptIds(Long userAppId) {
        return userAppDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserAppDept>().eq(
                    SysUserAppDept::getUserAppId, userAppId)).stream()
            .map(SysUserAppDept::getDeptId)
            .collect(java.util.stream.Collectors.toSet());
    }

    private Set<Long> roleDeptIds(Long roleId) {
        return roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId,
                    roleId)).stream().map(SysRoleDept::getDeptId)
            .collect(java.util.stream.Collectors.toSet());
    }

    private Set<Long> deptWithDescendants(Long rootId) {
        List<SysDept> depts = deptMapper.selectList(null);
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysDept dept : depts) {
            children.computeIfAbsent(dept.getParentId(),
                key -> new ArrayList<>()).add(dept.getId());
        }
        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (result.add(current)) {
                queue.addAll(children.getOrDefault(current, List.of()));
            }
        }
        return result;
    }

    private List<SysMenuTreeVO> buildMenuTree(List<SysMenu> menus) {
        Map<Long, SysMenuTreeVO> nodes = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodes.put(menu.getId(), toMenuVO(menu));
        }
        List<SysMenuTreeVO> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            SysMenuTreeVO node = nodes.get(menu.getId());
            SysMenuTreeVO parent = nodes.get(menu.getParentId());
            if (parent != null && !Objects.equals(menu.getParentId(),
                menu.getId())) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    private SysMenuTreeVO toMenuVO(SysMenu menu) {
        SysMenuTreeVO vo = new SysMenuTreeVO();
        vo.setId(menu.getId().toString());
        vo.setParentId(
            menu.getParentId() == null ? "0" : menu.getParentId().toString());
        vo.setMenuCode(menu.getMenuCode());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSortNo(menu.getSortNo());
        vo.setHidden(menu.getHidden());
        vo.setStatus(menu.getStatus());
        return vo;
    }

    private record ScopeAccumulator(boolean all, boolean selfOnly,
                                    Set<Long> deptIds) {
    }
}

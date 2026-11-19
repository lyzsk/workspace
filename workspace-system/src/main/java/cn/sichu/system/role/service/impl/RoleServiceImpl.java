package cn.sichu.system.role.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.PageResult;
import cn.sichu.common.utils.QuerySortUtils;
import cn.sichu.common.base.BaseImpl;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.mapper.SysMenuMapper;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.entity.SysRoleDept;
import cn.sichu.system.role.entity.SysRoleMenu;
import cn.sichu.system.role.mapper.SysRoleDeptMapper;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.role.mapper.SysRoleMenuMapper;
import cn.sichu.system.role.model.form.SysRoleForm;
import cn.sichu.system.role.model.query.SysRoleQuery;
import cn.sichu.system.role.model.vo.SysRoleVO;
import cn.sichu.system.role.service.IRoleService;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUserRole;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
public class RoleServiceImpl extends BaseImpl<SysRoleMapper, SysRole> implements IRoleService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysAppMapper appMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageResult<SysRoleVO> page(SysRoleQuery query) {
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysRole> result = roleMapper.selectPage(page, buildWrapper(query));
        List<SysRoleVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<SysRoleVO> list(SysRoleQuery query) {
        return roleMapper.selectList(buildWrapper(query)).stream().map(this::toVO).toList();
    }

    @Override
    public List<SysRoleVO> listActive() {
        return listActive("workspace");
    }

    @Override
    public List<SysRoleVO> listActive(String appCode) {
        SysApp app = requireApp(appCode);
        return roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1)
                .eq(SysRole::getAppId, app.getId())
                .orderByAsc(SysRole::getRoleSort).orderByAsc(SysRole::getId)).stream()
            .map(this::toVO).toList();
    }

    @Override
    @Transactional
    public Long create(SysRoleForm form) {
        if (SUPER_ADMIN.equals(form.getRoleCode())) {
            throw new BusinessException(SystemCode.ROLE_PROTECTED);
        }
        SysApp app = requireApp(form.getAppCode());
        checkCodeUnique(form.getRoleCode(), app.getId(), null);
        SysRole role = new SysRole();
        role.setAppId(app.getId());
        applyForm(role, form);
        roleMapper.insert(role);
        saveDepts(role.getId(), form);
        saveMenus(role, form.getMenuIds());
        return role.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysRoleForm form) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(SystemCode.ROLE_NOT_FOUND);
        }
        checkRoleAllowed(role);
        SysApp app = requireApp(form.getAppCode());
        if (!role.getAppId().equals(app.getId())) {
            throw new BusinessException(SystemCode.ROLE_PROTECTED, "角色所属应用不允许修改");
        }
        checkCodeUnique(form.getRoleCode(), role.getAppId(), id);
        applyForm(role, form);
        roleMapper.updateById(role);
        saveDepts(role.getId(), form);
        saveMenus(role, form.getMenuIds());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(SystemCode.ROLE_NOT_FOUND);
        }
        checkRoleAllowed(role);
        Long assigned = userRoleMapper.selectCount(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (assigned > 0) {
            throw new BusinessException(SystemCode.ROLE_IN_USE);
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, id));
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(SystemCode.ROLE_NOT_FOUND);
        }
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId)).stream()
            .map(SysRoleMenu::getMenuId).toList();
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(SystemCode.ROLE_NOT_FOUND);
        }
        checkRoleAllowed(role);
        saveMenus(role, menuIds);
    }

    private LambdaQueryWrapper<SysRole> buildWrapper(SysRoleQuery query) {
        SysApp app = requireApp(query.getAppCode());
        LambdaQueryWrapper<SysRole> wrapper =
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getAppId, app.getId())
                .like(StringUtils.hasText(query.getRoleCode()),
                    SysRole::getRoleCode, query.getRoleCode())
                .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName,
                    query.getRoleName())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus());
        boolean sorted = QuerySortUtils.apply(wrapper, query.getSortBy(), query.getOrder(),
            Map.of("roleCode", SysRole::getRoleCode, "roleName", SysRole::getRoleName, "roleSort",
                SysRole::getRoleSort, "status", SysRole::getStatus, "createTime",
                SysRole::getCreateTime));
        if (!sorted) {
            wrapper.orderByAsc(SysRole::getRoleSort).orderByAsc(SysRole::getId);
        }
        return wrapper;
    }

    private void checkCodeUnique(String roleCode, Long appId, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper =
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode)
                .eq(SysRole::getAppId, appId);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemCode.ROLE_CODE_EXISTS);
        }
    }

    private void checkRoleAllowed(SysRole role) {
        if (SUPER_ADMIN.equals(role.getRoleCode())) {
            throw new BusinessException(SystemCode.ROLE_PROTECTED);
        }
    }

    private void applyForm(SysRole role, SysRoleForm form) {
        role.setRoleCode(form.getRoleCode());
        role.setRoleName(form.getRoleName());
        role.setDataScope(form.getDataScope() == null ? 5 : form.getDataScope());
        role.setRoleSort(form.getRoleSort() == null ? 0 : form.getRoleSort());
        role.setMenuCheckStrictly(
            form.getMenuCheckStrictly() == null || form.getMenuCheckStrictly());
        role.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        role.setRemark(form.getRemark());
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId() == null ? null : role.getId().toString());
        SysApp app = role.getAppId() == null ? null : appMapper.selectById(role.getAppId());
        vo.setAppCode(app == null ? null : app.getCode());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDataScope(role.getDataScope());
        vo.setRoleSort(role.getRoleSort());
        vo.setMenuCheckStrictly(role.getMenuCheckStrictly());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        vo.setDeptIds(deptIdsOf(role.getId()));
        return vo;
    }

    private List<String> deptIdsOf(Long roleId) {
        return roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId)).stream()
            .map(roleDept -> roleDept.getDeptId().toString()).toList();
    }

    private void saveDepts(Long roleId, SysRoleForm form) {
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        if (form.getDataScope() == null || form.getDataScope() != 2 || form.getDeptIds() == null) {
            return;
        }
        Long tenantId = currentTenantId();
        for (Long deptId : form.getDeptIds().stream().distinct().toList()) {
            SysRoleDept roleDept = new SysRoleDept();
            roleDept.setTenantId(tenantId);
            roleDept.setRoleId(roleId);
            roleDept.setDeptId(deptId);
            roleDeptMapper.insert(roleDept);
        }
    }

    private void saveMenus(SysRole role, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getId()));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        SysApp app = appMapper.selectById(role.getAppId());
        List<Long> requested = menuIds.stream().distinct().toList();
        List<SysMenu> menus = menuMapper.selectBatchIds(requested);
        if (menus.size() != requested.size()) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND);
        }
        for (SysMenu menu : menus) {
            if (app == null || !Objects.equals(menu.getAppCode(), app.getCode())) {
                throw new BusinessException(SystemCode.ROLE_PROTECTED, "角色与权限资源不属于同一应用");
            }
        }
        Long tenantId = currentTenantId();
        for (Long menuId : requested) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setTenantId(tenantId);
            roleMenu.setRoleId(role.getId());
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
    }

    private SysApp requireApp(String appCode) {
        String code = StringUtils.hasText(appCode) ? appCode.trim() : "workspace";
        SysApp app = appMapper.selectOne(new LambdaQueryWrapper<SysApp>().eq(SysApp::getCode, code));
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        return app;
    }

    private Long currentTenantId() {
        return SecurityUtils.getLoginUserOrNull() == null ? 0L :
            SecurityUtils.getLoginUserOrNull().getTenantId();
    }
}

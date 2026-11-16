package cn.sichu.system.security.service.impl;

import cn.sichu.system.security.service.IDataScopeService;

import cn.sichu.system.dept.entity.SysDept;
import cn.sichu.system.security.model.DataScopeFilter;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.entity.SysRoleDept;
import cn.sichu.system.role.mapper.SysRoleDeptMapper;
import cn.sichu.system.role.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements IDataScopeService {

    private final SysRoleMapper roleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public DataScopeFilter current() {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null) {
            return new DataScopeFilter(false, Set.of(), true, null);
        }
        if (loginUser.getRoles().contains("SUPER_ADMIN")) {
            return new DataScopeFilter(true, Set.of(), false, loginUser.getUserId());
        }

        List<SysRole> roles = roleMapper.selectByUserId(loginUser.getUserId());
        boolean all = false;
        boolean selfOnly = false;
        Set<Long> deptIds = new HashSet<>();

        for (SysRole role : roles) {
            int scope = role.getDataScope() == null ? 5 : role.getDataScope();
            switch (scope) {
                case 1 -> all = true;
                case 2 -> deptIds.addAll(roleDeptIds(role.getId()));
                case 3 -> {
                    if (loginUser.getDeptId() != null) {
                        deptIds.add(loginUser.getDeptId());
                    }
                }
                case 4 -> {
                    if (loginUser.getDeptId() != null) {
                        deptIds.addAll(deptWithDescendants(loginUser.getDeptId()));
                    }
                }
                default -> selfOnly = true;
            }
        }

        if (all) {
            return new DataScopeFilter(true, Set.of(), false, loginUser.getUserId());
        }
        if (deptIds.isEmpty() && !selfOnly) {
            selfOnly = true;
        }
        return new DataScopeFilter(false, deptIds, selfOnly, loginUser.getUserId());
    }

    private List<Long> roleDeptIds(Long roleId) {
        return roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId))
            .stream().map(SysRoleDept::getDeptId).toList();
    }

    private Set<Long> deptWithDescendants(Long rootId) {
        List<SysDept> depts = deptMapper.selectList(null);
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysDept dept : depts) {
            children.computeIfAbsent(dept.getParentId(), key -> new ArrayList<>())
                .add(dept.getId());
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
}

package cn.sichu.system.role.service;

import cn.sichu.common.result.PageResult;
import cn.sichu.system.role.model.form.SysRoleForm;
import cn.sichu.system.role.model.query.SysRoleQuery;
import cn.sichu.system.role.model.vo.SysRoleVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IRoleService {

    PageResult<SysRoleVO> page(SysRoleQuery query);

    List<SysRoleVO> list(SysRoleQuery query);

    List<SysRoleVO> listActive();

    List<SysRoleVO> listActive(String appCode);

    Long create(SysRoleForm form);

    void update(Long id, SysRoleForm form);

    void delete(Long id);

    List<Long> getMenuIds(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}

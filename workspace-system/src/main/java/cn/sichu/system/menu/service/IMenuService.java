package cn.sichu.system.menu.service;

import cn.sichu.system.menu.model.form.SysMenuForm;
import cn.sichu.system.menu.model.vo.SysMenuTreeVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IMenuService {

    List<SysMenuTreeVO> tree(boolean includeButtons);

    List<SysMenuTreeVO> currentUserTree();

    Long create(SysMenuForm form);

    void update(Long id, SysMenuForm form);

    void delete(Long id);
}

package cn.sichu.system.user.service;

import cn.sichu.common.result.PageResult;
import cn.sichu.system.user.model.form.SysUserForm;
import cn.sichu.system.user.model.query.SysUserQuery;
import cn.sichu.system.user.model.vo.SysUserPageVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IUserService {

    PageResult<SysUserPageVO> page(SysUserQuery query);

    SysUserPageVO get(Long id);

    Long create(SysUserForm form);

    void update(Long id, SysUserForm form);

    void delete(Long id);

    void deleteBatch(List<Long> ids);

    List<SysUserPageVO> list(SysUserQuery query);
}

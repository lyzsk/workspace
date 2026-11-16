package cn.sichu.system.dept.service;

import cn.sichu.system.dept.model.form.SysDeptForm;
import cn.sichu.system.dept.model.vo.SysDeptTreeVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IDeptService {

    List<SysDeptTreeVO> tree();

    Long create(SysDeptForm form);

    void update(Long id, SysDeptForm form);

    void delete(Long id);
}

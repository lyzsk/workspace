package cn.sichu.system.app.service;

import cn.sichu.system.app.model.form.SysAppForm;
import cn.sichu.system.app.model.vo.SysAppVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
public interface ISysAppService {

    List<SysAppVO> listAll();

    SysAppVO getRuntime(String path);

    Long create(SysAppForm form);

    void update(Long id, SysAppForm form);

    void delete(Long id);
}

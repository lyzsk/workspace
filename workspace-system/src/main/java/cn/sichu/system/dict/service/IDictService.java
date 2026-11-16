package cn.sichu.system.dict.service;

import cn.sichu.common.result.PageResult;
import cn.sichu.system.dict.model.form.SysDictDataForm;
import cn.sichu.system.dict.model.form.SysDictTypeForm;
import cn.sichu.system.dict.model.query.SysDictDataQuery;
import cn.sichu.system.dict.model.query.SysDictTypeQuery;
import cn.sichu.system.dict.model.vo.SysDictDataVO;
import cn.sichu.system.dict.model.vo.SysDictTypeVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IDictService {

    PageResult<SysDictTypeVO> pageTypes(SysDictTypeQuery query);

    List<SysDictTypeVO> listTypesForExport(SysDictTypeQuery query);

    List<SysDictTypeVO> listTypes();

    Long createType(SysDictTypeForm form);

    void updateType(Long id, SysDictTypeForm form);

    void deleteType(Long id);

    PageResult<SysDictDataVO> pageData(SysDictDataQuery query);

    List<SysDictDataVO> listDataForExport(SysDictDataQuery query);

    List<SysDictDataVO> listDataByType(String dictType);

    Long createData(SysDictDataForm form);

    void updateData(Long id, SysDictDataForm form);

    void deleteData(Long id);
}

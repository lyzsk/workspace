package cn.sichu.system.dict.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.PageResult;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.dict.entity.SysDictData;
import cn.sichu.system.dict.entity.SysDictType;
import cn.sichu.system.dict.mapper.SysDictDataMapper;
import cn.sichu.system.dict.mapper.SysDictTypeMapper;
import cn.sichu.system.dict.model.form.SysDictDataForm;
import cn.sichu.system.dict.model.form.SysDictTypeForm;
import cn.sichu.system.dict.model.query.SysDictDataQuery;
import cn.sichu.system.dict.model.query.SysDictTypeQuery;
import cn.sichu.system.dict.model.vo.SysDictDataVO;
import cn.sichu.system.dict.model.vo.SysDictTypeVO;
import cn.sichu.system.dict.service.IDictService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.sichu.common.core.base.BaseImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class DictServiceImpl extends BaseImpl<SysDictTypeMapper, SysDictType> implements IDictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;

    @Override
    public PageResult<SysDictTypeVO> pageTypes(SysDictTypeQuery query) {
        Page<SysDictType> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictType> wrapper =
            new LambdaQueryWrapper<SysDictType>().like(StringUtils.hasText(query.getDictType()),
                    SysDictType::getDictType, query.getDictType())
                .like(StringUtils.hasText(query.getDictName()), SysDictType::getDictName, query.getDictName())
                .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                .orderByAsc(SysDictType::getId);
        Page<SysDictType> result = dictTypeMapper.selectPage(page, wrapper);
        List<SysDictTypeVO> records = result.getRecords().stream().map(this::toTypeVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<SysDictTypeVO> listTypesForExport(SysDictTypeQuery query) {
        return dictTypeMapper.selectList(
            new LambdaQueryWrapper<SysDictType>().like(StringUtils.hasText(query.getDictType()),
                    SysDictType::getDictType, query.getDictType())
                .like(StringUtils.hasText(query.getDictName()), SysDictType::getDictName, query.getDictName())
                .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                .orderByAsc(SysDictType::getId)).stream().map(this::toTypeVO).toList();
    }

    @Override
    public List<SysDictTypeVO> listTypes() {
        return dictTypeMapper.selectList(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getId)).stream().map(this::toTypeVO).toList();
    }

    @Override
    @Transactional
    public Long createType(SysDictTypeForm form) {
        if (dictTypeMapper.selectCount(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, form.getDictType())) > 0) {
            throw new BusinessException(SystemCode.DICT_TYPE_EXISTS);
        }
        SysDictType type = new SysDictType();
        type.setDictType(form.getDictType());
        type.setDictName(form.getDictName());
        type.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        type.setRemark(form.getRemark());
        dictTypeMapper.insert(type);
        return type.getId();
    }

    @Override
    @Transactional
    public void updateType(Long id, SysDictTypeForm form) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(SystemCode.DICT_TYPE_NOT_FOUND);
        }
        Long count = dictTypeMapper.selectCount(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, form.getDictType())
                .ne(SysDictType::getId, id));
        if (count > 0) {
            throw new BusinessException(SystemCode.DICT_TYPE_EXISTS);
        }
        String oldType = type.getDictType();
        type.setDictType(form.getDictType());
        type.setDictName(form.getDictName());
        type.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        type.setRemark(form.getRemark());
        dictTypeMapper.updateById(type);
        if (!oldType.equals(form.getDictType())) {
            SysDictData update = new SysDictData();
            update.setDictType(form.getDictType());
            dictDataMapper.update(update,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysDictData>().eq(
                    SysDictData::getDictType, oldType));
        }
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(SystemCode.DICT_TYPE_NOT_FOUND);
        }
        if (dictDataMapper.selectCount(
            new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, type.getDictType())) > 0) {
            throw new BusinessException(SystemCode.DICT_TYPE_HAS_DATA);
        }
        dictTypeMapper.deleteById(id);
    }

    @Override
    public PageResult<SysDictDataVO> pageData(SysDictDataQuery query) {
        Page<SysDictData> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDictData> wrapper =
            new LambdaQueryWrapper<SysDictData>().eq(StringUtils.hasText(query.getDictType()),
                    SysDictData::getDictType, query.getDictType())
                .like(StringUtils.hasText(query.getLabel()), SysDictData::getLabel, query.getLabel())
                .eq(query.getStatus() != null, SysDictData::getStatus, query.getStatus())
                .orderByAsc(SysDictData::getSortNo).orderByAsc(SysDictData::getId);
        Page<SysDictData> result = dictDataMapper.selectPage(page, wrapper);
        List<SysDictDataVO> records = result.getRecords().stream().map(this::toDataVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<SysDictDataVO> listDataForExport(SysDictDataQuery query) {
        return dictDataMapper.selectList(
                new LambdaQueryWrapper<SysDictData>().eq(StringUtils.hasText(query.getDictType()),
                        SysDictData::getDictType, query.getDictType())
                    .like(StringUtils.hasText(query.getLabel()), SysDictData::getLabel, query.getLabel())
                    .eq(query.getStatus() != null, SysDictData::getStatus, query.getStatus())
                    .orderByAsc(SysDictData::getSortNo).orderByAsc(SysDictData::getId)).stream()
            .map(this::toDataVO).toList();
    }

    @Override
    public List<SysDictDataVO> listDataByType(String dictType) {
        return dictDataMapper.selectList(
            new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1).orderByAsc(SysDictData::getSortNo)
                .orderByAsc(SysDictData::getId)).stream().map(this::toDataVO).toList();
    }

    @Override
    @Transactional
    public Long createData(SysDictDataForm form) {
        checkDataType(form.getDictType());
        checkDataValueUnique(form.getDictType(), form.getValue(), null);
        SysDictData data = new SysDictData();
        applyData(data, form);
        dictDataMapper.insert(data);
        return data.getId();
    }

    @Override
    @Transactional
    public void updateData(Long id, SysDictDataForm form) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data == null) {
            throw new BusinessException(SystemCode.DICT_DATA_NOT_FOUND);
        }
        checkDataType(form.getDictType());
        checkDataValueUnique(form.getDictType(), form.getValue(), id);
        applyData(data, form);
        dictDataMapper.updateById(data);
    }

    @Override
    @Transactional
    public void deleteData(Long id) {
        if (dictDataMapper.selectById(id) == null) {
            throw new BusinessException(SystemCode.DICT_DATA_NOT_FOUND);
        }
        dictDataMapper.deleteById(id);
    }

    private void applyData(SysDictData data, SysDictDataForm form) {
        data.setDictType(form.getDictType());
        data.setLabel(form.getLabel());
        data.setValue(form.getValue());
        data.setSortNo(form.getSortNo() == null ? 0 : form.getSortNo());
        data.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        data.setRemark(form.getRemark());
    }

    private void checkDataType(String dictType) {
        if (dictTypeMapper.selectCount(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType)) == 0) {
            throw new BusinessException(SystemCode.DICT_TYPE_NOT_FOUND);
        }
    }

    private void checkDataValueUnique(String dictType, String value, Long excludeId) {
        LambdaQueryWrapper<SysDictData> wrapper =
            new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getValue, value);
        if (excludeId != null) {
            wrapper.ne(SysDictData::getId, excludeId);
        }
        if (dictDataMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemCode.DICT_DATA_EXISTS);
        }
    }

    private SysDictTypeVO toTypeVO(SysDictType type) {
        SysDictTypeVO vo = new SysDictTypeVO();
        vo.setId(type.getId() == null ? null : type.getId().toString());
        vo.setDictType(type.getDictType());
        vo.setDictName(type.getDictName());
        vo.setStatus(type.getStatus());
        vo.setCreateTime(type.getCreateTime());
        vo.setRemark(type.getRemark());
        return vo;
    }

    private SysDictDataVO toDataVO(SysDictData data) {
        SysDictDataVO vo = new SysDictDataVO();
        vo.setId(data.getId() == null ? null : data.getId().toString());
        vo.setDictType(data.getDictType());
        vo.setLabel(data.getLabel());
        vo.setValue(data.getValue());
        vo.setSortNo(data.getSortNo());
        vo.setStatus(data.getStatus());
        vo.setCreateTime(data.getCreateTime());
        vo.setRemark(data.getRemark());
        return vo;
    }
}

package cn.sichu.system.dept.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.dept.entity.SysDept;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.dept.model.form.SysDeptForm;
import cn.sichu.system.dept.model.vo.SysDeptTreeVO;
import cn.sichu.system.dept.service.IDeptService;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.sichu.common.core.base.BaseImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends BaseImpl<SysDeptMapper, SysDept> implements IDeptService {

    private static final Long PROTECTED_DEPT_ID = 1L;

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<SysDeptTreeVO> tree() {
        List<SysDept> depts = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortNo).orderByAsc(SysDept::getId));
        Map<Long, SysDeptTreeVO> nodes = new LinkedHashMap<>();
        for (SysDept dept : depts) {
            nodes.put(dept.getId(), toVO(dept));
        }
        List<SysDeptTreeVO> roots = new ArrayList<>();
        for (SysDept dept : depts) {
            SysDeptTreeVO node = nodes.get(dept.getId());
            SysDeptTreeVO parent = nodes.get(dept.getParentId());
            if (parent != null && !Objects.equals(dept.getParentId(), dept.getId())) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public Long create(SysDeptForm form) {
        checkCodeUnique(form.getDeptCode(), null);
        checkParent(form.getParentId(), null);
        SysDept dept = new SysDept();
        applyForm(dept, form);
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysDeptForm form) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(SystemCode.DEPT_NOT_FOUND);
        }
        checkDeptAllowed(dept);
        checkCodeUnique(form.getDeptCode(), id);
        checkParent(form.getParentId(), id);
        applyForm(dept, form);
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(SystemCode.DEPT_NOT_FOUND);
        }
        checkDeptAllowed(dept);
        Long children =
            deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BusinessException(SystemCode.DEPT_HAS_CHILDREN);
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw new BusinessException(SystemCode.DEPT_HAS_CHILDREN, "部门下存在用户，不能删除");
        }
        deptMapper.deleteById(id);
    }

    private void checkDeptAllowed(SysDept dept) {
        if (PROTECTED_DEPT_ID.equals(dept.getId())) {
            throw new BusinessException(SystemCode.DEPT_PROTECTED);
        }
    }

    private void applyForm(SysDept dept, SysDeptForm form) {
        dept.setDeptCode(form.getDeptCode());
        dept.setDeptName(form.getDeptName());
        dept.setParentId(form.getParentId() == null ? 0L : form.getParentId());
        dept.setSortNo(form.getSortNo() == null ? 0 : form.getSortNo());
        dept.setStatus(form.getStatus() == null ? 1 : form.getStatus());
    }

    private void checkCodeUnique(String deptCode, Long excludeId) {
        LambdaQueryWrapper<SysDept> wrapper =
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeptCode, deptCode);
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        if (deptMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemCode.DEPT_CODE_EXISTS);
        }
    }

    private void checkParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (Objects.equals(parentId, selfId)) {
            throw new BusinessException(SystemCode.DEPT_NOT_FOUND, "上级部门不能是自己");
        }
        if (deptMapper.selectById(parentId) == null) {
            throw new BusinessException(SystemCode.DEPT_NOT_FOUND, "上级部门不存在");
        }
    }

    private SysDeptTreeVO toVO(SysDept dept) {
        SysDeptTreeVO vo = new SysDeptTreeVO();
        vo.setId(dept.getId() == null ? null : dept.getId().toString());
        vo.setParentId(dept.getParentId() == null ? "0" : dept.getParentId().toString());
        vo.setDeptCode(dept.getDeptCode());
        vo.setDeptName(dept.getDeptName());
        vo.setSortNo(dept.getSortNo());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }
}

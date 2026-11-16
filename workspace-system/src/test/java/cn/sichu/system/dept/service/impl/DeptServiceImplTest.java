package cn.sichu.system.dept.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.dept.entity.SysDept;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.dept.model.form.SysDeptForm;
import cn.sichu.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class DeptServiceImplTest {

    private SysDeptMapper deptMapper;
    private DeptServiceImpl deptService;

    @BeforeEach
    void setUp() {
        deptMapper = mock(SysDeptMapper.class);
        deptService = new DeptServiceImpl(deptMapper, mock(SysUserMapper.class));
    }

    @Test
    void updateRejectsHeadquarters() {
        when(deptMapper.selectById(1L)).thenReturn(headquarters());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deptService.update(1L, new SysDeptForm()));

        assertEquals(SystemCode.DEPT_PROTECTED, exception.getResultCode());
        verify(deptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    void deleteRejectsHeadquarters() {
        when(deptMapper.selectById(1L)).thenReturn(headquarters());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deptService.delete(1L));

        assertEquals(SystemCode.DEPT_PROTECTED, exception.getResultCode());
        verify(deptMapper, never()).deleteById(1L);
    }

    private SysDept headquarters() {
        SysDept dept = new SysDept();
        dept.setId(1L);
        dept.setDeptCode("HQ");
        dept.setDeptName("总公司");
        return dept;
    }
}

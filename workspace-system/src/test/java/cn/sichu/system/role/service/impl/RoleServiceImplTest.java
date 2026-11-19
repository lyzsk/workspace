package cn.sichu.system.role.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.mapper.SysMenuMapper;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.entity.SysRoleDept;
import cn.sichu.system.role.entity.SysRoleMenu;
import cn.sichu.system.role.mapper.SysRoleDeptMapper;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.role.mapper.SysRoleMenuMapper;
import cn.sichu.system.role.model.form.SysRoleForm;
import cn.sichu.system.user.entity.SysUserRole;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class RoleServiceImplTest {

    private SysRoleMapper roleMapper;
    private SysRoleMenuMapper roleMenuMapper;
    private SysRoleDeptMapper roleDeptMapper;
    private SysUserRoleMapper userRoleMapper;
    private SysAppMapper appMapper;
    private SysMenuMapper menuMapper;
    private RoleServiceImpl roleService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleDept.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
    }

    @BeforeEach
    void setUp() {
        roleMapper = mock(SysRoleMapper.class);
        roleMenuMapper = mock(SysRoleMenuMapper.class);
        roleDeptMapper = mock(SysRoleDeptMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        appMapper = mock(SysAppMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        SysApp workspace = new SysApp();
        workspace.setId(1000L);
        workspace.setCode("workspace");
        when(appMapper.selectOne(any())).thenReturn(workspace);
        when(appMapper.selectById(1000L)).thenReturn(workspace);
        when(menuMapper.selectBatchIds(any())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> {
                SysMenu menu = new SysMenu();
                menu.setId(id);
                menu.setAppCode("workspace");
                return menu;
            }).toList();
        });
        roleService = new RoleServiceImpl(roleMapper, roleMenuMapper, roleDeptMapper, userRoleMapper,
            appMapper, menuMapper);
    }

    @Test
    void createSavesRoleFieldsAndDistinctMenus() {
        doAnswer(invocation -> {
            invocation.getArgument(0, SysRole.class).setId(9L);
            return 1;
        }).when(roleMapper).insert(any(SysRole.class));
        SysRoleForm form = form();
        form.setMenuIds(List.of(1L, 2L, 2L));

        Long id = roleService.create(form);

        assertEquals(9L, id);
        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).insert(captor.capture());
        assertEquals(3, captor.getValue().getRoleSort());
        assertEquals("值班角色", captor.getValue().getRemark());
        assertFalse(captor.getValue().getMenuCheckStrictly());
        verify(roleMenuMapper, times(2)).insert(any(SysRoleMenu.class));
    }

    @Test
    void updateRejectsSuperAdmin() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("SUPER_ADMIN");
        when(roleMapper.selectById(1L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> roleService.update(1L, form()));

        assertEquals(SystemCode.ROLE_PROTECTED, exception.getResultCode());
        verify(roleMapper, never()).updateById(any(SysRole.class));
    }

    @Test
    void deleteRejectsRoleAssignedToUser() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleCode("OPERATOR");
        when(roleMapper.selectById(2L)).thenReturn(role);
        when(userRoleMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> roleService.delete(2L));

        assertEquals(SystemCode.ROLE_IN_USE, exception.getResultCode());
        verify(roleMapper, never()).deleteById(anyLong());
    }

    private SysRoleForm form() {
        SysRoleForm form = new SysRoleForm();
        form.setRoleCode("OPERATOR");
        form.setRoleName("操作员");
        form.setRoleSort(3);
        form.setMenuCheckStrictly(false);
        form.setRemark("值班角色");
        form.setDataScope(5);
        form.setStatus(1);
        return form;
    }
}

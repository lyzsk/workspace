package cn.sichu.system.app.grant.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.entity.SysUserApp;
import cn.sichu.system.app.grant.entity.SysUserAppDept;
import cn.sichu.system.app.grant.mapper.AppAuthorizationMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppDeptMapper;
import cn.sichu.system.app.grant.mapper.SysUserAppMapper;
import cn.sichu.system.app.grant.model.form.AppGrantForm;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.entity.SysUserRole;
import cn.sichu.system.user.mapper.SysUserMapper;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppGrantServiceImplTest {

    private SysUserAppMapper userAppMapper;
    private SysRoleMapper roleMapper;
    private SysUserRoleMapper userRoleMapper;
    private AppGrantServiceImpl service;

    @BeforeAll
    static void initTables() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysUserApp.class);
        TableInfoHelper.initTableInfo(assistant, SysUserAppDept.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
    }

    @BeforeEach
    void setUp() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysAppMapper appMapper = mock(SysAppMapper.class);
        userAppMapper = mock(SysUserAppMapper.class);
        SysUserAppDeptMapper userAppDeptMapper = mock(SysUserAppDeptMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        service = new AppGrantServiceImpl(userMapper, appMapper, userAppMapper, userAppDeptMapper,
            mock(AppAuthorizationMapper.class), roleMapper, userRoleMapper, mock(SysDeptMapper.class),
            mock(ApplicationEventPublisher.class), mock(cn.sichu.common.redis.cache.AuthorizationStore.class));
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(0L);
        when(userMapper.selectById(1L)).thenReturn(user);
        SysApp app = new SysApp();
        app.setId(2000L);
        app.setCode("app-a");
        when(appMapper.selectOne(any())).thenReturn(app);
        doAnswer(invocation -> {
            invocation.getArgument(0, SysUserApp.class).setId(10L);
            return 1;
        }).when(userAppMapper).insert(any(SysUserApp.class));
    }

    @Test
    void enabledGrantRequiresRole() {
        AppGrantForm form = form();
        form.setRoleIds(List.of());

        assertThrows(BusinessException.class, () -> service.saveGrant(1L, "app-a", form));
        verify(userAppMapper, never()).insert(any(SysUserApp.class));
    }

    @Test
    void rejectsRoleFromAnotherApplication() {
        AppGrantForm form = form();
        when(roleMapper.selectBatchIds(form.getRoleIds())).thenReturn(List.of(role(3000L)));

        assertThrows(BusinessException.class, () -> service.saveGrant(1L, "app-a", form));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void savesRolesOnlyInsideTargetApplication() {
        AppGrantForm form = form();
        when(roleMapper.selectBatchIds(form.getRoleIds())).thenReturn(List.of(role(2000L)));
        SysRole previous = role(2000L);
        previous.setId(21L);
        when(roleMapper.selectList(any())).thenReturn(List.of(previous));

        service.saveGrant(1L, "app-a", form);

        verify(userRoleMapper).delete(any());
        verify(userRoleMapper).insert(any(SysUserRole.class));
        verify(userAppMapper).insert(any(SysUserApp.class));
    }

    private AppGrantForm form() {
        AppGrantForm form = new AppGrantForm();
        form.setStatus(1);
        form.setRoleIds(List.of(20L));
        form.setDeptIds(List.of());
        return form;
    }

    private SysRole role(Long appId) {
        SysRole role = new SysRole();
        role.setId(20L);
        role.setAppId(appId);
        role.setRoleCode("APP_ADMIN");
        role.setStatus(1);
        return role;
    }
}

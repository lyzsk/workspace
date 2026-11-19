package cn.sichu.system.app.service.impl;

import cn.sichu.common.core.base.BaseImpl;
import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.app.grant.entity.SysUserApp;
import cn.sichu.system.app.grant.mapper.SysUserAppMapper;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.app.model.form.SysAppForm;
import cn.sichu.system.app.model.vo.SysAppVO;
import cn.sichu.system.app.service.ISysAppService;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.mapper.SysMenuMapper;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.entity.SysRoleMenu;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.role.mapper.SysRoleMenuMapper;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Service
@RequiredArgsConstructor
public class SysAppServiceImpl extends BaseImpl<SysAppMapper, SysApp> implements ISysAppService {

    private static final String APP_WORKSPACE = "workspace";

    private final SysAppMapper appMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final IAppAuthorizationService authorizationService;
    private final SysUserAppMapper userAppMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public List<SysAppVO> listAll() {
        return appMapper.selectList(new LambdaQueryWrapper<SysApp>()
                .ne(SysApp::getCode, APP_WORKSPACE)
                .orderByAsc(SysApp::getSortNo)
                .orderByAsc(SysApp::getId))
            .stream().map(SysAppVO::from).toList();
    }

    @Override
    public SysAppVO getRuntime(String path) {
        if (path == null || path.isBlank()) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        SysApp app = appMapper.selectOne(new LambdaQueryWrapper<SysApp>()
            .eq(SysApp::getBasePath, normalizeBasePath(path))
            .eq(SysApp::getStatus, 1));
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        LoginUser current = SecurityUtils.getLoginUser();
        authorizationService.assertAccess(current.getUserId(), app.getCode());
        return SysAppVO.from(app);
    }

    @Override
    @Transactional
    public Long create(SysAppForm form) {
        validate(form);
        checkCodeUnique(form.getCode(), null);
        checkBasePathUnique(form.getBasePath(), null);
        SysApp app = new SysApp();
        applyForm(app, form);
        appMapper.insert(app);
        return app.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysAppForm form) {
        SysApp app = appMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        if (APP_WORKSPACE.equals(app.getCode()) && !APP_WORKSPACE.equals(form.getCode().trim())) {
            throw new BusinessException(SystemCode.APP_PROTECTED);
        }
        if (!app.getCode().equals(form.getCode().trim())) {
            Long menus = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getAppCode, app.getCode()));
            if (menus > 0 || findAccessMenu(app.getCode()) != null) {
                throw new BusinessException(SystemCode.APP_CODE_IMMUTABLE);
            }
        }
        validate(form);
        checkCodeUnique(form.getCode(), id);
        checkBasePathUnique(form.getBasePath(), id);
        applyForm(app, form);
        appMapper.updateById(app);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysApp app = appMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
        if (APP_WORKSPACE.equals(app.getCode())) {
            throw new BusinessException(SystemCode.APP_PROTECTED);
        }
        Long grants = userAppMapper.selectCount(new LambdaQueryWrapper<SysUserApp>()
            .eq(SysUserApp::getAppId, id));
        Long roles = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getAppId, id));
        if (grants > 0 || roles > 0) {
            throw new BusinessException(SystemCode.APP_HAS_AUTHORIZATIONS);
        }
        Long menus = menuMapper.selectCount(
            new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getAppCode, app.getCode()));
        if (menus > 0) {
            throw new BusinessException(SystemCode.APP_HAS_MENUS);
        }
        SysMenu accessMenu = findAccessMenu(app.getCode());
        if (accessMenu != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, accessMenu.getId()));
            menuMapper.deleteById(accessMenu.getId());
        }
        appMapper.deleteById(id);
    }

    private void validate(SysAppForm form) {
        if (form.getModuleEntry() == null || form.getModuleEntry().isBlank()) {
            throw new BusinessException(SystemCode.APP_ENTRY_URL_REQUIRED);
        }
    }

    private void applyForm(SysApp app, SysAppForm form) {
        app.setCode(form.getCode().trim());
        app.setName(form.getName().trim());
        app.setIcon(form.getIcon());
        app.setBasePath(normalizeBasePath(form.getBasePath()));
        app.setModuleEntry(form.getModuleEntry().trim());
        app.setStyleEntry(form.getStyleEntry() == null ? null : form.getStyleEntry().trim());
        app.setAccessPermission(app.getCode() + ":access");
        app.setSortNo(form.getSortNo() == null ? 0 : form.getSortNo());
        app.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        app.setRemark(form.getRemark());
    }

    private SysMenu findAccessMenu(String code) {
        return menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getAppCode, APP_WORKSPACE)
            .eq(SysMenu::getMenuCode, "apps:" + code + ":access"));
    }

    private String normalizeBasePath(String basePath) {
        String path = basePath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysApp> wrapper =
            new LambdaQueryWrapper<SysApp>().eq(SysApp::getCode, code.trim());
        if (excludeId != null) {
            wrapper.ne(SysApp::getId, excludeId);
        }
        if (appMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemCode.APP_CODE_EXISTS);
        }
    }

    private void checkBasePathUnique(String basePath, Long excludeId) {
        LambdaQueryWrapper<SysApp> wrapper = new LambdaQueryWrapper<SysApp>()
            .eq(SysApp::getBasePath, normalizeBasePath(basePath));
        if (excludeId != null) {
            wrapper.ne(SysApp::getId, excludeId);
        }
        if (appMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemCode.APP_BASE_PATH_EXISTS);
        }
    }
}

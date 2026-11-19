package cn.sichu.system.menu.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.mapper.SysMenuMapper;
import cn.sichu.system.menu.model.form.SysMenuForm;
import cn.sichu.system.menu.model.vo.SysMenuTreeVO;
import cn.sichu.system.menu.service.IMenuService;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.util.SecurityUtils;
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
public class MenuServiceImpl extends BaseImpl<SysMenuMapper, SysMenu> implements IMenuService {

    private static final String TYPE_DIR = "DIR";
    private static final String TYPE_MENU = "MENU";
    private static final String TYPE_BUTTON = "BUTTON";

    private final SysMenuMapper menuMapper;
    private final IAppAuthorizationService authorizationService;
    private final SysAppMapper appMapper;
    private final cn.sichu.common.redis.cache.AuthorizationStore authorizationStore;

    @Override
    public List<SysMenuTreeVO> tree(boolean includeButtons) {
        return tree(includeButtons, "workspace");
    }

    @Override
    public List<SysMenuTreeVO> tree(boolean includeButtons, String appCode) {
        String app = appCode == null || appCode.isBlank() ? "workspace" : appCode;
        LambdaQueryWrapper<SysMenu> wrapper =
            new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getAppCode, app)
                .orderByAsc(SysMenu::getSortNo).orderByAsc(SysMenu::getId);
        if (!includeButtons) {
            wrapper.in(SysMenu::getMenuType, TYPE_DIR, TYPE_MENU);
        }
        return buildTree(menuMapper.selectList(wrapper));
    }

    @Override
    public List<SysMenuTreeVO> currentUserTree() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        authorizationService.assertAccess(loginUser.getUserId(), "workspace");
        List<SysMenu> menus;
        if (loginUser.getRoles().contains("SUPER_ADMIN")) {
            menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getAppCode, "workspace")
                    .eq(SysMenu::getStatus, 1).eq(SysMenu::getHidden, 1)
                    .in(SysMenu::getMenuType, TYPE_DIR, TYPE_MENU).orderByAsc(SysMenu::getSortNo)
                    .orderByAsc(SysMenu::getId));
        } else {
            menus = menuMapper.selectByUserId(loginUser.getUserId()).stream()
                .filter(menu -> Objects.equals(menu.getHidden(), 1)).toList();
        }
        List<SysMenuTreeVO> tree = buildTree(menus);
        appendRegisteredApps(tree, loginUser);
        return tree;
    }

    private void appendRegisteredApps(List<SysMenuTreeVO> tree, LoginUser loginUser) {
        List<SysApp> apps = authorizationService.listAccessibleApps(loginUser.getUserId());
        if (apps.isEmpty()) {
            return;
        }
        SysMenuTreeVO root = tree.stream().filter(node -> "apps".equals(node.getMenuCode()))
            .findFirst().orElse(null);
        if (root == null) {
            root = new SysMenuTreeVO();
            root.setId("apps-root");
            root.setParentId("0");
            root.setMenuCode("apps");
            root.setMenuName("应用管理");
            root.setMenuType(TYPE_DIR);
            root.setPath("/apps");
            root.setSortNo(100);
            root.setHidden(1);
            root.setStatus(1);
            tree.add(root);
        }
        for (SysApp app : apps) {
            if (app.getBasePath() == null || app.getBasePath().isBlank()) {
                continue;
            }
            SysMenuTreeVO child = new SysMenuTreeVO();
            child.setId("app-" + app.getCode());
            child.setParentId(root.getId());
            child.setMenuCode("app:" + app.getCode());
            child.setMenuName(app.getName());
            child.setMenuType(TYPE_MENU);
            child.setPath(app.getBasePath());
            child.setComponent("apps/detail/MicroFrontendHost");
            child.setIcon(app.getIcon());
            child.setSortNo(app.getSortNo());
            child.setHidden(1);
            child.setStatus(1);
            root.getChildren().add(child);
        }
    }

    @Override
    @Transactional
    public Long create(SysMenuForm form) {
        requireApp(form.getAppCode());
        validateType(form.getMenuType());
        checkCodeUnique(form.getAppCode(), form.getMenuCode(), null);
        checkParent(form.getParentId(), null, form.getAppCode());
        SysMenu menu = new SysMenu();
        applyForm(menu, form);
        menuMapper.insert(menu);
        authorizationStore.deleteApplication(menu.getAppCode());
        return menu.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysMenuForm form) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND);
        }
        requireApp(form.getAppCode());
        validateType(form.getMenuType());
        checkCodeUnique(form.getAppCode(), form.getMenuCode(), id);
        checkParent(form.getParentId(), id, form.getAppCode());
        applyForm(menu, form);
        menuMapper.updateById(menu);
        authorizationStore.deleteApplication(menu.getAppCode());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysMenu existing = menuMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND);
        }
        Long children =
            menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BusinessException(SystemCode.MENU_HAS_CHILDREN);
        }
        menuMapper.deleteById(id);
        authorizationStore.deleteApplication(existing.getAppCode());
    }

    private void applyForm(SysMenu menu, SysMenuForm form) {
        menu.setAppCode(form.getAppCode() == null ? "workspace" : form.getAppCode());
        menu.setMenuCode(form.getMenuCode());
        menu.setParentId(form.getParentId() == null ? 0L : form.getParentId());
        menu.setMenuName(form.getMenuName());
        menu.setMenuType(form.getMenuType());
        menu.setPath(form.getPath());
        menu.setComponent(form.getComponent());
        menu.setPermission(form.getPermission());
        menu.setIcon(form.getIcon());
        menu.setSortNo(form.getSortNo() == null ? 0 : form.getSortNo());
        menu.setHidden(form.getHidden() == null ? 1 : form.getHidden());
        menu.setStatus(form.getStatus() == null ? 1 : form.getStatus());
    }

    private void requireApp(String appCode) {
        String code = appCode == null || appCode.isBlank() ? "workspace" : appCode;
        if (appMapper.selectCount(new LambdaQueryWrapper<SysApp>().eq(SysApp::getCode, code)) == 0) {
            throw new BusinessException(SystemCode.APP_NOT_FOUND);
        }
    }

    private void validateType(String menuType) {
        if (!TYPE_DIR.equals(menuType) && !TYPE_MENU.equals(menuType) && !TYPE_BUTTON.equals(
            menuType)) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND,
                "菜单类型必须是 DIR/MENU/BUTTON");
        }
    }

    private void checkCodeUnique(String appCode, String menuCode, Long excludeId) {
        String app = appCode == null ? "workspace" : appCode;
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getAppCode, app)
            .eq(SysMenu::getMenuCode, menuCode);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemCode.MENU_CODE_EXISTS);
        }
    }

    private void checkParent(Long parentId, Long selfId, String appCode) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (Objects.equals(parentId, selfId)) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND, "上级菜单不能是自己");
        }
        SysMenu parent = menuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND, "上级菜单不存在");
        }
        String expectedApp = appCode == null ? "workspace" : appCode;
        if (!Objects.equals(parent.getAppCode(), expectedApp)) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND, "上级菜单与当前资源不属于同一应用");
        }
    }

    private List<SysMenuTreeVO> buildTree(List<SysMenu> menus) {
        Map<Long, SysMenuTreeVO> nodes = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodes.put(menu.getId(), toVO(menu));
        }
        List<SysMenuTreeVO> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            SysMenuTreeVO node = nodes.get(menu.getId());
            SysMenuTreeVO parent = nodes.get(menu.getParentId());
            if (parent != null && !Objects.equals(menu.getParentId(), menu.getId())) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    private SysMenuTreeVO toVO(SysMenu menu) {
        SysMenuTreeVO vo = new SysMenuTreeVO();
        vo.setId(menu.getId() == null ? null : menu.getId().toString());
        vo.setParentId(menu.getParentId() == null ? "0" : menu.getParentId().toString());
        vo.setMenuCode(menu.getMenuCode());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSortNo(menu.getSortNo());
        vo.setHidden(menu.getHidden());
        vo.setStatus(menu.getStatus());
        return vo;
    }
}

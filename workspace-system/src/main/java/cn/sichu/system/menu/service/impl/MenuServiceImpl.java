package cn.sichu.system.menu.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.app.mapper.SysAppMapper;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.menu.mapper.SysMenuMapper;
import cn.sichu.system.menu.model.form.SysMenuForm;
import cn.sichu.system.menu.model.vo.SysMenuTreeVO;
import cn.sichu.system.menu.service.IMenuService;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.sichu.common.base.BaseImpl;
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
    private final SysAppMapper appMapper;

    @Override
    public List<SysMenuTreeVO> tree(boolean includeButtons) {
        LambdaQueryWrapper<SysMenu> wrapper =
            new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortNo).orderByAsc(SysMenu::getId);
        if (!includeButtons) {
            wrapper.in(SysMenu::getMenuType, TYPE_DIR, TYPE_MENU);
        }
        return buildTree(menuMapper.selectList(wrapper));
    }

    @Override
    public List<SysMenuTreeVO> currentUserTree() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        List<SysMenu> menus;
        if (loginUser.getRoles().contains("SUPER_ADMIN")) {
            menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1).eq(SysMenu::getHidden, 1)
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
        SysMenuTreeVO root = tree.stream().filter(node -> "apps".equals(node.getMenuCode()))
            .findFirst().orElse(null);
        if (root == null) {
            return;
        }
        List<SysApp> apps = appMapper.selectList(new LambdaQueryWrapper<SysApp>()
            .eq(SysApp::getStatus, 1)
            .ne(SysApp::getCode, "workspace")
            .orderByAsc(SysApp::getSortNo)
            .orderByAsc(SysApp::getId));
        for (SysApp app : apps) {
            if (app.getBasePath() == null || app.getBasePath().isBlank()
                || !loginUser.getRoles().contains("SUPER_ADMIN")
                && !loginUser.getPermissions().contains(app.getAccessPermission())) {
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
        validateType(form.getMenuType());
        checkCodeUnique(form.getAppCode(), form.getMenuCode(), null);
        checkParent(form.getParentId(), null);
        SysMenu menu = new SysMenu();
        applyForm(menu, form);
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysMenuForm form) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND);
        }
        validateType(form.getMenuType());
        checkCodeUnique(form.getAppCode(), form.getMenuCode(), id);
        checkParent(form.getParentId(), id);
        applyForm(menu, form);
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (menuMapper.selectById(id) == null) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND);
        }
        Long children =
            menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BusinessException(SystemCode.MENU_HAS_CHILDREN);
        }
        menuMapper.deleteById(id);
    }

    private void applyForm(SysMenu menu, SysMenuForm form) {
        menu.setAppCode(form.getAppCode() == null ? "platform" : form.getAppCode());
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

    private void validateType(String menuType) {
        if (!TYPE_DIR.equals(menuType) && !TYPE_MENU.equals(menuType) && !TYPE_BUTTON.equals(
            menuType)) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND,
                "菜单类型必须是 DIR/MENU/BUTTON");
        }
    }

    private void checkCodeUnique(String appCode, String menuCode, Long excludeId) {
        String app = appCode == null ? "platform" : appCode;
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getAppCode, app)
            .eq(SysMenu::getMenuCode, menuCode);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemCode.MENU_CODE_EXISTS);
        }
    }

    private void checkParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (Objects.equals(parentId, selfId)) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND, "上级菜单不能是自己");
        }
        if (menuMapper.selectById(parentId) == null) {
            throw new BusinessException(SystemCode.MENU_NOT_FOUND, "上级菜单不存在");
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

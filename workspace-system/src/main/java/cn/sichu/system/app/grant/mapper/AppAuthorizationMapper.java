package cn.sichu.system.app.grant.mapper;

import cn.sichu.system.app.entity.SysApp;
import cn.sichu.system.menu.entity.SysMenu;
import cn.sichu.system.role.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Mapper
public interface AppAuthorizationMapper {

    List<SysApp> selectAccessibleApps(@Param("userId") Long userId);

    List<SysRole> selectRoles(@Param("userId") Long userId,
        @Param("appCode") String appCode);

    List<SysRole> selectAssignedRoles(@Param("userId") Long userId,
        @Param("appCode") String appCode);

    Set<String> selectRoleCodes(@Param("userId") Long userId,
        @Param("appCode") String appCode);

    Set<String> selectPermissions(@Param("userId") Long userId,
        @Param("appCode") String appCode);

    List<SysMenu> selectMenus(@Param("userId") Long userId,
        @Param("appCode") String appCode);

    List<SysMenu> selectAllMenus(@Param("appCode") String appCode);

    Set<String> selectAllPermissions(@Param("appCode") String appCode);
}

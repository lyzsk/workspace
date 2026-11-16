package cn.sichu.system.role.mapper;

import cn.sichu.system.role.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    int deleteUserRolesByRoleId(@Param("roleId") Long roleId);

    List<SysRole> selectByUserId(@Param("userId") Long userId);
}

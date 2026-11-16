package cn.sichu.system.menu.mapper;

import cn.sichu.system.menu.entity.SysMenu;
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
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<SysMenu> selectByUserId(@Param("userId") Long userId);
}

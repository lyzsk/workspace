package cn.sichu.system.security.mapper;

import cn.sichu.system.security.entity.SysUserSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author sichu huang
 * @since 2026/11/16
 */
@Mapper
public interface SysUserSessionMapper extends BaseMapper<SysUserSession> {

    @Select("SELECT * FROM system.sys_user_session WHERE id = #{id} FOR UPDATE")
    SysUserSession selectForUpdateById(@Param("id") Long id);

    @Select("SELECT * FROM system.sys_user_session WHERE session_code = #{sessionCode} FOR UPDATE")
    SysUserSession selectForUpdateBySessionCode(@Param("sessionCode") String sessionCode);

    @Select("SELECT * FROM system.sys_user_session WHERE user_id = #{userId} AND revoked = 0 FOR UPDATE")
    List<SysUserSession> selectForUpdateByUserId(@Param("userId") Long userId);
}

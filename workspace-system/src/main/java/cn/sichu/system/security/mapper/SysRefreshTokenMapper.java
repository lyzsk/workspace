package cn.sichu.system.security.mapper;

import cn.sichu.system.security.entity.SysRefreshToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Mapper
public interface SysRefreshTokenMapper extends BaseMapper<SysRefreshToken> {

    @Select("SELECT * FROM system.sys_refresh_token WHERE token_hash = #{tokenHash}")
    SysRefreshToken selectByHash(@Param("tokenHash") String tokenHash);

    @Select("SELECT * FROM system.sys_refresh_token WHERE token_hash = #{tokenHash} FOR UPDATE")
    SysRefreshToken selectForUpdateByHash(@Param("tokenHash") String tokenHash);

    @Update("UPDATE system.sys_refresh_token SET consumed_at = #{consumedAt}, "
        + "replaced_by_hash = #{replacedByHash} WHERE id = #{id} "
        + "AND consumed_at IS NULL AND revoked_at IS NULL")
    int consume(@Param("id") Long id, @Param("consumedAt") LocalDateTime consumedAt,
        @Param("replacedByHash") String replacedByHash);

    @Update("UPDATE system.sys_refresh_token SET revoked_at = #{revokedAt} "
        + "WHERE session_id = #{sessionId} AND revoked_at IS NULL")
    int revokeBySessionId(@Param("sessionId") Long sessionId,
        @Param("revokedAt") LocalDateTime revokedAt);
}

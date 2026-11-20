package cn.sichu.system.sync;

import cn.sichu.system.sync.model.vo.InternalUserSyncVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
@Mapper
public interface InternalUserSyncMapper {

    InternalUserSyncVO selectAuthorizedUser(@Param("appCode") String appCode,
        @Param("username") String username);

    List<InternalUserSyncVO> selectAuthorizedUsers(@Param("appCode") String appCode,
        @Param("updatedAfter") LocalDateTime updatedAfter, @Param("offset") long offset,
        @Param("size") int size);

    long countAuthorizedUsers(@Param("appCode") String appCode,
        @Param("updatedAfter") LocalDateTime updatedAfter);
}

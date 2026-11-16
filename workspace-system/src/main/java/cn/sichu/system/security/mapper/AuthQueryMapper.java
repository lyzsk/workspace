package cn.sichu.system.security.mapper;

import cn.sichu.system.auth.model.vo.UserOrgVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Mapper
public interface AuthQueryMapper {

    List<String> selectRoleCodes(@Param("userId") Long userId);

    List<String> selectPermissions(@Param("userId") Long userId);

    UserOrgVO selectUserOrg(@Param("userId") Long userId);
}

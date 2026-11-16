package cn.sichu.system.user.model.vo;

import cn.sichu.system.user.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysUserPageVO {
    private String id;

    private String username;

    private String realName;

    private Integer gender;

    private String email;

    private String mobile;

    private String deptId;

    private Integer status;

    private LocalDateTime createTime;

    private List<String> roleIds;

    public static SysUserPageVO from(SysUser user, List<String> roleIds) {
        SysUserPageVO vo = new SysUserPageVO();
        vo.setId(user.getId() == null ? null : user.getId().toString());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setGender(user.getGender());
        vo.setEmail(user.getEmail());
        vo.setMobile(user.getMobile());
        vo.setDeptId(user.getDeptId() == null ? null : user.getDeptId().toString());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoleIds(roleIds);
        return vo;
    }
}

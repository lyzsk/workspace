package cn.sichu.system.sync.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
@Data
public class InternalUserSyncVO {

    private Long workspaceUserId;
    private String username;
    private String realName;
    private String email;
    private String mobile;
    private Integer gender;
    private Long deptId;
    private Integer status;
    private Integer userStatus;
    private Integer appStatus;
    private Integer grantStatus;
    private String passwordHash;
    private String roleCodes;
    private LocalDateTime updatedAt;
}

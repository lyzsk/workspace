package cn.sichu.system.app.grant.model.vo;

import lombok.Data;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
public class AppGrantVO {

    private String appCode;

    private String appName;

    private Integer appStatus;

    private boolean granted;

    private boolean effective;

    private Integer status;

    private List<String> roleIds;

    private List<String> roleNames;

    private Integer dataScope;

    private List<String> deptIds;
}

package cn.sichu.system.app.model.vo;

import cn.sichu.system.app.entity.SysApp;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysAppVO {

    private String id;

    private String code;

    private String name;

    private String icon;

    private String basePath;

    private String moduleEntry;

    private String styleEntry;

    private String accessPermission;

    private Integer sortNo;

    private Integer status;

    private String remark;

    public static SysAppVO from(SysApp app) {
        SysAppVO vo = new SysAppVO();
        vo.setId(app.getId() == null ? null : app.getId().toString());
        vo.setCode(app.getCode());
        vo.setName(app.getName());
        vo.setIcon(app.getIcon());
        vo.setBasePath(app.getBasePath());
        vo.setModuleEntry(app.getModuleEntry());
        vo.setStyleEntry(app.getStyleEntry());
        vo.setAccessPermission(app.getAccessPermission());
        vo.setSortNo(app.getSortNo());
        vo.setStatus(app.getStatus());
        vo.setRemark(app.getRemark());
        return vo;
    }
}

package cn.sichu.system.dept.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysDeptTreeVO {

    private String id;

    private String parentId;

    private String deptCode;

    private String deptName;

    private Integer sortNo;

    private Integer status;

    private LocalDateTime createTime;

    private List<SysDeptTreeVO> children = new ArrayList<>();
}

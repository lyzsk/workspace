package cn.sichu.system.dict.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysDictDataVO {

    private String id;

    private String dictType;

    private String label;

    private String value;

    private Integer sortNo;

    private Integer status;

    private LocalDateTime createTime;

    private String remark;
}

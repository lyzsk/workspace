package cn.sichu.system.dict.entity;

import cn.sichu.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    private String dictType;

    private String dictName;

    private Integer status;
}

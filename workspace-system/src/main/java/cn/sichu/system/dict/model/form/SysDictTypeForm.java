package cn.sichu.system.dict.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysDictTypeForm {

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100)
    private String dictType;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100)
    private String dictName;

    private Integer status = 1;

    @Size(max = 500)
    private String remark;
}

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
public class SysDictDataForm {

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100)
    private String dictType;

    @NotBlank(message = "显示文本不能为空")
    @Size(max = 100)
    private String label;

    @NotBlank(message = "字典值不能为空")
    @Size(max = 100)
    private String value;

    private Integer sortNo = 0;

    private Integer status = 1;

    @Size(max = 500)
    private String remark;
}

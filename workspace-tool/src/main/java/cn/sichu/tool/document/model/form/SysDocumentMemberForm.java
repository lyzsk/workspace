package cn.sichu.tool.document.model.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysDocumentMemberForm {

    @NotNull
    private Long userId;

    @NotNull
    @Pattern(regexp = "READ|WRITE")
    private String permission;
}

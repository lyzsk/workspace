package cn.sichu.tool.document.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysDocumentForm {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Pattern(regexp = "FLOW|MIND|UML|WHITEBOARD")
    private String docType;
}

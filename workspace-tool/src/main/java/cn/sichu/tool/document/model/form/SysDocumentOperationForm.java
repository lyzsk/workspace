package cn.sichu.tool.document.model.form;

import com.fasterxml.jackson.databind.JsonNode;
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
public class SysDocumentOperationForm {

    @NotBlank
    @Pattern(regexp = "UPSERT|DELETE|SCENE_REPLACE")
    private String opType;

    private String targetType;

    @Size(max = 100)
    private String targetId;

    private JsonNode payload;
}

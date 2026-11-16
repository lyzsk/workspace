package cn.sichu.generator.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Data
public class GeneratorRequest {

    @NotBlank
    private String schemaName;

    @NotBlank
    private String tableName;

    @NotBlank
    private String moduleName;

    @NotBlank
    private String businessName;

    @NotBlank
    private String packageName;

    @NotBlank
    private String functionName;

    @NotBlank
    private String permissionPrefix;

    private String className;

    private Boolean tenantScoped = true;
}

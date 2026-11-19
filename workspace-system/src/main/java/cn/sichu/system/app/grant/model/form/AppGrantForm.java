package cn.sichu.system.app.grant.model.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
public class AppGrantForm {

    @NotNull
    private Integer status;

    @NotNull
    private List<Long> roleIds = new ArrayList<>();

    private Integer dataScope;

    @NotNull
    private List<Long> deptIds = new ArrayList<>();
}

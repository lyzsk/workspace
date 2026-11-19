package cn.sichu.quartz.controller;

import cn.sichu.common.core.result.Result;
import cn.sichu.quartz.model.form.CronValidationForm;
import cn.sichu.quartz.model.vo.CronValidationVO;
import cn.sichu.quartz.service.CronExpressionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@RestController
@RequestMapping("/api/system/jobs/cron")
@RequiredArgsConstructor
public class CronExpressionController {

    private final CronExpressionService cronExpressionService;

    @PostMapping("/validate")
    @PreAuthorize("@ss.hasPerm('sys:job:list')")
    public Result<CronValidationVO> validate(
        @RequestBody @Valid CronValidationForm form) {
        return Result.success(
            cronExpressionService.validate(form.expression()));
    }
}

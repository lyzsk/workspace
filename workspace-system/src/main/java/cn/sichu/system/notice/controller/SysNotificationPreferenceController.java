package cn.sichu.system.notice.controller;

import cn.sichu.common.core.result.Result;
import cn.sichu.system.notice.entity.SysNotificationPreference;
import cn.sichu.system.notice.model.form.SysNotificationPreferenceForm;
import cn.sichu.system.notice.service.INotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@RestController
@RequestMapping("/api/system/notification-preferences")
@RequiredArgsConstructor
public class SysNotificationPreferenceController {

    private final INotificationPreferenceService preferenceService;

    @GetMapping("/my")
    public Result<SysNotificationPreference> getMy() {
        return Result.success(preferenceService.getMy());
    }

    @PutMapping("/my")
    public Result<SysNotificationPreference> updateMy(
        @RequestBody @Valid SysNotificationPreferenceForm form) {
        return Result.success(preferenceService.updateMy(form));
    }
}

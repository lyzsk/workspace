package cn.sichu.system.notice.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.common.core.log.annotation.Log;
import cn.sichu.system.notice.model.form.SysNoticeForm;
import cn.sichu.system.notice.model.form.SysNoticePublishForm;
import cn.sichu.system.notice.model.query.SysNoticeQuery;
import cn.sichu.system.notice.model.vo.SysNoticeVO;
import cn.sichu.system.notice.service.INoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/system/notices")
@RequiredArgsConstructor
public class SysNoticeController {

    private final INoticeService noticeService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('system:notice:list')")
    public Result<PageResult<SysNoticeVO>> page(@Valid SysNoticeQuery query) {
        return Result.success(noticeService.page(query));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('system:notice:create')")
    @Log(module = "通知管理", operation = "创建通知")
    public Result<String> create(@RequestBody @Valid SysNoticeForm form) {
        return Result.success(noticeService.create(form).toString());
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPerm('system:notice:publish')")
    @Log(module = "通知管理", operation = "发布通知")
    public Result<Integer> publish(@PathVariable Long id,
        @RequestBody(required = false) @Valid SysNoticePublishForm form) {
        return Result.success(noticeService.publish(id, form));
    }
}

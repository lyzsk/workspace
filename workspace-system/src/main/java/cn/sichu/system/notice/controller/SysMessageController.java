package cn.sichu.system.notice.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.system.notice.model.query.SysMessageQuery;
import cn.sichu.system.notice.model.vo.SysMessageVO;
import cn.sichu.system.notice.service.IMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@RestController
@RequestMapping("/api/system/messages")
@RequiredArgsConstructor
public class SysMessageController {

    private final IMessageService messageService;

    @GetMapping("/my")
    public Result<PageResult<SysMessageVO>> my(@Valid SysMessageQuery query) {
        return Result.success(messageService.pageMy(query));
    }

    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        return Result.success(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.read(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Integer> readAll() {
        return Result.success(messageService.readAll());
    }

    @PutMapping("/{id}/done")
    public Result<Void> done(@PathVariable Long id) {
        messageService.done(id);
        return Result.success();
    }
}

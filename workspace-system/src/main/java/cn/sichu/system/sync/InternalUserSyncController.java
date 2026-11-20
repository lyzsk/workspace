package cn.sichu.system.sync;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.system.sync.model.vo.InternalUserSyncVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserSyncController {

    private final IInternalUserSyncService syncService;

    @GetMapping("/sync")
    public Result<PageResult<InternalUserSyncVO>> sync(Authentication authentication,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime updatedAfter, @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "100") int size) {
        return Result.success(
            syncService.pageAuthorizedUsers(authentication.getName(), updatedAfter, page, size));
    }

    @GetMapping("/sync/{username}")
    public Result<InternalUserSyncVO> syncOne(Authentication authentication,
        @PathVariable String username) {
        return Result.success(
            syncService.findAuthorizedUser(authentication.getName(), username));
    }
}

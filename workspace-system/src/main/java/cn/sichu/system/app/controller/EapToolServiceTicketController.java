package cn.sichu.system.app.controller;

import cn.sichu.common.result.Result;
import cn.sichu.system.app.model.vo.EapToolServiceTicketVO;
import cn.sichu.system.app.service.IEapToolServiceTicketService;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@RestController
@RequestMapping("/api/eap-tool-service")
@RequiredArgsConstructor
public class EapToolServiceTicketController {

    private final IEapToolServiceTicketService ticketService;

    @PostMapping("/ticket")
    @PreAuthorize("@ss.hasPerm('eap:access')")
    public Result<EapToolServiceTicketVO> ticket() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return Result.success(ticketService.issue(loginUser));
    }
}

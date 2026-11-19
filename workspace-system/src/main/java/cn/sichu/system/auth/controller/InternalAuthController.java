package cn.sichu.system.auth.controller;

import cn.sichu.system.auth.model.form.IntrospectionForm;
import cn.sichu.system.auth.model.vo.IntrospectionVO;
import cn.sichu.system.security.service.OpaqueTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@RestController
@RequestMapping("/api/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final OpaqueTokenService tokenService;

    @PostMapping("/introspect")
    public IntrospectionVO introspect(@RequestBody @Valid IntrospectionForm form) {
        return tokenService.introspect(form.token());
    }
}

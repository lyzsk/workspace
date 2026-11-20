package cn.sichu.system.apiclient.controller;

import cn.sichu.common.core.log.annotation.Log;
import cn.sichu.common.core.result.Result;
import cn.sichu.system.apiclient.model.form.ApiClientForm;
import cn.sichu.system.apiclient.model.vo.ApiClientSecretVO;
import cn.sichu.system.apiclient.model.vo.ApiClientVO;
import cn.sichu.system.apiclient.service.IApiClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@RestController
@RequestMapping("/api/system/api-clients")
@RequiredArgsConstructor
public class ApiClientController {

    private final IApiClientService apiClientService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:api-client:view')")
    public Result<List<ApiClientVO>> list() {
        return Result.success(apiClientService.list());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:api-client:manage')")
    @Log(module = "调用方管理", operation = "新增调用方")
    public Result<ApiClientSecretVO> create(@RequestBody @Valid ApiClientForm form) {
        return Result.success(apiClientService.create(form));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:api-client:manage')")
    @Log(module = "调用方管理", operation = "修改调用方")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ApiClientForm form) {
        apiClientService.update(id, form);
        return Result.success();
    }

    @PostMapping("/{id}/secret")
    @PreAuthorize("@ss.hasPerm('sys:api-client:manage')")
    @Log(module = "调用方管理", operation = "重置调用方密钥")
    public Result<ApiClientSecretVO> resetSecret(@PathVariable Long id) {
        return Result.success(apiClientService.resetSecret(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:api-client:manage')")
    @Log(module = "调用方管理", operation = "删除调用方")
    public Result<Void> delete(@PathVariable Long id) {
        apiClientService.delete(id);
        return Result.success();
    }
}

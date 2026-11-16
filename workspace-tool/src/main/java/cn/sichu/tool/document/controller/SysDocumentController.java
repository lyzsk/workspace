package cn.sichu.tool.document.controller;

import cn.sichu.common.result.PageResult;
import cn.sichu.common.result.Result;
import cn.sichu.tool.document.model.form.SysDocumentForm;
import cn.sichu.tool.document.model.form.SysDocumentMemberForm;
import cn.sichu.tool.document.model.form.SysDocumentOperationForm;
import cn.sichu.tool.document.model.query.SysDocumentQuery;
import cn.sichu.tool.document.model.vo.SysDocumentMemberVO;
import cn.sichu.tool.document.model.vo.SysDocumentVO;
import cn.sichu.tool.document.service.IDocumentService;
import cn.sichu.system.security.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@RestController
@RequestMapping("/api/system/documents")
@RequiredArgsConstructor
public class SysDocumentController {

    private final IDocumentService documentService;

    @GetMapping
    public Result<PageResult<SysDocumentVO>> page(@Valid SysDocumentQuery query) {
        return Result.success(documentService.page(query));
    }

    @PostMapping
    public Result<String> create(@RequestBody @Valid SysDocumentForm form) {
        return Result.success(documentService.create(form).toString());
    }

    @GetMapping("/{id}")
    public Result<SysDocumentVO> get(@PathVariable Long id) {
        return Result.success(documentService.get(id));
    }

    @PutMapping("/{id}")
    public Result<Void> rename(@PathVariable Long id, @RequestBody @Valid SysDocumentForm form) {
        documentService.rename(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/members")
    public Result<List<SysDocumentMemberVO>> members(@PathVariable Long id) {
        return Result.success(documentService.members(id));
    }

    @PutMapping("/{id}/members")
    public Result<Void> share(@PathVariable Long id, @RequestBody @Valid SysDocumentMemberForm form) {
        documentService.share(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        documentService.removeMember(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/operations")
    public Result<Long> operate(@PathVariable Long id,
        @RequestBody @Valid SysDocumentOperationForm form) {
        return Result.success(documentService.applyOperation(id,
            SecurityUtils.getLoginUser().getUserId(), form));
    }

    @PostMapping("/{id}/import")
    public Result<Long> importContent(@PathVariable Long id, @RequestBody JsonNode content) {
        SysDocumentOperationForm form = new SysDocumentOperationForm();
        form.setOpType("SCENE_REPLACE");
        form.setPayload(content);
        return Result.success(documentService.applyOperation(id,
            SecurityUtils.getLoginUser().getUserId(), form));
    }

    @GetMapping("/{id}/export")
    public void exportContent(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysDocumentVO document = documentService.get(id);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename(document.getTitle() + ".json", StandardCharsets.UTF_8).build().toString());
        response.getWriter().write(document.getContent());
        response.getWriter().flush();
    }
}

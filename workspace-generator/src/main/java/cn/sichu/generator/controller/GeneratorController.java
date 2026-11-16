package cn.sichu.generator.controller;

import cn.sichu.common.result.Result;
import cn.sichu.generator.model.GenColumn;
import cn.sichu.generator.model.form.GeneratorRequest;
import cn.sichu.generator.service.GeneratorMetadataService;
import cn.sichu.generator.service.GeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "workspace.generator.enabled", havingValue = "true")
public class GeneratorController {

    private final GeneratorMetadataService metadataService;
    private final GeneratorService generatorService;

    @GetMapping("/tables")
    @PreAuthorize("@ss.hasPerm('system:generator:list')")
    public Result<List<String>> tables(@RequestParam String schemaName) {
        return Result.success(metadataService.listTables(schemaName));
    }

    @GetMapping("/columns")
    @PreAuthorize("@ss.hasPerm('system:generator:list')")
    public Result<List<GenColumn>> columns(@RequestParam String schemaName,
        @RequestParam String tableName) {
        return Result.success(metadataService.listColumns(schemaName, tableName));
    }

    @PostMapping("/preview")
    @PreAuthorize("@ss.hasPerm('system:generator:preview')")
    public Result<Map<String, String>> preview(@RequestBody @Valid GeneratorRequest request) {
        return Result.success(generatorService.preview(request));
    }

    @PostMapping("/download")
    @PreAuthorize("@ss.hasPerm('system:generator:download')")
    public ResponseEntity<byte[]> download(@RequestBody @Valid GeneratorRequest request) {
        byte[] content = generatorService.download(request);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename("workspace-generator.zip", java.nio.charset.StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(content.length)
            .body(content);
    }
}

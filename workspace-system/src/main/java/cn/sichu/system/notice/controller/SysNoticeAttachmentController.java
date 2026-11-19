package cn.sichu.system.notice.controller;

import cn.sichu.common.core.result.Result;
import cn.sichu.system.file.model.FileDownload;
import cn.sichu.system.file.model.vo.SysFileVO;
import cn.sichu.system.notice.service.INoticeAttachmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@RestController
@RequestMapping("/api/system/notices/{noticeId}/attachments")
@RequiredArgsConstructor
public class SysNoticeAttachmentController {

    private final INoticeAttachmentService attachmentService;

    @GetMapping
    public Result<List<SysFileVO>> list(@PathVariable Long noticeId) {
        return Result.success(attachmentService.list(noticeId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('system:notice:create')")
    public Result<SysFileVO> upload(@PathVariable Long noticeId,
        @RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(attachmentService.upload(noticeId, file));
    }

    @GetMapping("/{fileId}")
    public void download(@PathVariable Long noticeId, @PathVariable Long fileId,
        HttpServletResponse response) throws IOException {
        FileDownload download = attachmentService.download(noticeId, fileId);
        response.setContentType(download.file().getContentType() == null
            ? "application/octet-stream" : download.file().getContentType());
        response.setContentLengthLong(download.file().getSizeBytes());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename(download.file().getOriginalName(), StandardCharsets.UTF_8).build().toString());
        try (InputStream input = download.input()) {
            input.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}

package cn.sichu.system.notice.service;

import cn.sichu.system.file.model.FileDownload;
import cn.sichu.system.file.model.vo.SysFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
public interface INoticeAttachmentService {

    SysFileVO upload(Long noticeId, MultipartFile file) throws IOException;

    List<SysFileVO> list(Long noticeId);

    FileDownload download(Long noticeId, Long fileId) throws IOException;
}

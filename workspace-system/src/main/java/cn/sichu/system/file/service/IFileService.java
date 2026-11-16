package cn.sichu.system.file.service;

import cn.sichu.system.file.entity.SysFile;
import cn.sichu.system.file.model.FileDownload;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public interface IFileService {

    SysFile store(MultipartFile file) throws IOException;

    SysFile get(Long fileId);

    List<SysFile> listByFileIds(Collection<Long> fileIds);

    FileDownload download(Long fileId) throws IOException;

    void delete(Long fileId) throws IOException;
}

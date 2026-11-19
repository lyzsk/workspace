package cn.sichu.system.file.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.file.FileStorage;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.common.core.base.BaseImpl;
import cn.sichu.system.file.entity.SysFile;
import cn.sichu.system.file.mapper.SysFileMapper;
import cn.sichu.system.file.model.FileDownload;
import cn.sichu.system.file.service.IFileService;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends BaseImpl<SysFileMapper, SysFile> implements IFileService {

    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
        "txt", "pdf", "png", "jpg", "jpeg", "docx", "xlsx", "pptx");

    private final FileStorage fileStorage;

    @Override
    @Transactional
    public SysFile store(MultipartFile multipart) throws IOException {
        LoginUser current = SecurityUtils.getLoginUser();
        if (multipart == null || multipart.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (multipart.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能超过 20MB");
        }
        String originalName = multipart.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || !ALLOWED.contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件类型不允许");
        }
        String safeName = originalName == null ? "file" : originalName;
        String storedName = UUID.randomUUID() + "." + extension.toLowerCase();
        String storageKey =
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/" + storedName;
        try {
            byte[] content = multipart.getBytes();
            fileStorage.store(new ByteArrayInputStream(content), storageKey);
            SysFile file = new SysFile();
            file.setTenantId(current.getTenantId());
            file.setOriginalName(safeName);
            file.setStoredName(storedName);
            file.setStorageKey(storageKey);
            file.setContentType(multipart.getContentType());
            file.setSizeBytes((long) content.length);
            file.setSha256(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)));
            file.setUploaderId(current.getUserId());
            baseMapper.insert(file);
            return file;
        } catch (Exception e) {
            try {
                fileStorage.delete(storageKey);
            } catch (IOException ignored) {
            }
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException(e);
        }
    }

    @Override
    public SysFile get(Long fileId) {
        return fileId == null ? null : baseMapper.selectById(fileId);
    }

    @Override
    public List<SysFile> listByFileIds(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysFile> byId = baseMapper.selectBatchIds(fileIds).stream()
            .collect(Collectors.toMap(SysFile::getId, Function.identity()));
        return fileIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    @Override
    public FileDownload download(Long fileId) throws IOException {
        SysFile file = get(fileId);
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        return new FileDownload(file, fileStorage.open(file.getStorageKey()));
    }

    @Override
    @Transactional
    public void delete(Long fileId) throws IOException {
        SysFile file = get(fileId);
        if (file == null) {
            return;
        }
        fileStorage.delete(file.getStorageKey());
        baseMapper.deleteById(fileId);
    }
}

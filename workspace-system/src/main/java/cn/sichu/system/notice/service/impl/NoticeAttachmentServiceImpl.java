package cn.sichu.system.notice.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.system.file.entity.SysFile;
import cn.sichu.system.file.model.FileDownload;
import cn.sichu.system.file.model.vo.SysFileVO;
import cn.sichu.system.file.service.IFileService;
import cn.sichu.system.notice.entity.SysMessage;
import cn.sichu.system.notice.entity.SysNotice;
import cn.sichu.system.notice.entity.SysNoticeAttachment;
import cn.sichu.system.notice.mapper.SysMessageMapper;
import cn.sichu.system.notice.mapper.SysNoticeAttachmentMapper;
import cn.sichu.system.notice.mapper.SysNoticeMapper;
import cn.sichu.system.notice.service.INoticeAttachmentService;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Service
@RequiredArgsConstructor
public class NoticeAttachmentServiceImpl implements INoticeAttachmentService {

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeAttachmentMapper attachmentMapper;
    private final SysMessageMapper messageMapper;
    private final IFileService fileService;

    @Override
    @Transactional
    public SysFileVO upload(Long noticeId, MultipartFile multipart) throws IOException {
        SysNotice notice = noticeMapper.selectById(noticeId);
        LoginUser current = SecurityUtils.getLoginUser();
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        if (notice.getStatus() == null || notice.getStatus() != 0) {
            throw new BusinessException(ResultCode.CONFLICT, "仅草稿通知可以上传附件");
        }
        assertOwner(notice, current);
        SysFile file = fileService.store(multipart);
        try {
            SysNoticeAttachment attachment = new SysNoticeAttachment();
            attachment.setTenantId(notice.getTenantId());
            attachment.setNoticeId(noticeId);
            attachment.setFileId(file.getId());
            attachment.setSortNo(0);
            attachmentMapper.insert(attachment);
            return SysFileVO.from(file);
        } catch (RuntimeException e) {
            try {
                fileService.delete(file.getId());
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    @Override
    public List<SysFileVO> list(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        assertReadable(notice, SecurityUtils.getLoginUser());
        List<SysNoticeAttachment> attachments = attachmentMapper.selectList(
            new LambdaQueryWrapper<SysNoticeAttachment>()
                .eq(SysNoticeAttachment::getNoticeId, noticeId)
                .orderByAsc(SysNoticeAttachment::getSortNo));
        List<Long> fileIds = attachments.stream().map(SysNoticeAttachment::getFileId).toList();
        return fileService.listByFileIds(fileIds).stream().map(SysFileVO::from).toList();
    }

    @Override
    public FileDownload download(Long noticeId, Long fileId) throws IOException {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        assertReadable(notice, SecurityUtils.getLoginUser());
        SysNoticeAttachment attachment = attachmentMapper.selectOne(
            new LambdaQueryWrapper<SysNoticeAttachment>()
                .eq(SysNoticeAttachment::getNoticeId, noticeId)
                .eq(SysNoticeAttachment::getFileId, fileId));
        if (attachment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "附件不存在");
        }
        return fileService.download(fileId);
    }

    private void assertReadable(SysNotice notice, LoginUser current) {
        if (!current.getRoles().contains("SUPER_ADMIN")
            && !Objects.equals(notice.getCreateBy(), current.getUserId())) {
            Long count = messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getNoticeId, notice.getId())
                .eq(SysMessage::getReceiverId, current.getUserId()));
            if (count == 0) {
                throw new AccessDeniedException("无权查看该附件");
            }
        }
    }

    private void assertOwner(SysNotice notice, LoginUser current) {
        if (!current.getRoles().contains("SUPER_ADMIN")
            && !Objects.equals(notice.getCreateBy(), current.getUserId())) {
            throw new AccessDeniedException("无权修改该通知附件");
        }
    }
}

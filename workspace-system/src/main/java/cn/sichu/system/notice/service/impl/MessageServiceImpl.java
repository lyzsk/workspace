package cn.sichu.system.notice.service.impl;

import cn.sichu.common.result.PageResult;
import cn.sichu.common.base.BaseImpl;
import cn.sichu.system.notice.entity.SysMessage;
import cn.sichu.system.notice.entity.SysNotice;
import cn.sichu.system.notice.mapper.SysMessageMapper;
import cn.sichu.system.notice.mapper.SysNoticeMapper;
import cn.sichu.system.notice.model.query.SysMessageQuery;
import cn.sichu.system.notice.model.vo.SysMessageVO;
import cn.sichu.system.notice.service.IMessageService;
import cn.sichu.system.notice.service.NoticeTemplateRenderer;
import cn.sichu.system.notice.websocket.NotificationEventPublisher;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends BaseImpl<SysMessageMapper, SysMessage>
    implements IMessageService {

    private final SysNoticeMapper noticeMapper;
    private final NotificationEventPublisher eventPublisher;
    private final SysUserMapper userMapper;
    private final NoticeTemplateRenderer templateRenderer;

    @Override
    public PageResult<SysMessageVO> pageMy(SysMessageQuery query) {
        LoginUser current = SecurityUtils.getLoginUser();
        Page<SysMessage> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getReceiverId, current.getUserId())
            .eq(SysMessage::getTenantId, current.getTenantId())
            .eq(query.getReadFlag() != null, SysMessage::getReadFlag, query.getReadFlag())
            .eq(query.getDoneFlag() != null, SysMessage::getDoneFlag, query.getDoneFlag())
            .eq(StringUtils.hasText(query.getCategory()),
                SysMessage::getCategory, query.getCategory())
            .orderByDesc(SysMessage::getCreateTime);
        Page<SysMessage> result = baseMapper.selectPage(page, wrapper);
        List<Long> noticeIds = result.getRecords().stream().map(SysMessage::getNoticeId)
            .distinct().toList();
        Map<Long, SysNotice> notices = noticeIds.isEmpty() ? Map.of()
            : noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .in(SysNotice::getId, noticeIds)).stream()
                .collect(Collectors.toMap(SysNotice::getId, Function.identity()));
        SysUser receiver = userMapper.selectById(current.getUserId());
        List<SysMessageVO> records = result.getRecords().stream()
            .map(message -> toVO(message, notices.get(message.getNoticeId()), receiver)).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public int unreadCount() {
        LoginUser current = SecurityUtils.getLoginUser();
        return baseMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
            .eq(SysMessage::getReceiverId, current.getUserId())
            .eq(SysMessage::getTenantId, current.getTenantId())
            .eq(SysMessage::getReadFlag, 0)).intValue();
    }

    @Override
    public void read(Long id) {
        SysMessage message = requiredOwn(id);
        if (message.getReadFlag() != null && message.getReadFlag() == 1) {
            return;
        }
        message.setReadFlag(1);
        message.setReadTime(LocalDateTime.now());
        baseMapper.updateById(message);
        eventPublisher.publishAfterCommit(message.getReceiverId(), "unread-count.changed",
            Map.of("count", unreadCount()));
    }

    @Override
    public int readAll() {
        LoginUser current = SecurityUtils.getLoginUser();
        LambdaUpdateWrapper<SysMessage> update = new LambdaUpdateWrapper<>();
        update.eq(SysMessage::getReceiverId, current.getUserId())
            .eq(SysMessage::getTenantId, current.getTenantId())
            .eq(SysMessage::getReadFlag, 0)
            .set(SysMessage::getReadFlag, 1)
            .set(SysMessage::getReadTime, LocalDateTime.now());
        int affected = baseMapper.update(null, update);
        eventPublisher.publishAfterCommit(current.getUserId(), "unread-count.changed",
            Map.of("count", unreadCount()));
        return affected;
    }

    @Override
    public void done(Long id) {
        SysMessage message = requiredOwn(id);
        if (message.getDoneFlag() != null && message.getDoneFlag() == 1) {
            return;
        }
        message.setDoneFlag(1);
        message.setDoneTime(LocalDateTime.now());
        baseMapper.updateById(message);
        eventPublisher.publishAfterCommit(message.getReceiverId(), "unread-count.changed",
            Map.of("count", unreadCount()));
    }

    private SysMessage requiredOwn(Long id) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysMessage message = baseMapper.selectOne(new LambdaQueryWrapper<SysMessage>()
            .eq(SysMessage::getId, id)
            .eq(SysMessage::getReceiverId, current.getUserId())
            .eq(SysMessage::getTenantId, current.getTenantId()));
        if (message == null) {
            throw new AccessDeniedException("无权操作该消息");
        }
        return message;
    }

    private SysMessageVO toVO(SysMessage message, SysNotice notice, SysUser receiver) {
        SysMessageVO vo = new SysMessageVO();
        vo.setId(message.getId());
        vo.setNoticeId(message.getNoticeId());
        vo.setTitle(notice == null ? null : templateRenderer.title(notice, message, receiver));
        vo.setContent(notice == null ? null : templateRenderer.content(notice, message, receiver));
        vo.setCategory(message.getCategory());
        vo.setLevel(message.getLevel());
        vo.setLinkUrl(message.getLinkUrl());
        vo.setReadFlag(message.getReadFlag());
        vo.setReadTime(message.getReadTime());
        vo.setDoneFlag(message.getDoneFlag());
        vo.setDoneTime(message.getDoneTime());
        vo.setDueTime(message.getDueTime());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}

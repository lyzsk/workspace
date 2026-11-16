package cn.sichu.system.notice.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.PageResult;
import cn.sichu.common.base.BaseImpl;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.dept.entity.SysDept;
import cn.sichu.system.dept.mapper.SysDeptMapper;
import cn.sichu.system.notice.entity.SysMessage;
import cn.sichu.system.notice.entity.SysNotice;
import cn.sichu.system.notice.mapper.SysMessageMapper;
import cn.sichu.system.notice.mapper.SysNoticeMapper;
import cn.sichu.system.notice.model.form.SysNoticeForm;
import cn.sichu.system.notice.model.form.SysNoticePublishForm;
import cn.sichu.system.notice.model.query.SysNoticeQuery;
import cn.sichu.system.notice.model.vo.SysNoticeVO;
import cn.sichu.system.notice.service.INoticeService;
import cn.sichu.system.notice.websocket.NotificationEventPublisher;
import cn.sichu.system.role.entity.SysRole;
import cn.sichu.system.role.mapper.SysRoleMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.entity.SysUserRole;
import cn.sichu.system.user.mapper.SysUserMapper;
import cn.sichu.system.user.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends BaseImpl<SysNoticeMapper, SysNotice>
    implements INoticeService {

    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {
    };

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final ObjectMapper objectMapper;
    private final NotificationEventPublisher eventPublisher;

    @Override
    public PageResult<SysNoticeVO> page(SysNoticeQuery query) {
        Long tenantId = SecurityUtils.getLoginUser().getTenantId();
        Page<SysNotice> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotice::getTenantId, tenantId)
            .like(StringUtils.hasText(query.getTitle()),
                SysNotice::getTitle, query.getTitle())
            .eq(StringUtils.hasText(query.getNoticeType()),
                SysNotice::getNoticeType, query.getNoticeType())
            .eq(query.getStatus() != null, SysNotice::getStatus, query.getStatus())
            .orderByDesc(SysNotice::getCreateTime);
        Page<SysNotice> result = baseMapper.selectPage(page, wrapper);
        List<SysNoticeVO> records = result.getRecords().stream().map(notice -> {
            SysNoticeVO vo = new SysNoticeVO();
            BeanUtils.copyProperties(notice, vo);
            return vo;
        }).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public Long create(SysNoticeForm form) {
        validateTargetDefinition(form.getTargetType(), form.getTargetIds());
        LoginUser current = SecurityUtils.getLoginUser();
        SysNotice notice = new SysNotice();
        notice.setTenantId(current.getTenantId());
        notice.setTitle(form.getTitle());
        notice.setContent(form.getContent());
        notice.setNoticeType(form.getNoticeType());
        notice.setPriority(form.getPriority() == null ? 0 : form.getPriority());
        notice.setRepeatIntervalSeconds(form.getRepeatIntervalSeconds() == null
            ? 0L : form.getRepeatIntervalSeconds());
        notice.setMaxRemindCount(form.getMaxRemindCount() == null ? 1 : form.getMaxRemindCount());
        notice.setTemplateCode(form.getTemplateCode());
        notice.setTemplateParams(writeTemplateParams(form.getTemplateParams()));
        notice.setTargetType(form.getTargetType());
        notice.setTargetIds(writeTargetIds(form.getTargetType(), form.getTargetIds()));
        notice.setStatus(0);
        notice.setRemindTime(form.getRemindTime());
        notice.setExpireTime(form.getExpireTime());
        baseMapper.insert(notice);
        return notice.getId();
    }

    @Override
    @Transactional
    public int publish(Long id, SysNoticePublishForm form) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysNotice notice = required(id, current.getTenantId());
        if (notice.getStatus() != null && notice.getStatus() != 0) {
            throw new BusinessException(SystemCode.NOTICE_ALREADY_PUBLISHED);
        }
        Set<Long> receiverIds = resolveReceivers(notice, current.getTenantId());
        if (receiverIds.isEmpty()) {
            throw new BusinessException(SystemCode.NOTICE_TARGET_INVALID, "发布对象没有有效用户");
        }

        SysNoticePublishForm publishForm = form == null ? new SysNoticePublishForm() : form;
        LocalDateTime now = LocalDateTime.now();
        for (Long receiverId : receiverIds) {
            SysMessage message = new SysMessage();
            message.setTenantId(notice.getTenantId());
            message.setNoticeId(notice.getId());
            message.setReceiverId(receiverId);
            message.setCategory(notice.getNoticeType());
            message.setLevel(notice.getPriority() == null ? 0 : notice.getPriority());
            message.setLinkUrl(publishForm.getLinkUrl());
            message.setReadFlag(0);
            message.setDoneFlag(0);
            message.setDueTime(publishForm.getDueTime() == null
                ? notice.getRemindTime() : publishForm.getDueTime());
            message.setNextRemindTime(notice.getRemindTime());
            message.setRemindCount(0);
            messageMapper.insert(message);
            eventPublisher.publishAfterCommit(receiverId, "message.created", Map.of(
                "id", message.getId().toString(),
                "noticeId", notice.getId().toString(),
                "title", notice.getTitle(),
                "category", message.getCategory()));
        }

        notice.setStatus(1);
        notice.setPublisherId(current.getUserId());
        notice.setPublishTime(now);
        baseMapper.updateById(notice);
        return receiverIds.size();
    }

    private Set<Long> resolveReceivers(SysNotice notice, Long tenantId) {
        Set<Long> targetIds = readTargetIds(notice.getTargetIds());
        validateTargetDefinition(notice.getTargetType(), List.copyOf(targetIds));
        Set<Long> candidateIds = new LinkedHashSet<>();
        if ("ROLE".equals(notice.getTargetType())) {
            Long validRoles = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, targetIds)
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getStatus, 1));
            if (validRoles != targetIds.size()) {
                throw new BusinessException(SystemCode.NOTICE_TARGET_INVALID, "角色不存在或已停用");
            }
            candidateIds.addAll(userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                    .in(SysUserRole::getRoleId, targetIds)
                    .eq(SysUserRole::getTenantId, tenantId))
                .stream().map(SysUserRole::getUserId).toList());
        } else if ("DEPT".equals(notice.getTargetType())) {
            Long validDepts = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getId, targetIds)
                .eq(SysDept::getTenantId, tenantId)
                .eq(SysDept::getStatus, 1));
            if (validDepts != targetIds.size()) {
                throw new BusinessException(SystemCode.NOTICE_TARGET_INVALID, "部门不存在或已停用");
            }
        }
        if (!"ALL".equals(notice.getTargetType()) && candidateIds.isEmpty()
            && "ROLE".equals(notice.getTargetType())) {
            return candidateIds;
        }

        LambdaQueryWrapper<SysUser> users = new LambdaQueryWrapper<>();
        users.eq(SysUser::getTenantId, tenantId).eq(SysUser::getStatus, 1);
        if ("ROLE".equals(notice.getTargetType())) {
            users.in(SysUser::getId, candidateIds);
        } else if ("DEPT".equals(notice.getTargetType())) {
            users.in(SysUser::getDeptId, targetIds);
        }
        return userMapper.selectList(users).stream().map(SysUser::getId)
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private SysNotice required(Long id, Long tenantId) {
        SysNotice notice = baseMapper.selectById(id);
        if (notice == null || !Objects.equals(notice.getTenantId(), tenantId)) {
            throw new BusinessException(SystemCode.NOTICE_NOT_FOUND);
        }
        return notice;
    }

    private void validateTargetDefinition(String targetType, List<Long> targetIds) {
        if (!"ALL".equals(targetType) && (targetIds == null || targetIds.isEmpty())) {
            throw new BusinessException(SystemCode.NOTICE_TARGET_INVALID, "角色或部门发布必须选择目标");
        }
    }

    private String writeTargetIds(String targetType, List<Long> targetIds) {
        try {
            if ("ALL".equals(targetType) || targetIds == null) {
                return "[]";
            }
            return objectMapper.writeValueAsString(
                targetIds.stream().filter(Objects::nonNull).distinct().toList());
        } catch (JsonProcessingException e) {
            throw new BusinessException(SystemCode.NOTICE_TARGET_INVALID);
        }
    }

    private String writeTemplateParams(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params == null ? Map.of() : params);
        } catch (JsonProcessingException e) {
            throw new BusinessException(SystemCode.NOTICE_TARGET_INVALID, "模板参数格式错误");
        }
    }

    private Set<Long> readTargetIds(String value) {
        try {
            if (value == null || value.isBlank()) {
                return new LinkedHashSet<>();
            }
            return new LinkedHashSet<>(objectMapper.readValue(value, LONG_LIST));
        } catch (JsonProcessingException e) {
            throw new BusinessException(SystemCode.NOTICE_TARGET_INVALID, "发布目标格式错误");
        }
    }
}

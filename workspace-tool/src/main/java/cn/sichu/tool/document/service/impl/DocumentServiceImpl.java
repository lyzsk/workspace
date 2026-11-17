package cn.sichu.tool.document.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.PageResult;
import cn.sichu.common.result.ResultCode;
import cn.sichu.common.base.BaseImpl;
import cn.sichu.tool.document.entity.SysDocument;
import cn.sichu.tool.document.entity.SysDocumentMember;
import cn.sichu.tool.document.entity.SysDocumentOperation;
import cn.sichu.tool.document.mapper.SysDocumentMapper;
import cn.sichu.tool.document.mapper.SysDocumentMemberMapper;
import cn.sichu.tool.document.mapper.SysDocumentOperationMapper;
import cn.sichu.tool.document.model.form.SysDocumentForm;
import cn.sichu.tool.document.model.form.SysDocumentMemberForm;
import cn.sichu.tool.document.model.form.SysDocumentOperationForm;
import cn.sichu.tool.document.model.query.SysDocumentQuery;
import cn.sichu.tool.document.model.vo.SysDocumentMemberVO;
import cn.sichu.tool.document.model.vo.SysDocumentVO;
import cn.sichu.tool.document.service.IDocumentService;
import cn.sichu.tool.document.websocket.DocumentWebSocketPublisher;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends BaseImpl<SysDocumentMapper, SysDocument>
    implements IDocumentService {

    private static final String DEFAULT_CONTENT =
        "{\"schemaVersion\":1,\"elements\":{},\"relations\":{},\"appState\":{}}";

    private final SysDocumentMemberMapper memberMapper;
    private final SysDocumentOperationMapper operationMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final DocumentWebSocketPublisher eventPublisher;

    @Override
    public PageResult<SysDocumentVO> page(SysDocumentQuery query) {
        LoginUser current = SecurityUtils.getLoginUser();
        List<Long> memberIds = memberMapper.selectList(new LambdaQueryWrapper<SysDocumentMember>()
                .eq(SysDocumentMember::getUserId, current.getUserId()))
            .stream().map(SysDocumentMember::getDocumentId).toList();
        Page<SysDocument> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDocument::getTenantId, current.getTenantId())
            .eq(StringUtils.hasText(query.getDocType()),
                SysDocument::getDocType, query.getDocType())
            .like(StringUtils.hasText(query.getTitle()),
                SysDocument::getTitle, query.getTitle());
        if (memberIds.isEmpty()) {
            wrapper.eq(SysDocument::getOwnerId, current.getUserId());
        } else {
            wrapper.and(nested -> nested.eq(SysDocument::getOwnerId, current.getUserId())
                .or().in(SysDocument::getId, memberIds));
        }
        wrapper.orderByDesc(SysDocument::getUpdateTime);
        Page<SysDocument> result = baseMapper.selectPage(page, wrapper);
        List<SysDocumentVO> records = result.getRecords().stream()
            .map(document -> toVO(document, permissionOf(document, current.getUserId()))).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public Long create(SysDocumentForm form) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysDocument document = new SysDocument();
        document.setTenantId(current.getTenantId());
        document.setTitle(form.getTitle());
        document.setDocType(form.getDocType());
        document.setOwnerId(current.getUserId());
        document.setContent(DEFAULT_CONTENT);
        document.setRevision(0L);
        baseMapper.insert(document);

        SysDocumentMember member = new SysDocumentMember();
        member.setTenantId(document.getTenantId());
        member.setDocumentId(document.getId());
        member.setUserId(current.getUserId());
        member.setPermission("WRITE");
        memberMapper.insert(member);
        return document.getId();
    }

    @Override
    public SysDocumentVO get(Long id) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysDocument document = required(id, current.getUserId(), "READ");
        return toVO(document, permissionOf(document, current.getUserId()));
    }

    @Override
    public void rename(Long id, SysDocumentForm form) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysDocument document = required(id, current.getUserId(), "WRITE");
        document.setTitle(form.getTitle());
        baseMapper.updateById(document);
    }

    @Override
    public void delete(Long id) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysDocument document = requiredOwner(id, current.getUserId());
        baseMapper.deleteById(document.getId());
    }

    @Override
    public List<SysDocumentMemberVO> members(Long id) {
        LoginUser current = SecurityUtils.getLoginUser();
        requiredOwner(id, current.getUserId());
        List<SysDocumentMember> members = memberMapper.selectList(
            new LambdaQueryWrapper<SysDocumentMember>().eq(SysDocumentMember::getDocumentId, id));
        List<Long> userIds = members.stream().map(SysDocumentMember::getUserId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of()
            : userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds))
                .stream().collect(java.util.stream.Collectors.toMap(SysUser::getId, user -> user));
        return members.stream().map(member -> {
            SysDocumentMemberVO vo = new SysDocumentMemberVO();
            vo.setId(member.getId());
            vo.setUserId(member.getUserId());
            SysUser user = users.get(member.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
            vo.setRealName(user == null ? null : user.getRealName());
            vo.setPermission(member.getPermission());
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public void share(Long id, SysDocumentMemberForm form) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysDocument document = requiredOwner(id, current.getUserId());
        SysUser target = userMapper.selectById(form.getUserId());
        if (target == null || target.getStatus() == null || target.getStatus() != 1
            || !Objects.equals(target.getTenantId(), document.getTenantId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "共享用户不存在或已停用");
        }
        if (Objects.equals(document.getOwnerId(), target.getId())) {
            return;
        }
        SysDocumentMember member = memberMapper.selectOne(new LambdaQueryWrapper<SysDocumentMember>()
            .eq(SysDocumentMember::getDocumentId, id)
            .eq(SysDocumentMember::getUserId, target.getId()));
        if (member == null) {
            member = new SysDocumentMember();
            member.setTenantId(document.getTenantId());
            member.setDocumentId(id);
            member.setUserId(target.getId());
        }
        member.setPermission(form.getPermission());
        if (member.getId() == null) {
            memberMapper.insert(member);
        } else {
            memberMapper.updateById(member);
        }
    }

    @Override
    public void removeMember(Long id, Long userId) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysDocument document = requiredOwner(id, current.getUserId());
        if (Objects.equals(document.getOwnerId(), userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能移除文档所有者");
        }
        SysDocumentMember member = memberMapper.selectOne(new LambdaQueryWrapper<SysDocumentMember>()
            .eq(SysDocumentMember::getDocumentId, id)
            .eq(SysDocumentMember::getUserId, userId));
        if (member != null) {
            memberMapper.deleteById(member.getId());
        }
    }

    @Override
    @Transactional
    public long applyOperation(Long documentId, Long userId, SysDocumentOperationForm form) {
        SysDocument document = required(documentId, userId, "WRITE");
        try {
            ObjectNode content = (ObjectNode) objectMapper.readTree(document.getContent());
            String opType = form.getOpType();
            if ("SCENE_REPLACE".equals(opType)) {
                if (form.getPayload() == null || !form.getPayload().isObject()) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "场景内容格式错误");
                }
                content = (ObjectNode) form.getPayload();
            } else {
                String containerName = containerName(form.getTargetType());
                if (form.getTargetId() == null || form.getTargetId().isBlank()) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "操作目标不能为空");
                }
                ObjectNode container = content.with(containerName);
                if ("UPSERT".equals(opType)) {
                    if (form.getPayload() == null) {
                        throw new BusinessException(ResultCode.PARAM_ERROR, "元素内容不能为空");
                    }
                    container.set(form.getTargetId(), form.getPayload());
                } else if ("DELETE".equals(opType)) {
                    container.remove(form.getTargetId());
                } else {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "未知操作类型");
                }
            }

            long revision = (document.getRevision() == null ? 0L : document.getRevision()) + 1;
            LambdaUpdateWrapper<SysDocument> update = new LambdaUpdateWrapper<>();
            update.eq(SysDocument::getId, documentId)
                .eq(SysDocument::getRevision, document.getRevision())
                .set(SysDocument::getContent, objectMapper.writeValueAsString(content))
                .set(SysDocument::getRevision, revision);
            if (baseMapper.update(null, update) != 1) {
                throw new BusinessException(ResultCode.CONFLICT, "文档已被他人更新，请刷新后重试");
            }

            SysDocumentOperation operation = new SysDocumentOperation();
            operation.setTenantId(document.getTenantId());
            operation.setDocumentId(documentId);
            operation.setRevision(revision);
            operation.setOpType(opType);
            operation.setTargetType(form.getTargetType());
            operation.setTargetId(form.getTargetId());
            operation.setPayload(form.getPayload() == null ? null : form.getPayload().toString());
            operation.setOperatorId(userId);
            operationMapper.insert(operation);

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("opType", opType);
            event.put("targetType", form.getTargetType());
            event.put("targetId", form.getTargetId());
            event.put("payload", form.getPayload());
            event.put("revision", revision);
            event.put("operatorId", userId.toString());
            eventPublisher.publishAfterCommit(documentId, "document.operation", event);
            return revision;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAILED, "文档操作处理失败");
        }
    }

    @Override
    public void assertPermission(Long documentId, Long userId, String permission) {
        required(documentId, userId, permission);
    }

    private SysDocument required(Long id, Long userId, String permission) {
        SysDocument document = baseMapper.selectById(id);
        if (document == null) {
            throw new AccessDeniedException("文档不存在或无权访问");
        }
        if (Objects.equals(document.getOwnerId(), userId)) {
            return document;
        }
        SysDocumentMember member = memberMapper.selectOne(new LambdaQueryWrapper<SysDocumentMember>()
            .eq(SysDocumentMember::getDocumentId, id)
            .eq(SysDocumentMember::getUserId, userId));
        if (member == null || ("WRITE".equals(permission) && !"WRITE".equals(member.getPermission()))) {
            throw new AccessDeniedException("文档不存在或无权访问");
        }
        return document;
    }

    private SysDocument requiredOwner(Long id, Long userId) {
        SysDocument document = baseMapper.selectById(id);
        if (document == null || !Objects.equals(document.getOwnerId(), userId)) {
            throw new AccessDeniedException("只有文档所有者可以执行该操作");
        }
        return document;
    }

    private String permissionOf(SysDocument document, Long userId) {
        if (Objects.equals(document.getOwnerId(), userId)) {
            return "WRITE";
        }
        SysDocumentMember member = memberMapper.selectOne(new LambdaQueryWrapper<SysDocumentMember>()
            .eq(SysDocumentMember::getDocumentId, document.getId())
            .eq(SysDocumentMember::getUserId, userId));
        return member == null ? null : member.getPermission();
    }

    private SysDocumentVO toVO(SysDocument document, String permission) {
        SysDocumentVO vo = new SysDocumentVO();
        BeanUtils.copyProperties(document, vo);
        vo.setPermission(permission);
        return vo;
    }

    private String containerName(String targetType) {
        if ("element".equals(targetType)) {
            return "elements";
        }
        if ("relation".equals(targetType)) {
            return "relations";
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "操作目标类型错误");
    }
}

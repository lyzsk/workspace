package cn.sichu.tool.document.service.impl;

import cn.sichu.tool.document.entity.SysDocument;
import cn.sichu.tool.document.entity.SysDocumentMember;
import cn.sichu.tool.document.entity.SysDocumentOperation;
import cn.sichu.tool.document.mapper.SysDocumentMapper;
import cn.sichu.tool.document.mapper.SysDocumentMemberMapper;
import cn.sichu.tool.document.mapper.SysDocumentOperationMapper;
import cn.sichu.tool.document.model.form.SysDocumentOperationForm;
import cn.sichu.tool.document.websocket.DocumentWebSocketPublisher;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isNull;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
class DocumentServiceImplTest {

    private SysDocumentMapper documentMapper;
    private SysDocumentMemberMapper memberMapper;
    private SysDocumentOperationMapper operationMapper;
    private DocumentWebSocketPublisher eventPublisher;
    private DocumentServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            SysDocument.class);
    }

    @BeforeEach
    void setUp() {
        documentMapper = mock(SysDocumentMapper.class);
        memberMapper = mock(SysDocumentMemberMapper.class);
        operationMapper = mock(SysDocumentOperationMapper.class);
        eventPublisher = mock(DocumentWebSocketPublisher.class);
        service = new DocumentServiceImpl(memberMapper, operationMapper, mock(SysUserMapper.class),
            objectMapper, eventPublisher);
        ReflectionTestUtils.setField(service, "baseMapper", documentMapper);
    }

    @Test
    void mergesDifferentElementsAndRecordsOperation() {
        SysDocument document = document();
        when(documentMapper.selectById(1L)).thenReturn(document);
        when(documentMapper.update(any(), any())).thenReturn(1);
        SysDocumentOperationForm form = new SysDocumentOperationForm();
        form.setOpType("UPSERT");
        form.setTargetType("element");
        form.setTargetId("B");
        form.setPayload(objectMapper.createObjectNode().put("id", "B").put("x", 200));

        long revision = service.applyOperation(1L, 1L, form);

        assertEquals(1L, revision);
        ArgumentCaptor<SysDocumentOperation> operation = ArgumentCaptor.forClass(
            SysDocumentOperation.class);
        verify(operationMapper).insert(operation.capture());
        assertEquals("B", operation.getValue().getTargetId());
        assertEquals(1L, operation.getValue().getRevision());
        verify(eventPublisher).publishAfterCommit(anyLong(), anyString(), any());
        assertTrue(operation.getValue().getPayload().contains("\"x\":200"));
        ArgumentCaptor<LambdaUpdateWrapper> update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(documentMapper).update(isNull(), update.capture());
        assertTrue(update.getValue().getParamNameValuePairs().toString().contains("A"));
        assertTrue(update.getValue().getParamNameValuePairs().toString().contains("B"));
    }

    @Test
    void rejectsReadOnlyMemberWrite() {
        SysDocument document = document();
        document.setOwnerId(2L);
        SysDocumentMember member = new SysDocumentMember();
        member.setPermission("READ");
        when(documentMapper.selectById(1L)).thenReturn(document);
        when(memberMapper.selectOne(any())).thenReturn(member);
        SysDocumentOperationForm form = new SysDocumentOperationForm();
        form.setOpType("DELETE");
        form.setTargetType("element");
        form.setTargetId("A");

        assertThrows(AccessDeniedException.class,
            () -> service.applyOperation(1L, 1L, form));
    }

    private SysDocument document() {
        SysDocument document = new SysDocument();
        document.setId(1L);
        document.setTenantId(0L);
        document.setOwnerId(1L);
        document.setRevision(0L);
        document.setContent("{\"schemaVersion\":1,\"elements\":{\"A\":{\"id\":\"A\",\"x\":100}},\"relations\":{},\"appState\":{}}");
        return document;
    }
}

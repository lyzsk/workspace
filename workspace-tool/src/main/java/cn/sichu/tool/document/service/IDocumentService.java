package cn.sichu.tool.document.service;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.tool.document.model.form.SysDocumentForm;
import cn.sichu.tool.document.model.form.SysDocumentMemberForm;
import cn.sichu.tool.document.model.form.SysDocumentOperationForm;
import cn.sichu.tool.document.model.query.SysDocumentQuery;
import cn.sichu.tool.document.model.vo.SysDocumentMemberVO;
import cn.sichu.tool.document.model.vo.SysDocumentVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public interface IDocumentService {

    PageResult<SysDocumentVO> page(SysDocumentQuery query);

    Long create(SysDocumentForm form);

    SysDocumentVO get(Long id);

    void rename(Long id, SysDocumentForm form);

    void delete(Long id);

    List<SysDocumentMemberVO> members(Long id);

    void share(Long id, SysDocumentMemberForm form);

    void removeMember(Long id, Long userId);

    long applyOperation(Long documentId, Long userId, SysDocumentOperationForm form);

    void assertPermission(Long documentId, Long userId, String permission);
}

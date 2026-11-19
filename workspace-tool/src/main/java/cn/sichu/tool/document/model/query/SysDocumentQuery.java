package cn.sichu.tool.document.model.query;

import cn.sichu.common.core.base.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDocumentQuery extends BaseQuery {

    private String title;

    private String docType;
}

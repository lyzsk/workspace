package cn.sichu.common.core.base;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class BaseQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String sortBy;

    private String order;

    public boolean isPaged() {
        return pageNum != null && pageSize != null && pageSize > 0;
    }
}

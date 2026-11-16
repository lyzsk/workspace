package cn.sichu.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records;

    private long total;

    private long pageNum;

    private long pageSize;

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}

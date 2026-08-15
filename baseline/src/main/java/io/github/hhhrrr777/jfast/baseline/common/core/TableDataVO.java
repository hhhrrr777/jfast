package io.github.hhhrrr777.jfast.baseline.common.core;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体。rows/total 两段式,与前端表格契约对应。
 */
public class TableDataVO<T> implements Serializable {

    private long total;
    private List<T> rows;

    public TableDataVO() {
    }

    public TableDataVO(long total, List<T> rows) {
        this.total = total;
        this.rows = rows;
    }

    public static <T> TableDataVO<T> of(long total, List<T> rows) {
        return new TableDataVO<>(total, rows);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }
}

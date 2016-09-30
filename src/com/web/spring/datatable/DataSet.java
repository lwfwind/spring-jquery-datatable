package com.web.spring.datatable;

import java.util.List;

/**
 * Wrapping bean that must be sent back to Datatables when server-side
 * processing is enabled.
 */
public final class DataSet<T> {

    private final List<T> rows;
    private final Long totalDisplayRecords;
    private final Long totalRecords;

    public DataSet(List<T> rows, Long totalRecords, Long totalDisplayRecords) {
        this.rows = rows;
        this.totalRecords = totalRecords;
        this.totalDisplayRecords = totalDisplayRecords;
    }

    public List<T> getRows() {
        return rows;
    }

    public Long getTotalDisplayRecords() {
        return totalDisplayRecords;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }
}

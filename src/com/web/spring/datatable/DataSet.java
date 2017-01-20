package com.web.spring.datatable;

import java.util.List;

/**
 * Wrapping bean that must be sent back to Datatables when server-side
 * processing is enabled.
 *
 * @param <T> the type parameter
 */
public final class DataSet<T> {

    private final List<T> rows;
    private final Long totalDisplayRecords;
    private final Long totalRecords;

    /**
     * Instantiates a new Data set.
     *
     * @param rows                the rows
     * @param totalRecords        the total records
     * @param totalDisplayRecords the total display records
     */
    public DataSet(List<T> rows, Long totalRecords, Long totalDisplayRecords) {
        this.rows = rows;
        this.totalRecords = totalRecords;
        this.totalDisplayRecords = totalDisplayRecords;
    }

    /**
     * Gets rows.
     *
     * @return the rows
     */
    public List<T> getRows() {
        return rows;
    }

    /**
     * Gets total display records.
     *
     * @return the total display records
     */
    public Long getTotalDisplayRecords() {
        return totalDisplayRecords;
    }

    /**
     * Gets total records.
     *
     * @return the total records
     */
    public Long getTotalRecords() {
        return totalRecords;
    }
}

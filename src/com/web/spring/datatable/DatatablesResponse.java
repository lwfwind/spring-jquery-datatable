package com.web.spring.datatable;

import java.util.List;

/**
 * Wrapper object the response that must be sent back to Datatables to update
 * the table when server-side processing is enabled.
 * Since Datatables only support JSON at the moment, this bean must be converted
 * to JSON by the server.
 *
 * @param <T> the type parameter
 */
public class DatatablesResponse<T> {

    private final List<T> data;
    private final Long recordsTotal;
    private final Long recordsFiltered;
    private final Integer draw;

    private DatatablesResponse(DataSet<T> dataSet, DatatablesCriterias criterias) {
        this.data = dataSet.getRows();
        this.recordsTotal = dataSet.getTotalRecords();
        this.recordsFiltered = dataSet.getTotalDisplayRecords();
        this.draw = criterias.getDraw();
    }

    /**
     * Build datatables response.
     *
     * @param <T>       the type parameter
     * @param dataSet   the data set
     * @param criterias the criterias
     * @return the datatables response
     */
    public static <T> DatatablesResponse<T> build(DataSet<T> dataSet, DatatablesCriterias criterias) {
        return new DatatablesResponse<T>(dataSet, criterias);
    }

    /**
     * Gets data.
     *
     * @return the data
     */
    public List<T> getData() {
        return data;
    }

    /**
     * Gets records total.
     *
     * @return the records total
     */
    public Long getRecordsTotal() {
        return recordsTotal;
    }

    /**
     * Gets records filtered.
     *
     * @return the records filtered
     */
    public Long getRecordsFiltered() {
        return recordsFiltered;
    }

    /**
     * Gets draw.
     *
     * @return the draw
     */
    public Integer getDraw() {
        return draw;
    }
}
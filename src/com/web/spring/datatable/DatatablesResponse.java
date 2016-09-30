package com.web.spring.datatable;

import java.util.List;

/**
 * Wrapper object the response that must be sent back to Datatables to update
 * the table when server-side processing is enabled.
 * Since Datatables only support JSON at the moment, this bean must be converted
 * to JSON by the server.
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

    public static <T> DatatablesResponse<T> build(DataSet<T> dataSet, DatatablesCriterias criterias) {
        return new DatatablesResponse<T>(dataSet, criterias);
    }

    public List<T> getData() {
        return data;
    }

    public Long getRecordsTotal() {
        return recordsTotal;
    }

    public Long getRecordsFiltered() {
        return recordsFiltered;
    }

    public Integer getDraw() {
        return draw;
    }
}
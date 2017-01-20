package com.web.spring.datatable.export;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * The type Xlsx streaming view.
 */
public class XlsxStreamingView extends AbstractXlsxStreamingView {

    @Override
    protected void buildExcelDocument(
            Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String fileName = (String) model.get("fileName");
        response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\".xlsx");
        ExcelCommon.buildExcelDocument(model, workbook, request, response);
    }

}

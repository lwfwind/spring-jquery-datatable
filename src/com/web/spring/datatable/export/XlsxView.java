package com.web.spring.datatable.export;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;

/**
 * The type Xlsx view.
 */
public class XlsxView extends AbstractXlsxView {

    @Override
    protected void buildExcelDocument(
            Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String fileName = (String) model.get("fileName");
        response.setHeader("Content-disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
        ExcelCommon.buildExcelDocument(model, workbook, request, response);
    }

}

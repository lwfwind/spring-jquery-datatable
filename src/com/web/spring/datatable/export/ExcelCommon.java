package com.web.spring.datatable.export;

import com.library.common.ReflectHelper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The type Excel common.
 */
public class ExcelCommon {
    private static final int TITLE_START_POSITION = 0;

    private static final int DATEHEAD_START_POSITION = 1;

    private static final int HEAD_START_POSITION = 2;

    private static final int CONTENT_START_POSITION = 3;

    private static void createTitleRow(Sheet sheet, String sheetName, Map<String, String> headerMap) {
        CellRangeAddress titleRange = new CellRangeAddress(0, 0, 0, headerMap.size() - 1);
        sheet.addMergedRegion(titleRange);
        Row titleRow = sheet.createRow(TITLE_START_POSITION);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(sheetName);
    }

    private static void createDateHeadRow(Sheet sheet, Map<String, String> headerMap) {
        CellRangeAddress dateRange = new CellRangeAddress(1, 1, 0, headerMap.size() - 1);
        sheet.addMergedRegion(dateRange);
        Row dateRow = sheet.createRow(DATEHEAD_START_POSITION);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("当前时间为 " + new SimpleDateFormat("yyyy年MM月dd日").format(new Date()));
    }

    private static void createHeadRow(Sheet sheet, Map<String, String> headerMap) {
        Row headRow = sheet.createRow(HEAD_START_POSITION);
        int i = 0;
        for (String entry : headerMap.keySet()) {
            Cell headCell = headRow.createCell(i);
            headCell.setCellValue(headerMap.get(entry));
            i++;
        }
    }

    private static void createContentRow(Sheet sheet, List<?> dataList, Map<String, String> headerMap) {
        try {
            int i = 0;
            for (Object obj : dataList) {
                Row textRow = sheet.createRow(CONTENT_START_POSITION + i);
                int j = 0;
                for (String entry : headerMap.keySet()) {
                    String value = (String) ReflectHelper.getMethod(obj, entry);
                    Cell textcell = textRow.createCell(j);
                    textcell.setCellValue(value);
                    j++;
                }
                i++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Build excel document.
     *
     * @param model    the model
     * @param workbook the workbook
     * @param request  the request
     * @param response the response
     */
    public static void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) {
        String sheetName = (String) model.get("sheetName");
        @SuppressWarnings("unchecked")
        Map<String, String> headerMap = (Map<String, String>) model.get("header");
        List<?> dataList = (List<?>) model.get("data");
        Sheet sheet = workbook.createSheet(sheetName);
        createTitleRow(sheet, sheetName, headerMap);
        createDateHeadRow(sheet, headerMap);
        createHeadRow(sheet, headerMap);
        createContentRow(sheet, dataList, headerMap);
    }
}

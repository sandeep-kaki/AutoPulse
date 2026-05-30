package com.autopulse.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelReader - Reads test data from Excel files.
 *
 * Uses Apache POI library (already in your pom.xml).
 *
 * HOW APACHE POI MODELS EXCEL:
 * Workbook → the entire .xlsx file
 * Sheet    → one tab in the workbook
 * Row      → one horizontal row
 * Cell     → one individual cell
 *
 * We read row 1 as headers, then rows 2+ as data.
 * Returns List of Maps — each Map is one row of data
 * where key=column header, value=cell value.
 *
 * Example result:
 * [
 *   {"email": "test@gmail.com", "password": "pass123",
 *    "expectedResult": "success"},
 *   {"email": "wrong@email.com", "password": "wrong",
 *    "expectedResult": "failure"}
 * ]
 */
public class ExcelReader {

    /**
     * getTestData() - Reads all rows from an Excel sheet.
     *
     * @param filePath  - path to the .xlsx file
     * @param sheetName - which sheet/tab to read
     * @return          - List of Maps, one Map per data row
     */
    public static List<Map<String, String>> getTestData(
            String filePath, String sheetName) {

        List<Map<String, String>> testData = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                throw new RuntimeException(
                        "Sheet '" + sheetName + "' not found in "
                                + filePath
                );
            }

            // Row 0 = header row (column names)
            Row headerRow = sheet.getRow(0);
            int columnCount = headerRow.getLastCellNum();

            // Read from row 1 onwards (skip header)
            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row dataRow = sheet.getRow(rowIndex);

                // Skip completely empty rows
                if (dataRow == null) continue;

                Map<String, String> rowData = new HashMap<>();

                for (int colIndex = 0;
                     colIndex < columnCount;
                     colIndex++) {

                    // Get header name for this column
                    String header = headerRow
                            .getCell(colIndex)
                            .getStringCellValue()
                            .trim();

                    // Get cell value (handle different cell types)
                    String value = getCellValue(
                            dataRow.getCell(colIndex)
                    );

                    rowData.put(header, value);
                }

                testData.add(rowData);
            }

            workbook.close();
            fis.close();

            System.out.println("📊 Excel loaded: " + testData.size()
                    + " rows from sheet '" + sheetName + "'");

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read Excel file: " + filePath, e
            );
        }

        return testData;
    }

    /**
     * getCellValue() - Handles different Excel cell types.
     *
     * Excel cells can be STRING, NUMERIC, BOOLEAN, FORMULA,
     * or BLANK. If you always read as String without checking
     * — numbers come back as "1234.0" instead of "1234".
     * This method handles each type correctly.
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                // Avoid "password123" becoming "123.0"
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                return cell.getCellFormula();

            case BLANK:
            default:
                return "";
        }
    }

    /**
     * getTestDataAs2DArray() - Returns data as Object[][]
     * for use with TestNG @DataProvider directly.
     *
     * TestNG's @DataProvider expects Object[][] format.
     * This converts our List<Map> into that format.
     */
    public static Object[][] getTestDataAs2DArray(
            String filePath, String sheetName) {

        List<Map<String, String>> data =
                getTestData(filePath, sheetName);

        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        return result;
    }
}
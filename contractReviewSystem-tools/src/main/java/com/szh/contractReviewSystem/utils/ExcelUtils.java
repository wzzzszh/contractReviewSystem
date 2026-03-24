package com.szh.parseModule.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel宸ュ叿绫� */
public class ExcelUtils {
    
    /**
     * 璇诲彇Excel鏂囦欢
     */
    public static List<List<String>> readExcel(String filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath);
             Workbook workbook = getWorkbook(inputStream, filePath)) {
            return readWorkbook(workbook);
        }
    }
    
    /**
     * 璇诲彇Excel鏂囦欢
     */
    public static List<List<String>> readExcel(InputStream inputStream, String fileName) throws IOException {
        try (Workbook workbook = getWorkbook(inputStream, fileName)) {
            return readWorkbook(workbook);
        }
    }
    
    /**
     * 鑾峰彇Workbook
     */
    private static Workbook getWorkbook(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new IllegalArgumentException("涓嶆敮鎸佺殑鏂囦欢鏍煎紡");
        }
    }
    
    /**
     * 璇诲彇Workbook
     */
    private static List<List<String>> readWorkbook(Workbook workbook) {
        List<List<String>> result = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(0);
        int rowCount = sheet.getPhysicalNumberOfRows();
        
        for (int i = 0; i < rowCount; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            
            List<String> rowData = new ArrayList<>();
            int cellCount = row.getPhysicalNumberOfCells();
            
            for (int j = 0; j < cellCount; j++) {
                Cell cell = row.getCell(j);
                rowData.add(getCellValue(cell));
            }
            
            result.add(rowData);
        }
        
        return result;
    }
    
    /**
     * 鑾峰彇鍗曞厓鏍煎��
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    /**
     * 鍐欏叆Excel鏂囦欢
     */
    public static void writeExcel(String filePath, List<List<String>> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = new FileOutputStream(filePath)) {
            writeWorkbook(workbook, data);
            workbook.write(outputStream);
        }
    }
    
    /**
     * 鍐欏叆Workbook
     */
    private static void writeWorkbook(Workbook workbook, List<List<String>> data) {
        Sheet sheet = workbook.createSheet("Sheet1");
        
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i);
            List<String> rowData = data.get(i);
            
            for (int j = 0; j < rowData.size(); j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(rowData.get(j));
            }
        }
    }
}
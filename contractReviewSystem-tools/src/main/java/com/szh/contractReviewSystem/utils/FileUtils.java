package com.szh.contractReviewSystem.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 文件工具类
 */
public class FileUtils {
    
    /**
     * 默认上传路径
     */
    private static final String DEFAULT_UPLOAD_PATH = "uploads";
    
    /**
     * 允许的文件扩展名
     */
    private static final String[] ALLOWED_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "bmp", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar"
    };
    
    /**
     * 上传文件
     *
     * @param file 文件
     * @param uploadPath 上传路径
     * @return 文件路径
     */
    public static String uploadFile(MultipartFile file, String uploadPath) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (!isValidExtension(originalFilename)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        
        // 生成唯一文件名
        String fileName = generateUniqueFileName(originalFilename);
        
        // 构建保存路径
        String savePath = buildSavePath(uploadPath, fileName);
        
        // 创建目录
        File destDir = new File(savePath).getParentFile();
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        // 保存文件
        File destFile = new File(savePath);
        file.transferTo(destFile);
        
        return savePath;
    }
    
    /**
     * 上传文件（使用默认路径）
     *
     * @param file 文件
     * @return 文件路径
     */
    public static String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, DEFAULT_UPLOAD_PATH);
    }
    
    /**
     * 检查文件扩展名是否有效
     *
     * @param filename 文件名
     * @return 是否有效
     */
    public static boolean isValidExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 生成唯一文件名
     *
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    public static String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return timestamp + "_" + uuid + "." + extension;
    }
    
    /**
     * 构建保存路径
     *
     * @param uploadPath 上传路径
     * @param fileName 文件名
     * @return 完整路径
     */
    public static String buildSavePath(String uploadPath, String fileName) {
        if (uploadPath == null || uploadPath.isEmpty()) {
            uploadPath = DEFAULT_UPLOAD_PATH;
        }
        
        // 确保路径以分隔符结尾
        if (!uploadPath.endsWith(File.separator)) {
            uploadPath += File.separator;
        }
        
        return uploadPath + fileName;
    }
    
    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }
    
    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }
    
    /**
     * 写入字符串到文件
     *
     * @param filePath 文件路径
     * @param content 内容
     * @throws IOException IO异常
     */
    public static void writeFile(String filePath, String content) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
    
    /**
     * 写入字节数组到文件
     *
     * @param filePath 文件路径
     * @param bytes 字节数组
     * @throws IOException IO异常
     */
    public static void writeFile(String filePath, byte[] bytes) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
    }
    
    /**
     * 读取文件内容为字符串
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    public static String readFileToString(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
    
    /**
     * 读取文件内容为字节数组
     *
     * @param filePath 文件路径
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] readFileToBytes(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }
}
package com.szh.parseModule.controller;

import com.szh.parseModule.common.Result;
import com.szh.parseModule.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

/**
 * 文件控制器 */
@RestController
@RequestMapping("/api/file")
public class FileController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    /**
     * 上传文件
     *
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return error("文件不能为空");
            }
            
            // 上传文件
            String filePath = FileUtils.uploadFile(file);
            return success("文件上传成功", filePath);
        } catch (Exception e) {
            return error("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     *
     * @param filePath 文件路径
     * @param response 响应对象
     */
    @GetMapping("/download")
    public void downloadFile(@RequestParam("path") String filePath, HttpServletResponse response) {
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }
            
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + 
                              URLEncoder.encode(file.getName(), "UTF-8"));
            response.setContentLength((int) file.length());
            
            // 写入响应流
            fis = new FileInputStream(file);
            os = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            logger.error("文件下载失败", e);
            try {
                // 检查响应是否已经提交
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件下载失败");
                }
            } catch (IOException ioException) {
                logger.error("发送错误响应失败", ioException);
            }
        } finally {
            // 关闭流
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("关闭文件输入流失败", e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.error("关闭输出流失败", e);
                }
            }
        }
    }
    
    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Result<String> deleteFile(@RequestParam("path") String filePath) {
        boolean result = FileUtils.deleteFile(filePath);
        if (result) {
            return success("文件删除成功");
        } else {
            return error("文件删除失败");
        }
    }
}
package com.szh.fileconvert;

import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;
import com.szh.fileconvert.pdf.AiPdfParser;
import com.szh.fileconvert.pdf.ArkLLMService;
import com.szh.fileconvert.pdf.LLMService;

import java.io.File;

/**
 * AiPdfParser性能测试 - 100次循环测试
 */
public class AiPdfParserPerformanceTest {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建LLM服务实例（使用火山引擎Ark API）
        LLMService llmService = new ArkLLMService(
            "fdd9c300-621c-42e8-a6c6-c946bb956006",  // API Key
            "doubao-seed-2-0-pro-260215"   // 使用的模型
        );
        
        // 2. 创建AI PDF解析器
        AiPdfParser parser = new AiPdfParser(llmService);
        
        // 3. 准备测试文件
        File pdfFile = new File("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\北京市朝阳区住宅租赁合同（个人出租）.pdf");
        
        // 4. 创建解析上下文
        ParseContext context = new ParseContext();
        context.setContractMode(false); // 设置为普通文档模式
        
        // 5. 性能测试参数
        int iterations = 100;
        long totalTime = 0;
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("开始性能测试：共 " + iterations + " 次循环");
        System.out.println("测试文件：" + pdfFile.getAbsolutePath());
        System.out.println("==========================================");
        
        // 6. 执行100次循环测试
        for (int i = 1; i <= iterations; i++) {
            long startTime = System.currentTimeMillis();
            
            try {
                if (parser.supports("pdf")) {
                    MarkdownResult result = parser.parse(pdfFile, context);
                    
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    totalTime += duration;
                    successCount++;
                    
                    System.out.println("第 " + i + " 次测试 - 成功 - 耗时：" + duration + "ms");
                } else {
                    failCount++;
                    System.out.println("第 " + i + " 次测试 - 失败 - 不支持PDF格式");
                }
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                totalTime += duration;
                failCount++;
                
                System.out.println("第 " + i + " 次测试 - 异常 - 耗时：" + duration + "ms");
                System.out.println("异常信息：" + e.getMessage());
            }
        }
        
        // 7. 统计结果
        System.out.println("\n==========================================");
        System.out.println("性能测试结果:");
        System.out.println("总测试次数：" + iterations);
        System.out.println("成功次数：" + successCount);
        System.out.println("失败次数：" + failCount);
        System.out.println("总耗时：" + totalTime + "ms");
        
        if (successCount > 0) {
            double averageTime = (double) totalTime / successCount;
            System.out.println("平均耗时：" + String.format("%.2f", averageTime) + "ms");
        }
        
        System.out.println("==========================================");
    }
}
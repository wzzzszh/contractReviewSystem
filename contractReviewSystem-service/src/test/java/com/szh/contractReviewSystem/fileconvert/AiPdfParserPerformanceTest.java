package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;
import com.szh.contractReviewSystem.fileconvert.pdf.AiPdfParser;
import com.szh.contractReviewSystem.fileconvert.pdf.ArkConfigLoader;
import com.szh.contractReviewSystem.fileconvert.pdf.ArkLLMService;
import com.szh.contractReviewSystem.fileconvert.pdf.LLMService;

import java.io.File;

public class AiPdfParserPerformanceTest {
    
    public static void main(String[] args) throws Exception {
        LLMService llmService = new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel());
        
        AiPdfParser parser = new AiPdfParser(llmService);
        
        File pdfFile = new File("src/test/resources/北京市朝阳区住宅租赁合同（个人出租）.pdf");
        
        ParseContext context = new ParseContext();
        context.setContractMode(false);
        
        int iterations = 100;
        long totalTime = 0;
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("开始性能测试：共 " + iterations + " 次循环");
        System.out.println("测试文件：" + pdfFile.getAbsolutePath());
        System.out.println("==========================================");
        
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

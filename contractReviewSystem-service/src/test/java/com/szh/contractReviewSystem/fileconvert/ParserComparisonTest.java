package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;
import com.szh.contractReviewSystem.fileconvert.pdf.AiPdfParser;
import com.szh.contractReviewSystem.fileconvert.pdf.AiPdfParser2;
import com.szh.contractReviewSystem.fileconvert.pdf.ArkConfigLoader;
import com.szh.contractReviewSystem.fileconvert.pdf.ArkLLMService;
import com.szh.contractReviewSystem.fileconvert.pdf.LLMService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class ParserComparisonTest {
    
    public static void main(String[] args) throws Exception {
        LLMService llmService = new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel());
        
        File pdfFile = new File("D:\\JavaExercise\\SLYT\\contractReviewSystem\\contractReviewSystem-service\\src\\test\\resources\\北京市朝阳区住宅租赁合同（个人出租）.pdf");
        ParseContext context = new ParseContext();
        context.setContractMode(true);
        
        System.out.println("========================================");
        System.out.println("PDF解析器性能对比测试");
        System.out.println("========================================");
        System.out.println("测试文件: " + pdfFile.getName());
        System.out.println();
        
        System.out.println("----------------------------------------");
        System.out.println("【测试1】AiPdfParser2 (原始版 - 直接喂给AI)");
        System.out.println("----------------------------------------");
        AiPdfParser2 parser2 = new AiPdfParser2(llmService);
        long start2 = System.currentTimeMillis();
        MarkdownResult result2 = parser2.parse(pdfFile, context);
        long end2 = System.currentTimeMillis();
        System.out.println("耗时: " + (end2 - start2) + "ms");
        System.out.println("结果长度: " + result2.getMarkdown().length() + " 字符");
        FileUtils.writeStringToFile(new File("target/parser2_result.md"), result2.getMarkdown(), StandardCharsets.UTF_8);
        System.out.println("已保存到: target/parser2_result.md");
        System.out.println();
        
        System.out.println("----------------------------------------");
        System.out.println("【测试2】AiPdfParser (优化版 - 预处理+并行)");
        System.out.println("----------------------------------------");
        AiPdfParser parser1 = new AiPdfParser(llmService);
        parser1.setParallelThreads(4);
        long start1 = System.currentTimeMillis();
        MarkdownResult result1 = parser1.parse(pdfFile, context);
        long end1 = System.currentTimeMillis();
        System.out.println("耗时: " + (end1 - start1) + "ms");
        System.out.println("结果长度: " + result1.getMarkdown().length() + " 字符");
        FileUtils.writeStringToFile(new File("target/parser1_result.md"), result1.getMarkdown(), StandardCharsets.UTF_8);
        System.out.println("已保存到: target/parser1_result.md");
        System.out.println();
        
        System.out.println("========================================");
        System.out.println("对比结果");
        System.out.println("========================================");
        System.out.printf("AiPdfParser2 (原始版): %dms\n", (end2 - start2));
        System.out.printf("AiPdfParser  (优化版): %dms\n", (end1 - start1));
        long saved = (end2 - start2) - (end1 - start1);
        double percent = (double) saved / (end2 - start2) * 100;
        System.out.printf("节省时间: %dms (%.1f%%)\n", saved, percent);
        System.out.println();
        System.out.println("结果文件:");
        System.out.println("  - target/parser2_result.md (原始版)");
        System.out.println("  - target/parser1_result.md (优化版)");
        System.out.println("========================================");
    }
}

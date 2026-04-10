package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;
import com.szh.contractReviewSystem.fileconvert.pdf.AiPdfParser;
import com.szh.contractReviewSystem.fileconvert.pdf.ArkConfigLoader;
import com.szh.contractReviewSystem.fileconvert.pdf.ArkLLMService;
import com.szh.contractReviewSystem.fileconvert.pdf.LLMService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class AiPdfParserExample2 {
    
    public static void main(String[] args) throws Exception {
        LLMService llmService = new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel());
        
        AiPdfParser parser = new AiPdfParser(llmService);
        
        File pdfFile = new File("src/test/resources/北京市朝阳区住宅租赁合同（个人出租）.pdf");
        
        ParseContext context = new ParseContext();
        context.setContractMode(true);
        long startTime = System.currentTimeMillis();
        
        if (parser.supports("pdf")) {
            MarkdownResult result = parser.parse(pdfFile, context);
            long endTime = System.currentTimeMillis();
            System.out.println("转换耗时：" + (endTime - startTime) + "ms");

            System.out.println("=== 转换后的Markdown ===");
            System.out.println(result.getMarkdown());

            File outputFile = new File("target/contractM2.md");
            FileUtils.writeStringToFile(outputFile, result.getMarkdown(), StandardCharsets.UTF_8);
            System.out.println("\n已保存到：" + outputFile.getAbsolutePath());
        }
    }
}

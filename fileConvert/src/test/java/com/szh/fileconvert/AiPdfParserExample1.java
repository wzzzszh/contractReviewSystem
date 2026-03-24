package com.szh.fileconvert;

import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;
import com.szh.fileconvert.pdf.AiPdfParser;
import com.szh.fileconvert.pdf.ArkLLMService;
import com.szh.fileconvert.pdf.LLMService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * AiPdfParser使用示例（火山引擎Ark API）
 */
public class AiPdfParserExample1 {
    
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
        
        // 5. 执行解析
        if (parser.supports("pdf")) {
            MarkdownResult result = parser.parse(pdfFile, context);
            
            // 6. 输出结果
            System.out.println("=== 转换后的Markdown ===");
            System.out.println(result.getMarkdown());
            
            // 7. 保存到文件
            File outputFile = new File("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\contractM1.md");
            FileUtils.writeStringToFile(outputFile, result.getMarkdown(), StandardCharsets.UTF_8);
            System.out.println("\n已保存到：" + outputFile.getAbsolutePath());
        }
    }
}
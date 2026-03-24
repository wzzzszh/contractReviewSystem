package com.szh.fileconvert;

import com.szh.contractReviewSystem.utils.FileUtils;
import com.szh.fileconvert.base.DocumentToMarkdownConverter;
import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;

import java.io.File;

/**
 * PDF解析测试1 - 合同审查法律意见书
 */
@SuppressWarnings("all")
public class pdfParseMdTest1 {
    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(new File("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\合同审查法律意见书.pdf"), context);

        System.out.println(result.getMarkdown());

        // 输出成文件
        FileUtils.writeFile("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\contract2.md", result.getMarkdown());
    }
}
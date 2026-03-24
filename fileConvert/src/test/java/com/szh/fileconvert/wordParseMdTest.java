package com.szh.fileconvert;

import com.szh.parseModule.utils.FileUtils;
import com.szh.fileconvert.base.DocumentToMarkdownConverter;
import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;

import java.io.File;

/**
 * Word解析测试 - 劳动合同
 */
public class wordParseMdTest {
    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(new File("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\劳动合同（word范本）.docx"), context);

        System.out.println(result.getMarkdown());

        // 输出成文件
        FileUtils.writeFile("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\contract2.md", result.getMarkdown());
    }
}
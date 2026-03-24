package com.szh.fileconvert;

import com.szh.parseModule.utils.FileUtils;
import com.szh.fileconvert.base.DocumentToMarkdownConverter;
import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;

import java.io.File;

/**
 * PDF解析测试2 - 北京市朝阳区住宅租赁合同
 */
@SuppressWarnings("all")
public class pdfParseMdTest2 {
    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(new File("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\北京市朝阳区住宅租赁合同（个人出租）.pdf"), context);

        System.out.println(result.getMarkdown());

        // 输出成文件
        FileUtils.writeFile("D:\\JavaExercise\\SLYT\\demo\\fileConvert\\src\\test\\java\\com\\szh\\fileconvert\\contract1.md", result.getMarkdown());
    }
}
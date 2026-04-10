package com.szh.contractReviewSystem.fileconvert.pdf;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfTextPreprocessor {
    
    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
        "第([一二三四五六七八九十百]+)[条章]\\s*(.*)"
    );
    
    private static final Map<String, Integer> CHINESE_NUM_MAP = new HashMap<>();
    
    static {
        CHINESE_NUM_MAP.put("一", 1);
        CHINESE_NUM_MAP.put("二", 2);
        CHINESE_NUM_MAP.put("三", 3);
        CHINESE_NUM_MAP.put("四", 4);
        CHINESE_NUM_MAP.put("五", 5);
        CHINESE_NUM_MAP.put("六", 6);
        CHINESE_NUM_MAP.put("七", 7);
        CHINESE_NUM_MAP.put("八", 8);
        CHINESE_NUM_MAP.put("九", 9);
        CHINESE_NUM_MAP.put("十", 10);
        CHINESE_NUM_MAP.put("十一", 11);
        CHINESE_NUM_MAP.put("十二", 12);
        CHINESE_NUM_MAP.put("十三", 13);
        CHINESE_NUM_MAP.put("十四", 14);
        CHINESE_NUM_MAP.put("十五", 15);
        CHINESE_NUM_MAP.put("十六", 16);
        CHINESE_NUM_MAP.put("十七", 17);
        CHINESE_NUM_MAP.put("十八", 18);
        CHINESE_NUM_MAP.put("十九", 19);
        CHINESE_NUM_MAP.put("二十", 20);
        CHINESE_NUM_MAP.put("二十一", 21);
        CHINESE_NUM_MAP.put("二十二", 22);
        CHINESE_NUM_MAP.put("二十三", 23);
        CHINESE_NUM_MAP.put("二十四", 24);
        CHINESE_NUM_MAP.put("二十五", 25);
        CHINESE_NUM_MAP.put("三十", 30);
        CHINESE_NUM_MAP.put("四十", 40);
        CHINESE_NUM_MAP.put("五十", 50);
        CHINESE_NUM_MAP.put("一百", 100);
    }
    
    public static PreprocessResult preprocess(String rawText) {
        List<Clause> clauses = extractAndSortClauses(rawText);
        
        StringBuilder cleanedText = new StringBuilder();
        List<TextBlock> blocks = new ArrayList<>();
        
        for (Clause clause : clauses) {
            String content = clause.content.trim();
            if (!content.isEmpty()) {
                cleanedText.append(content).append("\n\n");
                blocks.add(new TextBlock(content, "clause", clause.order));
            }
        }
        
        return new PreprocessResult(cleanedText.toString().trim(), blocks);
    }
    
    private static List<Clause> extractAndSortClauses(String rawText) {
        List<Clause> clauses = new ArrayList<>();
        StringBuilder headerContent = new StringBuilder();
        
        String[] lines = rawText.split("\n");
        StringBuilder currentClauseContent = new StringBuilder();
        int currentClauseOrder = 0;
        String currentClauseTitle = "";
        boolean foundFirstClause = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty() || isPageHeader(trimmed)) {
                continue;
            }
            
            Matcher matcher = CLAUSE_PATTERN.matcher(trimmed);
            
            if (matcher.find()) {
                if (foundFirstClause && currentClauseContent.length() > 0) {
                    clauses.add(new Clause(currentClauseOrder, currentClauseTitle, currentClauseContent.toString().trim()));
                }
                
                String chineseNum = matcher.group(1);
                currentClauseOrder = chineseToNumber(chineseNum);
                currentClauseTitle = "第" + chineseNum + "条" + (matcher.group(2).isEmpty() ? "" : " " + matcher.group(2));
                currentClauseContent = new StringBuilder(currentClauseTitle).append("\n");
                foundFirstClause = true;
            } else {
                if (foundFirstClause) {
                    currentClauseContent.append(cleanLine(trimmed)).append("\n");
                } else {
                    if (!isJunkLine(trimmed)) {
                        headerContent.append(cleanLine(trimmed)).append("\n");
                    }
                }
            }
        }
        
        if (currentClauseContent.length() > 0) {
            clauses.add(new Clause(currentClauseOrder, currentClauseTitle, currentClauseContent.toString().trim()));
        }
        
        clauses.sort(Comparator.comparingInt(c -> c.order));
        
        List<Clause> result = new ArrayList<>();
        
        if (headerContent.length() > 0) {
            result.add(new Clause(0, "合同基本信息", headerContent.toString().trim()));
        }
        
        result.addAll(clauses);
        
        return result;
    }
    
    private static boolean isPageHeader(String line) {
        return line.matches("^(第\\s*\\d+\\s*页|Page\\s*\\d+|\\d+\\s*/\\s*\\d+)$");
    }
    
    private static boolean isJunkLine(String line) {
        return line.matches("^\\d+\\.?\\s*$") ||
               line.matches("^[○○●]\\s*$") ||
               line.matches("^\\s*$");
    }
    
    private static String cleanLine(String line) {
        return line
            .replaceAll("[○○●]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    private static int chineseToNumber(String chinese) {
        if (CHINESE_NUM_MAP.containsKey(chinese)) {
            return CHINESE_NUM_MAP.get(chinese);
        }
        
        int result = 0;
        int i = 0;
        
        while (i < chinese.length()) {
            char c = chinese.charAt(i);
            
            if (c == '十') {
                if (result == 0) result = 10;
                else result *= 10;
            } else if (c == '百') {
                result *= 100;
            } else {
                String single = String.valueOf(c);
                if (CHINESE_NUM_MAP.containsKey(single)) {
                    int val = CHINESE_NUM_MAP.get(single);
                    if (i + 1 < chinese.length()) {
                        char next = chinese.charAt(i + 1);
                        if (next == '十') {
                            result += val * 10;
                            i++;
                        } else if (next == '百') {
                            result += val * 100;
                            i++;
                        } else {
                            result += val;
                        }
                    } else {
                        result += val;
                    }
                }
            }
            i++;
        }
        
        return result > 0 ? result : 999;
    }
    
    private static class Clause {
        int order;
        String title;
        String content;
        
        Clause(int order, String title, String content) {
            this.order = order;
            this.title = title;
            this.content = content;
        }
    }
    
    public static class TextBlock {
        public String content;
        public String type;
        public int level;
        
        public TextBlock(String content, String type, int level) {
            this.content = content;
            this.type = type;
            this.level = level;
        }
    }
    
    public static class PreprocessResult {
        public String cleanedText;
        public List<TextBlock> blocks;
        
        public PreprocessResult(String cleanedText, List<TextBlock> blocks) {
            this.cleanedText = cleanedText;
            this.blocks = blocks;
        }
    }
}

package com.moviereview.keyword;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TF-IDF 关键词提取器。
 *
 * spec：成本极低，让界面和论文都饱满。
 * 简化版：用词频 + 逆文档频率（此处简化为词频）。
 */
public class TfIdfKeywordExtractor implements KeywordExtractor {

    /** 停用词。 */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "他", "她", "它", "们", "那", "些", "什么", "可以", "这个", "那个",
            "电影", "影片", "觉得", "感觉", "真的", "还", "但", "而", "又"
    );

    @Override
    public List<Keyword> extract(List<String> texts, int topN) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts 不能为空");
        }
        if (topN <= 0) {
            throw new IllegalArgumentException("topN 必须为正");
        }

        // 分词（简单按 2-4 字窗口，实际项目应用 HanLP）
        Map<String, Integer> wordCount = new HashMap<>();
        for (String text : texts) {
            if (text == null || text.isBlank()) continue;
            String cleaned = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ");
            for (String token : cleaned.split("\\s+")) {
                if (token.length() >= 2 && token.length() <= 6 && !STOP_WORDS.contains(token)) {
                    wordCount.merge(token, 1, Integer::sum);
                }
            }
        }

        if (wordCount.isEmpty()) {
            return List.of();
        }

        // 计算权重（TF，此处简化）
        int maxCount = wordCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        return wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(e -> new Keyword(e.getKey(),
                        (double) e.getValue() / maxCount,
                        e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public String name() {
        return "tfidf";
    }
}

package com.moviereview.keyword;

import java.util.List;

/**
 * 关键词提取器接口。
 */
public interface KeywordExtractor {

    /**
     * 从文本列表中提取关键词。
     *
     * @param texts 文本列表
     * @param topN 返回前 N 个关键词
     * @return 关键词列表（按权重降序）
     */
    List<Keyword> extract(List<String> texts, int topN);

    /** 提取器名称。 */
    String name();
}

package com.moviereview.keyword;

import java.util.Map;

/**
 * 关键词条目。
 *
 * @param word 关键词
 * @param weight 权重（0-1）
 * @param count 出现次数
 */
public record Keyword(String word, double weight, int count) {
    public Keyword {
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException("word 不能为空");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count 不能为负");
        }
    }
}

package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentPolarity;

/**
 * 二分类标注样本（用于 θ 敏感性分析）。
 *
 * 包含 L2 模型输出的正/负概率 + 人工标注的金标准三分类。
 */
public record BinaryLabeledSample(
        String id,
        String text,
        double posProb,
        double negProb,
        SentimentPolarity golden
) {
    public BinaryLabeledSample {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id 不能为空");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text 不能为空");
        }
        if (posProb < 0 || posProb > 1 || negProb < 0 || negProb > 1) {
            throw new IllegalArgumentException("概率必须在 [0,1]");
        }
        if (golden == null) {
            throw new IllegalArgumentException("golden 不能为空");
        }
    }
}

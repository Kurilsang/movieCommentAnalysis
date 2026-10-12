package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentPolarity;

/**
 * 评测样本。
 *
 * 一条带标注的评论。goldenPolarity 是人工标注的金标准，analyzer 输出的是预测值。
 * 评测框架只关心"输入 text，断言 goldenPolarity"，不关心 analyzer 内部怎么算。
 */
public record EvalSample(
        String id,
        String text,
        SentimentPolarity goldenPolarity,
        String source
) {
    public EvalSample {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("样本 id 不能为空");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("样本 text 不能为空");
        }
        if (goldenPolarity == null) {
            throw new IllegalArgumentException("样本 goldenPolarity 不能为空");
        }
        if (source == null || source.isBlank()) {
            source = "unknown";
        }
    }
}

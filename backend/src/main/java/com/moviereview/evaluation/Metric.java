package com.moviereview.evaluation;

import java.util.Map;

/**
 * 评测指标。
 *
 * 包含 Accuracy、Precision、Recall、F1（macro）和混淆矩阵。
 * 论文实验章节的所有表格都由 {@link Evaluator} 产出的 Metric 计算得出。
 */
public record Metric(
        double accuracy,
        double macroPrecision,
        double macroRecall,
        double macroF1,
        Map<String, Integer> confusionMatrix,
        int sampleCount
) {
    public Metric {
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount 不能为负");
        }
    }

    /** 混淆矩阵的 key：actual-predicted，如 "POSITIVE-NEGATIVE"。 */
    public static String confusionKey(com.moviereview.sentiment.SentimentPolarity actual,
                                      com.moviereview.sentiment.SentimentPolarity predicted) {
        return actual + "-" + predicted;
    }
}

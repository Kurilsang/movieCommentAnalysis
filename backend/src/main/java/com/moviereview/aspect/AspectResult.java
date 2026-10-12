package com.moviereview.aspect;

import com.moviereview.sentiment.SentimentPolarity;

/**
 * 方面级情感分析结果。
 *
 * 单个方面维度的分析结果：是否提及、极性、置信度、触发词。
 */
public record AspectResult(
        String aspectName,
        boolean mentioned,
        SentimentPolarity polarity,
        double confidence,
        int sampleCount,
        java.util.List<String> triggerWords
) {
    public AspectResult {
        if (aspectName == null || aspectName.isBlank()) {
            throw new IllegalArgumentException("aspectName 不能为空");
        }
        if (polarity == null) {
            polarity = SentimentPolarity.NEUTRAL;
        }
        if (triggerWords == null) {
            triggerWords = java.util.List.of();
        }
        if (!mentioned) {
            polarity = SentimentPolarity.NEUTRAL;
            confidence = 0.0;
            sampleCount = 0;
            triggerWords = java.util.List.of();
        }
    }

    /** 未提及的方面。 */
    public static AspectResult unmentioned(String aspectName) {
        return new AspectResult(aspectName, false, SentimentPolarity.NEUTRAL, 0.0, 0, java.util.List.of());
    }

    /** 已提及的方面。 */
    public static AspectResult of(String aspectName, SentimentPolarity polarity,
                                  double confidence, int sampleCount, java.util.List<String> triggerWords) {
        return new AspectResult(aspectName, true, polarity, confidence, sampleCount, triggerWords);
    }
}

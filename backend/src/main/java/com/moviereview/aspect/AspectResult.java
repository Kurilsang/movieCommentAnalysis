package com.moviereview.aspect;

import com.moviereview.sentiment.SentimentPolarity;

/**
 * 方面级情感分析结果。
 *
 * 单个方面维度的分析结果：是否提及、极性、置信度。
 */
public record AspectResult(
        String aspectName,
        boolean mentioned,
        SentimentPolarity polarity,
        double confidence,
        int sampleCount
) {
    public AspectResult {
        if (aspectName == null || aspectName.isBlank()) {
            throw new IllegalArgumentException("aspectName 不能为空");
        }
        if (polarity == null) {
            polarity = SentimentPolarity.NEUTRAL;
        }
        if (!mentioned) {
            polarity = SentimentPolarity.NEUTRAL;
            confidence = 0.0;
            sampleCount = 0;
        }
    }

    /** 未提及的方面。 */
    public static AspectResult unmentioned(String aspectName) {
        return new AspectResult(aspectName, false, SentimentPolarity.NEUTRAL, 0.0, 0);
    }

    /** 已提及的方面。 */
    public static AspectResult of(String aspectName, SentimentPolarity polarity,
                                  double confidence, int sampleCount) {
        return new AspectResult(aspectName, true, polarity, confidence, sampleCount);
    }
}

package com.moviereview.sentiment;

/**
 * 情感分析结果。
 *
 * 字段设计遵循 spec：每个分析结果都带置信度 + 触发关键词 + 六维方面分数 + 各方面样本量。
 * rawJson 用于保存完整输出快照（答辩时可证明结果不是编的、换模型后可对比新旧结果）。
 */
public record AnalysisResult(
        SentimentPolarity polarity,
        double confidence,
        java.util.List<String> triggerWords,
        java.util.Map<String, AspectScore> aspects,
        String rawJson
) {
    /** 单个方面的情感评分。 */
    public record AspectScore(SentimentPolarity polarity, double confidence, int sampleCount) {}
}

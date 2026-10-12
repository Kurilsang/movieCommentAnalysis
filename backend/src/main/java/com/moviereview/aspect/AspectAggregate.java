package com.moviereview.aspect;

import com.moviereview.sentiment.SentimentPolarity;

/**
 * 电影级方面聚合结果。
 *
 * 贝叶斯平滑：小样本向 50 分收缩，避免只有 2 条评论的维度分数剧烈跳动。
 */
public record AspectAggregate(
        String aspectName,
        int mentionedCount,
        int positiveCount,
        int neutralCount,
        int negativeCount,
        double rawScore,
        double smoothedScore,
        boolean sufficientData
) {
    public AspectAggregate {
        if (aspectName == null || aspectName.isBlank()) {
            throw new IllegalArgumentException("aspectName 不能为空");
        }
    }

    /** 正面占比原始分数（0-100）。 */
    public static double rawScore(int positive, int total) {
        return total == 0 ? 0.0 : 100.0 * positive / total;
    }

    /**
     * 贝叶斯平滑分数。
     *
     * smoothed = (positive + priorMean * priorStrength) / (total + priorStrength) * 100
     * priorMean = 0.5（先验正面率 50%）
     * priorStrength = 10（先验样本量，等于"最低样本量"阈值）
     */
    public static double smoothedScore(int positive, int total) {
        double priorMean = 0.5;
        double priorStrength = 10.0;
        return (positive + priorMean * priorStrength) / (total + priorStrength) * 100.0;
    }

    /** 样本量是否足够（≥ 10 条）。 */
    public static boolean sufficientData(int total) {
        return total >= 10;
    }
}

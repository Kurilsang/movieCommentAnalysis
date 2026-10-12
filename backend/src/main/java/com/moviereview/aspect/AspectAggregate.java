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

    /** 先验样本量（贝叶斯平滑强度），也是"样本量是否足够"的阈值。 */
    private static final double PRIOR_STRENGTH = 10.0;

    /** 正面占比原始分数（0-100）。 */
    public static double rawScore(int positive, int total) {
        return total == 0 ? 0.0 : 100.0 * positive / total;
    }

    /**
     * 贝叶斯平滑分数。
     *
     * smoothed = (positive + priorMean * priorStrength) / (total + priorStrength) * 100
     * priorMean = 0.5（先验正面率 50%）
     */
    public static double smoothedScore(int positive, int total) {
        double priorMean = 0.5;
        return (positive + priorMean * PRIOR_STRENGTH) / (total + PRIOR_STRENGTH) * 100.0;
    }

    /** 样本量是否足够（≥ PRIOR_STRENGTH 条）。 */
    public static boolean sufficientData(int total) {
        return total >= PRIOR_STRENGTH;
    }
}

package com.moviereview.sentiment;

/**
 * 三分类派生器。
 *
 * spec 创新点 3：基于置信度间隔的中性类识别 + 阈值敏感性分析。
 *
 * 核心思想：L2 模型只输出正/负二分类，如何派生出中性？
 * 1. 置信度间隔法：|p_pos − p_neg| < θ 时判为中性
 * 2. 叠加规则信号（评论过短、无情感词、含中性标记词）作为辅助判据
 *
 * θ 通过验证集网格搜索确定，并输出 θ 敏感性曲线。
 */
public class ThreeClassDeriver {

    /** 中性阈值 θ。 */
    private final double theta;

    /** 短评论阈值（少于 N 个字符视为过短）。 */
    private static final int SHORT_TEXT_THRESHOLD = 5;

    /** 短评论时阈值放松倍率。 */
    private static final double SHORT_TEXT_RELAX = 1.5;

    /** 中性标记词时阈值放松倍率。 */
    private static final double NEUTRAL_MARKER_RELAX = 2.0;

    /** 中性标记词。 */
    private static final java.util.Set<String> NEUTRAL_MARKERS = java.util.Set.of(
            "还行", "一般", "凑合", "普通", "中规中矩", "过得去", "还可以", "没什么"
    );

    /**
     * @param theta 中性阈值（0.05–0.50）
     */
    public ThreeClassDeriver(double theta) {
        if (theta < 0.0 || theta > 1.0) {
            throw new IllegalArgumentException("theta 必须在 [0,1]");
        }
        this.theta = theta;
    }

    public double theta() {
        return theta;
    }

    /** 浮点比较容差。 */
    private static final double EPSILON = 1e-9;

    /**
     * 从二分类概率派生三分类（仅置信度间隔法，不叠加规则）。
     *
     * @param posProb 正面概率
     * @param negProb 负面概率
     * @return 三分类极性
     */
    public SentimentPolarity derive(double posProb, double negProb) {
        if (Math.abs(posProb - negProb) < theta - EPSILON) {
            return SentimentPolarity.NEUTRAL;
        }
        return posProb > negProb ? SentimentPolarity.POSITIVE : SentimentPolarity.NEGATIVE;
    }

    /**
     * 从二分类概率 + 文本派生三分类（置信度间隔法 + 规则信号）。
     *
     * @param posProb 正面概率
     * @param negProb 负面概率
     * @param text    原始文本（用于规则信号）
     * @return 三分类极性
     */
    public SentimentPolarity deriveWithRules(double posProb, double negProb, String text) {
        SentimentPolarity base = derive(posProb, negProb);

        if (text == null || text.isBlank()) {
            return base;
        }

        // 规则信号 1：评论过短 → 倾向中性
        if (text.trim().length() < SHORT_TEXT_THRESHOLD && base != SentimentPolarity.NEUTRAL) {
            if (Math.abs(posProb - negProb) < theta * SHORT_TEXT_RELAX) {
                return SentimentPolarity.NEUTRAL;
            }
        }

        // 规则信号 2：含中性标记词 → 倾向中性
        for (String marker : NEUTRAL_MARKERS) {
            if (text.contains(marker) && base != SentimentPolarity.NEUTRAL) {
                if (Math.abs(posProb - negProb) < theta * NEUTRAL_MARKER_RELAX) {
                    return SentimentPolarity.NEUTRAL;
                }
            }
        }

        return base;
    }

    /**
     * 计算置信度间隔。
     */
    public double confidenceGap(double posProb, double negProb) {
        return Math.abs(posProb - negProb);
    }
}

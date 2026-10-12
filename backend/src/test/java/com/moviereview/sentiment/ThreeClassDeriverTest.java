package com.moviereview.sentiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三分类派生器测试。
 *
 * spec 创新点 3：基于置信度间隔的中性类识别。
 */
class ThreeClassDeriverTest {

    @Test
    @DisplayName("derive - 概率差距 < θ 判为中性")
    void derive_smallGap_returnsNeutral() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        // |0.55 - 0.45| = 0.10 < 0.2 → NEUTRAL
        assertEquals(SentimentPolarity.NEUTRAL, deriver.derive(0.55, 0.45));
    }

    @Test
    @DisplayName("derive - 概率差距 ≥ θ 判为正面")
    void derive_largeGapPositive_returnsPositive() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        // |0.85 - 0.15| = 0.70 ≥ 0.2 → POSITIVE
        assertEquals(SentimentPolarity.POSITIVE, deriver.derive(0.85, 0.15));
    }

    @Test
    @DisplayName("derive - 概率差距 ≥ θ 判为负面")
    void derive_largeGapNegative_returnsNegative() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        // |0.15 - 0.85| = 0.70 ≥ 0.2 → NEGATIVE
        assertEquals(SentimentPolarity.NEGATIVE, deriver.derive(0.15, 0.85));
    }

    @Test
    @DisplayName("derive - 边界：差距恰好等于 θ 时判为非中性（严格小于）")
    void derive_exactBoundary() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        // |0.60 - 0.40| = 0.20 == theta → 不是 < theta，所以判为 POSITIVE
        assertEquals(SentimentPolarity.POSITIVE, deriver.derive(0.60, 0.40));
    }

    @Test
    @DisplayName("deriveWithRules - 短评论 + 中等差距 → 中性")
    void deriveWithRules_shortText_returnsNeutral() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        // "好" 1 个字符，|0.55 - 0.45| = 0.10 < 0.3（θ*1.5）→ NEUTRAL
        assertEquals(SentimentPolarity.NEUTRAL,
                deriver.deriveWithRules(0.55, 0.45, "好"));
    }

    @Test
    @DisplayName("deriveWithRules - 中性标记词 + 中等差距 → 中性")
    void deriveWithRules_neutralMarker_returnsNeutral() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        // "还行吧" 含 "还行"，|0.55 - 0.45| = 0.10 < 0.4（θ*2.0）→ NEUTRAL
        assertEquals(SentimentPolarity.NEUTRAL,
                deriver.deriveWithRules(0.55, 0.45, "还行吧"));
    }

    @Test
    @DisplayName("deriveWithRules - 长文本 + 大差距 → 保持原极性")
    void deriveWithRules_longTextLargeGap_keepsPolarity() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        assertEquals(SentimentPolarity.POSITIVE,
                deriver.deriveWithRules(0.90, 0.10, "这部电影真的非常精彩，强烈推荐给大家"));
    }

    @Test
    @DisplayName("deriveWithRules - null text 回退到 derive")
    void deriveWithRules_nullText_fallsBack() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        assertEquals(SentimentPolarity.POSITIVE, deriver.deriveWithRules(0.85, 0.15, null));
    }

    @Test
    @DisplayName("构造 - 非法 theta 抛 IllegalArgumentException")
    void constructor_invalidTheta_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ThreeClassDeriver(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new ThreeClassDeriver(1.1));
    }

    @Test
    @DisplayName("confidenceGap - 返回概率绝对差")
    void confidenceGap_returnsAbsoluteDifference() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.2);
        assertEquals(0.3, deriver.confidenceGap(0.65, 0.35), 1e-9);
        assertEquals(0.3, deriver.confidenceGap(0.35, 0.65), 1e-9);
    }

    @Test
    @DisplayName("theta - 返回构造时传入的阈值")
    void theta_returnsValue() {
        ThreeClassDeriver deriver = new ThreeClassDeriver(0.25);
        assertEquals(0.25, deriver.theta(), 1e-9);
    }
}

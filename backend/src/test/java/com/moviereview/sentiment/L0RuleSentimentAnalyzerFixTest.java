package com.moviereview.sentiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L0 规则引擎 —— code review 修复验证。
 *
 * 针对 spec review 发现的三个 wrong 问题：
 * (5) 全局否定词翻转 → 局部否定
 * (6) 子串双重计数
 * (7) 程度副词作用域
 */
class L0RuleSentimentAnalyzerFixTest {

    private final SentimentAnalyzer analyzer = new L0RuleSentimentAnalyzer();

    @Test
    @DisplayName("修复(5) - 局部否定：'不推荐，但是演技在线剧情紧凑' 不应整体翻转为负面")
    void localNegation_doesNotFlipWholeComment() {
        AnalysisResult result = analyzer.analyze("不推荐，但是演技在线剧情紧凑");
        // "不推荐" 的否定作用于 "推荐"，但 "演技在线" "剧情紧凑" 是独立的正面信号
        // 期望：整体偏正面（因为正面信号远多于负面）
        assertNotEquals(SentimentPolarity.NEGATIVE, result.polarity(),
                "不应被 '不推荐' 整体翻转为负面");
    }

    @Test
    @DisplayName("修复(5) - '不好' 仍应判为负面（spec 原文要求）")
    void localNegation_stillFlipsSimpleCase() {
        AnalysisResult result = analyzer.analyze("这个电影不好");
        assertEquals(SentimentPolarity.NEGATIVE, result.polarity());
    }

    @Test
    @DisplayName("修复(6) - 子串双重计数：'演技在线' 应只计一次正面")
    void substring_noDoubleCounting() {
        // "演技在线" 包含 "在线"，但应只算一个正面信号
        AnalysisResult result = analyzer.analyze("演技在线");
        // 置信度应基于实际正面词数量（1 个），而非子串重叠（2 个）
        assertTrue(result.confidence() <= 0.7,
                "置信度不应因子串重叠而虚高，实际：" + result.confidence());
    }

    @Test
    @DisplayName("修复(7) - 程度副词应加权对应方向：'非常好看' 置信度 > '好看'")
    void intensifier_scopesCorrectly() {
        AnalysisResult mild = analyzer.analyze("好看");
        AnalysisResult strong = analyzer.analyze("非常好看");
        assertEquals(SentimentPolarity.POSITIVE, mild.polarity());
        assertEquals(SentimentPolarity.POSITIVE, strong.polarity());
        assertTrue(strong.confidence() > mild.confidence(),
                "'非常好看' 置信度应高于 '好看'");
    }

    @Test
    @DisplayName("aspect - '演技在线' 应产出 演技=POSITIVE，其他维度未提及")
    void aspect_lexicon_capturesDimensionScore() {
        AnalysisResult result = analyzer.analyze("演技在线");
        AnalysisResult.AspectScore acting = result.aspects().get("演技");
        assertNotNull(acting);
        assertEquals(SentimentPolarity.POSITIVE, acting.polarity(),
                "演技维度应为正面");
        assertEquals(1, acting.sampleCount(), "演技维度样本量应为 1");
        // 其他维度未提及
        AnalysisResult.AspectScore plot = result.aspects().get("剧情");
        assertEquals(0, plot.sampleCount(), "剧情维度未提及，样本量应为 0");
    }

    @Test
    @DisplayName("aspect - '剧情拖沓' 应产出 剧情=NEGATIVE")
    void aspect_negativeDimension() {
        AnalysisResult result = analyzer.analyze("剧情拖沓");
        AnalysisResult.AspectScore plot = result.aspects().get("剧情");
        assertEquals(SentimentPolarity.NEGATIVE, plot.polarity(), "剧情维度应为负面");
    }

    @Test
    @DisplayName("aspect - 未提及的维度应为 NEUTRAL + sampleCount=0")
    void aspect_unmentioned_neutralZero() {
        AnalysisResult result = analyzer.analyze("这部电影还行");
        for (var entry : result.aspects().entrySet()) {
            assertEquals(0, entry.getValue().sampleCount(),
                    entry.getKey() + " 维度未提及，样本量应为 0");
        }
    }
}

package com.moviereview.sentiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L0 规则情感引擎测试。
 *
 * 测试接缝：{@link SentimentAnalyzer#analyze(String)}。
 * 只测外部行为：给一个中文影评，断言返回的极性、置信度、触发关键词。
 */
class L0RuleSentimentAnalyzerTest {

    private final SentimentAnalyzer analyzer = new L0RuleSentimentAnalyzer();

    @Test
    @DisplayName("analyze - 显式正面词应判为正面")
    void analyze_explicitPositiveWord_returnsPositive() {
        AnalysisResult result = analyzer.analyze("这部电影太好看了");
        assertEquals(SentimentPolarity.POSITIVE, result.polarity());
    }

    @Test
    @DisplayName("analyze - 显式负面词应判为负面")
    void analyze_explicitNegativeWord_returnsNegative() {
        AnalysisResult result = analyzer.analyze("剧情非常拖沓");
        assertEquals(SentimentPolarity.NEGATIVE, result.polarity());
    }

    @Test
    @DisplayName("analyze - 否定词应翻转极性：'不好' → 负面")
    void analyze_negation_flipsPolarity() {
        AnalysisResult result = analyzer.analyze("这个电影不好");
        assertEquals(SentimentPolarity.NEGATIVE, result.polarity());
    }

    @Test
    @DisplayName("analyze - 程度副词应提高置信度：'非常好看' > '好看'")
    void analyze_intensifier_increasesConfidence() {
        AnalysisResult mild = analyzer.analyze("好看");
        AnalysisResult strong = analyzer.analyze("非常好看");
        assertEquals(SentimentPolarity.POSITIVE, mild.polarity());
        assertEquals(SentimentPolarity.POSITIVE, strong.polarity());
        assertTrue(strong.confidence() > mild.confidence(),
                "'非常好看' 的置信度应高于 '好看'");
    }

    @Test
    @DisplayName("analyze - 中性标记词应判为中性")
    void analyze_neutralMarkerWord_returnsNeutral() {
        AnalysisResult result = analyzer.analyze("还行吧");
        assertEquals(SentimentPolarity.NEUTRAL, result.polarity());
    }

    @Test
    @DisplayName("analyze - 影视领域词典应命中：'演技在线' → 触发词包含")
    void analyze_domainLexicon_capturesTriggerWord() {
        AnalysisResult result = analyzer.analyze("演技在线，值得一看");
        assertTrue(result.triggerWords().contains("演技在线"),
                "触发词应包含 '演技在线'，实际：" + result.triggerWords());
    }

    @Test
    @DisplayName("analyze - null 文本应抛 IllegalArgumentException")
    void analyze_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null));
    }

    @Test
    @DisplayName("analyze - 空白文本应抛 IllegalArgumentException")
    void analyze_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze("   "));
    }
}

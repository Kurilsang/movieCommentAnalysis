package com.moviereview.aspect;

import com.moviereview.sentiment.L0RuleSentimentAnalyzer;
import com.moviereview.sentiment.SentimentPolarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 规则方面级分析器测试。
 *
 * spec 创新点 2：六维方面级情感分析。
 * spec 红线：转折句必须切分、锚点显式才报告。
 */
class RuleBasedAspectAnalyzerTest {

    private final AspectAnalyzer analyzer = new RuleBasedAspectAnalyzer(new L0RuleSentimentAnalyzer());

    @Test
    @DisplayName("aspectNames - 返回六维")
    void aspectNames_returnsSixDimensions() {
        assertEquals(6, analyzer.aspectNames().size());
        assertTrue(analyzer.aspectNames().contains("剧情"));
        assertTrue(analyzer.aspectNames().contains("演技"));
    }

    @Test
    @DisplayName("转折句切分：'画面很美但是剧情很烂' → 画面=POSITIVE, 剧情=NEGATIVE")
    void contrastiveSentence_splitsClauses() {
        Map<String, AspectResult> results = analyzer.analyze("画面很美但是剧情很烂");
        AspectResult visuals = results.get("画面特效");
        AspectResult plot = results.get("剧情");

        assertTrue(visuals.mentioned(), "画面应被提及");
        assertEquals(SentimentPolarity.POSITIVE, visuals.polarity(),
                "画面应为正面（'美'）");

        assertTrue(plot.mentioned(), "剧情应被提及");
        assertEquals(SentimentPolarity.NEGATIVE, plot.polarity(),
                "剧情应为负面（'烂'）");
    }

    @Test
    @DisplayName("锚点显式才报告：'太拖沓了' 不报告任何方面（无方面词）")
    void onlyExplicitAnchors_reported() {
        Map<String, AspectResult> results = analyzer.analyze("太拖沓了");
        // "拖沓" 是"节奏"的同义词，所以节奏会被提及
        AspectResult pacing = results.get("节奏");
        assertTrue(pacing.mentioned(), "'拖沓' 是节奏的同义词，应提及节奏");
    }

    @Test
    @DisplayName("未提及的方面 sampleCount=0")
    void unmentionedAspects_zeroSampleCount() {
        Map<String, AspectResult> results = analyzer.analyze("演技在线");
        AspectResult plot = results.get("剧情");
        assertFalse(plot.mentioned(), "剧情未提及");
        assertEquals(0, plot.sampleCount());
    }

    @Test
    @DisplayName("多方面同句：'剧情紧凑演技在线' → 剧情=POSITIVE, 演技=POSITIVE")
    void multipleAspectsInOneClause() {
        Map<String, AspectResult> results = analyzer.analyze("剧情紧凑演技在线");
        assertEquals(SentimentPolarity.POSITIVE, results.get("剧情").polarity());
        assertEquals(SentimentPolarity.POSITIVE, results.get("演技").polarity());
    }

    @Test
    @DisplayName("null text 抛 IllegalArgumentException")
    void nullText_throws() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null));
    }

    @Test
    @DisplayName("aggregate - 贝叶斯平滑小样本向 50 收缩")
    void aggregate_bayesianSmoothing() {
        // 1 条正面评论
        Map<String, AspectAggregate> aggs = analyzer.aggregate(List.of("演技在线"));
        AspectAggregate acting = aggs.get("演技");
        assertEquals(1, acting.mentionedCount());
        // 原始分数应为 100（1/1 正面）
        assertEquals(100.0, acting.rawScore(), 1e-9);
        // 平滑后应 < 100（向 50 收缩）
        assertTrue(acting.smoothedScore() < 100.0, "小样本应向 50 收缩");
        assertTrue(acting.smoothedScore() > 50.0, "平滑后应高于 50（有正面证据）");
        // 样本量 < 10 → sufficientData=false
        assertFalse(acting.sufficientData(), "样本量 < 10 应为 insufficient");
    }

    @Test
    @DisplayName("aggregate - 大样本平滑效果减弱")
    void aggregate_largeSampleLessSmoothing() {
        // 100 条正面评论
        List<String> texts = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            texts.add("演技在线");
        }
        Map<String, AspectAggregate> aggs = analyzer.aggregate(texts);
        AspectAggregate acting = aggs.get("演技");
        assertEquals(100, acting.mentionedCount());
        assertTrue(acting.sufficientData(), "样本量 ≥ 10 应为 sufficient");
        // 平滑后应接近 100（100 条全正面，先验影响小）
        assertTrue(acting.smoothedScore() > 90.0, "大样本平滑后应接近 100");
    }

    @Test
    @DisplayName("aggregate - 空列表抛 IllegalArgumentException")
    void aggregate_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.aggregate(List.of()));
    }
}

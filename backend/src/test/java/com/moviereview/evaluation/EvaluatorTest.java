package com.moviereview.evaluation;

import com.moviereview.sentiment.AnalysisResult;
import com.moviereview.sentiment.L0RuleSentimentAnalyzer;
import com.moviereview.sentiment.SentimentAnalyzer;
import com.moviereview.sentiment.SentimentPolarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评测器测试。
 *
 * 测试接缝：{@link Evaluator#evaluate(SentimentAnalyzer, List)}。
 * 只测外部行为：给一个分析器 + 一批样本，断言返回的指标字段值。
 */
class EvaluatorTest {

    private final Evaluator evaluator = new Evaluator();

    /** 一个固定的假分析器，总是返回 POSITIVE。用于精确控制预测结果。 */
    private static class AlwaysPositiveAnalyzer implements SentimentAnalyzer {
        @Override
        public AnalysisResult analyze(String text) {
            return new AnalysisResult(SentimentPolarity.POSITIVE, 1.0, List.of(), Map.of(), "{}");
        }
        @Override
        public String name() { return "always-positive"; }
    }

    /** 一个假分析器，按样本 text 内容返回预设极性。 */
    private static class ScriptedAnalyzer implements SentimentAnalyzer {
        private final Map<String, SentimentPolarity> script;
        ScriptedAnalyzer(Map<String, SentimentPolarity> script) { this.script = script; }
        @Override
        public AnalysisResult analyze(String text) {
            return new AnalysisResult(script.get(text), 1.0, List.of(), Map.of(), "{}");
        }
        @Override
        public String name() { return "scripted"; }
    }

    @Test
    @DisplayName("evaluate - 全对时 accuracy=1.0, macroF1=1.0")
    void evaluate_allCorrect_returnsPerfectScore() {
        List<EvalSample> samples = List.of(
                new EvalSample("1", "好", SentimentPolarity.POSITIVE, "test"),
                new EvalSample("2", "差", SentimentPolarity.NEGATIVE, "test"),
                new EvalSample("3", "中", SentimentPolarity.NEUTRAL, "test")
        );
        ScriptedAnalyzer analyzer = new ScriptedAnalyzer(Map.of(
                "好", SentimentPolarity.POSITIVE,
                "差", SentimentPolarity.NEGATIVE,
                "中", SentimentPolarity.NEUTRAL
        ));
        Metric m = evaluator.evaluate(analyzer, samples);
        assertEquals(1.0, m.accuracy(), 1e-9);
        assertEquals(1.0, m.macroF1(), 1e-9);
        assertEquals(3, m.sampleCount());
    }

    @Test
    @DisplayName("evaluate - 全错时 accuracy=0.0")
    void evaluate_allWrong_returnsZeroAccuracy() {
        List<EvalSample> samples = List.of(
                new EvalSample("1", "好", SentimentPolarity.POSITIVE, "test"),
                new EvalSample("2", "好", SentimentPolarity.POSITIVE, "test")
        );
        // 总是返回 NEGATIVE
        SentimentAnalyzer alwaysNegative = new SentimentAnalyzer() {
            @Override
            public AnalysisResult analyze(String text) {
                return new AnalysisResult(SentimentPolarity.NEGATIVE, 1.0, List.of(), Map.of(), "{}");
            }
            @Override
            public String name() { return "always-negative"; }
        };
        Metric m = evaluator.evaluate(alwaysNegative, samples);
        assertEquals(0.0, m.accuracy(), 1e-9);
    }

    @Test
    @DisplayName("evaluate - 混淆矩阵正确计数")
    void evaluate_confusionMatrix_correct() {
        List<EvalSample> samples = List.of(
                new EvalSample("1", "好", SentimentPolarity.POSITIVE, "test"),
                new EvalSample("2", "差", SentimentPolarity.NEGATIVE, "test")
        );
        // 都预测 POSITIVE
        Metric m = evaluator.evaluate(new AlwaysPositiveAnalyzer(), samples);
        assertEquals(1, m.confusionMatrix().get("POSITIVE-POSITIVE"));
        assertEquals(1, m.confusionMatrix().get("NEGATIVE-POSITIVE"));
        assertEquals(0, m.confusionMatrix().get("POSITIVE-NEGATIVE"));
    }

    @Test
    @DisplayName("evaluate - L0 真实分析器跑通，指标在合理范围")
    void evaluate_realL0Analyzer_smokeTest() {
        List<EvalSample> samples = List.of(
                new EvalSample("1", "这部电影太好看了", SentimentPolarity.POSITIVE, "test"),
                new EvalSample("2", "剧情非常拖沓", SentimentPolarity.NEGATIVE, "test"),
                new EvalSample("3", "还行吧", SentimentPolarity.NEUTRAL, "test")
        );
        Metric m = evaluator.evaluate(new L0RuleSentimentAnalyzer(), samples);
        assertTrue(m.accuracy() >= 0.0 && m.accuracy() <= 1.0);
        assertEquals(3, m.sampleCount());
    }

    @Test
    @DisplayName("evaluate - null analyzer 抛 IllegalArgumentException")
    void evaluate_nullAnalyzer_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(null, List.of(new EvalSample("1", "x", SentimentPolarity.POSITIVE, "test"))));
    }

    @Test
    @DisplayName("evaluate - 空 samples 抛 IllegalArgumentException")
    void evaluate_emptySamples_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(new AlwaysPositiveAnalyzer(), List.of()));
    }

    @Test
    @DisplayName("compare - 多分析器对比返回按 name 分组的结果")
    void compare_multipleAnalyzers_returnsGrouped() {
        List<EvalSample> samples = List.of(
                new EvalSample("1", "好", SentimentPolarity.POSITIVE, "test")
        );
        List<SentimentAnalyzer> analyzers = List.of(
                new AlwaysPositiveAnalyzer(),
                new L0RuleSentimentAnalyzer()
        );
        Map<String, Metric> results = evaluator.compare(analyzers, samples);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("always-positive"));
        assertTrue(results.containsKey("L0-rule"));
    }
}

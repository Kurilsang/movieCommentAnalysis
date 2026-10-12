package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentPolarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * θ 敏感性分析器测试。
 */
class ThetaSensitivityAnalyzerTest {

    private final ThetaSensitivityAnalyzer analyzer = new ThetaSensitivityAnalyzer();

    private BinaryLabeledSample sample(String id, double pos, double neg, SentimentPolarity golden) {
        return new BinaryLabeledSample(id, "text" + id, pos, neg, golden);
    }

    @Test
    @DisplayName("scan - 网格搜索返回正确数量的点")
    void scan_returnsCorrectNumberOfPoints() {
        List<BinaryLabeledSample> samples = List.of(
                sample("1", 0.85, 0.15, SentimentPolarity.POSITIVE),
                sample("2", 0.15, 0.85, SentimentPolarity.NEGATIVE),
                sample("3", 0.52, 0.48, SentimentPolarity.NEUTRAL)
        );
        // θ 从 0.05 到 0.50，步长 0.05 → 10 个点
        List<ThetaSensitivityAnalyzer.ThetaPoint> points =
                analyzer.scan(samples, 0.05, 0.50, 0.05);
        assertEquals(10, points.size());
    }

    @Test
    @DisplayName("scan - θ 越大中性类占比越高")
    void scan_largerTheta_moreNeutrals() {
        List<BinaryLabeledSample> samples = List.of(
                sample("1", 0.65, 0.35, SentimentPolarity.POSITIVE),
                sample("2", 0.55, 0.45, SentimentPolarity.NEUTRAL),
                sample("3", 0.52, 0.48, SentimentPolarity.NEUTRAL)
        );
        List<ThetaSensitivityAnalyzer.ThetaPoint> points =
                analyzer.scan(samples, 0.05, 0.50, 0.05);
        // θ=0.05 时只有差距<0.05 的判为中性
        // θ=0.50 时差距<0.50 的都判为中性
        ThetaSensitivityAnalyzer.ThetaPoint first = points.get(0);
        ThetaSensitivityAnalyzer.ThetaPoint last = points.get(points.size() - 1);
        assertTrue(last.neutralRatio() >= first.neutralRatio(),
                "θ 越大中性类占比应越高");
    }

    @Test
    @DisplayName("scan - 空 samples 抛 IllegalArgumentException")
    void scan_emptySamples_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.scan(List.of(), 0.05, 0.50, 0.05));
    }

    @Test
    @DisplayName("scan - 非正 step 抛 IllegalArgumentException")
    void scan_invalidStep_throws() {
        List<BinaryLabeledSample> samples = List.of(sample("1", 0.85, 0.15, SentimentPolarity.POSITIVE));
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.scan(samples, 0.05, 0.50, 0));
    }

    @Test
    @DisplayName("findBest - 返回 macroF1 最高的点")
    void findBest_returnsHighestF1() {
        List<ThetaSensitivityAnalyzer.ThetaPoint> points = List.of(
                new ThetaSensitivityAnalyzer.ThetaPoint(0.1, 0.7, 0.6, 0.1, 10),
                new ThetaSensitivityAnalyzer.ThetaPoint(0.2, 0.8, 0.75, 0.15, 15),
                new ThetaSensitivityAnalyzer.ThetaPoint(0.3, 0.75, 0.7, 0.2, 20)
        );
        ThetaSensitivityAnalyzer.ThetaPoint best = analyzer.findBest(points);
        assertEquals(0.2, best.theta(), 1e-9);
        assertEquals(0.75, best.macroF1(), 1e-9);
    }

    @Test
    @DisplayName("findBest - 空列表抛 IllegalArgumentException")
    void findBest_empty_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.findBest(List.of()));
    }

    @Test
    @DisplayName("scan - perfect classifier 在正确 θ 下达到 F1=1.0")
    void scan_perfectClassifier() {
        // 设计：正/负概率差距都很大，中性概率差距都很小
        // θ=0.20 时应完美分类
        List<BinaryLabeledSample> samples = List.of(
                sample("1", 0.95, 0.05, SentimentPolarity.POSITIVE),  // gap=0.90 > 0.20 → POSITIVE ✓
                sample("2", 0.05, 0.95, SentimentPolarity.NEGATIVE),  // gap=0.90 > 0.20 → NEGATIVE ✓
                sample("3", 0.55, 0.45, SentimentPolarity.NEUTRAL),   // gap=0.10 < 0.20 → NEUTRAL ✓
                sample("4", 0.48, 0.52, SentimentPolarity.NEUTRAL)    // gap=0.04 < 0.20 → NEUTRAL ✓
        );
        List<ThetaSensitivityAnalyzer.ThetaPoint> points =
                analyzer.scan(samples, 0.20, 0.20, 0.05);
        assertEquals(1, points.size());
        assertEquals(1.0, points.get(0).macroF1(), 1e-9);
        assertEquals(1.0, points.get(0).accuracy(), 1e-9);
    }
}

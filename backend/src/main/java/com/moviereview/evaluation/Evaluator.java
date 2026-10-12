package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentAnalyzer;
import com.moviereview.sentiment.SentimentPolarity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评测器 —— 测试接缝（{@link SentimentAnalyzer}）的批量模式。
 *
 * 跑一个 {@link SentimentAnalyzer} 在一批 {@link EvalSample} 上，产出 {@link Metric}。
 * 同一批样本、不同 analyzer，即可产出对比表（L0/L1/L2 三方案对比）。
 */
public class Evaluator {

    /**
     * 跑评测，返回指标。
     *
     * @param analyzer  被测分析器
     * @param samples   金标准样本集
     * @return 指标
     * @throws IllegalArgumentException 当 analyzer 或 samples 为 null/空
     */
    public Metric evaluate(SentimentAnalyzer analyzer, List<EvalSample> samples) {
        if (analyzer == null) {
            throw new IllegalArgumentException("analyzer 不能为空");
        }
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("samples 不能为空");
        }

        // 混淆矩阵
        Map<String, Integer> confusion = new LinkedHashMap<>();
        for (SentimentPolarity actual : SentimentPolarity.values()) {
            for (SentimentPolarity predicted : SentimentPolarity.values()) {
                confusion.put(Metric.confusionKey(actual, predicted), 0);
            }
        }

        ClassificationMetrics metrics = new ClassificationMetrics();
        for (EvalSample sample : samples) {
            SentimentPolarity actual = sample.goldenPolarity();
            SentimentPolarity predicted = analyzer.analyze(sample.text()).polarity();
            confusion.merge(Metric.confusionKey(actual, predicted), 1, Integer::sum);
            metrics.record(actual, predicted);
        }

        return new Metric(
                metrics.accuracy(),
                metrics.macroPrecision(),
                metrics.macroRecall(),
                metrics.macroF1(),
                Map.copyOf(confusion),
                samples.size()
        );
    }

    /**
     * 批量评测多个分析器，返回对比结果（按 analyzer.name() 分组）。
     */
    public Map<String, Metric> compare(List<SentimentAnalyzer> analyzers, List<EvalSample> samples) {
        if (analyzers == null || analyzers.isEmpty()) {
            throw new IllegalArgumentException("analyzers 不能为空");
        }
        Map<String, Metric> results = new LinkedHashMap<>();
        for (SentimentAnalyzer analyzer : analyzers) {
            results.put(analyzer.name(), evaluate(analyzer, samples));
        }
        return results;
    }
}

package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentAnalyzer;
import com.moviereview.sentiment.SentimentPolarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模型对比服务。
 *
 * spec：同一评论可在 L0/L1/L2 间切换并对比结果。
 * 记录各方法准确率与耗时，供论文实验章节使用。
 */
public class ModelComparisonService {

    private final List<SentimentAnalyzer> analyzers;
    private final Evaluator evaluator;

    public ModelComparisonService(List<SentimentAnalyzer> analyzers) {
        if (analyzers == null || analyzers.isEmpty()) {
            throw new IllegalArgumentException("analyzers 不能为空");
        }
        this.analyzers = new ArrayList<>(analyzers);
        this.evaluator = new Evaluator();
    }

    /**
     * 单条文本多模型对比结果。
     */
    public record ComparisonResult(
            String text,
            Map<String, SentimentPolarity> polarities,
            Map<String, Double> confidences,
            Map<String, Long> elapsedMs,
            Map<String, String> rawJsons
    ) {}

    /**
     * 对比单条文本。
     */
    public ComparisonResult compareOne(String text) {
        Map<String, SentimentPolarity> polarities = new LinkedHashMap<>();
        Map<String, Double> confidences = new LinkedHashMap<>();
        Map<String, Long> elapsedMs = new LinkedHashMap<>();
        Map<String, String> rawJsons = new LinkedHashMap<>();

        for (SentimentAnalyzer analyzer : analyzers) {
            long start = System.currentTimeMillis();
            try {
                var result = analyzer.analyze(text);
                long elapsed = System.currentTimeMillis() - start;
                polarities.put(analyzer.name(), result.polarity());
                confidences.put(analyzer.name(), result.confidence());
                elapsedMs.put(analyzer.name(), elapsed);
                rawJsons.put(analyzer.name(), result.rawJson());
            } catch (Exception e) {
                elapsedMs.put(analyzer.name(), System.currentTimeMillis() - start);
                rawJsons.put(analyzer.name(), "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
        return new ComparisonResult(text, polarities, confidences, elapsedMs, rawJsons);
    }

    /**
     * 对比评测（批量）。
     *
     * @param samples 测试集
     * @return 各模型的指标
     */
    public Map<String, Metric> compareEval(List<EvalSample> samples) {
        return evaluator.compare(analyzers, samples);
    }

    public List<SentimentAnalyzer> analyzers() {
        return List.copyOf(analyzers);
    }
}

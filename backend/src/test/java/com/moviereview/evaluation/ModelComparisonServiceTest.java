package com.moviereview.evaluation;

import com.moviereview.sentiment.L0RuleSentimentAnalyzer;
import com.moviereview.sentiment.SentimentAnalyzer;
import com.moviereview.sentiment.SentimentPolarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多模型对比服务测试。
 */
class ModelComparisonServiceTest {

    @Test
    @DisplayName("compareOne - 对比单条文本返回多模型结果")
    void compareOne_basic() {
        ModelComparisonService service = new ModelComparisonService(
                List.of(new L0RuleSentimentAnalyzer()));
        ModelComparisonService.ComparisonResult result = service.compareOne("这部电影太好看了");
        assertNotNull(result);
        assertTrue(result.polarities().size() >= 1);
        assertTrue(result.confidences().size() >= 1);
    }

    @Test
    @DisplayName("compareEval - 批量评测返回各模型指标")
    void compareEval_basic() {
        ModelComparisonService service = new ModelComparisonService(
                List.of(new L0RuleSentimentAnalyzer()));
        List<EvalSample> samples = List.of(
                new EvalSample("1", "好看", SentimentPolarity.POSITIVE, "test"),
                new EvalSample("2", "难看", SentimentPolarity.NEGATIVE, "test")
        );
        Map<String, Metric> results = service.compareEval(samples);
        assertFalse(results.isEmpty());
    }
}

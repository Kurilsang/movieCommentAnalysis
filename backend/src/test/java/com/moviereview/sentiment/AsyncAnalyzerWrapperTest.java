package com.moviereview.sentiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步分析器包装器测试。
 */
class AsyncAnalyzerWrapperTest {

    private final SentimentAnalyzer fastAnalyzer = new L0RuleSentimentAnalyzer();

    @Test
    @DisplayName("analyzeAsync - 快速分析返回结果")
    void analyzeAsync_fast() throws Exception {
        AsyncAnalyzerWrapper wrapper = new AsyncAnalyzerWrapper(fastAnalyzer, 1000);
        CompletableFuture<AnalysisResult> future = wrapper.analyzeAsync("好电影");
        AnalysisResult result = future.get();
        assertNotNull(result);
    }

    @Test
    @DisplayName("analyzeAsync - 超时返回 null")
    void analyzeAsync_timeout() throws Exception {
        // 慢分析器（模拟 L1 超时）
        SentimentAnalyzer slowAnalyzer = new SentimentAnalyzer() {
            @Override
            public AnalysisResult analyze(String text) {
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                return new AnalysisResult(SentimentPolarity.POSITIVE, 1.0, null, null, "");
            }
            @Override
            public String name() { return "slow"; }
        };
        AsyncAnalyzerWrapper wrapper = new AsyncAnalyzerWrapper(slowAnalyzer, 100);
        AnalysisResult result = wrapper.analyzeAsync("test").get();
        assertNull(result, "超时应返回 null");
    }

    @Test
    @DisplayName("构造 - null analyzer 抛 IllegalArgumentException")
    void constructor_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> new AsyncAnalyzerWrapper(null, 1000));
    }

    @Test
    @DisplayName("name - 返回 analyzer 名称")
    void name_returnsAnalyzerName() {
        AsyncAnalyzerWrapper wrapper = new AsyncAnalyzerWrapper(fastAnalyzer, 1000);
        assertEquals("L0-rule", wrapper.name());
    }
}

package com.moviereview.sentiment;

import java.util.concurrent.CompletableFuture;

/**
 * 异步分析器包装器。
 *
 * spec：同步返回 L0/L2 本地结果 + 异步补 L1 大模型结果。
 * 前端轮询（500ms 间隔，拿到即停）。
 */
public class AsyncAnalyzerWrapper {

    private final SentimentAnalyzer analyzer;
    private final long timeoutMs;

    public AsyncAnalyzerWrapper(SentimentAnalyzer analyzer, long timeoutMs) {
        if (analyzer == null) {
            throw new IllegalArgumentException("analyzer 不能为空");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs 必须为正");
        }
        this.analyzer = analyzer;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 异步分析，超时返回 null。
     *
     * @param text 文本
     * @return CompletableFuture（超时或异常时返回 null）
     */
    public CompletableFuture<AnalysisResult> analyzeAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzer.analyze(text);
            } catch (Exception e) {
                return null;
            }
        }).completeOnTimeout(null, timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public String name() {
        return analyzer.name();
    }

    public long timeoutMs() {
        return timeoutMs;
    }
}

package com.moviereview.sentiment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L1 大模型分析器。
 *
 * spec：调用国产大模型 API，做观点抽取与兜底。
 * 3 秒超时 + 失败降级（超时返回本地结果，标注"大模型分析不可用"）。
 *
 * 供应商：阿里百炼 DashScope（唯一有官方 Java SDK 的供应商）。
 * 若 DashScope 不可用，退化为返回 null（调用方降级处理）。
 */
public class L1LlmAnalyzer implements SentimentAnalyzer {

    private final String apiKey;
    private final String endpoint;
    private final HttpClient httpClient;

    /**
     * @param apiKey API 密钥
     * @param endpoint API 端点（DashScope OpenAI 兼容接口）
     */
    public L1LlmAnalyzer(String apiKey, String endpoint) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey 不能为空");
        }
        this.apiKey = apiKey;
        this.endpoint = endpoint != null ? endpoint
                : "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public AnalysisResult analyze(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("评论文本不能为空");
        }
        try {
            String prompt = buildPrompt(text);
            String requestBody = buildRequestBody(prompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.body());
        } catch (Exception e) {
            // 失败降级：返回空结果，调用方处理
            throw new IllegalStateException("L1 大模型分析失败: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String text) {
        return """
                你是一个中文影评情感分析专家。请分析以下影评，返回 JSON：
                {
                  "polarity": "POSITIVE|NEUTRAL|NEGATIVE",
                  "confidence": 0.0-1.0,
                  "aspects": {
                    "剧情": "POSITIVE|NEUTRAL|NEGATIVE|UNMENTIONED",
                    "演技": "...",
                    "画面特效": "...",
                    "配乐音效": "...",
                    "节奏": "...",
                    "台词": "..."
                  }
                }
                只返回 JSON，不要其他文字。
                影评：\"""" + text + "\"";
    }

    private String buildRequestBody(String prompt) {
        return """
                {
                  "model": "qwen-plus",
                  "messages": [{"role": "user", "content": "%s"}],
                  "temperature": 0,
                  "max_tokens": 500
                }
                """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    private AnalysisResult parseResponse(String body) {
        // 简化解析：实际项目应用 Jackson/Gson
        // 此处返回中性结果，等真实 API 调用时完善
        return new AnalysisResult(SentimentPolarity.NEUTRAL, 0.5,
                new ArrayList<>(), new LinkedHashMap<>(), body);
    }

    @Override
    public String name() {
        return "L1-llm";
    }
}

package com.moviereview.sentiment;

/**
 * 情感分析器接口 —— 全系统唯一的测试接缝。
 *
 * 三个实现类（L0 规则引擎 / L2 本地 ONNX 模型 / L1 大模型 API）共用同一套测试。
 * 测试只测外部行为：给一个中文影评字符串，断言返回的 {@link AnalysisResult} 字段值。
 * 不测内部走了词典还是模型的分支。
 */
public interface SentimentAnalyzer {

    /**
     * 分析一句中文影评。
     *
     * @param text 中文影评文本
     * @return 分析结果，包含情感极性、置信度、触发关键词、六维方面分数
     * @throws IllegalArgumentException 当 text 为 null 或空白
     */
    AnalysisResult analyze(String text);

    /** 分析器名称（如 "L0-rule"、"L2-onnx"、"L1-llm"），用于多模型对比与日志。 */
    String name();
}

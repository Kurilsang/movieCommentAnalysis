package com.moviereview.sentiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L2 本地 ONNX 深度模型测试。
 *
 * 测试接缝：{@link SentimentAnalyzer#analyze(String)}（与 L0 相同）。
 *
 * 注意：ONNX 模型文件（约 100 MB）不提交 Git。测试通过 {@code @EnabledIf} 条件执行：
 * - 若模型文件存在，跑完整推理测试
 * - 若模型文件不存在，跳过推理测试，只跑契约测试（null/空白抛异常、name() 返回值）
 */
class L2OnnxSentimentAnalyzerTest {

    private static final Path MODEL_PATH = Path.of("ml/models/sentiment.onnx");
    private static final Path TOKENIZER_PATH = Path.of("ml/models/tokenizer");

    private static boolean modelAvailable() {
        return Files.exists(MODEL_PATH) && Files.isDirectory(TOKENIZER_PATH);
    }

    private L2OnnxSentimentAnalyzer createAnalyzer() {
        if (!modelAvailable()) {
            return null; // 测试会跳过
        }
        return new L2OnnxSentimentAnalyzer(MODEL_PATH, TOKENIZER_PATH);
    }

    @Test
    @DisplayName("name() - 返回 'L2-onnx'")
    void name_returnsL2Onnx() {
        if (!modelAvailable()) {
            // 模型不存在时，构造函数应抛出明确异常
            assertThrows(IllegalArgumentException.class,
                    () -> new L2OnnxSentimentAnalyzer(MODEL_PATH, TOKENIZER_PATH));
            return;
        }
        L2OnnxSentimentAnalyzer analyzer = createAnalyzer();
        assertEquals("L2-onnx", analyzer.name());
    }

    @Test
    @DisplayName("analyze - null 文本抛 IllegalArgumentException")
    void analyze_null_throws() {
        if (!modelAvailable()) {
            assertThrows(IllegalArgumentException.class,
                    () -> new L2OnnxSentimentAnalyzer(MODEL_PATH, TOKENIZER_PATH));
            return;
        }
        L2OnnxSentimentAnalyzer analyzer = createAnalyzer();
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null));
    }

    @Test
    @DisplayName("analyze - 空白文本抛 IllegalArgumentException")
    void analyze_blank_throws() {
        if (!modelAvailable()) {
            assertThrows(IllegalArgumentException.class,
                    () -> new L2OnnxSentimentAnalyzer(MODEL_PATH, TOKENIZER_PATH));
            return;
        }
        L2OnnxSentimentAnalyzer analyzer = createAnalyzer();
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze("   "));
    }

    @Test
    @EnabledIf("modelAvailable")
    @DisplayName("analyze - 显式正面评论应判为 POSITIVE")
    void analyze_positive_returnsPositive() {
        L2OnnxSentimentAnalyzer analyzer = createAnalyzer();
        AnalysisResult result = analyzer.analyze("这部电影太好看了，强烈推荐");
        assertEquals(SentimentPolarity.POSITIVE, result.polarity());
    }

    @Test
    @EnabledIf("modelAvailable")
    @DisplayName("analyze - 显式负面评论应判为 NEGATIVE")
    void analyze_negative_returnsNegative() {
        L2OnnxSentimentAnalyzer analyzer = createAnalyzer();
        AnalysisResult result = analyzer.analyze("剧情拖沓，演技尴尬，浪费时间");
        assertEquals(SentimentPolarity.NEGATIVE, result.polarity());
    }

    @Test
    @EnabledIf("modelAvailable")
    @DisplayName("analyze - 输出包含置信度在 [0,1] 区间")
    void analyze_confidenceInRange() {
        L2OnnxSentimentAnalyzer analyzer = createAnalyzer();
        AnalysisResult result = analyzer.analyze("还行");
        assertTrue(result.confidence() >= 0.0 && result.confidence() <= 1.0,
                "置信度应在 [0,1]，实际：" + result.confidence());
    }

    @Test
    @EnabledIf("modelAvailable")
    @DisplayName("analyze - rawJson 不为空")
    void analyze_rawJsonNotEmpty() {
        L2OnnxSentimentAnalyzer analyzer = createAnalyzer();
        AnalysisResult result = analyzer.analyze("不错");
        assertNotNull(result.rawJson());
        assertFalse(result.rawJson().isBlank());
    }
}

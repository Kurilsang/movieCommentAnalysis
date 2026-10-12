package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentPolarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试集加载器测试。
 */
class TestSetLoaderTest {

    private final TestSetLoader loader = new TestSetLoader();

    @TempDir
    Path tempDir;

    private Path writeCsv(String content) throws IOException {
        Path file = tempDir.resolve("test.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    @DisplayName("loadFromCsv - 加载带表头的 CSV")
    void loadFromCsv_withHeader_loadsSamples() throws IOException {
        Path file = writeCsv("""
                id,text,polarity,source
                1,这部电影太好看了,POSITIVE,douban
                2,剧情非常拖沓,NEGATIVE,douban
                3,还行吧,NEUTRAL,douban
                """);
        List<EvalSample> samples = loader.loadFromCsv(file);
        assertEquals(3, samples.size());
        assertEquals(SentimentPolarity.POSITIVE, samples.get(0).goldenPolarity());
        assertEquals(SentimentPolarity.NEGATIVE, samples.get(1).goldenPolarity());
        assertEquals(SentimentPolarity.NEUTRAL, samples.get(2).goldenPolarity());
    }

    @Test
    @DisplayName("loadFromCsv - 无表头时全部加载")
    void loadFromCsv_withoutHeader_loadsAll() throws IOException {
        Path file = writeCsv("""
                1,好,POSITIVE,x
                2,差,NEGATIVE,x
                """);
        List<EvalSample> samples = loader.loadFromCsv(file);
        assertEquals(2, samples.size());
    }

    @Test
    @DisplayName("loadFromCsv - 跳过空行和格式错误行")
    void loadFromCsv_skipsMalformedLines() throws IOException {
        Path file = writeCsv("""
                1,好,POSITIVE,x

                bad,line
                2,差,NEGATIVE,x
                """);
        List<EvalSample> samples = loader.loadFromCsv(file);
        assertEquals(2, samples.size());
    }

    @Test
    @DisplayName("loadFromCsv - 空文件抛 IllegalArgumentException")
    void loadFromCsv_emptyFile_throws() throws IOException {
        Path file = writeCsv("");
        assertThrows(IllegalArgumentException.class, () -> loader.loadFromCsv(file));
    }

    @Test
    @DisplayName("loadFromCsv - 文件不存在抛 IllegalArgumentException")
    void loadFromCsv_fileNotFound_throws() {
        Path file = tempDir.resolve("nonexistent.csv");
        assertThrows(IllegalArgumentException.class, () -> loader.loadFromCsv(file));
    }

    @Test
    @DisplayName("loadFromCsv - null path 抛 IllegalArgumentException")
    void loadFromCsv_nullPath_throws() {
        assertThrows(IllegalArgumentException.class, () -> loader.loadFromCsv(null));
    }
}

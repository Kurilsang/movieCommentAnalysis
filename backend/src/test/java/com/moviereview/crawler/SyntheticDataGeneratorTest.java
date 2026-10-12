package com.moviereview.crawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 合成数据生成器测试。
 */
class SyntheticDataGeneratorTest {

    private final SyntheticDataGenerator generator = new SyntheticDataGenerator();

    @Test
    @DisplayName("generate - 生成 20 部 × 200 条 = 4000 条")
    void generate_correctTotalCount() {
        List<Comment> comments = generator.generate(20, 200, 42);
        assertEquals(4000, comments.size());
    }

    @Test
    @DisplayName("generate - 每部电影恰好 200 条")
    void generate_perMovieCount() {
        List<Comment> comments = generator.generate(20, 200, 42);
        for (int m = 1; m <= 20; m++) {
            String movieId = String.format("m%03d", m);
            long count = comments.stream().filter(c -> c.movieId().equals(movieId)).count();
            assertEquals(200, count, movieId + " 应有 200 条评论");
        }
    }

    @Test
    @DisplayName("generate - 同一 seed 复现相同数据")
    void generate_deterministic() {
        List<Comment> a = generator.generate(5, 10, 42);
        List<Comment> b = generator.generate(5, 10, 42);
        assertEquals(a, b);
    }

    @Test
    @DisplayName("generate - 不同 seed 产生不同数据")
    void generate_differentSeeds() {
        List<Comment> a = generator.generate(5, 10, 42);
        List<Comment> b = generator.generate(5, 10, 43);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("generate - 非法 moviesCount 抛 IllegalArgumentException")
    void generate_invalidMoviesCount_throws() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(0, 10, 42));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(21, 10, 42));
    }

    @Test
    @DisplayName("generate + export - 生成 4000 条并导出 CSV")
    void generateAndExport_fullDataset(@TempDir Path tempDir) throws IOException {
        List<Comment> comments = generator.generate(20, 200, 42);
        Path out = tempDir.resolve("comments.csv");
        new CommentCsvExporter().export(comments, out);
        assertTrue(java.nio.file.Files.exists(out));
        // 4000 条 + 1 行表头
        long lines = java.nio.file.Files.readAllLines(out).size();
        assertEquals(4001, lines);
    }
}

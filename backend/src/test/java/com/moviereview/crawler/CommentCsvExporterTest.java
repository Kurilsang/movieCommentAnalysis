package com.moviereview.crawler;

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
 * CSV 导出器测试。
 */
class CommentCsvExporterTest {

    private final CommentCsvExporter exporter = new CommentCsvExporter();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("export - 导出 CSV 含表头和数据")
    void export_writesHeaderAndData() throws IOException {
        List<Comment> comments = List.of(
                new Comment("m1", "好电影", "张三", "2024-01-01", 10)
        );
        Path out = tempDir.resolve("test.csv");
        exporter.export(comments, out);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("movieId,text,author,publishTime,likeCount"));
        assertTrue(content.contains("m1,好电影,张三,2024-01-01,10"));
    }

    @Test
    @DisplayName("export - 自动创建父目录")
    void export_createsParentDirectory() throws IOException {
        Path out = tempDir.resolve("sub/dir/test.csv");
        exporter.export(List.of(new Comment("m1", "x", "a", "", 0)), out);
        assertTrue(Files.exists(out));
    }

    @Test
    @DisplayName("export - 含逗号的字段自动转义")
    void export_escapesCommas() throws IOException {
        List<Comment> comments = List.of(
                new Comment("m1", "好,电影", "张三", "2024-01-01", 10)
        );
        Path out = tempDir.resolve("test.csv");
        exporter.export(comments, out);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"好,电影\""));
    }

    @Test
    @DisplayName("export - 含引号的字段自动转义")
    void export_escapesQuotes() throws IOException {
        List<Comment> comments = List.of(
                new Comment("m1", "他说\"好\"", "张三", "2024-01-01", 10)
        );
        Path out = tempDir.resolve("test.csv");
        exporter.export(comments, out);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"他说\"\"好\"\"\""));
    }

    @Test
    @DisplayName("export - null comments 抛 IllegalArgumentException")
    void export_nullComments_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> exporter.export(null, tempDir.resolve("x.csv")));
    }

    @Test
    @DisplayName("export - null path 抛 IllegalArgumentException")
    void export_nullPath_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> exporter.export(List.of(), null));
    }
}

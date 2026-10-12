package com.moviereview.crawler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 评论 CSV 导出器。
 *
 * spec 铁律：爬完立即导出 CSV/JSON 存入 data/ 并提交 Git。
 * 即使站点改版，数据永远在手。
 *
 * CSV 格式：movieId,text,author,publishTime,likeCount
 */
public class CommentCsvExporter {

    /**
     * 导出评论到 CSV 文件。
     *
     * @param comments 评论列表
     * @param outputPath 输出文件路径（父目录会自动创建）
     * @throws IOException 文件写入失败
     */
    public void export(List<Comment> comments, Path outputPath) throws IOException {
        if (comments == null) {
            throw new IllegalArgumentException("comments 不能为空");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath 不能为空");
        }

        // 自动创建父目录
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("movieId,text,author,publishTime,likeCount\n");
        for (Comment c : comments) {
            sb.append(escape(c.movieId())).append(",")
              .append(escape(c.text())).append(",")
              .append(escape(c.author())).append(",")
              .append(escape(c.publishTime())).append(",")
              .append(c.likeCount()).append("\n");
        }
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }

    /** CSV 转义：含逗号、引号、换行的字段用双引号包裹。 */
    private String escape(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}

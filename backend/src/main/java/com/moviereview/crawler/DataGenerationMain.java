package com.moviereview.crawler;

import java.nio.file.Path;
import java.util.List;

/**
 * 数据生成 CLI 入口。
 *
 * 用法：
 *   cd backend
 *   mvn compile exec:java -Dexec.mainClass="com.moviereview.crawler.DataGenerationMain" \
 *       -Dexec.args="20 200 42 ../data/comments.csv"
 *
 * 或在 IDE 中直接运行 main 方法。
 */
public class DataGenerationMain {

    public static void main(String[] args) throws Exception {
        int moviesCount = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int perMovie = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 42;
        String outputPath = args.length > 3 ? args[3] : "data/comments.csv";

        System.out.printf("生成 %d 部电影 × %d 条评论（seed=%d）...%n", moviesCount, perMovie, seed);
        SyntheticDataGenerator generator = new SyntheticDataGenerator();
        List<Comment> comments = generator.generate(moviesCount, perMovie, seed);

        Path out = Path.of(outputPath);
        new CommentCsvExporter().export(comments, out);
        System.out.printf("✅ 已导出 %d 条评论到 %s%n", comments.size(), out.toAbsolutePath());

        // 同时生成 movies.csv（占位元数据，等真实数据集补充）
        Path moviesOut = out.getParent().resolve("movies.csv");
        StringBuilder sb = new StringBuilder("movieId,title,director,actors,year,posterUrl,synopsis\n");
        for (int m = 1; m <= moviesCount; m++) {
            String id = String.format("m%03d", m);
            sb.append(id).append(",电影").append(id).append(",,,,,\n");
        }
        java.nio.file.Files.writeString(moviesOut, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
        System.out.printf("✅ 已导出 %d 部电影元数据到 %s%n", moviesCount, moviesOut.toAbsolutePath());
    }
}

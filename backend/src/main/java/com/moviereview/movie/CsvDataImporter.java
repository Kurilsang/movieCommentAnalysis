package com.moviereview.movie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 启动时从 CSV 导入电影与评论数据（幂等）。
 *
 * 幂等策略：
 * - movie：按主键 id 存在则跳过
 * - comment：仅当该电影评论数为 0 时导入（避免重复）
 *
 * 数据来源：data/movies.csv、data/comments.csv（apache-2.0 许可的豆瓣数据集）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvDataImporter implements ApplicationRunner {

    private final MovieMapper movieMapper;
    private final CommentMapper commentMapper;

    @Value("${app.data.dir:../data}")
    private String dataDir;

    @Value("${app.data.import-enabled:true}")
    private boolean importEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!importEnabled) {
            log.info("数据导入已禁用（app.data.import-enabled=false）");
            return;
        }
        try {
            importMovies();
            importComments();
        } catch (Exception e) {
            log.warn("数据导入失败（不影响启动）：{}", e.getMessage());
        }
    }

    private void importMovies() throws Exception {
        Path csv = Path.of(dataDir, "movies.csv");
        if (!Files.exists(csv)) {
            log.warn("未找到 {}，跳过电影导入", csv.toAbsolutePath());
            return;
        }
        int inserted = 0;
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] p = splitCsv(line, 7);
                if (p.length < 2) continue;
                String id = p[0].trim();
                if (id.isEmpty() || movieMapper.selectById(id) != null) continue;

                Movie m = new Movie();
                m.setId(id);
                m.setTitle(stripQuotes(p[1].trim()));
                m.setDirector(emptyToNull(stripQuotes(p.length > 2 ? p[2] : null)));
                m.setActors(emptyToNull(stripQuotes(p.length > 3 ? p[3] : null)));
                m.setReleaseYear(parseInt(p.length > 4 ? p[4] : null));
                m.setPosterUrl(emptyToNull(stripQuotes(p.length > 5 ? p[5] : null)));
                m.setSynopsis(emptyToNull(stripQuotes(p.length > 6 ? p[6] : null)));
                m.setCommentCount(0);
                m.setCreatedAt(LocalDateTime.now());
                movieMapper.insert(m);
                inserted++;
            }
        }
        log.info("电影导入完成：新增 {} 部", inserted);
    }

    private void importComments() throws Exception {
        Path csv = Path.of(dataDir, "comments.csv");
        if (!Files.exists(csv)) {
            log.warn("未找到 {}，跳过评论导入", csv.toAbsolutePath());
            return;
        }
        // 幂等保护：评论表非空则整表跳过（避免重复导入）
        Long existing = commentMapper.selectCount(null);
        if (existing != null && existing > 0) {
            log.info("评论表已有 {} 条记录，跳过导入（幂等）", existing);
            refreshCommentCounts();
            return;
        }
        int inserted = 0;
        int skippedMovies = 0;
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;

                // 格式: movieId,text,author,publishTime,likeCount,rating
                // text 可能含逗号（已用双引号包裹），从两端切更稳
                int firstComma = line.indexOf(',');
                if (firstComma < 0) continue;
                String movieId = line.substring(0, firstComma).trim();
                int lastComma = line.lastIndexOf(',');
                if (lastComma <= firstComma) continue;
                String ratingStr = line.substring(lastComma + 1).trim();
                String beforeRating = line.substring(0, lastComma);
                int likeComma = beforeRating.lastIndexOf(',');
                if (likeComma <= firstComma) continue;
                String likeStr = beforeRating.substring(likeComma + 1).trim();
                String beforeLike = beforeRating.substring(0, likeComma);
                int timeComma = beforeLike.lastIndexOf(',');
                if (timeComma <= firstComma) continue;
                String publishTime = beforeLike.substring(timeComma + 1).trim();
                String beforeTime = beforeLike.substring(0, timeComma);
                int authorComma = beforeTime.lastIndexOf(',');
                if (authorComma <= firstComma) continue;
                String author = beforeTime.substring(authorComma + 1).trim();
                String text = beforeTime.substring(firstComma + 1, authorComma).trim();
                if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
                    text = text.substring(1, text.length() - 1).replace("\"\"", "\"");
                }
                if (text.isEmpty()) continue;

                // 幂等：该电影已有评论则跳过
                Movie movie = movieMapper.selectById(movieId);
                if (movie == null) { skippedMovies++; continue; }

                CommentEntity c = new CommentEntity();
                c.setMovieId(movieId);
                c.setText(text);
                c.setAuthor(author.isEmpty() ? "匿名" : author);
                c.setPublishTime(publishTime);
                c.setLikeCount(parseInt(likeStr) == null ? 0 : parseInt(likeStr));
                c.setRating(parseInt(ratingStr));
                c.setCreatedAt(LocalDateTime.now());
                commentMapper.insert(c);
                inserted++;
            }
        }
        refreshCommentCounts();
        log.info("评论导入完成：新增 {} 条（跳过 {} 条无对应电影）", inserted, skippedMovies);
    }

    /** 重算每部电影的评论数。 */
    private void refreshCommentCounts() {
        List<Movie> movies = movieMapper.selectList(null);
        for (Movie m : movies) {
            Long count = commentMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommentEntity>()
                            .eq(CommentEntity::getMovieId, m.getId()));
            m.setCommentCount(count == null ? 0 : count.intValue());
            movieMapper.updateById(m);
        }
    }

    /** 按最大段数切分 CSV（不做完整 CSV 解析，够用即可）。 */
    private String[] splitCsv(String line, int limit) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                cur.append(ch);
            } else if (ch == ',' && !inQuotes && parts.size() < limit - 1) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /** 剥掉 CSV 包裹的双引号。 */
    private String stripQuotes(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1).replace("\"\"", "\"");
        }
        return t;
    }

    private Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

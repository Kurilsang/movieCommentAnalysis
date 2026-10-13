package com.moviereview.movie;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 电影服务。
 *
 * 好评率来自真实星级（rating >= 4 的占比），这是数据集里唯一的真实标签。
 */
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieMapper movieMapper;
    private final CommentMapper commentMapper;

    /**
     * 电影列表（含好评率）。
     *
     * @param sort 排序：positiveRate / commentCount / title
     * @param q    搜索关键词（按片名过滤）
     */
    public List<Map<String, Object>> list(String sort, String q) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (q != null && !q.isBlank()) {
            wrapper.like(Movie::getTitle, q.trim());
        }
        List<Movie> movies = movieMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Movie m : movies) {
            result.add(toListItem(m));
        }

        String sortKey = (sort == null || sort.isBlank()) ? "positiveRate" : sort;
        switch (sortKey) {
            case "commentCount" -> result.sort(
                    Comparator.comparingInt((Map<String, Object> r) -> (int) r.get("commentCount")).reversed());
            case "title" -> result.sort(
                    Comparator.comparing(r -> (String) r.get("title")));
            default -> result.sort(
                    Comparator.comparingDouble((Map<String, Object> r) -> (double) r.get("positiveRate")).reversed());
        }
        return result;
    }

    private Map<String, Object> toListItem(Movie m) {
        Map<String, Object> item = new java.util.LinkedHashMap<>();
        item.put("id", m.getId());
        item.put("title", m.getTitle());
        item.put("year", m.getReleaseYear());
        item.put("director", m.getDirector());
        item.put("posterUrl", m.getPosterUrl());
        item.put("commentCount", m.getCommentCount() == null ? 0 : m.getCommentCount());
        item.put("positiveRate", positiveRate(m.getId()));
        return item;
    }

    /**
     * 好评率 = rating >= 4 的评论占比（真实星级，非模型预测）。
     */
    public double positiveRate(String movieId) {
        List<CommentEntity> comments = commentsOf(movieId);
        if (comments.isEmpty()) return 0.0;
        long positive = comments.stream()
                .filter(c -> c.getRating() != null && c.getRating() >= 4)
                .count();
        return (double) positive / comments.size();
    }

    /**
     * 情感分布（来自预计算的分析结果）。
     */
    public Map<String, Integer> sentimentDistribution(String movieId) {
        List<CommentEntity> comments = commentsOf(movieId);
        Map<String, Integer> dist = new java.util.LinkedHashMap<>();
        dist.put("positive", 0);
        dist.put("neutral", 0);
        dist.put("negative", 0);
        // 无分析结果时回退到真实星级映射，保证饼图不空
        for (CommentEntity c : comments) {
            String polarity = polarityOf(c);
            switch (polarity) {
                case "POSITIVE" -> dist.merge("positive", 1, Integer::sum);
                case "NEGATIVE" -> dist.merge("negative", 1, Integer::sum);
                default -> dist.merge("neutral", 1, Integer::sum);
            }
        }
        return dist;
    }

    /** 从真实星级推导极性（无分析结果时的回退）。 */
    private String polarityOf(CommentEntity c) {
        if (c.getRating() == null) return "NEUTRAL";
        if (c.getRating() >= 4) return "POSITIVE";
        if (c.getRating() == 3) return "NEUTRAL";
        return "NEGATIVE";
    }

    public List<CommentEntity> commentsOf(String movieId) {
        return commentMapper.selectList(
                new LambdaQueryWrapper<CommentEntity>().eq(CommentEntity::getMovieId, movieId));
    }

    public Movie findById(String id) {
        return movieMapper.selectById(id);
    }

    public long countMovies() {
        return movieMapper.selectCount(null);
    }

    public long countComments() {
        return commentMapper.selectCount(null);
    }
}

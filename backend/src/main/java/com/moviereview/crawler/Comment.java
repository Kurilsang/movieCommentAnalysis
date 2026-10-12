package com.moviereview.crawler;

/**
 * 评论数据对象。
 *
 * 字段对应 spec 中的评论表结构：原始文本、匿名化作者、时间、点赞数。
 */
public record Comment(
        String movieId,
        String text,
        String author,
        String publishTime,
        int likeCount
) {
    public Comment {
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException("movieId 不能为空");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text 不能为空");
        }
        if (author == null) {
            author = "匿名";
        }
        if (publishTime == null) {
            publishTime = "";
        }
    }
}

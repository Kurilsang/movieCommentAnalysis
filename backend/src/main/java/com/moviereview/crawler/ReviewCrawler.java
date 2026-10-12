package com.moviereview.crawler;

import java.util.List;

/**
 * 爬虫接口。
 *
 * 不同站点实现不同（豆瓣 / MTime / B站），但接口统一。
 * 爬虫管理模块通过此接口调度任务。
 */
public interface ReviewCrawler {

    /**
     * 爬取一部电影的评论。
     *
     * @param movieId 电影 ID
     * @param maxCount 最多爬取条数
     * @return 评论列表
     */
    List<Comment> crawl(String movieId, int maxCount);

    /** 爬虫名称（如 "douban"、"mtime"）。 */
    String name();

    /** 是否需要遵守 robots.txt 与限流。 */
    default boolean respectRobots() {
        return true;
    }
}

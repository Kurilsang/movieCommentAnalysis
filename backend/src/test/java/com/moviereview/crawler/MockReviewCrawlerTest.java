package com.moviereview.crawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mock 爬虫测试。
 */
class MockReviewCrawlerTest {

    private final List<Comment> presets = List.of(
            new Comment("m1", "好电影", "张三", "2024-01-01", 10),
            new Comment("m1", "不错", "李四", "2024-01-02", 5),
            new Comment("m2", "差", "王五", "2024-01-03", 2)
    );

    private final ReviewCrawler crawler = new MockReviewCrawler(presets);

    @Test
    @DisplayName("crawl - 按 movieId 过滤")
    void crawl_filtersByMovieId() {
        List<Comment> result = crawler.crawl("m1", 10);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.movieId().equals("m1")));
    }

    @Test
    @DisplayName("crawl - maxCount 限制条数")
    void crawl_respectsMaxCount() {
        List<Comment> result = crawler.crawl("m1", 1);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("crawl - 不存在的 movieId 返回空列表")
    void crawl_unknownMovie_returnsEmpty() {
        List<Comment> result = crawler.crawl("m999", 10);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("crawl - null movieId 抛 IllegalArgumentException")
    void crawl_nullMovieId_throws() {
        assertThrows(IllegalArgumentException.class, () -> crawler.crawl(null, 10));
    }

    @Test
    @DisplayName("crawl - 非正 maxCount 抛 IllegalArgumentException")
    void crawl_invalidMaxCount_throws() {
        assertThrows(IllegalArgumentException.class, () -> crawler.crawl("m1", 0));
    }

    @Test
    @DisplayName("name() - 返回 'mock'")
    void name_returnsMock() {
        assertEquals("mock", crawler.name());
    }

    @Test
    @DisplayName("respectRobots() - mock 爬虫不违反 robots")
    void respectRobots_true() {
        assertTrue(crawler.respectRobots());
    }
}

package com.moviereview.crawler;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 爬虫。
 *
 * **重要**：这是演示用的 mock，不发真实网络请求。
 *
 * spec 铁律：演示路径绝不依赖实时网络请求。
 * 真实影评数据开发阶段爬好落库（CSV/JSON 存入 data/），
 * 爬虫模块作为可触发功能存在，触发失败不影响演示。
 *
 * 此 mock 用于：
 * 1. 单元测试（不依赖网络）
 * 2. 演示时模拟爬取（避免现场被反爬）
 * 3. 爬虫管理模块的接口契约
 */
public class MockReviewCrawler implements ReviewCrawler {

    private final List<Comment> presetComments;

    public MockReviewCrawler(List<Comment> presetComments) {
        this.presetComments = presetComments == null ? List.of() : presetComments;
    }

    @Override
    public List<Comment> crawl(String movieId, int maxCount) {
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException("movieId 不能为空");
        }
        if (maxCount <= 0) {
            throw new IllegalArgumentException("maxCount 必须为正");
        }
        List<Comment> result = new ArrayList<>();
        for (Comment c : presetComments) {
            if (c.movieId().equals(movieId) && result.size() < maxCount) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean respectRobots() {
        return true; // mock 不发请求，不违反
    }
}

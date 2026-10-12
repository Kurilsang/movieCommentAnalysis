package com.moviereview.keyword;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TF-IDF 关键词提取器测试。
 */
class TfIdfKeywordExtractorTest {

    private final KeywordExtractor extractor = new TfIdfKeywordExtractor();

    @Test
    @DisplayName("extract - 从文本列表提取关键词")
    void extract_basic() {
        List<String> texts = List.of(
                "剧情 演技",
                "剧情 演技",
                "剧情 演技"
        );
        List<Keyword> keywords = extractor.extract(texts, 10);
        assertFalse(keywords.isEmpty(), "keywords 不应为空");
        // "剧情" 和 "演技" 应出现 3 次，是最热关键词
        assertTrue(keywords.stream().anyMatch(k -> k.word().equals("剧情")));
        assertTrue(keywords.stream().anyMatch(k -> k.word().equals("演技")));
    }

    @Test
    @DisplayName("extract - 停用词被过滤")
    void extract_filtersStopWords() {
        List<String> texts = List.of("的电影真的很好看");
        List<Keyword> keywords = extractor.extract(texts, 10);
        assertTrue(keywords.stream().noneMatch(k -> k.word().equals("的")));
        assertTrue(keywords.stream().noneMatch(k -> k.word().equals("电影")));
    }

    @Test
    @DisplayName("extract - topN 限制返回数量")
    void extract_topN() {
        List<String> texts = List.of("剧情 演技 画面 音乐 节奏 台词 特效 导演");
        List<Keyword> keywords = extractor.extract(texts, 3);
        assertTrue(keywords.size() <= 3);
    }

    @Test
    @DisplayName("extract - 空列表抛 IllegalArgumentException")
    void extract_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(List.of(), 5));
    }

    @Test
    @DisplayName("name - 返回 tfidf")
    void name_returnsTfidf() {
        assertEquals("tfidf", extractor.name());
    }
}

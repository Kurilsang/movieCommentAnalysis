package com.moviereview.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 电影列表与详情接口测试（01 + 02 号工单）。
 *
 * 测试接缝：HTTP API 边界。只断言响应 JSON 的字段和值，不测内部实现。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String M1 = "Mtest001";
    private static final String M2 = "Mtest002";

    @BeforeEach
    void seed() {
        commentMapper.delete(null);
        movieMapper.delete(null);

        insertMovie(M1, "测试电影A", 2020, "导演A");
        insertMovie(M2, "测试电影B", 2021, "导演B");

        // A: 3 条好评 + 1 条中评 => 好评率 0.75
        insertComment(M1, "非常好看", 5);
        insertComment(M1, "剧情紧凑", 5);
        insertComment(M1, "演技在线", 4);
        insertComment(M1, "还行吧", 3);

        // B: 1 条好评 + 3 条差评 => 好评率 0.25
        insertComment(M2, "还不错", 4);
        insertComment(M2, "太差了", 1);
        insertComment(M2, "浪费时间", 1);
        insertComment(M2, "很无聊", 2);
    }

    private void insertMovie(String id, String title, int year, String director) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle(title);
        m.setReleaseYear(year);
        m.setDirector(director);
        m.setCommentCount(0);
        m.setCreatedAt(LocalDateTime.now());
        movieMapper.insert(m);
    }

    private void insertComment(String movieId, String text, int rating) {
        CommentEntity c = new CommentEntity();
        c.setMovieId(movieId);
        c.setText(text);
        c.setAuthor("匿名");
        c.setLikeCount(0);
        c.setRating(rating);
        c.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(c);
    }

    // ========== 01 号工单：电影列表 ==========

    @Test
    @DisplayName("GET /api/movies - 返回 movies 数组，字段完整")
    void list_returnsMoviesWithFields() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies").isArray())
                .andExpect(jsonPath("$.movies.length()").value(2))
                .andExpect(jsonPath("$.movies[0].id").exists())
                .andExpect(jsonPath("$.movies[0].title").exists())
                .andExpect(jsonPath("$.movies[0].year").exists())
                .andExpect(jsonPath("$.movies[0].director").exists())
                .andExpect(jsonPath("$.movies[0].positiveRate").exists())
                .andExpect(jsonPath("$.movies[0].commentCount").exists());
    }

    @Test
    @DisplayName("GET /api/movies - 好评率来自真实星级（A=0.75, B=0.25）")
    void list_positiveRateFromRating() throws Exception {
        String json = mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var root = objectMapper.readTree(json);
        double rateA = -1, rateB = -1;
        for (var m : root.get("movies")) {
            if (m.get("id").asText().equals(M1)) rateA = m.get("positiveRate").asDouble();
            if (m.get("id").asText().equals(M2)) rateB = m.get("positiveRate").asDouble();
        }
        assertEquals(0.75, rateA, 1e-9, "A 电影好评率应为 3/4");
        assertEquals(0.25, rateB, 1e-9, "B 电影好评率应为 1/4");
    }

    @Test
    @DisplayName("GET /api/movies?sort=positiveRate - 按好评率降序")
    void list_sortByPositiveRate() throws Exception {
        mockMvc.perform(get("/api/movies").param("sort", "positiveRate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies[0].id").value(M1))
                .andExpect(jsonPath("$.movies[1].id").value(M2));
    }

    @Test
    @DisplayName("GET /api/movies?sort=commentCount - 按评论数降序")
    void list_sortByCommentCount() throws Exception {
        mockMvc.perform(get("/api/movies").param("sort", "commentCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies").isArray());
    }

    @Test
    @DisplayName("GET /api/movies?q= - 按片名搜索")
    void list_searchByTitle() throws Exception {
        mockMvc.perform(get("/api/movies").param("q", "电影A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies.length()").value(1))
                .andExpect(jsonPath("$.movies[0].id").value(M1));
    }

    @Test
    @DisplayName("GET /api/movies?q=不存在 - 返回空数组")
    void list_searchNoMatch_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/movies").param("q", "不存在的电影名"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies.length()").value(0));
    }

    // ========== 02 号工单：电影详情 ==========

    @Test
    @DisplayName("GET /api/movies/{id} - 返回 movie 对象含情感分布")
    void detail_returnsMovieWithDistribution() throws Exception {
        mockMvc.perform(get("/api/movies/" + M1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie.id").value(M1))
                .andExpect(jsonPath("$.movie.title").value("测试电影A"))
                .andExpect(jsonPath("$.movie.sentimentDistribution").exists())
                .andExpect(jsonPath("$.movie.sentimentDistribution.positive").exists())
                .andExpect(jsonPath("$.movie.sentimentDistribution.neutral").exists())
                .andExpect(jsonPath("$.movie.sentimentDistribution.negative").exists());
    }

    @Test
    @DisplayName("GET /api/movies/{id} - 情感分布三项之和 = 评论总数")
    void detail_distributionSumsToTotal() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var dist = objectMapper.readTree(json).get("movie").get("sentimentDistribution");
        int sum = dist.get("positive").asInt() + dist.get("neutral").asInt() + dist.get("negative").asInt();
        assertEquals(4, sum, "A 电影有 4 条评论，分布之和应为 4");
    }

    @Test
    @DisplayName("GET /api/movies/{id} - 不存在的电影返回 404")
    void detail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/movies/Mnotexist"))
                .andExpect(status().isNotFound());
    }
}

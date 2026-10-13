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
 * 方面雷达图与关键词接口测试（03 + 04 号工单）。
 *
 * 测试接缝：HTTP API 边界。
 * 关键断言：字段名必须是 sampleCount（前端读取的名字）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AspectApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MovieMapper movieMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private CommentAnalysisMapper analysisMapper;
    @Autowired private CommentAspectMapper aspectMapper;
    @Autowired private AnalysisService analysisService;
    @Autowired private ObjectMapper objectMapper;

    private static final String M1 = "Maspect01";

    @BeforeEach
    void seed() {
        aspectMapper.delete(null);
        analysisMapper.delete(null);
        commentMapper.delete(null);
        movieMapper.delete(null);

        Movie m = new Movie();
        m.setId(M1);
        m.setTitle("雷达图测试片");
        m.setCommentCount(0);
        m.setCreatedAt(LocalDateTime.now());
        movieMapper.insert(m);

        // 12 条评论提到"剧情"，其中 9 条正面 → sampleCount=12, 充分
        for (int i = 0; i < 9; i++) insertComment("剧情紧凑精彩好看");
        for (int i = 0; i < 3; i++) insertComment("剧情拖沓难看");

        // 2 条提到"演技" → sampleCount=2, 不充分（<10）
        insertComment("演技在线");
        insertComment("演技尴尬");

        // 重新计算每部电影评论数
        Long cnt = commentMapper.selectCount(null);
        m.setCommentCount(cnt == null ? 0 : cnt.intValue());
        movieMapper.updateById(m);

        // 预计算分析结果
        analysisService.rerunAll();
    }

    private void insertComment(String text) {
        CommentEntity c = new CommentEntity();
        c.setMovieId(M1);
        c.setText(text);
        c.setAuthor("匿名");
        c.setLikeCount(0);
        c.setRating(5);
        c.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(c);
    }

    /** 读取响应体为 UTF-8 字符串（MockMvc 默认 ISO-8859-1 会让中文 key 乱码）。 */
    private String utf8(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return new String(result.getResponse().getContentAsByteArray(),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    // ========== 03 号工单：雷达图 ==========

    @Test
    @DisplayName("GET /api/movies/{id}/analyze - 返回 aspects + 代表句")
    void analyze_returnsAspectsAndComments() throws Exception {
        mockMvc.perform(get("/api/movies/" + M1 + "/analyze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aspects").exists())
                .andExpect(jsonPath("$.positiveComments").isArray())
                .andExpect(jsonPath("$.negativeComments").isArray());
    }

    @Test
    @DisplayName("GET analyze - 字段名是 sampleCount（不是 mentionedCount）")
    void analyze_usesSampleCountFieldName() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1 + "/analyze"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        var aspects = objectMapper.readTree(json).get("aspects");
        var plot = aspects.get("剧情");
        assertNotNull(plot, "应含剧情维度");
        assertTrue(plot.has("sampleCount"), "字段名必须是 sampleCount（前端读取的名字）");
        assertFalse(plot.has("mentionedCount"), "不应出现 mentionedCount");
        assertTrue(plot.has("smoothedScore"), "应含 smoothedScore");
        assertTrue(plot.has("sufficientData"), "应含 sufficientData");
    }

    @Test
    @DisplayName("GET analyze - 只返回四个达标维度")
    void analyze_onlyFourAspects() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1 + "/analyze"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var aspects = objectMapper.readTree(json).get("aspects");
        assertEquals(4, aspects.size(), "应只返回四个达标维度");
        assertTrue(aspects.has("剧情"));
        assertTrue(aspects.has("演技"));
        assertTrue(aspects.has("画面特效"));
        assertTrue(aspects.has("配乐音效"));
        assertFalse(aspects.has("节奏"), "节奏覆盖率低于 5%，不应展示");
        assertFalse(aspects.has("台词"), "台词覆盖率低于 5%，不应展示");
    }

    @Test
    @DisplayName("GET analyze - 样本量正确（剧情 12 条）")
    void analyze_sampleCountCorrect() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1 + "/analyze"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var plot = objectMapper.readTree(json).get("aspects").get("剧情");
        assertEquals(12, plot.get("sampleCount").asInt(), "剧情应有 12 条提及");
    }

    @Test
    @DisplayName("GET analyze - 样本量不足时 sufficientData=false")
    void analyze_insufficientDataFlag() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1 + "/analyze"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var aspects = objectMapper.readTree(json).get("aspects");

        assertTrue(aspects.get("剧情").get("sufficientData").asBoolean(),
                "剧情 12 条 ≥ 10，应充分");
        assertFalse(aspects.get("演技").get("sufficientData").asBoolean(),
                "演技 2 条 < 10，应不充分（前端置灰）");
    }

    @Test
    @DisplayName("GET analyze - 贝叶斯平滑：小样本向 50 收缩")
    void analyze_bayesianSmoothing() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1 + "/analyze"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var acting = objectMapper.readTree(json).get("aspects").get("演技");
        // 演技 2 条，1 正 1 负 → 原始分 50，平滑后仍接近 50
        double smoothed = acting.get("smoothedScore").asDouble();
        assertTrue(smoothed > 40 && smoothed < 60,
                "小样本应向 50 收缩，实际：" + smoothed);
    }

    @Test
    @DisplayName("GET analyze - 代表句含 text/confidence/triggerWords")
    void analyze_representativeCommentsStructure() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1 + "/analyze"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var positive = objectMapper.readTree(json).get("positiveComments");
        assertTrue(positive.size() > 0, "应有正面代表句");
        var first = positive.get(0);
        assertTrue(first.has("text"), "代表句应含 text");
        assertTrue(first.has("confidence"), "代表句应含 confidence");
        assertTrue(first.has("triggerWords"), "代表句应含 triggerWords");
    }

    @Test
    @DisplayName("GET analyze - 不存在的电影返回 404")
    void analyze_notFound() throws Exception {
        mockMvc.perform(get("/api/movies/Mnotexist/analyze"))
                .andExpect(status().isNotFound());
    }

    // ========== 04 号工单：关键词 ==========

    @Test
    @DisplayName("GET /api/movies/{id}/keywords - 返回非空关键词列表")
    void keywords_returnsList() throws Exception {
        mockMvc.perform(get("/api/movies/" + M1 + "/keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords").isArray());
    }

    @Test
    @DisplayName("GET keywords - 按权重降序")
    void keywords_sortedByWeightDesc() throws Exception {
        String json = mockMvc.perform(get("/api/movies/" + M1 + "/keywords"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var kws = objectMapper.readTree(json).get("keywords");
        double prev = Double.MAX_VALUE;
        for (var k : kws) {
            double w = k.get("weight").asDouble();
            assertTrue(w <= prev, "关键词应按权重降序");
            prev = w;
        }
    }

    @Test
    @DisplayName("GET keywords - 不存在的电影返回 404")
    void keywords_notFound() throws Exception {
        mockMvc.perform(get("/api/movies/Mnotexist/keywords"))
                .andExpect(status().isNotFound());
    }
}

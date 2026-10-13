package com.moviereview.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 电影接口。
 *
 * 路径与前端 api.js 严格对齐。
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final AnalysisService analysisService;

    /**
     * 电影列表。
     */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String sort,
                                  @RequestParam(required = false) String q) {
        return ResponseEntity.ok(Map.of("movies", movieService.list(sort, q)));
    }

    /**
     * 电影详情（含情感分布）。
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        Movie movie = movieService.findById(id);
        if (movie == null) {
            return ResponseEntity.status(404).body(Map.of("error", "电影不存在"));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", movie.getId());
        m.put("title", movie.getTitle());
        m.put("director", movie.getDirector());
        m.put("actors", movie.getActors());
        m.put("year", movie.getReleaseYear());
        m.put("posterUrl", movie.getPosterUrl());
        m.put("synopsis", movie.getSynopsis());
        m.put("sentimentDistribution", movieService.sentimentDistribution(id));
        return ResponseEntity.ok(Map.of("movie", m));
    }

    /**
     * 方面雷达图 + 好评差评代表句（03 号工单）。
     *
     * 注意：字段名是 sampleCount（前端读取的名字），不是 mentionedCount。
     */
    @GetMapping("/{id}/analyze")
    public ResponseEntity<?> analyze(@PathVariable String id) {
        if (movieService.findById(id) == null) {
            return ResponseEntity.status(404).body(Map.of("error", "电影不存在"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("aspects", analysisService.movieAspects(id));
        body.put("positiveComments", analysisService.representativeComments(id, "POSITIVE", 10));
        body.put("negativeComments", analysisService.representativeComments(id, "NEGATIVE", 10));
        return ResponseEntity.ok(body);
    }

    /**
     * 关键词（04 号工单）。
     */
    @GetMapping("/{id}/keywords")
    public ResponseEntity<?> keywords(@PathVariable String id) {
        if (movieService.findById(id) == null) {
            return ResponseEntity.status(404).body(Map.of("error", "电影不存在"));
        }
        return ResponseEntity.ok(Map.of("keywords", analysisService.keywords(id, 30)));
    }
}

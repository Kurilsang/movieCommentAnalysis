package com.moviereview.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理后台接口（06 + 07 号工单）。
 *
 * 路径与前端 api.js 严格对齐，受 SecurityConfig 的 /api/admin/** 保护（需 ADMIN 角色）。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CrawlerTaskService crawlerTaskService;
    private final AnalysisService analysisService;
    private final MovieService movieService;

    // ========== 06 号工单：爬虫任务 ==========

    @GetMapping("/crawler/tasks")
    public ResponseEntity<?> tasks() {
        return ResponseEntity.ok(Map.of("tasks", crawlerTaskService.list()));
    }

    @PostMapping("/crawler/trigger")
    public ResponseEntity<?> trigger(@RequestBody Map<String, Object> body) {
        try {
            String movieId = body.get("movieId") == null ? null : String.valueOf(body.get("movieId"));
            int maxCount = body.get("maxCount") == null ? 100
                    : Integer.parseInt(String.valueOf(body.get("maxCount")));
            Long taskId = crawlerTaskService.trigger(movieId, maxCount);
            return ResponseEntity.ok(Map.of("taskId", taskId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== 07 号工单：一键重跑 + 数据总览 ==========

    @PostMapping("/analysis/rerun-all")
    public ResponseEntity<?> rerunAll() {
        int count = analysisService.rerunAll();
        return ResponseEntity.ok(Map.of(
                "message", "重跑完成",
                "count", count
        ));
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalMovies", movieService.countMovies());
        m.put("totalComments", movieService.countComments());
        m.put("analyzedCount", analysisService.countAnalyzed());
        m.put("modelCount", 1);
        return ResponseEntity.ok(m);
    }
}

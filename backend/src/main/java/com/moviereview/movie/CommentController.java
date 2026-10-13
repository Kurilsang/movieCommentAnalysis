package com.moviereview.movie;

import com.moviereview.aspect.AspectAnalyzer;
import com.moviereview.aspect.AspectResult;
import com.moviereview.aspect.RuleBasedAspectAnalyzer;
import com.moviereview.sentiment.AnalysisResult;
import com.moviereview.sentiment.L0RuleSentimentAnalyzer;
import com.moviereview.sentiment.SentimentAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 评论接口（05 号工单）。
 *
 * 提交评论 → 实时分析 → 落库。
 * 实时执行（不走预计算），这是答辩现场唯一有交互冲击力的环节。
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentMapper commentMapper;
    private final AnalysisService analysisService;

    private final SentimentAnalyzer analyzer = new L0RuleSentimentAnalyzer();
    private final AspectAnalyzer aspectAnalyzer = new RuleBasedAspectAnalyzer(new L0RuleSentimentAnalyzer());

    /** 异步结果暂存（L1 预留；内存实现，P0 够用）。 */
    private final Map<String, Object> asyncResults = new ConcurrentHashMap<>();

    /**
     * 提交评论并实时分析。
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody Map<String, Object> body) {
        String text = body.get("text") == null ? null : String.valueOf(body.get("text"));
        String movieId = body.get("movieId") == null ? null : String.valueOf(body.get("movieId"));

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "评论文本不能为空"));
        }
        if (text.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "评论不能超过 500 字"));
        }

        // 实时分析
        AnalysisResult result = analyzer.analyze(text);

        // 落库（有 movieId 时才关联）
        if (movieId != null && !movieId.isBlank()) {
            CommentEntity c = new CommentEntity();
            c.setMovieId(movieId);
            c.setText(text);
            c.setAuthor(currentUsername());
            c.setLikeCount(0);
            c.setCreatedAt(LocalDateTime.now());
            commentMapper.insert(c);
            analysisService.analyzeAndSave(c);
        }

        String taskId = UUID.randomUUID().toString();
        Map<String, Object> payload = toPayload(result, text);
        asyncResults.put(taskId, payload);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", payload);
        response.put("taskId", taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询异步结果（L1 预留）。
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<?> result(@PathVariable String taskId) {
        Object cached = asyncResults.get(taskId);
        if (cached == null) {
            return ResponseEntity.ok(Map.of("result", Map.of()));
        }
        return ResponseEntity.ok(Map.of("result", cached));
    }

    private Map<String, Object> toPayload(AnalysisResult result, String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", text);
        payload.put("polarity", result.polarity().name());
        payload.put("confidence", result.confidence());
        payload.put("triggerWords", result.triggerWords());
        payload.put("analyzerName", analyzer.name());

        Map<String, Object> aspects = new LinkedHashMap<>();
        for (Map.Entry<String, AspectResult> e : aspectAnalyzer.analyze(text).entrySet()) {
            AspectResult ar = e.getValue();
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("mentioned", ar.mentioned());
            a.put("polarity", ar.polarity().name());
            a.put("confidence", ar.confidence());
            a.put("sampleCount", ar.sampleCount());
            a.put("triggerWords", ar.triggerWords());
            aspects.put(e.getKey(), a);
        }
        payload.put("aspects", aspects);
        return payload;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return "匿名";
        Object p = auth.getPrincipal();
        return p instanceof Long ? "用户" + p : String.valueOf(p);
    }
}

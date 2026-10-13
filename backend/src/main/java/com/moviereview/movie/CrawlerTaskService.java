package com.moviereview.movie;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 爬虫任务服务。
 *
 * 演示铁律：触发不发真实网络请求（用 MockReviewCrawler）。
 * 任务只落库记录，保证答辩现场不因反爬而翻车。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerTaskService {

    private final CrawlerTaskMapper taskMapper;
    private final MovieMapper movieMapper;

    /**
     * 触发爬虫任务（只创建记录，不发网络请求）。
     */
    public Long trigger(String movieId, int maxCount) {
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException("电影ID不能为空");
        }
        if (maxCount <= 0) {
            throw new IllegalArgumentException("抓取数量必须为正");
        }
        CrawlerTask task = new CrawlerTask();
        task.setMovieId(movieId);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setMessage("任务已创建（演示模式：不发起真实网络请求）");
        task.setStartedAt(LocalDateTime.now());
        taskMapper.insert(task);
        log.info("爬虫任务已创建：id={}, movieId={}, maxCount={}", task.getId(), movieId, maxCount);
        return task.getId();
    }

    /**
     * 任务列表（按创建时间倒序）。
     */
    public List<Map<String, Object>> list() {
        List<CrawlerTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<CrawlerTask>().orderByDesc(CrawlerTask::getId));
        return tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("movieId", t.getMovieId());
            m.put("status", statusLabel(t.getStatus()));
            m.put("progress", t.getProgress() == null ? 0 : t.getProgress());
            m.put("message", t.getMessage());
            return m;
        }).collect(Collectors.toList());
    }

    /** 状态中文标签（前端直接显示）。 */
    private String statusLabel(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING" -> "待执行";
            case "RUNNING" -> "进行中";
            case "SUCCESS" -> "完成";
            case "FAILED" -> "失败";
            default -> status;
        };
    }
}

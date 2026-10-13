package com.moviereview.movie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时预计算全库分析结果。
 *
 * 幂等：已有分析结果则跳过（重跑请用 /api/admin/analysis/rerun-all）。
 * 在 CsvDataImporter 之后执行（@Order）。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PrecomputeRunner implements ApplicationRunner {

    private final AnalysisService analysisService;
    private final CommentMapper commentMapper;

    @Value("${app.analysis.precompute-on-startup:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("启动预计算已禁用");
            return;
        }
        try {
            long analyzed = analysisService.countAnalyzed();
            long total = commentMapper.selectCount(null);
            if (total == 0) {
                log.info("无评论数据，跳过预计算");
                return;
            }
            if (analyzed >= total) {
                log.info("分析结果已存在（{}/{}），跳过预计算", analyzed, total);
                return;
            }
            log.info("开始预计算 {} 条评论的分析结果...", total);
            long start = System.currentTimeMillis();
            int count = analysisService.rerunAll();
            log.info("预计算完成：{} 条，耗时 {} ms", count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("预计算失败（不影响启动）：{}", e.getMessage());
        }
    }
}

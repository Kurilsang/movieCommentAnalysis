package com.moviereview.aspect;

import java.util.List;
import java.util.Map;

/**
 * 方面级情感分析器。
 *
 * spec 创新点 2：面向电影领域构建六维评价体系，实现细粒度口碑分析。
 *
 * 核心流水线：分句（转折连词处切分）→ 方面锚定 → 子句级情感分类 → 贝叶斯平滑聚合
 *
 * spec 红线：
 * 1. 必须在转折连词处切分子句（但是/不过/然而/却）
 * 2. 只在锚点显式出现时才报告该方面（接受低召回、保住高精度）
 * 3. 要求锚点词与观点词同处一个子句才计分
 */
public interface AspectAnalyzer {

    /**
     * 分析一句话的方面级情感。
     *
     * @param text 中文影评
     * @return 六维方面结果
     */
    Map<String, AspectResult> analyze(String text);

    /**
     * 分析一批评论，聚合到电影级别（贝叶斯平滑）。
     *
     * @param texts 评论列表
     * @return 电影级方面聚合结果
     */
    Map<String, AspectAggregate> aggregate(List<String> texts);

    /** 六维方面名称列表。 */
    List<String> aspectNames();
}

package com.moviereview.demo;

import com.moviereview.aspect.AspectAggregate;
import com.moviereview.aspect.AspectAnalyzer;
import com.moviereview.aspect.RuleBasedAspectAnalyzer;
import com.moviereview.keyword.Keyword;
import com.moviereview.keyword.TfIdfKeywordExtractor;
import com.moviereview.sentiment.AnalysisResult;
import com.moviereview.sentiment.L0RuleSentimentAnalyzer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 端到端演示：用真实豆瓣影评数据跑完整 AI 流水线。
 *
 * 用法（在 backend 目录）：
 *   java -cp "target/classes" com.moviereview.demo.PipelineDemo ../data/comments.csv
 */
public class PipelineDemo {

    public static void main(String[] args) throws Exception {
        Path csv = Path.of(args.length > 0 ? args[0] : "../data/comments.csv");
        System.out.println("读取数据: " + csv.toAbsolutePath());

        List<String> texts = new ArrayList<>();
        List<String> movieIds = new ArrayList<>();
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {  // 跳过表头
            String line = lines.get(i);
            if (line.isBlank()) continue;
            String[] parts = line.split(",", 7);
            if (parts.length < 2) continue;
            movieIds.add(parts[0]);
            texts.add(parts[1].replace("\"", ""));
        }
        System.out.printf("加载 %d 条评论%n%n", texts.size());

        // === 1. L0 规则引擎（文档级三分类）===
        L0RuleSentimentAnalyzer l0 = new L0RuleSentimentAnalyzer();
        int pos = 0, neu = 0, neg = 0;
        for (String t : texts) {
            switch (l0.analyze(t).polarity()) {
                case POSITIVE -> pos++;
                case NEUTRAL -> neu++;
                case NEGATIVE -> neg++;
            }
        }
        System.out.println("=== 1. L0 规则引擎：全库情感分布 ===");
        System.out.printf("  正面 %d (%.1f%%) / 中性 %d (%.1f%%) / 负面 %d (%.1f%%)%n%n",
                pos, 100.0 * pos / texts.size(),
                neu, 100.0 * neu / texts.size(),
                neg, 100.0 * neg / texts.size());

        // === 2. 单条评论的完整分析（可解释性）===
        System.out.println("=== 2. 单条评论分析（含触发词）===");
        String sample = "剧情紧凑，演技在线，但是画面特效有点粗糙";
        AnalysisResult r = l0.analyze(sample);
        System.out.println("  输入: " + sample);
        System.out.println("  极性: " + r.polarity() + "  置信度: " + String.format("%.2f", r.confidence()));
        System.out.println("  触发词: " + r.triggerWords());
        System.out.println();

        // === 3. 方面级分析（六维雷达图数据）===
        AspectAnalyzer aspectAnalyzer = new RuleBasedAspectAnalyzer(l0);
        System.out.println("=== 3. 方面级分析：六维雷达图数据 ===");
        // 用一部电影的全部评论聚合
        String targetMovie = movieIds.get(0);
        List<String> movieTexts = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            if (movieIds.get(i).equals(targetMovie)) movieTexts.add(texts.get(i));
        }
        Map<String, AspectAggregate> aggs = aspectAnalyzer.aggregate(movieTexts);
        System.out.println("  电影 " + targetMovie + "（" + movieTexts.size() + " 条评论）");
        System.out.printf("  %-10s %8s %8s %10s %8s%n", "维度", "提及数", "原始分", "平滑分", "可信");
        for (var e : aggs.entrySet()) {
            AspectAggregate a = e.getValue();
            System.out.printf("  %-10s %8d %8.1f %10.1f %8s%n",
                    e.getKey(), a.mentionedCount(), a.rawScore(), a.smoothedScore(),
                    a.sufficientData() ? "是" : "否(置灰)");
        }
        System.out.println();

        // === 4. 关键词提取 ===
        System.out.println("=== 4. 关键词提取（词云数据）===");
        List<Keyword> keywords = new TfIdfKeywordExtractor().extract(movieTexts, 15);
        for (Keyword k : keywords) {
            System.out.printf("  %-12s 权重 %.2f  出现 %d 次%n", k.word(), k.weight(), k.count());
        }
    }
}

package com.moviereview.demo;

import com.moviereview.evaluation.ClassificationMetrics;
import com.moviereview.sentiment.L0RuleSentimentAnalyzer;
import com.moviereview.sentiment.SentimentAnalyzer;
import com.moviereview.sentiment.SentimentPolarity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 用真实星级评分（1-5星）当金标准，评测 L0 规则引擎。
 *
 * 三分类映射：4-5星=正面，3星=中性，1-2星=负面。
 * 这是论文实验章节的基线数字。
 */
public class BaselineEval {

    public static void main(String[] args) throws Exception {
        Path csv = Path.of(args.length > 0 ? args[0] : "../data/comments.csv");
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);

        List<String> texts = new ArrayList<>();
        List<SentimentPolarity> gold = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            // 格式: movieId,text,author,publishTime,likeCount,rating
            int lastComma = line.lastIndexOf(',');
            if (lastComma < 0) continue;
            String ratingStr = line.substring(lastComma + 1).trim();
            if (!ratingStr.matches("[1-5]")) continue;
            int rating = Integer.parseInt(ratingStr);

            String rest = line.substring(0, lastComma);
            int firstComma = rest.indexOf(',');
            int secondComma = rest.indexOf(',', firstComma + 1);
            if (secondComma < 0) continue;
            String text = rest.substring(firstComma + 1, secondComma).replace("\"", "");

            texts.add(text);
            gold.add(rating >= 4 ? SentimentPolarity.POSITIVE
                    : rating == 3 ? SentimentPolarity.NEUTRAL
                    : SentimentPolarity.NEGATIVE);
        }

        System.out.printf("评测集: %d 条（金标准来自真实星级）%n%n", texts.size());

        // 金标准分布
        int gp = 0, gn = 0, gg = 0;
        for (SentimentPolarity p : gold) {
            switch (p) {
                case POSITIVE -> gp++;
                case NEUTRAL -> gn++;
                case NEGATIVE -> gg++;
            }
        }
        System.out.println("=== 金标准分布（真实星级映射）===");
        System.out.printf("  正面 %.1f%% / 中性 %.1f%% / 负面 %.1f%%%n%n",
                100.0 * gp / gold.size(), 100.0 * gn / gold.size(), 100.0 * gg / gold.size());

        // 评测 L0
        SentimentAnalyzer l0 = new L0RuleSentimentAnalyzer();
        ClassificationMetrics m = new ClassificationMetrics();
        int pp = 0, pn = 0, pg = 0;
        for (int i = 0; i < texts.size(); i++) {
            SentimentPolarity pred = l0.analyze(texts.get(i)).polarity();
            m.record(gold.get(i), pred);
            switch (pred) {
                case POSITIVE -> pp++;
                case NEUTRAL -> pn++;
                case NEGATIVE -> pg++;
            }
        }

        System.out.println("=== L0 规则引擎预测分布 ===");
        System.out.printf("  正面 %.1f%% / 中性 %.1f%% / 负面 %.1f%%%n%n",
                100.0 * pp / texts.size(), 100.0 * pn / texts.size(), 100.0 * pg / texts.size());

        System.out.println("=== L0 基线指标 ===");
        System.out.printf("  Accuracy    %.4f%n", m.accuracy());
        System.out.printf("  Macro-F1    %.4f%n", m.macroF1());
        System.out.printf("  Macro-P     %.4f%n", m.macroPrecision());
        System.out.printf("  Macro-R     %.4f%n", m.macroRecall());
        System.out.println();
        System.out.println("=== 关键诊断 ===");
        System.out.println("  金标准中性占比 " + String.format("%.1f%%", 100.0 * gn / gold.size())
                + " vs L0 预测中性占比 " + String.format("%.1f%%", 100.0 * pn / texts.size()));
        System.out.println("  → L0 把大量真实好评误判为中性（词典覆盖不足）");
    }
}

package com.moviereview.demo;

import com.moviereview.aspect.AspectAggregate;
import com.moviereview.aspect.RuleBasedAspectAnalyzer;
import com.moviereview.sentiment.L0RuleSentimentAnalyzer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 六维覆盖率统计。
 *
 * ticket 07 要求："先做覆盖率统计，覆盖率 < 5% 的维度换掉"。
 */
public class CoverageStats {

    public static void main(String[] args) throws Exception {
        Path csv = Path.of(args.length > 0 ? args[0] : "../data/comments.csv");
        List<String> texts = new ArrayList<>();
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            String[] parts = line.split(",", 7);
            if (parts.length >= 2) texts.add(parts[1].replace("\"", ""));
        }

        RuleBasedAspectAnalyzer analyzer = new RuleBasedAspectAnalyzer(new L0RuleSentimentAnalyzer());
        Map<String, Integer> mentioned = new LinkedHashMap<>();
        for (String name : analyzer.aspectNames()) mentioned.put(name, 0);

        for (String t : texts) {
            for (var e : analyzer.analyze(t).entrySet()) {
                if (e.getValue().mentioned()) {
                    mentioned.merge(e.getKey(), 1, Integer::sum);
                }
            }
        }

        System.out.printf("全库 %d 条评论的六维覆盖率：%n%n", texts.size());
        System.out.printf("%-10s %10s %10s%n", "维度", "提及数", "覆盖率");
        for (var e : mentioned.entrySet()) {
            double rate = 100.0 * e.getValue() / texts.size();
            String flag = rate < 5.0 ? "  <-- 低于 5%，建议替换" : "";
            System.out.printf("%-10s %10d %9.2f%%%s%n", e.getKey(), e.getValue(), rate, flag);
        }
    }
}

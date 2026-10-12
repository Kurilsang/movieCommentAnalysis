package com.moviereview.aspect;

import com.moviereview.sentiment.L0RuleSentimentAnalyzer;
import com.moviereview.sentiment.SentimentAnalyzer;
import com.moviereview.sentiment.SentimentPolarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于规则的方面级情感分析器。
 *
 * 流水线（spec 创新点 2）：
 * 1. 转折连词处切分子句（但是/不过/然而/却/可是/只是）
 * 2. 方面锚定（六维同义词匹配）
 * 3. 子句级情感分类（复用 SentimentAnalyzer，L0/L2）
 * 4. 只在锚点显式出现时才报告该方面（接受低召回、保住高精度）
 *
 * spec 红线：
 * - 要求锚点词与观点词同处一个子句才计分
 * - "画面很美但是剧情很烂" → 画面=POSITIVE, 剧情=NEGATIVE（转折句必须切分）
 */
public class RuleBasedAspectAnalyzer implements AspectAnalyzer {

    /** 六维方面词典（同义词扩展）。 */
    private static final Map<String, List<String>> ASPECT_LEXICON = Map.of(
            "剧情", List.of("剧情", "故事", "剧本", "情节"),
            "演技", List.of("演技", "表演", "演得", "演员", "主演", "角色"),
            "画面特效", List.of("画面", "特效", "视觉", "摄影", "镜头", "画质"),
            "配乐音效", List.of("音乐", "配乐", "音效", "BGM", "背景音乐", "原声"),
            "节奏", List.of("节奏", "拖沓", "紧凑", "流畅", "冗长", "剪辑"),
            "台词", List.of("台词", "对白", "独白", "对话")
    );

    /** 转折连词（用于切分子句）。 */
    private static final Set<String> CONTRASTIVE_MARKERS = Set.of(
            "但是", "不过", "然而", "却", "可是", "只是", "虽然", "尽管"
    );

    private final SentimentAnalyzer sentenceAnalyzer;

    /**
     * @param sentenceAnalyzer 子句级情感分析器（L0 或 L2）
     */
    public RuleBasedAspectAnalyzer(SentimentAnalyzer sentenceAnalyzer) {
        if (sentenceAnalyzer == null) {
            throw new IllegalArgumentException("sentenceAnalyzer 不能为空");
        }
        this.sentenceAnalyzer = sentenceAnalyzer;
    }

    /** 默认用 L0 规则引擎做子句级分类。 */
    public RuleBasedAspectAnalyzer() {
        this(new L0RuleSentimentAnalyzer());
    }

    @Override
    public Map<String, AspectResult> analyze(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本不能为空");
        }

        // 第 1 步：转折连词处切分子句
        List<String> clauses = splitClauses(text.trim());

        // 第 2 步：逐子句做方面锚定 + 情感分类
        Map<String, List<SentimentPolarity>> aspectPolarities = new LinkedHashMap<>();
        for (String aspectName : aspectNames()) {
            aspectPolarities.put(aspectName, new ArrayList<>());
        }

        for (String clause : clauses) {
            // 找出这个子句提到了哪些方面
            Set<String> mentionedAspects = findMentionedAspects(clause);
            if (mentionedAspects.isEmpty()) {
                continue;
            }
            // 对这个子句做情感分类
            SentimentPolarity clausePolarity = sentenceAnalyzer.analyze(clause).polarity();
            // 把这个子句的情感分给所有提到的方面（锚点+观点同句）
            for (String aspectName : mentionedAspects) {
                aspectPolarities.get(aspectName).add(clausePolarity);
            }
        }

        // 第 3 步：聚合
        Map<String, AspectResult> results = new LinkedHashMap<>();
        for (Map.Entry<String, List<SentimentPolarity>> entry : aspectPolarities.entrySet()) {
            String aspectName = entry.getKey();
            List<SentimentPolarity> polarities = entry.getValue();

            if (polarities.isEmpty()) {
                results.put(aspectName, AspectResult.unmentioned(aspectName));
                continue;
            }

            PolarityCounts counts = new PolarityCounts();
            for (SentimentPolarity p : polarities) {
                counts.increment(p);
            }
            results.put(aspectName, AspectResult.of(aspectName,
                    counts.dominant(), counts.dominantRatio(), counts.total()));
        }
        return results;
    }

    @Override
    public Map<String, AspectAggregate> aggregate(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts 不能为空");
        }

        Map<String, PolarityCounts> counters = new LinkedHashMap<>();
        for (String aspectName : aspectNames()) {
            counters.put(aspectName, new PolarityCounts());
        }

        for (String text : texts) {
            Map<String, AspectResult> results = analyze(text);
            for (Map.Entry<String, AspectResult> entry : results.entrySet()) {
                AspectResult r = entry.getValue();
                if (!r.mentioned()) continue;
                counters.get(entry.getKey()).increment(r.polarity());
            }
        }

        Map<String, AspectAggregate> aggregates = new LinkedHashMap<>();
        for (Map.Entry<String, PolarityCounts> entry : counters.entrySet()) {
            String aspectName = entry.getKey();
            PolarityCounts c = entry.getValue();
            int pos = c.positive(), neu = c.neutral(), neg = c.negative(), total = c.total();
            double raw = AspectAggregate.rawScore(pos, total);
            double smoothed = AspectAggregate.smoothedScore(pos, total);
            boolean sufficient = AspectAggregate.sufficientData(total);
            aggregates.put(aspectName,
                    new AspectAggregate(aspectName, total, pos, neu, neg, raw, smoothed, sufficient));
        }
        return aggregates;
    }

    @Override
    public List<String> aspectNames() {
        return List.of("剧情", "演技", "画面特效", "配乐音效", "节奏", "台词");
    }

    /**
     * 转折连词处切分子句。
     *
     * "画面很美但是剧情很烂" → ["画面很美", "剧情很烂"]
     */
    private List<String> splitClauses(String text) {
        List<String> clauses = new ArrayList<>();
        for (String marker : CONTRASTIVE_MARKERS) {
            if (text.contains(marker)) {
                String[] parts = text.split(marker);
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        clauses.add(trimmed);
                    }
                }
                break; // 只用第一个匹配的转折连词切分
            }
        }
        if (clauses.isEmpty()) {
            clauses.add(text);
        }
        return clauses;
    }

    /**
     * 找出一个子句提到了哪些方面。
     * 只在锚点显式出现时才报告该方面（接受低召回、保住高精度）。
     */
    private Set<String> findMentionedAspects(String clause) {
        Set<String> mentioned = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : ASPECT_LEXICON.entrySet()) {
            for (String word : entry.getValue()) {
                if (clause.contains(word)) {
                    mentioned.add(entry.getKey());
                    break;
                }
            }
        }
        return mentioned;
    }
}

package com.moviereview.sentiment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L0 规则情感引擎。
 *
 * 情感词典 + 否定词 + 程度副词 + 自建影视领域词典。论文对比基线。
 *
 * 算法（清晰版）：
 * 1. 扫描文本，分别累计正面词、负面词、中性词的命中次数
 * 2. 扫描否定词（任一命中即视为含否定）
 * 3. 扫描程度副词，取最大权重倍率
 * 4. 若含否定词，翻转正/负得分
 * 5. 按正/负得分判定极性，程度副词只加权置信度（不翻转）
 * 6. 无任何情感信号时判为中性
 */
public class L0RuleSentimentAnalyzer implements SentimentAnalyzer {

    /** 正面情感词（含影视领域词）。 */
    private static final Set<String> POSITIVE_WORDS = Set.of(
            "好看", "精彩", "出色", "优秀", "感人", "震撼", "完美", "惊艳", "在线", "值得一看",
            "演技在线", "剧情紧凑", "画面精美", "配乐动人", "节奏流畅", "好"
    );

    /** 负面情感词（含影视领域词）。 */
    private static final Set<String> NEGATIVE_WORDS = Set.of(
            "难看", "糟糕", "拖沓", "无聊", "尴尬", "烂", "失望", "浮夸", "降智", "稀碎",
            "剧情拖沓", "演技尴尬", "画面粗糙", "配乐突兀", "节奏稀碎", "五毛特效", "面瘫"
    );

    /** 中性标记词。 */
    private static final Set<String> NEUTRAL_WORDS = Set.of(
            "还行", "一般", "凑合", "普通", "中规中矩", "过得去"
    );

    /** 否定词。注意：不能用单字 "非"，因为它会误中 "非常"。 */
    private static final Set<String> NEGATION_WORDS = Set.of(
            "不", "没", "无", "未", "别", "莫", "勿", "未必", "不够", "不算",
            "不是", "没有", "无法", "并不"
    );

    /** 程度副词及其权重倍率。 */
    private static final Map<String, Double> INTENSIFIERS = Map.of(
            "非常", 1.5,
            "极其", 1.6,
            "特别", 1.4,
            "十分", 1.4,
            "相当", 1.3,
            "太", 1.4,
            "很", 1.3,
            "有点", 0.7,
            "稍微", 0.6
    );

    @Override
    public AnalysisResult analyze(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("评论文本不能为空");
        }

        String trimmed = text.trim();
        List<String> triggerWords = new ArrayList<>();

        double positiveScore = 0.0;
        double negativeScore = 0.0;
        boolean hasNeutralMarker = false;

        // 正面词命中
        for (String word : POSITIVE_WORDS) {
            if (trimmed.contains(word)) {
                triggerWords.add(word);
                positiveScore += 1.0;
            }
        }
        // 负面词命中
        for (String word : NEGATIVE_WORDS) {
            if (trimmed.contains(word)) {
                triggerWords.add(word);
                negativeScore += 1.0;
            }
        }
        // 中性词命中
        for (String word : NEUTRAL_WORDS) {
            if (trimmed.contains(word)) {
                triggerWords.add(word);
                hasNeutralMarker = true;
            }
        }
        // 否定词
        boolean hasNegation = false;
        for (String word : NEGATION_WORDS) {
            if (trimmed.contains(word)) {
                hasNegation = true;
                break;
            }
        }
        // 程度副词（取最大权重）
        double intensifierMultiplier = 1.0;
        for (Map.Entry<String, Double> entry : INTENSIFIERS.entrySet()) {
            if (trimmed.contains(entry.getKey())) {
                intensifierMultiplier = Math.max(intensifierMultiplier, entry.getValue());
            }
        }

        // 局部否定：只翻转否定词附近的极性，不全局翻转
        // 解决 spec 发现的 bug："不推荐，但是演技在线剧情紧凑" 不应被整体翻转为负面
        if (hasNegation) {
            // 把文本按否定词切分，只在否定词后 6 字窗口内翻转极性
            double[] scores = applyLocalNegation(trimmed, positiveScore, negativeScore);
            positiveScore = scores[0];
            negativeScore = scores[1];
        }

        SentimentPolarity polarity;
        double confidence;

        if (positiveScore == 0.0 && negativeScore == 0.0) {
            // 无任何情感信号 → 中性
            polarity = SentimentPolarity.NEUTRAL;
            confidence = 0.5;
        } else if (positiveScore > negativeScore) {
            polarity = SentimentPolarity.POSITIVE;
            confidence = Math.min(1.0, 0.5 + 0.1 * positiveScore * intensifierMultiplier);
        } else if (negativeScore > positiveScore) {
            polarity = SentimentPolarity.NEGATIVE;
            confidence = Math.min(1.0, 0.5 + 0.1 * negativeScore * intensifierMultiplier);
        } else {
            // 正负相等 → 中性
            polarity = SentimentPolarity.NEUTRAL;
            confidence = 0.5;
        }

        // 六维方面分数：扫描方面词，命中则根据窗口内情感词判定该维度极性
        Map<String, AnalysisResult.AspectScore> aspects = extractAspects(trimmed, triggerWords);

        String rawJson = "{\"polarity\":\"" + polarity + "\",\"confidence\":" + confidence
                + ",\"triggerWords\":" + triggerWords + ",\"analyzer\":\"L0-rule\"}";

        return new AnalysisResult(polarity, confidence, triggerWords, aspects, rawJson);
    }

    /**
     * 局部否定：在否定词后 6 字窗口内，把正面词命中的得分移到负面（反之亦然）。
     * 返回 [positiveScore, negativeScore]。
     */
    private double[] applyLocalNegation(String text, double positiveScore, double negativeScore) {
        double pos = positiveScore, neg = negativeScore;
        for (String negWord : NEGATION_WORDS) {
            int idx = text.indexOf(negWord);
            while (idx >= 0) {
                int windowStart = idx + negWord.length();
                int windowEnd = Math.min(text.length(), windowStart + 6);
                String window = text.substring(windowStart, windowEnd);
                // 窗口内的正面词，命中一次就从 pos 移到 neg
                for (String pw : POSITIVE_WORDS) {
                    if (window.contains(pw)) {
                        pos -= 1.0;
                        neg += 1.0;
                    }
                }
                // 窗口内的负面词，命中一次就从 neg 移到 pos
                for (String nw : NEGATIVE_WORDS) {
                    if (window.contains(nw)) {
                        neg -= 1.0;
                        pos += 1.0;
                    }
                }
                idx = text.indexOf(negWord, idx + 1);
            }
        }
        return new double[]{Math.max(0, pos), Math.max(0, neg)};
    }

    /** 六维方面词表。 */
    private static final Map<String, String> ASPECT_WORDS = Map.of(
            "剧情", "剧情",
            "演技", "演技",
            "画面特效", "画面",
            "配乐音效", "音乐",
            "节奏", "节奏",
            "台词", "台词"
    );

    /** 六维方面词的同义词/扩展。 */
    private static final Map<String, List<String>> ASPECT_SYNONYMS = Map.of(
            "剧情", List.of("剧情", "故事", "剧本"),
            "演技", List.of("演技", "表演", "演得", "演员"),
            "画面特效", List.of("画面", "特效", "视觉", "摄影"),
            "配乐音效", List.of("音乐", "配乐", "音效", "BGM"),
            "节奏", List.of("节奏", "拖沓", "紧凑", "流畅"),
            "台词", List.of("台词", "对白", "独白")
    );

    /**
     * 提取六维方面分数。只在方面词显式出现时才报告该方面（接受低召回、保住高精度）。
     * 窗口内（方面词后 10 字）扫描情感词，判定该维度极性。
     */
    private Map<String, AnalysisResult.AspectScore> extractAspects(String text, List<String> triggerWords) {
        Map<String, AnalysisResult.AspectScore> aspects = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ASPECT_SYNONYMS.entrySet()) {
            String aspectName = entry.getKey();
            boolean mentioned = false;
            double pos = 0.0, neg = 0.0;
            for (String word : entry.getValue()) {
                int idx = text.indexOf(word);
                if (idx < 0) {
                    continue;
                }
                mentioned = true;
                // 方面词后 10 字窗口
                int windowStart = idx + word.length();
                int windowEnd = Math.min(text.length(), windowStart + 10);
                String window = text.substring(windowStart, windowEnd);
                for (String pw : POSITIVE_WORDS) {
                    if (window.contains(pw)) {
                        pos += 1.0;
                    }
                }
                for (String nw : NEGATIVE_WORDS) {
                    if (window.contains(nw)) {
                        neg += 1.0;
                    }
                }
            }
            if (!mentioned) {
                aspects.put(aspectName, new AnalysisResult.AspectScore(SentimentPolarity.NEUTRAL, 0.0, 0));
            } else if (pos > neg) {
                aspects.put(aspectName, new AnalysisResult.AspectScore(SentimentPolarity.POSITIVE,
                        Math.min(1.0, 0.5 + 0.1 * pos), 1));
            } else if (neg > pos) {
                aspects.put(aspectName, new AnalysisResult.AspectScore(SentimentPolarity.NEGATIVE,
                        Math.min(1.0, 0.5 + 0.1 * neg), 1));
            } else {
                aspects.put(aspectName, new AnalysisResult.AspectScore(SentimentPolarity.NEUTRAL, 0.5, 1));
            }
        }
        return aspects;
    }

    @Override
    public String name() {
        return "L0-rule";
    }
}

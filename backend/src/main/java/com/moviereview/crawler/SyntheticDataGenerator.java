package com.moviereview.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 合成数据生成器。
 *
 * **用途**：在真实数据集就绪前，让系统立刻能跑起来演示。
 * **不用于**：论文实验数据（论文实验必须用真实数据）。
 *
 * 真实数据来源见 data/README.md（apache-2.0 许可的豆瓣数据集）。
 * 此生成器仅用于开发期占位与集成测试。
 */
public class SyntheticDataGenerator {

    private static final String[] MOVIE_IDS = {
            "m001", "m002", "m003", "m004", "m005",
            "m006", "m007", "m008", "m009", "m010",
            "m011", "m012", "m013", "m014", "m015",
            "m016", "m017", "m018", "m019", "m020"
    };

    private static final String[] POSITIVE_TEMPLATES = {
            "%s，值得一看。",
            "剧情%s，演技%s。",
            "画面%s，配乐%s。",
            "节奏%s，整体%s。",
            "太好看了，%s。",
            "强烈推荐，%s。"
    };

    private static final String[] NEGATIVE_TEMPLATES = {
            "%s，浪费时间。",
            "剧情%s，演技%s。",
            "画面%s，配乐%s。",
            "节奏%s，整体%s。",
            "太难看了，%s。",
            "不推荐，%s。"
    };

    private static final String[] NEUTRAL_TEMPLATES = {
            "还行吧，%s。",
            "一般般，%s。",
            "中规中矩，%s。",
            "凑合能看，%s。"
    };

    private static final String[] POSITIVE_WORDS = {
            "紧凑", "在线", "精美", "动人", "流畅", "出色", "精彩", "感人"
    };

    private static final String[] NEGATIVE_WORDS = {
            "拖沓", "尴尬", "粗糙", "突兀", "稀碎", "糟糕", "失望", "浮夸"
    };

    private static final String[] NEUTRAL_WORDS = {
            "普通", "过得去", "一般", "没什么特别"
    };

    /**
     * 生成合成评论。
     *
     * @param moviesCount 电影数
     * @param perMovie 每部电影评论数
     * @param seed 随机种子（便于复现）
     * @return 评论列表
     */
    public List<Comment> generate(int moviesCount, int perMovie, long seed) {
        if (moviesCount <= 0 || moviesCount > MOVIE_IDS.length) {
            throw new IllegalArgumentException("moviesCount 必须在 1-" + MOVIE_IDS.length);
        }
        if (perMovie <= 0) {
            throw new IllegalArgumentException("perMovie 必须为正");
        }
        Random rand = new Random(seed);
        List<Comment> comments = new ArrayList<>();
        String[] authors = {"影迷A", "影迷B", "影迷C", "匿名用户", "观众", "路人"};

        for (int m = 0; m < moviesCount; m++) {
            String movieId = MOVIE_IDS[m];
            for (int i = 0; i < perMovie; i++) {
                // 60% 正面，25% 负面，15% 中性（贴近真实影评分分布）
                double r = rand.nextDouble();
                String text;
                if (r < 0.6) {
                    text = String.format(POSITIVE_TEMPLATES[rand.nextInt(POSITIVE_TEMPLATES.length)],
                            POSITIVE_WORDS[rand.nextInt(POSITIVE_WORDS.length)],
                            POSITIVE_WORDS[rand.nextInt(POSITIVE_WORDS.length)]);
                } else if (r < 0.85) {
                    text = String.format(NEGATIVE_TEMPLATES[rand.nextInt(NEGATIVE_TEMPLATES.length)],
                            NEGATIVE_WORDS[rand.nextInt(NEGATIVE_WORDS.length)],
                            NEGATIVE_WORDS[rand.nextInt(NEGATIVE_WORDS.length)]);
                } else {
                    text = String.format(NEUTRAL_TEMPLATES[rand.nextInt(NEUTRAL_TEMPLATES.length)],
                            NEUTRAL_WORDS[rand.nextInt(NEUTRAL_WORDS.length)]);
                }
                String author = authors[rand.nextInt(authors.length)];
                String date = String.format("2024-%02d-%02d", 1 + rand.nextInt(12), 1 + rand.nextInt(28));
                int likes = rand.nextInt(100);
                comments.add(new Comment(movieId, text, author, date, likes));
            }
        }
        return comments;
    }
}

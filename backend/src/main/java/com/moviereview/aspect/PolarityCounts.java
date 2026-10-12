package com.moviereview.aspect;

import com.moviereview.sentiment.SentimentPolarity;

import java.util.EnumMap;
import java.util.Map;

/**
 * 极性计数器。
 *
 * 解决 standards review #2 (Data Clumps) 和 #3 (Repeated Switches)：
 * 把散落的 pos/neu/neg 计数和 switch 逻辑收敛到一个类型。
 */
public class PolarityCounts {

    private final Map<SentimentPolarity, Integer> counts = new EnumMap<>(SentimentPolarity.class);
    private int total = 0;

    public PolarityCounts() {
        for (SentimentPolarity p : SentimentPolarity.values()) {
            counts.put(p, 0);
        }
    }

    /** 计入一个极性。 */
    public void increment(SentimentPolarity polarity) {
        counts.merge(polarity, 1, Integer::sum);
        total++;
    }

    public int get(SentimentPolarity polarity) {
        return counts.getOrDefault(polarity, 0);
    }

    public int positive() { return get(SentimentPolarity.POSITIVE); }
    public int neutral()  { return get(SentimentPolarity.NEUTRAL); }
    public int negative() { return get(SentimentPolarity.NEGATIVE); }
    public int total()    { return total; }

    /** 占主导的极性。 */
    public SentimentPolarity dominant() {
        if (positive() >= negative() && positive() >= neutral()) return SentimentPolarity.POSITIVE;
        if (negative() >= positive() && negative() >= neutral()) return SentimentPolarity.NEGATIVE;
        return SentimentPolarity.NEUTRAL;
    }

    /** 主导极性的占比（置信度）。 */
    public double dominantRatio() {
        return total == 0 ? 0.0 : (double) get(dominant()) / total;
    }
}

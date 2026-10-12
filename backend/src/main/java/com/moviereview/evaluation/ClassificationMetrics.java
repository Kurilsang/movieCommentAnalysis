package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentPolarity;

/**
 * 分类指标计算器。
 *
 * 抽取自 Evaluator 和 ThetaSensitivityAnalyzer 的公共逻辑，
 * 避免重复的 TP/FP/FN 计数与 macro P/R/F1 计算（standards review #7）。
 */
public class ClassificationMetrics {

    /** per-class 计数器。 */
    private final int[] tp = new int[3];
    private final int[] fp = new int[3];
    private final int[] fn = new int[3];
    private int correct = 0;
    private int total = 0;

    /** 记录一个样本。 */
    public void record(SentimentPolarity actual, SentimentPolarity predicted) {
        total++;
        if (actual == predicted) {
            correct++;
            tp[actual.ordinal()]++;
        } else {
            fp[predicted.ordinal()]++;
            fn[actual.ordinal()]++;
        }
    }

    /** 计算 accuracy。 */
    public double accuracy() {
        return total == 0 ? 0.0 : (double) correct / total;
    }

    /** 计算 macro-F1。 */
    public double macroF1() {
        double sum = 0;
        int counted = 0;
        for (int i = 0; i < 3; i++) {
            if (tp[i] + fp[i] + fn[i] == 0) continue;
            double p = (tp[i] + fp[i] == 0) ? 0 : (double) tp[i] / (tp[i] + fp[i]);
            double r = (tp[i] + fn[i] == 0) ? 0 : (double) tp[i] / (tp[i] + fn[i]);
            sum += (p + r == 0) ? 0 : 2 * p * r / (p + r);
            counted++;
        }
        return counted == 0 ? 0.0 : sum / counted;
    }

    /** 计算 macro-precision。 */
    public double macroPrecision() {
        double sum = 0;
        int counted = 0;
        for (int i = 0; i < 3; i++) {
            if (tp[i] + fp[i] + fn[i] == 0) continue;
            sum += (tp[i] + fp[i] == 0) ? 0 : (double) tp[i] / (tp[i] + fp[i]);
            counted++;
        }
        return counted == 0 ? 0.0 : sum / counted;
    }

    /** 计算 macro-recall。 */
    public double macroRecall() {
        double sum = 0;
        int counted = 0;
        for (int i = 0; i < 3; i++) {
            if (tp[i] + fp[i] + fn[i] == 0) continue;
            sum += (tp[i] + fn[i] == 0) ? 0 : (double) tp[i] / (tp[i] + fn[i]);
            counted++;
        }
        return counted == 0 ? 0.0 : sum / counted;
    }

    public int total() {
        return total;
    }

    public int correct() {
        return correct;
    }
}

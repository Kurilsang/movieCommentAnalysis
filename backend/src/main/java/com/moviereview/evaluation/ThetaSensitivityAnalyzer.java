package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentPolarity;
import com.moviereview.sentiment.ThreeClassDeriver;

import java.util.ArrayList;
import java.util.List;

/**
 * θ 敏感性分析器。
 *
 * spec 创新点 3：基于置信度间隔的中性类识别 + 阈值敏感性分析。
 *
 * 网格搜索 θ（0.05–0.50，步长 0.05），在验证集上计算各 θ 下的指标，
 * 输出 θ-F1 曲线。这条曲线是论文里最有力的实验证据之一。
 */
public class ThetaSensitivityAnalyzer {

    /**
     * θ 扫描结果。
     *
     * @param theta 阈值
     * @param accuracy 准确率
     * @param macroF1 宏 F1
     * @param neutralRatio 中性类占比
     * @param neutralCount 中性类数量
     */
    public record ThetaPoint(double theta, double accuracy, double macroF1,
                             double neutralRatio, int neutralCount) {}

    /**
     * 网格搜索 θ，返回每个 θ 的指标。
     *
     * @param samples 金标准样本（含二分类概率）
     * @param thetaMin θ 最小值
     * @param thetaMax θ 最大值
     * @param step 步长
     * @return θ 扫描结果列表
     */
    public List<ThetaPoint> scan(List<BinaryLabeledSample> samples,
                                 double thetaMin, double thetaMax, double step) {
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("samples 不能为空");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step 必须为正");
        }

        List<ThetaPoint> results = new ArrayList<>();
        for (double theta = thetaMin; theta <= thetaMax + 1e-9; theta += step) {
            ThreeClassDeriver deriver = new ThreeClassDeriver(theta);
            ClassificationMetrics metrics = new ClassificationMetrics();
            int neutralCount = 0;

            for (BinaryLabeledSample s : samples) {
                SentimentPolarity predicted = deriver.derive(s.posProb(), s.negProb());
                SentimentPolarity actual = s.golden();
                if (predicted == SentimentPolarity.NEUTRAL) {
                    neutralCount++;
                }
                metrics.record(actual, predicted);
            }

            double accuracy = metrics.accuracy();
            double macroF1 = metrics.macroF1();
            double neutralRatio = (double) neutralCount / samples.size();
            results.add(new ThetaPoint(round(theta), accuracy, macroF1, neutralRatio, neutralCount));
        }
        return results;
    }

    /**
     * 找到 macroF1 最高的 θ。
     */
    public ThetaPoint findBest(List<ThetaPoint> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("points 不能为空");
        }
        return points.stream()
                .max((a, b) -> Double.compare(a.macroF1(), b.macroF1()))
                .orElseThrow();
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

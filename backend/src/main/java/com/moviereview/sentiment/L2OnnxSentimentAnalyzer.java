package com.moviereview.sentiment;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L2 本地 ONNX 深度模型。
 *
 * 主推理路径：离线、免费、可复现、答辩不怕断网。
 * 创新点 1：纯 Java 通过 ONNX Runtime 部署微调中文 Transformer，推理期零 Python。
 *
 * 模型：IDEA-CCNL/Erlangshen-Roberta-110M-Sentiment（apache-2.0）
 * → ONNX → int8 量化（约 100 MB），由 ml/export/export_onnx.py 导出。
 */
public class L2OnnxSentimentAnalyzer implements SentimentAnalyzer {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;

    /**
     * 加载 ONNX 模型与 tokenizer。
     *
     * @param modelPath     ONNX 模型文件路径（如 ml/models/sentiment.onnx）
     * @param tokenizerPath tokenizer 目录路径（如 ml/models/tokenizer）
     * @throws IllegalArgumentException 当任一路径不存在
     */
    public L2OnnxSentimentAnalyzer(Path modelPath, Path tokenizerPath) {
        if (modelPath == null || !Files.exists(modelPath)) {
            throw new IllegalArgumentException("ONNX 模型文件不存在: " + modelPath);
        }
        if (tokenizerPath == null || !Files.isDirectory(tokenizerPath)) {
            throw new IllegalArgumentException("tokenizer 目录不存在: " + tokenizerPath);
        }
        try {
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            this.session = env.createSession(modelPath.toString(), opts);
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath);
        } catch (Exception e) {
            throw new IllegalStateException("加载 ONNX 模型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AnalysisResult analyze(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("评论文本不能为空");
        }
        try {
            Encoding encoding = tokenizer.encode(text.trim());
            long[] inputIds = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();
            long[] tokenTypeIds = encoding.getTypeIds();

            long[][] idsArr = {inputIds};
            long[][] maskArr = {attentionMask};
            long[][] typeArr = {tokenTypeIds};

            try (OnnxTensor idsTensor = OnnxTensor.createTensor(env, idsArr);
                 OnnxTensor maskTensor = OnnxTensor.createTensor(env, maskArr);
                 OnnxTensor typeTensor = OnnxTensor.createTensor(env, typeArr)) {

                Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
                inputs.put("input_ids", idsTensor);
                inputs.put("attention_mask", maskTensor);
                inputs.put("token_type_ids", typeTensor);

                OrtSession.Result result = session.run(inputs);
                float[][] logits = (float[][]) result.get(0).getValue();
                float[] scores = softmax(logits[0]);

                // 模型标签：0=NEGATIVE, 1=POSITIVE（Erlangshen 二分类）
                double negProb = scores[0];
                double posProb = scores.length > 1 ? scores[1] : 1 - scores[0];

                SentimentPolarity polarity;
                double confidence;
                if (Math.abs(posProb - negProb) < 0.2) {
                    // 置信度间隔法：差距过小 → 中性
                    polarity = SentimentPolarity.NEUTRAL;
                    confidence = 0.5;
                } else if (posProb > negProb) {
                    polarity = SentimentPolarity.POSITIVE;
                    confidence = posProb;
                } else {
                    polarity = SentimentPolarity.NEGATIVE;
                    confidence = negProb;
                }

                // 六维方面分数（L2 阶段先简化为未提及，07 号工单会实现）
                Map<String, AnalysisResult.AspectScore> aspects = new LinkedHashMap<>();
                for (String aspect : List.of("剧情", "演技", "画面特效", "配乐音效", "节奏", "台词")) {
                    aspects.put(aspect, new AnalysisResult.AspectScore(SentimentPolarity.NEUTRAL, 0.0, 0));
                }

                String rawJson = "{\"polarity\":\"" + polarity + "\",\"confidence\":" + confidence
                        + ",\"posProb\":" + posProb + ",\"negProb\":" + negProb
                        + ",\"analyzer\":\"L2-onnx\"}";

                return new AnalysisResult(polarity, confidence, new ArrayList<>(), aspects, rawJson);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("推理失败: " + e.getMessage(), e);
        }
    }

    /** softmax 归一化。 */
    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) {
            if (v > max) max = v;
        }
        float sum = 0;
        float[] exp = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    @Override
    public String name() {
        return "L2-onnx";
    }
}

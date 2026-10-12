package com.moviereview.evaluation;

import com.moviereview.sentiment.SentimentPolarity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试集加载器。
 *
 * 支持 CSV 格式：`id,text,polarity,source`
 * - polarity 取值：POSITIVE / NEUTRAL / NEGATIVE（不区分大小写）
 * - 跳过空行与首行表头（若首行 polarity 不是合法枚举值则视为表头）
 *
 * 论文实验章节的所有对比表都基于由此加载的测试集。
 */
public class TestSetLoader {

    /**
     * 从 CSV 文件加载测试集。
     *
     * @param path CSV 文件路径
     * @return 样本列表
     * @throws IOException              文件读取失败
     * @throws IllegalArgumentException 文件为空或格式错误
     */
    public List<EvalSample> loadFromCsv(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path 不能为空");
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + path);
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<EvalSample> samples = new ArrayList<>();

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            // 简单 CSV 解析：按逗号切分，限制 4 段（id,text,polarity,source）
            String[] parts = line.split(",", 4);
            if (parts.length < 3) {
                continue; // 跳过格式错误的行
            }
            String id = parts[0].trim();
            String text = parts[1].trim();
            String polarityStr = parts[2].trim();
            String source = parts.length > 3 ? parts[3].trim() : "csv";

            SentimentPolarity polarity;
            try {
                polarity = SentimentPolarity.valueOf(polarityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 表头行（如 "id,text,polarity,source"），跳过
                continue;
            }

            if (id.isEmpty() || text.isEmpty()) {
                continue;
            }

            samples.add(new EvalSample(id, text, polarity, source));
        }

        if (samples.isEmpty()) {
            throw new IllegalArgumentException("CSV 中没有有效样本: " + path);
        }
        return samples;
    }
}

package com.moviereview.movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 启动顺序测试。
 *
 * 这是 code review 发现的 bug 的回归测试：
 * CsvDataImporter 原本没有 @Order（默认 LOWEST_PRECEDENCE），
 * 而 PrecomputeRunner 是 @Order(2)，导致预计算先于导入执行，
 * 看到 0 条评论就跳过 —— 雷达图全部为 0。
 *
 * 这个测试断言：导入必须排在预计算之前。
 */
@SpringBootTest
@ActiveProfiles("test")
class StartupOrderTest {

    @Autowired
    private List<ApplicationRunner> runners;

    @Test
    @DisplayName("CsvDataImporter 必须排在 PrecomputeRunner 之前")
    void importerRunsBeforePrecompute() {
        List<ApplicationRunner> sorted = new ArrayList<>(runners);
        sorted.sort(AnnotationAwareOrderComparator.INSTANCE);

        int importIdx = indexOf(sorted, CsvDataImporter.class);
        int precomputeIdx = indexOf(sorted, PrecomputeRunner.class);

        assertTrue(importIdx >= 0, "应存在 CsvDataImporter");
        assertTrue(precomputeIdx >= 0, "应存在 PrecomputeRunner");
        assertTrue(importIdx < precomputeIdx,
                "CsvDataImporter(索引 " + importIdx + ") 必须早于 PrecomputeRunner(索引 "
                        + precomputeIdx + ")，否则预计算会看到 0 条评论而跳过");
    }

    private int indexOf(List<ApplicationRunner> list, Class<?> type) {
        for (int i = 0; i < list.size(); i++) {
            if (type.isInstance(list.get(i))) return i;
        }
        return -1;
    }
}

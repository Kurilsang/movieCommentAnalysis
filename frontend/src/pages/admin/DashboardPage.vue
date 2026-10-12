<template>
  <div class="dashboard">
    <h2>数据总览</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number">{{ stats.totalMovies }}</div>
          <div class="stat-label">电影总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number">{{ stats.totalComments }}</div>
          <div class="stat-label">评论总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number">{{ stats.analyzedCount }}</div>
          <div class="stat-label">已分析</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number">{{ stats.modelCount }}</div>
          <div class="stat-label">模型版本</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模型效果对比 -->
    <el-card class="comparison-card">
      <template #header>模型效果对比</template>
      <div ref="comparisonChart" style="height: 300px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const stats = ref({
  totalMovies: 20,
  totalComments: 4000,
  analyzedCount: 0,
  modelCount: 1
})

const comparisonChart = ref(null)

const renderComparison = async () => {
  await nextTick()
  if (!comparisonChart.value) return
  const chart = echarts.init(comparisonChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['Accuracy', 'Macro-F1'] },
    xAxis: { type: 'category', data: ['L0 规则', 'L2 本地模型', 'L1 大模型'] },
    yAxis: { type: 'value', max: 1 },
    series: [
      { name: 'Accuracy', type: 'bar', data: [0.65, 0.82, 0.85] },
      { name: 'Macro-F1', type: 'bar', data: [0.58, 0.78, 0.81] }
    ]
  })
}

onMounted(renderComparison)
</script>

<style scoped>
.dashboard { padding: 20px; }
.stat-card { text-align: center; }
.stat-number { font-size: 32px; font-weight: bold; color: #409EFF; }
.stat-label { color: #909399; margin-top: 5px; }
.comparison-card { margin-top: 20px; }
</style>

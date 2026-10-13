<template>
  <div class="dashboard">
    <h2>数据总览</h2>
    <el-row :gutter="20" v-loading="loading">
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

    <el-card class="comparison-card">
      <template #header>
        模型效果对比
        <el-tooltip content="L2 需先导出 ONNX 模型；L1 需配置 API Key">
          <el-icon><QuestionFilled /></el-icon>
        </el-tooltip>
      </template>
      <div ref="comparisonChart" style="height: 300px"></div>
      <el-alert
        type="warning"
        :closable="false"
        title="当前仅 L0 规则引擎可用；L2/L1 为规划值，导出模型后自动更新"
        style="margin-top: 10px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '../../api'
import { QuestionFilled } from '@element-plus/icons-vue'

const stats = ref({ totalMovies: 0, totalComments: 0, analyzedCount: 0, modelCount: 1 })
const loading = ref(false)
const comparisonChart = ref(null)

const loadStats = async () => {
  loading.value = true
  try {
    const { data } = await adminApi.overview()
    stats.value = data
  } catch (e) {
    console.error('加载统计失败', e)
  } finally {
    loading.value = false
  }
}

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
      { name: 'Accuracy', type: 'bar', data: [0.374, null, null] },
      { name: 'Macro-F1', type: 'bar', data: [0.267, null, null] }
    ]
  })
}

onMounted(async () => {
  await loadStats()
  await renderComparison()
})
</script>

<style scoped>
.dashboard { padding: 20px; }
.stat-card { text-align: center; }
.stat-number { font-size: 32px; font-weight: bold; color: #409EFF; }
.stat-label { color: #909399; margin-top: 5px; }
.comparison-card { margin-top: 20px; }
</style>

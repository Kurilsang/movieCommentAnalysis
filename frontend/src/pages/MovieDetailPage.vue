<template>
  <div class="movie-detail" v-loading="loading">
    <!-- 电影信息 -->
    <el-card class="movie-header">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-image
            :src="movie.posterUrl || `/posters/${movieId}.svg`"
            fit="cover"
            class="poster"
          >
            <template #error>
              <div class="poster-fallback">{{ movie.title }}</div>
            </template>
          </el-image>
        </el-col>
        <el-col :span="18">
          <h1>{{ movie.title }}</h1>
          <p class="meta">{{ movie.year }} · 导演: {{ movie.director || '未知' }}</p>
          <p class="synopsis">{{ movie.synopsis || '暂无简介' }}</p>
          <el-button
            :type="favorited ? 'danger' : 'primary'"
            @click="toggleFavorite"
          >
            {{ favorited ? '取消收藏' : '收藏' }}
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 情感分析结果 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 情感分布饼图 -->
      <el-col :span="12">
        <el-card>
          <template #header>情感分布</template>
          <div ref="pieChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 方面雷达图 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            方面情感雷达图
            <el-tooltip content="基于提及该维度的评论，经贝叶斯平滑">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <div ref="radarChart" style="height: 300px"></div>
          <div class="radar-note" v-if="aspectNote">{{ aspectNote }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 关键词词云 -->
    <el-card class="keywords-card">
      <template #header>关键词</template>
      <div class="keywords">
        <el-tag
          v-for="kw in keywords"
          :key="kw.word"
          :size="getKeywordSize(kw.weight)"
          :type="getKeywordType(kw.weight)"
          class="keyword-tag"
        >
          {{ kw.word }} ({{ kw.count }})
        </el-tag>
      </div>
    </el-card>

    <!-- 好评/差评代表句 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <el-tag type="success">好评代表</el-tag>
          </template>
          <div v-for="(c, i) in positiveComments" :key="i" class="comment-item">
            <p class="comment-text">{{ c.text }}</p>
            <div class="comment-meta">
              <el-tag size="small" type="success">{{ (c.confidence * 100).toFixed(0) }}%</el-tag>
              <span class="trigger-words" v-if="c.triggerWords?.length">
                触发: {{ c.triggerWords.join('、') }}
              </span>
            </div>
          </div>
          <el-empty v-if="positiveComments.length === 0" description="暂无好评" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <el-tag type="danger">差评代表</el-tag>
          </template>
          <div v-for="(c, i) in negativeComments" :key="i" class="comment-item">
            <p class="comment-text">{{ c.text }}</p>
            <div class="comment-meta">
              <el-tag size="small" type="danger">{{ (c.confidence * 100).toFixed(0) }}%</el-tag>
              <span class="trigger-words" v-if="c.triggerWords?.length">
                触发: {{ c.triggerWords.join('、') }}
              </span>
            </div>
          </div>
          <el-empty v-if="negativeComments.length === 0" description="暂无差评" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { movieApi, favoriteApi } from '../api'
import { QuestionFilled } from '@element-plus/icons-vue'

const route = useRoute()
const movieId = route.params.id

const loading = ref(false)
const movie = ref({})
const aspectData = ref({})
const keywords = ref([])
const positiveComments = ref([])
const negativeComments = ref([])
const favorited = ref(false)

const pieChart = ref(null)
const radarChart = ref(null)

const aspectNote = computed(() => {
  const aspects = Object.values(aspectData.value)
  const total = aspects.reduce((sum, a) => sum + (a.sampleCount || 0), 0)
  return total > 0 ? `基于 ${total} 条维度提及，经贝叶斯平滑` : ''
})

const load = async () => {
  loading.value = true
  try {
    const [detail, analysis, kw, favStatus] = await Promise.all([
      movieApi.detail(movieId),
      movieApi.analyze(movieId),
      movieApi.keywords(movieId),
      favoriteApi.status(movieId).catch(() => ({ data: { favorited: false } }))
    ])
    movie.value = detail.data.movie || {}
    aspectData.value = analysis.data.aspects || {}
    positiveComments.value = analysis.data.positiveComments || []
    negativeComments.value = analysis.data.negativeComments || []
    keywords.value = kw.data.keywords || []
    favorited.value = favStatus.data.favorited

    await nextTick()
    renderPie()
    renderRadar()
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

const renderPie = () => {
  if (!pieChart.value) return
  const chart = echarts.init(pieChart.value)
  const dist = movie.value.sentimentDistribution || { positive: 60, neutral: 25, negative: 15 }
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: dist.positive, name: '正面', itemStyle: { color: '#67C23A' } },
        { value: dist.neutral, name: '中性', itemStyle: { color: '#E6A23C' } },
        { value: dist.negative, name: '负面', itemStyle: { color: '#F56C6C' } }
      ],
      label: { formatter: '{b}: {c} ({d}%)' }
    }]
  })
}

const renderRadar = () => {
  if (!radarChart.value) return
  const chart = echarts.init(radarChart.value)
  const aspects = aspectData.value
  const names = Object.keys(aspects)
  const values = names.map(n => Number((aspects[n].smoothedScore || 0).toFixed(1)))
  const counts = names.map(n => aspects[n].sampleCount || 0)
  const sufficient = names.map(n => aspects[n].sufficientData !== false)

  chart.setOption({
    tooltip: {
      formatter: (params) => {
        const i = params.dataIndex
        return `${names[i]}<br/>平滑分: ${values[i]}<br/>样本量: ${counts[i]} 条${sufficient[i] ? '' : '<br/><span style="color:#E6A23C">样本量不足，仅供参考</span>'}`
      }
    },
    radar: {
      indicator: names.map(n => ({ name: n, max: 100 })),
      radius: '58%',
      axisName: { color: '#606266', fontSize: 13 }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '方面情感',
        areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
        lineStyle: { color: '#409EFF' },
        itemStyle: {
          // 样本量不足的维度用橙色标记
          color: (params) => sufficient[params.dataIndex] ? '#409EFF' : '#E6A23C'
        },
        label: {
          show: true,
          fontSize: 11,
          formatter: (params) => `${params.value}\n(${counts[params.dataIndex]}条)`
        }
      }]
    }]
  })
}

const toggleFavorite = async () => {
  try {
    if (favorited.value) {
      await favoriteApi.remove(movieId)
      favorited.value = false
    } else {
      await favoriteApi.add(movieId)
      favorited.value = true
    }
  } catch (e) {
    console.error('收藏操作失败', e)
  }
}

const getKeywordSize = (weight) => weight > 0.7 ? 'large' : weight > 0.4 ? 'default' : 'small'
const getKeywordType = (weight) => weight > 0.7 ? 'danger' : weight > 0.4 ? 'warning' : 'info'

onMounted(load)
</script>

<style scoped>
.movie-detail { padding: 20px; }
.movie-header { margin-bottom: 20px; }
.poster { width: 100%; border-radius: 8px; }
.meta { color: #909399; margin: 10px 0; }
.synopsis { color: #606266; line-height: 1.6; margin-bottom: 15px; }
.charts-row { margin-bottom: 20px; }
.radar-note { text-align: center; color: #909399; font-size: 12px; margin-top: 10px; }
.keywords-card { margin-bottom: 20px; }
.keywords { display: flex; flex-wrap: wrap; gap: 8px; }
.keyword-tag { margin: 2px; }
.comment-item { padding: 10px 0; border-bottom: 1px solid #EBEEF5; }
.comment-item:last-child { border-bottom: none; }
.comment-text { margin: 0 0 8px; line-height: 1.5; }
.comment-meta { display: flex; align-items: center; gap: 8px; }
.trigger-words { font-size: 12px; color: #909399; }
</style>

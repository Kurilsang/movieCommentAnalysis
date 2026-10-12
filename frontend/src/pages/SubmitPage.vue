<template>
  <div class="submit-page">
    <el-card>
      <template #header>
        <h2>评论情感分析</h2>
      </template>

      <!-- 输入区 -->
      <el-input
        v-model="commentText"
        type="textarea"
        :rows="4"
        placeholder="请输入电影评论，例如：剧情紧凑演技在线，但是画面特效有点粗糙"
        maxlength="500"
        show-word-limit
      />

      <!-- 模型选择 -->
      <div class="model-selector">
        <span>分析方案：</span>
        <el-radio-group v-model="selectedModel">
          <el-radio-button label="L0">L0 规则引擎</el-radio-button>
          <el-radio-button label="L2">L2 本地模型</el-radio-button>
          <el-radio-button label="L1">L1 大模型</el-radio-button>
        </el-radio-group>
      </div>

      <el-button
        type="primary"
        :loading="analyzing"
        @click="handleAnalyze"
        :disabled="!commentText.trim()"
      >
        开始分析
      </el-button>
    </el-card>

    <!-- 分析结果 -->
    <el-card v-if="result" class="result-card">
      <template #header>分析结果</template>

      <!-- 整体情感 -->
      <div class="polarity-result">
        <el-tag :type="getPolarityType(result.polarity)" size="large">
          {{ getPolarityLabel(result.polarity) }}
        </el-tag>
        <span class="confidence">置信度: {{ (result.confidence * 100).toFixed(1) }}%</span>
      </div>

      <!-- 触发关键词 -->
      <div class="trigger-section" v-if="result.triggerWords?.length">
        <h4>触发关键词</h4>
        <el-tag
          v-for="word in result.triggerWords"
          :key="word"
          type="warning"
          class="trigger-tag"
        >
          {{ word }}
        </el-tag>
      </div>

      <!-- 方面分析 -->
      <div class="aspects-section" v-if="result.aspects">
        <h4>方面分析</h4>
        <el-table :data="aspectTableData" stripe>
          <el-table-column prop="name" label="维度" width="120" />
          <el-table-column label="情感" width="120">
            <template #default="{ row }">
              <el-tag :type="getPolarityType(row.polarity)" size="small">
                {{ getPolarityLabel(row.polarity) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" width="120">
            <template #default="{ row }">
              {{ (row.confidence * 100).toFixed(0) }}%
            </template>
          </el-table-column>
          <el-table-column label="触发词">
            <template #default="{ row }">
              <el-tag v-for="w in row.triggerWords" :key="w" size="small" class="trigger-tag">
                {{ w }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- L1 异步结果 -->
      <div v-if="llmResult" class="llm-section">
        <el-alert type="success" :closable="false" title="大模型分析结果（异步）">
          <template #default>
            <p>极性: {{ getPolarityLabel(llmResult.polarity) }}</p>
            <p>置信度: {{ (llmResult.confidence * 100).toFixed(1) }}%</p>
          </template>
        </el-alert>
      </div>
      <div v-else-if="selectedModel === 'L1' && analyzing" class="llm-loading">
        <el-alert type="info" :closable="false" title="大模型分析中..." />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { commentApi } from '../api'

const commentText = ref('')
const selectedModel = ref('L2')
const analyzing = ref(false)
const result = ref(null)
const llmResult = ref(null)

const aspectTableData = computed(() => {
  if (!result.value?.aspects) return []
  return Object.entries(result.value.aspects).map(([name, data]) => ({
    name,
    polarity: data.polarity,
    confidence: data.confidence || 0,
    triggerWords: data.triggerWords || []
  }))
})

const handleAnalyze = async () => {
  analyzing.value = true
  result.value = null
  llmResult.value = null

  try {
    // 同步分析（L0/L2）
    const { data } = await commentApi.submit({
      text: commentText.value,
      model: selectedModel.value
    })
    result.value = data.result

    // 如果选了 L1，异步补结果
    if (selectedModel.value === 'L1') {
      pollLlmResult(data.taskId)
    }
  } catch (e) {
    console.error('分析失败', e)
  } finally {
    analyzing.value = false
  }
}

const pollLlmResult = async (taskId) => {
  if (!taskId) return
  let attempts = 0
  const timer = setInterval(async () => {
    attempts++
    try {
      const { data } = await commentApi.pollResult(taskId)
      if (data.result) {
        llmResult.value = data.result
        clearInterval(timer)
      }
    } catch (e) {
      console.error('轮询失败', e)
    }
    if (attempts > 10) clearInterval(timer) // 最多轮询 5 秒
  }, 500)
}

const getPolarityType = (p) => {
  switch (p) {
    case 'POSITIVE': return 'success'
    case 'NEGATIVE': return 'danger'
    default: return 'warning'
  }
}

const getPolarityLabel = (p) => {
  switch (p) {
    case 'POSITIVE': return '正面'
    case 'NEGATIVE': return '负面'
    default: return '中性'
  }
}
</script>

<style scoped>
.submit-page { padding: 20px; max-width: 800px; margin: 0 auto; }
.model-selector { margin: 15px 0; display: flex; align-items: center; gap: 10px; }
.result-card { margin-top: 20px; }
.polarity-result { display: flex; align-items: center; gap: 15px; margin-bottom: 20px; }
.confidence { font-size: 18px; color: #606266; }
.trigger-section { margin-bottom: 20px; }
.trigger-tag { margin: 3px; }
.aspects-section { margin-bottom: 20px; }
.llm-section { margin-top: 20px; }
.llm-loading { margin-top: 20px; }
</style>

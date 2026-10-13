<template>
  <div class="data-page">
    <h2>数据管理</h2>

    <el-card class="actions">
      <el-button type="primary" @click="rerunAll" :loading="rerunning">
        一键重跑全库
      </el-button>
      <span class="hint">重跑会覆盖所有已有分析结果（换模型后使用）</span>
    </el-card>

    <el-card>
      <template #header>电影数据</template>
      <el-table :data="movies" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="150" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="commentCount" label="评论数" width="100" />
        <el-table-column label="好评率" width="100">
          <template #default="{ row }">
            {{ (row.positiveRate * 100).toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { movieApi, adminApi } from '../../api'

const router = useRouter()
const movies = ref([])
const loading = ref(false)
const rerunning = ref(false)

const loadMovies = async () => {
  loading.value = true
  try {
    const { data } = await movieApi.list({ sort: 'commentCount' })
    movies.value = data.movies || []
  } catch (e) {
    console.error('加载电影失败', e)
  } finally {
    loading.value = false
  }
}

const rerunAll = async () => {
  rerunning.value = true
  try {
    const { data } = await adminApi.rerunAll()
    ElMessage.success(`重跑完成，共 ${data.count} 条评论`)
  } catch (e) {
    ElMessage.error('重跑失败：' + (e.response?.data?.error || e.message))
  } finally {
    rerunning.value = false
  }
}

const viewDetail = (id) => router.push(`/movie/${id}`)

onMounted(loadMovies)
</script>

<style scoped>
.data-page { padding: 20px; }
.actions { margin-bottom: 20px; }
.hint { margin-left: 12px; color: #909399; font-size: 13px; }
</style>

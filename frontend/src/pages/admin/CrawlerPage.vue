<template>
  <div class="crawler-page">
    <h2>爬虫任务管理</h2>

    <el-card class="create-task">
      <template #header>创建爬虫任务</template>
      <el-alert
        type="info"
        :closable="false"
        title="演示模式：任务只记录，不发起真实网络请求"
        style="margin-bottom: 15px"
      />
      <el-form :model="form" inline>
        <el-form-item label="电影ID">
          <el-input v-model="form.movieId" placeholder="如 Mef6dbb82" />
        </el-form-item>
        <el-form-item label="抓取数量">
          <el-input-number v-model="form.maxCount" :min="10" :max="500" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="triggerTask" :loading="submitting">触发任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        任务列表
        <el-button size="small" style="float: right" @click="loadTasks">刷新</el-button>
      </template>
      <el-table :data="tasks" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="movieId" label="电影ID" width="160" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" />
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" />
      </el-table>
      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api'

const form = ref({ movieId: '', maxCount: 100 })
const tasks = ref([])
const loading = ref(false)
const submitting = ref(false)

const loadTasks = async () => {
  loading.value = true
  try {
    const { data } = await adminApi.crawlerTasks()
    tasks.value = data.tasks || []
  } catch (e) {
    console.error('加载任务失败', e)
  } finally {
    loading.value = false
  }
}

const triggerTask = async () => {
  if (!form.value.movieId) return
  submitting.value = true
  try {
    await adminApi.triggerCrawler({
      movieId: form.value.movieId,
      maxCount: form.value.maxCount
    })
    await loadTasks()
  } catch (e) {
    console.error('触发任务失败', e)
  } finally {
    submitting.value = false
  }
}

const getStatusType = (status) => {
  switch (status) {
    case '完成': return 'success'
    case '失败': return 'danger'
    case '进行中': return 'warning'
    default: return 'info'
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.crawler-page { padding: 20px; }
.create-task { margin-bottom: 20px; }
</style>

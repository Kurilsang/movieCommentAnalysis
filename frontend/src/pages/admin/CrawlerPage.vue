<template>
  <div class="crawler-page">
    <h2>爬虫任务管理</h2>

    <!-- 创建任务 -->
    <el-card class="create-task">
      <template #header>创建爬虫任务</template>
      <el-form :model="form" inline>
        <el-form-item label="电影ID">
          <el-input v-model="form.movieId" placeholder="输入电影ID" />
        </el-form-item>
        <el-form-item label="抓取数量">
          <el-input-number v-model="form.maxCount" :min="10" :max="500" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="triggerTask">触发任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务列表 -->
    <el-card>
      <template #header>任务列表</template>
      <el-table :data="tasks" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="movieId" label="电影ID" width="150" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" />
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const form = ref({ movieId: '', maxCount: 100 })
const tasks = ref([
  { id: 1, movieId: 'Mef6dbb82', status: '完成', progress: 100, message: '成功抓取 200 条' },
  { id: 2, movieId: 'Me7dccb3e', status: '进行中', progress: 45, message: '正在抓取...' }
])

const triggerTask = () => {
  tasks.value.unshift({
    id: tasks.value.length + 1,
    movieId: form.value.movieId,
    status: '待执行',
    progress: 0,
    message: '任务已创建'
  })
}

const getStatusType = (status) => {
  switch (status) {
    case '完成': return 'success'
    case '失败': return 'danger'
    case '进行中': return 'warning'
    default: return 'info'
  }
}
</script>

<style scoped>
.crawler-page { padding: 20px; }
.create-task { margin-bottom: 20px; }
</style>

<template>
  <div class="data-page">
    <h2>数据管理</h2>

    <!-- 操作栏 -->
    <el-card class="actions">
      <el-button type="primary" @click="rerunAll">一键重跑全库</el-button>
      <el-button>导出 CSV</el-button>
    </el-card>

    <!-- 电影列表 -->
    <el-card>
      <template #header>电影数据</template>
      <el-table :data="movies" stripe>
        <el-table-column prop="id" label="ID" width="150" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="commentCount" label="评论数" width="100" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row.id)">查看</el-button>
            <el-button size="small" type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const movies = ref([
  { id: 'Mef6dbb82', title: '星际穿越', commentCount: 200 },
  { id: 'Me7dccb3e', title: '无敌破坏王', commentCount: 200 }
])

const rerunAll = () => {
  // TODO: 调用 API
}

const viewDetail = (id) => router.push(`/movie/${id}`)
</script>

<style scoped>
.data-page { padding: 20px; }
.actions { margin-bottom: 20px; }
</style>

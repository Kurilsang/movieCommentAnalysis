<template>
  <div class="home">
    <el-row :gutter="20" class="search-bar">
      <el-col :span="16">
        <el-input
          v-model="searchQuery"
          placeholder="搜索电影名称、导演、演员..."
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </el-col>
      <el-col :span="8">
        <el-select v-model="sortBy" @change="loadMovies" style="width: 100%">
          <el-option label="按好评率排序" value="positiveRate" />
          <el-option label="按评论数排序" value="commentCount" />
          <el-option label="按名称排序" value="title" />
        </el-select>
      </el-col>
    </el-row>

    <el-row :gutter="20" v-loading="loading">
      <el-col :span="6" v-for="movie in movies" :key="movie.id" class="movie-card">
        <el-card shadow="hover" @click="goDetail(movie.id)">
          <div class="poster">
            <el-image
              :src="posterOf(movie)"
              fit="cover"
              lazy
            >
              <template #error>
                <div class="poster-placeholder">{{ movie.title }}</div>
              </template>
            </el-image>
          </div>
          <div class="movie-info">
            <h3 class="title">{{ movie.title }}</h3>
            <p class="meta">{{ movie.year }} · {{ movie.director || '未知导演' }}</p>
            <div class="sentiment-badge">
              <el-tag :type="getSentimentType(movie.positiveRate)" size="small">
                好评率 {{ (movie.positiveRate * 100).toFixed(1) }}%
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && movies.length === 0" description="暂无电影数据" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { movieApi } from '../api'

const router = useRouter()
const movies = ref([])
const loading = ref(false)
const searchQuery = ref('')
const sortBy = ref('positiveRate')

const loadMovies = async () => {
  loading.value = true
  try {
    const { data } = await movieApi.list({ sort: sortBy.value, q: searchQuery.value })
    movies.value = data.movies || []
  } catch (e) {
    console.error('加载电影失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadMovies()

const goDetail = (id) => router.push(`/movie/${id}`)

/**
 * 海报地址。数据源没有真实海报，用本地生成的 SVG 封面（零网络依赖）。
 * 演示铁律：绝不依赖实时网络请求。
 */
const posterOf = (movie) => movie.posterUrl || `/posters/${movie.id}.svg`

const getSentimentType = (rate) => {
  if (rate >= 0.7) return 'success'
  if (rate >= 0.4) return 'warning'
  return 'danger'
}

onMounted(loadMovies)
</script>

<style scoped>
.home { padding: 20px; }
.search-bar { margin-bottom: 20px; }
.movie-card { margin-bottom: 20px; cursor: pointer; }
.poster { height: 280px; overflow: hidden; }
.poster-placeholder {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
}
.movie-info { padding: 10px 0; }
.title { font-size: 14px; margin: 0 0 5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { font-size: 12px; color: #909399; margin: 0 0 8px; }
</style>

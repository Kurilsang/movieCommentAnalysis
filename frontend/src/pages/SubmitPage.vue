<template>
  <div class="submit">
    <h2>评论情感分析</h2>
    <el-input
      v-model="comment"
      type="textarea"
      :rows="4"
      placeholder="请输入电影评论"
    />
    <el-button type="primary" style="margin-top: 12px">分析</el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const comment = ref('')
</script>

<style scoped>
.submit {
  padding: 20px;
}
</style>

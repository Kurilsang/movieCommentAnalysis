<template>
  <div class="login">
    <h2>登录</h2>
    <el-form label-width="80px">
      <el-form-item label="用户名">
        <el-input v-model="username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="password" type="password" placeholder="请输入密码" />
      </el-form-item>
      <el-button type="primary">登录</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const username = ref('')
const password = ref('')
</script>

<style scoped>
.login {
  padding: 20px;
  max-width: 400px;
  margin: 0 auto;
}
</style>

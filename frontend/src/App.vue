<template>
  <el-container>
    <el-header>
      <el-menu mode="horizontal" :router="true" :default-active="activeIndex">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/submit">评论分析</el-menu-item>
        <el-menu-item index="/login">登录</el-menu-item>
        <el-menu-item index="/admin">管理后台</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeIndex = computed(() => route.path)
</script>

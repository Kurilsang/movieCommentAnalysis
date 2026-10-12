import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('./pages/HomePage.vue') },
    { path: '/movie/:id', name: 'movie-detail', component: () => import('./pages/MovieDetailPage.vue') },
    { path: '/submit', name: 'submit', component: () => import('./pages/SubmitPage.vue') },
    { path: '/login', name: 'login', component: () => import('./pages/LoginPage.vue') },
    { path: '/register', name: 'register', component: () => import('./pages/RegisterPage.vue') },
    { path: '/admin', name: 'admin-dashboard', component: () => import('./pages/admin/DashboardPage.vue') },
    { path: '/admin/crawler', name: 'admin-crawler', component: () => import('./pages/admin/CrawlerPage.vue') },
    { path: '/admin/data', name: 'admin-data', component: () => import('./pages/admin/DataPage.vue') }
  ]
})

createApp(App).use(ElementPlus).use(router).mount('#app')

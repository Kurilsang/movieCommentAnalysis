# 基于方面级情感分析的电影评论分析系统

本科毕业设计。论文题目：**《基于方面级情感分析的电影评论分析系统设计与实现》**。

## 快速开始

```bash
# 1. 启动 MySQL + Redis
docker compose up -d

# 2. 启动后端
cd backend
mvn spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

## 目录结构

| 目录 | 用途 |
|---|---|
| `backend/` | Spring Boot 3.2 + MyBatis-Plus + MySQL 8 后端 |
| `frontend/` | Vue 3 + Element Plus + ECharts 前端 |
| `ml/` | Python 离线训练管线（预留，暂不启用） |
| `data/` | 电影评论数据留存（**必须提交 Git**） |
| `docs/` | 决策记录、agent 配置 |
| `.scratch/` | Spec 与工单 |

## 技术栈

- **后端**：Java 17 (Temurin/Zulu 17) + Spring Boot 3.2.5 + MyBatis-Plus 3.5.5 + MySQL 8
- **前端**：Vue 3 + Element Plus + ECharts
- **AI 推理**：ONNX Runtime Java 1.31.0 + DJL tokenizers 0.38.0（推理期零 Python）
- **中文预处理**：HanLP 1.x `portable-1.8.6`
- **缓存**：Redis 7
- **LLM**：阿里百炼 DashScope Java SDK 2.23.2

## 核心决策

见 [docs/决策记录.md](docs/决策记录.md)。Spec 与工单见 [.scratch/movie-comment-sentiment/](.scratch/movie-comment-sentiment/)。

## 创新点

1. **纯 Java 通过 ONNX Runtime 部署微调中文 Transformer，推理期零 Python**（已验证空白）
2. **方面级细粒度情感分析**：自建中文电影六维评价体系
3. **二分类→三分类派生方法** + θ 敏感性分析
4. **中文电影领域情感词典构建**

# 06: 后台爬虫任务管理

**What to build:** 管理员在后台看到爬虫任务列表（ID、电影ID、状态、进度、消息），并能触发新任务。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] `GET /api/admin/crawler/tasks` 返回 `{ tasks: [...] }`
- [ ] `POST /api/admin/crawler/trigger`（body: `{ movieId, maxCount }`）返回 `{ taskId }`
- [ ] 触发的任务落库到 `crawler_task` 表（status=PENDING）
- [ ] 任务列表按创建时间倒序
- [ ] **触发不发真实网络请求**（用 `MockReviewCrawler`，符合演示铁律）
- [ ] 普通用户访问返回 403
- [ ] 后台爬虫页能显示列表和触发任务
- [ ] 测试：MockMvc 测任务接口（触发落库、列表、权限 403）

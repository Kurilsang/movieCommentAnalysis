# 07: 一键重跑全库 + 数据总览

**What to build:** 管理员能一键重跑全库分析（换模型后重算所有结果），并在数据总览页看到系统规模统计。

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] `POST /api/admin/analysis/rerun-all` 返回 `{ message, count }`
- [ ] 重跑覆盖已有分析结果（行数不变，不产生重复）
- [ ] 重跑后 `comment_analysis` 行数 = `comment` 行数
- [ ] 普通用户访问返回 403
- [ ] 后台数据总览显示：电影数、评论数、已分析数、模型数
- [ ] 数据管理页显示电影列表（ID、标题、评论数）
- [ ] 测试：MockMvc 测重跑接口（行数不变、幂等、权限 403）

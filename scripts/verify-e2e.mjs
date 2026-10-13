// 端到端验证：登录 → 管理员接口 → 雷达图 → 提交评论
const BASE = 'http://localhost:8080'

const log = (label, ok, detail) =>
  console.log(`${ok ? '✅' : '❌'} ${label}${detail ? ' — ' + detail : ''}`)

async function main() {
  // 1. 管理员登录
  const loginRes = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  })
  const login = await loginRes.json()
  const token = login.token
  log('管理员登录', !!token, token ? `role=${login.user?.role}` : JSON.stringify(login))
  if (!token) return

  const auth = { Authorization: `Bearer ${token}` }

  // 2. 数据总览
  const ovRes = await fetch(`${BASE}/api/admin/overview`, { headers: auth })
  const ov = await ovRes.json()
  log('数据总览', ovRes.ok && ov.totalComments > 0,
    `电影 ${ov.totalMovies} / 评论 ${ov.totalComments} / 已分析 ${ov.analyzedCount}`)

  // 3. 触发爬虫任务
  const trigRes = await fetch(`${BASE}/api/admin/crawler/trigger`, {
    method: 'POST',
    headers: { ...auth, 'Content-Type': 'application/json' },
    body: JSON.stringify({ movieId: 'Mef6dbb82', maxCount: 100 })
  })
  const trig = await trigRes.json()
  log('触发爬虫任务', trigRes.ok && trig.taskId, `taskId=${trig.taskId}`)

  // 4. 任务列表
  const tasksRes = await fetch(`${BASE}/api/admin/crawler/tasks`, { headers: auth })
  const tasks = await tasksRes.json()
  log('爬虫任务列表', tasksRes.ok && tasks.tasks?.length > 0, `${tasks.tasks?.length} 个任务`)

  // 5. 雷达图（公开接口）
  const analyzeRes = await fetch(`${BASE}/api/movies/Mef6dbb82/analyze`)
  const analyze = await analyzeRes.json()
  const aspects = analyze.aspects || {}
  const counts = Object.values(aspects).map(a => a.sampleCount)
  const allZero = counts.every(c => c === 0)
  log('雷达图数据', !allZero, `样本量 ${counts.join('/')}（不应全为 0）`)
  log('代表句', analyze.positiveComments?.length > 0,
    `好评 ${analyze.positiveComments?.length} 条 / 差评 ${analyze.negativeComments?.length} 条`)

  // 6. 一键重跑
  const rerunRes = await fetch(`${BASE}/api/admin/analysis/rerun-all`, {
    method: 'POST', headers: auth
  })
  const rerun = await rerunRes.json()
  log('一键重跑', rerunRes.ok, `count=${rerun.count}`)

  // 7. 提交评论（需要登录）
  const submitRes = await fetch(`${BASE}/api/comments/analyze`, {
    method: 'POST',
    headers: { ...auth, 'Content-Type': 'application/json' },
    body: JSON.stringify({ text: '剧情紧凑，演技在线，但是画面特效有点粗糙', movieId: 'Mef6dbb82' })
  })
  const submit = await submitRes.json()
  log('提交评论实时分析', submitRes.ok && submit.result,
    submit.result ? `${submit.result.polarity} (${(submit.result.confidence * 100).toFixed(0)}%) 触发词=${JSON.stringify(submit.result.triggerWords)}` : JSON.stringify(submit))
}

main().catch(e => console.error('❌ 验证失败:', e.message))

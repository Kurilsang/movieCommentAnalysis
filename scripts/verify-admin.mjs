import { chromium } from 'playwright'

const browser = await chromium.launch()
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
const page = await ctx.newPage()

const errors = []
page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message))

// 1. 登录
await page.goto('http://localhost:5173/login', { waitUntil: 'networkidle' })
await page.fill('input[placeholder="请输入用户名"]', 'admin')
await page.fill('input[placeholder="请输入密码"]', 'admin123')
await page.click('button:has-text("登录")')
await page.waitForTimeout(3000)
console.log(`登录后 URL: ${page.url()}`)
const token = await page.evaluate(() => localStorage.getItem('token'))
console.log(`token 已保存: ${token ? '是' : '否'}`)

// 2. 后台总览
await page.goto('http://localhost:5173/admin', { waitUntil: 'networkidle' })
await page.waitForTimeout(2500)
const dashText = (await page.textContent('body')).replace(/\s+/g, ' ').slice(0, 200)
console.log(`\n=== 后台总览 ===\n  ${dashText}`)
await page.screenshot({ path: '../docs/screenshots/后台总览.png' })

// 3. 爬虫页
await page.goto('http://localhost:5173/admin/crawler', { waitUntil: 'networkidle' })
await page.waitForTimeout(2500)
const crawlerText = (await page.textContent('body')).replace(/\s+/g, ' ').slice(0, 250)
console.log(`\n=== 爬虫任务页 ===\n  ${crawlerText}`)
await page.screenshot({ path: '../docs/screenshots/后台爬虫.png' })

// 4. 数据管理页
await page.goto('http://localhost:5173/admin/data', { waitUntil: 'networkidle' })
await page.waitForTimeout(2500)
const dataText = (await page.textContent('body')).replace(/\s+/g, ' ').slice(0, 250)
console.log(`\n=== 数据管理页 ===\n  ${dataText}`)
await page.screenshot({ path: '../docs/screenshots/后台数据.png' })

// 5. 提交页实际提交一条评论
await page.goto('http://localhost:5173/submit', { waitUntil: 'networkidle' })
await page.waitForTimeout(1500)
await page.fill('textarea', '剧情紧凑，演技在线，但是画面特效有点粗糙')
await page.click('button:has-text("开始分析")')
await page.waitForTimeout(4000)
const submitText = (await page.textContent('body')).replace(/\s+/g, ' ')
const hasResult = submitText.includes('正面') || submitText.includes('负面') || submitText.includes('中性')
console.log(`\n=== 提交评论 ===\n  出现分析结果: ${hasResult ? '是' : '否'}`)
console.log(`  页面文本: ${submitText.slice(100, 400)}`)
await page.screenshot({ path: '../docs/screenshots/提交分析.png' })

console.log(`\n页面错误: ${errors.length === 0 ? '无' : errors.join(' | ')}`)
await browser.close()

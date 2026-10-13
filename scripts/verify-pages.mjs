import { chromium } from 'playwright'

const pages = [
  { url: 'http://localhost:5173/', name: '首页' },
  { url: 'http://localhost:5173/movie/Mef6dbb82', name: '详情页' },
  { url: 'http://localhost:5173/submit', name: '提交页' },
  { url: 'http://localhost:5173/login', name: '登录页' },
  { url: 'http://localhost:5173/admin', name: '后台总览' }
]

const browser = await chromium.launch()
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })

for (const p of pages) {
  const page = await ctx.newPage()
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message))

  try {
    await page.goto(p.url, { waitUntil: 'networkidle', timeout: 20000 })
    await page.waitForTimeout(2500)

    const bodyText = (await page.textContent('body')) || ''
    const appHtml = (await page.innerHTML('#app')) || ''
    const visible = bodyText.replace(/\s+/g, ' ').trim().slice(0, 200)

    await page.screenshot({ path: `../docs/screenshots/${p.name}.png`, fullPage: false })

    console.log(`\n=== ${p.name} (${p.url}) ===`)
    console.log(`  #app 内容长度: ${appHtml.length}`)
    console.log(`  可见文本: ${visible || '(空)'}`)
    console.log(`  控制台错误: ${errors.length === 0 ? '无' : errors.slice(0, 3).join(' | ')}`)
  } catch (e) {
    console.log(`\n=== ${p.name} (${p.url}) ===`)
    console.log(`  ❌ 失败: ${e.message}`)
  }
  await page.close()
}

await browser.close()

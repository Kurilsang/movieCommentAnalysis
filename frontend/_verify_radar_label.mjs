import * as echarts from 'echarts'

// Real per-aspect sample counts for Interstellar (from replicating RuleBasedAspectAnalyzer on data/comments.csv)
const names = ['剧情', '演技', '画面特效', '配乐音效']
const counts = [27, 7, 26, 16]
const values = [54.1, 58.8, 52.8, 61.5]
const sufficient = names.map((n, i) => counts[i] >= 10)

const seen = []
const chart = echarts.init(null, null, { renderer: 'svg', ssr: true, width: 400, height: 300 })
chart.setOption({
  series: [{
    type: 'radar',
    data: [{
      value: values,
      name: '方面情感',
      label: {
        show: true,
        // EXACT formatter from MovieDetailPage.vue renderRadar()
        formatter: (params) => {
          seen.push({
            dataIndex: params.dataIndex,
            valueType: Array.isArray(params.value) ? 'array' : typeof params.value,
            value: params.value,
            countsLookup: counts[params.dataIndex]
          })
          return `${params.value}\n(${counts[params.dataIndex]}条)`
        }
      }
    }]
  }],
  radar: { indicator: names.map(n => ({ name: n, max: 100 })) }
})
chart.renderToSVGString()

console.log('formatter invoked', seen.length, 'times')
console.table(seen)

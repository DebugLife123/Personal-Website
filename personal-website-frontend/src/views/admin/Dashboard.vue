<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :style="{ borderTopColor: card.color }">
          <div class="stat-card-icon" :style="{ color: card.color, background: card.bg }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-card-info">
            <span class="stat-card-value">{{ card.value }}</span>
            <span class="stat-card-label">{{ card.label }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 上半部分：双面积图 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">浏览量趋势</span>
            <span class="chart-hint">近 7 天</span>
          </div>
          <div ref="pvChartRef" class="chart-container"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">访客数趋势</span>
          </div>
          <div ref="uvChartRef" class="chart-container"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 下半部分：横向柱状图 + 环形饼图 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">阅读量 TOP10</span>
          </div>
          <div ref="topArticlesRef" class="chart-container"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">文章分类分布</span>
          </div>
          <div ref="categoryRef" class="chart-container"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 底部 -->
    <div class="dashboard-footer">
      本站已稳定运行 <strong>{{ runDays }}</strong> 天
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'
import { View, User, TrendCharts, Plus, Document, ChatLineRound } from '@element-plus/icons-vue'

const pvChartRef = ref(null)
const uvChartRef = ref(null)
const topArticlesRef = ref(null)
const categoryRef = ref(null)
let pvChart = null
let uvChart = null
let topChart = null
let categoryChart = null

const statCards = ref([
  { icon: View, value: '-', label: '总浏览量', color: '#6c5fa0', bg: '#f0ecf6' },
  { icon: User, value: '-', label: '总访客数', color: '#5a8d7a', bg: '#eaf5f0' },
  { icon: TrendCharts, value: '-', label: '今日浏览', color: '#c08a5c', bg: '#f6f0e8' },
  { icon: Plus, value: '-', label: '今日新访客', color: '#6a8aaa', bg: '#e8eef6' },
  { icon: Document, value: '-', label: '文章总数', color: '#8a6a9a', bg: '#f2ecf6' },
  { icon: ChatLineRound, value: '-', label: '留言总数', color: '#9a7a6a', bg: '#f6f0ec' },
])

// 近7天数据（真实接口填充）
const days7 = ref(['-','-','-','-','-','-','-'])
const pvData = ref([0,0,0,0,0,0,0])
const uvData = ref([0,0,0,0,0,0,0])

// TOP10 文章（真实接口填充）
const topArticles = ref([])

// 文章分类分布（真实数据，来自文章列表聚合）
const categoryData = ref([])

// 运行天数
const startDate = new Date('2025-05-01')
const runDays = computed(() => {
  const diff = Date.now() - startDate.getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24))
})

// color palette
const primaryColor = '#6c5fa0'
const gradientColors = ['rgba(108,95,160,0.25)', 'rgba(108,95,160,0.02)']
const lightGray = '#e8e8ec'

const initPvChart = () => {
  if (!pvChartRef.value) return
  pvChart = echarts.init(pvChartRef.value)
  pvChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderWidth: 0 },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: days7.value,
      axisLine: { lineStyle: { color: lightGray } },
      axisLabel: { color: '#999', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { color: '#999', fontSize: 11 },
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: primaryColor, width: 2 },
      itemStyle: { color: primaryColor },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, gradientColors.map((c, i) => ({ offset: i, color: c })))
      },
      data: pvData.value,
    }],
  })
}

const initUvChart = () => {
  if (!uvChartRef.value) return
  uvChart = echarts.init(uvChartRef.value)
  uvChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderWidth: 0 },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: days7.value,
      axisLine: { lineStyle: { color: lightGray } },
      axisLabel: { color: '#999', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { color: '#999', fontSize: 11 },
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#5a8d7a', width: 2 },
      itemStyle: { color: '#5a8d7a' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(90,141,122,0.25)' },
          { offset: 1, color: 'rgba(90,141,122,0.02)' },
        ])
      },
      data: uvData.value,
    }],
  })
}

const initTopArticles = () => {
  if (!topArticlesRef.value) return
  topChart = echarts.init(topArticlesRef.value)
  const names = topArticles.value.map(a => a.name).reverse()
  const values = topArticles.value.map(a => a.value).reverse()
  topChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderWidth: 0,
    },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { color: '#999', fontSize: 10 },
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { show: false },
      axisLabel: { color: '#666', fontSize: 11, width: 80, overflow: 'truncate' },
      axisTick: { show: false },
    },
    series: [{
      type: 'bar',
      barWidth: 14,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#c8c4d8' },
          { offset: 1, color: primaryColor },
        ])
      },
      data: values,
    }],
  })
}

const initCategoryChart = () => {
  if (!categoryRef.value) return
  categoryChart = echarts.init(categoryRef.value)
  provinceChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderWidth: 0,
      formatter: '{b}: {c} ({d}%)',
    },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.15)' },
      },
      labelLine: { show: false },
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 2,
      },
      color: ['#6c5fa0', '#7a8aaa', '#5a8d7a', '#c08a5c', '#8a6a9a', '#6a8aaa', '#9a7a6a', '#b8968a', '#d0d0d8'],
      data: categoryData.value.map(d => ({ name: d.name, value: d.value })),
    }],
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#666', fontSize: 11 },
    },
  })
}

const resizeAll = () => {
  pvChart?.resize()
  uvChart?.resize()
  topChart?.resize()
  categoryChart?.resize()
}


// ---- 真实数据加载 ----
const loadStats = async () => {
  try {
    const [today, totalSt, arts, msgs, visSum] = await Promise.all([
      request.get('/statistic/today').then(r => r.data.code === 200 ? r.data.data : null).catch(() => null),
      request.get('/statistic/total').then(r => r.data.code === 200 ? r.data.data : null).catch(() => null),
      request.get('/article/list').then(r => r.data.code === 200 ? r.data.data : []).catch(() => []),
      request.get('/message/list').then(r => r.data.code === 200 ? r.data.data : []).catch(() => []),
      request.get('/visitor/summary').then(r => r.data.code === 200 ? r.data.data : null).catch(() => null),
    ])

    // 折线：最近 7 天（PV=访问次数，UV=去重 IP 数，使用本地时区日期）
    const days = []
    const pv = []
    const uv = []
    const visByDay = {}
    const uvByDay = {}
    if (visSum && Array.isArray(visSum.byDay)) {
      for (const d of visSum.byDay) {
        const key = String(d.visit_date || d.VISIT_DATE || d.visitDate).slice(0, 10)
        visByDay[key] = Number(d.cnt || d.CNT || 0)
      }
    }
    if (visSum && Array.isArray(visSum.byDayUv)) {
      for (const d of visSum.byDayUv) {
        const key = String(d.visit_date || d.VISIT_DATE || d.visitDate).slice(0, 10)
        uvByDay[key] = Number(d.uv || d.UV || 0)
      }
    }
    for (let i = 6; i >= 0; i--) {
      const dt = new Date(); dt.setDate(dt.getDate() - i)
      const iso = `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
      days.push(iso.slice(5).replace('-', '/'))
      pv.push(visByDay[iso] ?? 0)
      uv.push(uvByDay[iso] ?? 0)
    }
    days7.value = days
    pvData.value = pv
    uvData.value = uv

    // 顶部卡片
    const artsArr = Array.isArray(arts) ? arts : []
    const msgsArr = Array.isArray(msgs) ? msgs : []
    statCards.value = [
      { icon: View, value: totalSt ? totalSt.pageViews : '-', label: '总浏览量', color: '#6c5fa0', bg: '#f0ecf6' },
      { icon: User, value: totalSt ? totalSt.uniqueVisitors : '-', label: '总访客数', color: '#5a8d7a', bg: '#eaf5f0' },
      { icon: TrendCharts, value: today ? today.pageViews : '-', label: '今日浏览', color: '#c08a5c', bg: '#f6f0e8' },
      { icon: Plus, value: today ? today.uniqueVisitors : '-', label: '今日新访客', color: '#6a8aaa', bg: '#e8eef6' },
      { icon: Document, value: artsArr.length, label: '文章总数', color: '#8a6a9a', bg: '#f2ecf6' },
      { icon: ChatLineRound, value: msgsArr.length, label: '留言总数', color: '#9a7a6a', bg: '#f6f0ec' },
    ]

    // TOP 文章
    topArticles.value = artsArr
      .slice()
      .sort((a, b) => (b.views || 0) - (a.views || 0))
      .slice(0, 10)
      .map(a => ({ name: a.title, value: a.views || 0 }))

    // 文章分类分布（真实聚合）
    const catMap = {}
    for (const a of artsArr) {
      const c = (a.category || '').trim() || '未分类'
      catMap[c] = (catMap[c] || 0) + 1
    }
    categoryData.value = Object.entries(catMap)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  await loadStats()
  nextTick(() => {
    initPvChart()
    initUvChart()
    initTopArticles()
    initCategoryChart()
  })
  window.addEventListener('resize', resizeAll)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeAll)
  pvChart?.dispose()
  uvChart?.dispose()
  topChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 120px);
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  border-top: 3px solid #6c5fa0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
  margin-bottom: 12px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card-info {
  display: flex;
  flex-direction: column;
}

.stat-card-value {
  font-size: 1.3rem;
  font-weight: 700;
  color: #333;
  line-height: 1.3;
}

.stat-card-label {
  font-size: 0.78rem;
  color: #999;
}

/* Chart cards */
.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  margin-bottom: 12px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #444;
}

.chart-hint {
  font-size: 0.75rem;
  color: #bbb;
}

.chart-container {
  width: 100%;
  height: 260px;
}

/* Footer */
.dashboard-footer {
  text-align: center;
  padding: 20px 0;
  color: #aaa;
  font-size: 0.85rem;
}

.dashboard-footer strong {
  color: #6c5fa0;
  font-size: 1rem;
}

/* Responsive */
@media (max-width: 1200px) {
  .chart-container {
    height: 220px;
  }
}

@media (max-width: 768px) {
  .chart-container {
    height: 200px;
  }
  .chart-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>

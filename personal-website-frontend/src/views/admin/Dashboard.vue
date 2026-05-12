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
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="small"
              class="date-picker"
              value-format="YYYY-MM-DD"
              @change="updateCharts"
            />
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
            <span class="chart-title">访客省份分布</span>
          </div>
          <div ref="provinceRef" class="chart-container"></div>
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
import { View, User, TrendCharts, Plus, Document, ChatLineRound } from '@element-plus/icons-vue'

const pvChartRef = ref(null)
const uvChartRef = ref(null)
const topArticlesRef = ref(null)
const provinceRef = ref(null)
let pvChart = null
let uvChart = null
let topChart = null
let provinceChart = null

const dateRange = ref([])

const statCards = ref([
  { icon: View, value: '12,458', label: '总浏览量', color: '#6c5fa0', bg: '#f0ecf6' },
  { icon: User, value: '3,682', label: '总访客数', color: '#5a8d7a', bg: '#eaf5f0' },
  { icon: TrendCharts, value: '347', label: '今日浏览', color: '#c08a5c', bg: '#f6f0e8' },
  { icon: Plus, value: '28', label: '今日新访客', color: '#6a8aaa', bg: '#e8eef6' },
  { icon: Document, value: '8', label: '文章总数', color: '#8a6a9a', bg: '#f2ecf6' },
  { icon: ChatLineRound, value: '42', label: '评论总数', color: '#9a7a6a', bg: '#f6f0ec' },
])

// 模拟数据：近7天浏览量/访客数
const days7 = ['06', '07', '08', '09', '10', '11', '12']
const pvData = [820, 932, 901, 934, 1290, 1330, 1320]
const uvData = [320, 382, 401, 434, 590, 630, 620]

// TOP10 文章
const topArticles = [
  { name: 'Spring Boot 入门指南', value: 2340 },
  { name: 'Vue3 组合式 API 详解', value: 1890 },
  { name: '前端工程化实践', value: 1560 },
  { name: 'MySQL 性能优化技巧', value: 1420 },
  { name: 'Docker 容器化部署', value: 1280 },
  { name: 'JavaScript 异步编程', value: 1150 },
  { name: 'Git 工作流最佳实践', value: 980 },
  { name: 'RESTful API 设计规范', value: 870 },
  { name: 'TypeScript 入门', value: 760 },
  { name: 'Webpack 配置详解', value: 650 },
]

// 省份数据
const provinceData = [
  { name: '广东', value: 680 },
  { name: '江苏', value: 520 },
  { name: '浙江', value: 480 },
  { name: '北京', value: 420 },
  { name: '上海', value: 380 },
  { name: '四川', value: 290 },
  { name: '湖北', value: 240 },
  { name: '湖南', value: 210 },
  { name: '其他', value: 520 },
]

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
      data: days7,
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
      data: pvData,
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
      data: days7,
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
      data: uvData,
    }],
  })
}

const initTopArticles = () => {
  if (!topArticlesRef.value) return
  topChart = echarts.init(topArticlesRef.value)
  const names = topArticles.map(a => a.name).reverse()
  const values = topArticles.map(a => a.value).reverse()
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

const initProvince = () => {
  if (!provinceRef.value) return
  provinceChart = echarts.init(provinceRef.value)
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
      data: provinceData.map(d => ({ name: d.name, value: d.value })),
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
  provinceChart?.resize()
}

const updateCharts = () => {
  // In real app would fetch from API with date range
}

onMounted(() => {
  nextTick(() => {
    initPvChart()
    initUvChart()
    initTopArticles()
    initProvince()
  })
  window.addEventListener('resize', resizeAll)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeAll)
  pvChart?.dispose()
  uvChart?.dispose()
  topChart?.dispose()
  provinceChart?.dispose()
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

.date-picker {
  width: 210px;
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
  .date-picker {
    width: 100%;
  }
}
</style>

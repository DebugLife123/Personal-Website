<template>
  <div class="cat-page">
    <el-row :gutter="16">
      <!-- 分类 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="cat-card">
          <template #header>
            <div class="card-head"><span class="card-title">文章分类</span><el-tag size="small" type="info">{{ categories.length }} 个</el-tag></div>
          </template>
          <div v-loading="loading">
            <div v-if="categories.length" class="tag-list">
              <div v-for="c in categories" :key="c.name" class="cat-item">
                <el-tag size="large" effect="light" class="cat-tag">{{ c.name }}</el-tag>
                <span class="cat-count">{{ c.count }} 篇</span>
              </div>
            </div>
            <el-empty v-else description="暂无分类" :image-size="60" />
          </div>
          <p class="tip">分类来源于已发布文章的「分类」字段，在文章编辑器中填写即可新增。</p>
        </el-card>
      </el-col>

      <!-- 标签 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="cat-card">
          <template #header>
            <div class="card-head"><span class="card-title">文章标签</span><el-tag size="small" type="info">{{ tags.length }} 个</el-tag></div>
          </template>
          <div v-loading="loading">
            <div v-if="tags.length" class="tag-cloud">
              <el-tag v-for="t in tags" :key="t" class="cloud-tag" effect="plain" round>{{ t }}</el-tag>
            </div>
            <el-empty v-else description="暂无标签" :image-size="60" />
          </div>
          <p class="tip">标签来源于文章的「标签」字段（逗号分隔），随文章自动统计。</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 按分类统计 -->
    <el-card shadow="never" class="cat-card">
      <template #header><span class="card-title">分类文章数分布</span></template>
      <div ref="chartRef" class="chart"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import request from '../../utils/request'
import * as echarts from 'echarts'

const categories = ref([])
const tags = ref([])
const loading = ref(false)
const chartRef = ref(null)
let chart = null

const load = async () => {
  loading.value = true
  try {
    const [listRes, tagRes] = await Promise.all([
      request.get('/article/list'),
      request.get('/article/tags')
    ])
    if (listRes.data.code === 200) {
      const map = {}
      for (const a of listRes.data.data) {
        const c = a.category || '未分类'
        map[c] = (map[c] || 0) + 1
      }
      categories.value = Object.entries(map).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count)
    }
    if (tagRes.data.code === 200) tags.value = tagRes.data.data
  } finally { loading.value = false }
}

const renderChart = () => {
  if (!chartRef.value || !categories.value.length) return
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '6%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: categories.value.map(c => c.name), axisLabel: { color: '#666' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { type: 'dashed', color: '#eee' } } },
    series: [{
      type: 'bar', barWidth: 28,
      itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#6c5fa0' }, { offset: 1, color: '#c8c4d8' }]) },
      data: categories.value.map(c => c.count)
    }]
  })
}

const onResize = () => chart?.resize()
onMounted(async () => { await load(); await nextTick(); renderChart(); window.addEventListener('resize', onResize) })
onUnmounted(() => { window.removeEventListener('resize', onResize); chart?.dispose() })
</script>

<style scoped>
.cat-page { padding: 4px; }
.cat-card { border-radius: 10px; margin-bottom: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; color: #333; }
.tag-list { display: flex; flex-direction: column; gap: 12px; }
.cat-item { display: flex; align-items: center; gap: 12px; }
.cat-tag { min-width: 90px; text-align: center; }
.cat-count { color: #999; font-size: 0.85rem; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 10px; }
.cloud-tag { cursor: default; }
.tip { margin: 14px 0 0; font-size: 0.8rem; color: #aaa; }
.chart { height: 280px; }
</style>

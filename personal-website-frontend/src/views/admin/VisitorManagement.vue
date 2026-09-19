<template>
  <div class="visitor-page">
    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="sum-row">
      <el-col :span="8"><div class="sum-card"><div class="sum-num">{{ summary.total ?? '-' }}</div><div class="sum-label">累计访客记录</div></div></el-col>
      <el-col :span="8"><div class="sum-card"><div class="sum-num today">{{ summary.today ?? '-' }}</div><div class="sum-label">今日访问</div></div></el-col>
      <el-col :span="8"><div class="sum-card"><div class="sum-num">{{ weekTotal }}</div><div class="sum-label">近 7 天访问</div></div></el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <div class="table-head">
        <div class="left">
          <el-date-picker v-model="filterDate" type="date" value-format="YYYY-MM-DD" placeholder="按日期筛选" clearable size="default" style="width:180px" @change="load(1)" />
          <el-button @click="resetFilter">重置</el-button>
        </div>
        <el-popconfirm title="确定清空全部访客记录？" @confirm="clearAll">
          <template #reference><el-button type="danger" plain>清空记录</el-button></template>
        </el-popconfirm>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="path" label="访问路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="userAgent" label="设备 / 浏览器" min-width="220" show-overflow-tooltip />
        <el-table-column prop="visitDate" label="日期" width="120" />
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无访客记录" /></template>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" :current-page="page" @current-change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 15
const loading = ref(false)
const filterDate = ref('')
const summary = ref({})

const weekTotal = computed(() => (summary.value.byDay || []).reduce((s, d) => s + Number(d.cnt || d.CNT || 0), 0))

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const load = async (p = 1) => {
  page.value = p
  loading.value = true
  try {
    const res = await request.get('/visitor/page', { params: { page: p, pageSize, date: filterDate.value || undefined } })
    if (res.data.code === 200) { rows.value = res.data.data.records; total.value = res.data.data.total }
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}
const loadSummary = async () => {
  const res = await request.get('/visitor/summary')
  if (res.data.code === 200) summary.value = res.data.data
}
const resetFilter = () => { filterDate.value = ''; load(1) }
const clearAll = async () => {
  const res = await request.delete('/visitor/clear')
  if (res.data.code === 200) { ElMessage.success('已清空'); load(1); loadSummary() }
}
onMounted(() => { load(1); loadSummary() })
</script>

<style scoped>
.visitor-page { padding: 4px; }
.sum-row { margin-bottom: 16px; }
.sum-card { background: #fff; border-radius: 10px; padding: 20px; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.sum-num { font-size: 1.8rem; font-weight: 700; color: #6c5fa0; }
.sum-num.today { color: #5a8d7a; }
.sum-label { margin-top: 4px; color: #999; font-size: 0.85rem; }
.table-card { border-radius: 10px; }
.table-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.table-head .left { display: flex; gap: 10px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

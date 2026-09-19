<template>
  <div class="log-page">
    <el-card shadow="never" class="table-card">
      <div class="table-head">
        <el-input v-model="keyword" placeholder="按操作类型搜索，如：登录 / 文章 / 留言" clearable style="width:280px" @keyup.enter="load(1)" @clear="load(1)">
          <template #append><el-button @click="load(1)">搜索</el-button></template>
        </el-input>
        <el-popconfirm title="确定清空全部操作日志？" @confirm="clearAll">
          <template #reference><el-button type="danger" plain>清空日志</el-button></template>
        </el-popconfirm>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="action" label="操作" width="140">
          <template #default="{ row }"><el-tag size="small" effect="light">{{ row.action }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无操作日志" /></template>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" :current-page="page" @current-change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 15
const loading = ref(false)
const keyword = ref('')

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const load = async (p = 1) => {
  page.value = p
  loading.value = true
  try {
    const res = await request.get('/log/page', { params: { page: p, pageSize, action: keyword.value || undefined } })
    if (res.data.code === 200) { rows.value = res.data.data.records; total.value = res.data.data.total }
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}
const clearAll = async () => {
  const res = await request.delete('/log/clear')
  if (res.data.code === 200) { ElMessage.success('已清空'); load(1) }
}
onMounted(() => load(1))
</script>

<style scoped>
.log-page { padding: 4px; }
.table-card { border-radius: 10px; }
.table-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

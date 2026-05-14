<template>
  <div class="message-management">
    <!-- 顶部筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-input
          v-model="query.keyword"
          placeholder="搜索昵称/留言内容..."
          :prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable class="filter-select" @change="search">
          <el-option label="全部" value="all" />
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="date-picker"
          @change="search"
        />
      </div>
      <div class="filter-right">
        <el-button :icon="Refresh" @click="search">刷新</el-button>
      </div>
    </div>

    <!-- 批量操作 -->
    <div class="batch-bar" v-if="selectedIds.length > 0">
      <span class="batch-info">已选择 {{ selectedIds.length }} 项</span>
      <el-button size="small" type="success" @click="batchApprove">批量通过</el-button>
      <el-button size="small" type="warning" @click="batchReject">批量驳回</el-button>
      <el-button size="small" type="danger" @click="batchDelete">批量删除</el-button>
    </div>

    <!-- 留言表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        ref="tableRef"
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#fafafa', color: '#555', fontWeight: 600 }"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column type="index" label="#" width="48" align="center" />
        <el-table-column prop="nickname" label="昵称" width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="nickname-cell">
              <el-avatar :size="24" :src="row.avatar" class="nickname-avatar">
                {{ row.nickname?.charAt(0)?.toUpperCase() || '?' }}
              </el-avatar>
              <span>{{ row.nickname }}</span>
              <el-tag v-if="row.adminPost" size="small" effect="plain" class="admin-tag-mini">管理员</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="留言内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="content-cell">
              <span class="content-text">{{ row.content }}</span>
              <el-tag v-if="row.replyCount > 0" size="small" type="success" effect="plain" class="replied-tag">
                已回复 {{ row.replyCount }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="留言时间" width="170" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" :icon="View" @click="viewDetail(row)">查看</el-button>
            <el-button text size="small" :icon="ChatLineSquare" @click="openReply(row)">回复</el-button>
            <el-button
              v-if="row.status === 'pending'"
              text size="small"
              type="success"
              :icon="Select"
              @click="approve(row)"
            >通过</el-button>
            <el-button
              v-if="row.status === 'approved'"
              text size="small"
              type="warning"
              :icon="CloseBold"
              @click="reject(row)"
            >驳回</el-button>
            <el-button text size="small" type="danger" :icon="Delete" @click="deleteMsg(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" :image-size="60" description="暂无留言数据" />

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          small
          @change="fetchData"
        />
      </div>
    </el-card>

    <!-- ========== 详情弹窗 ========== -->
    <el-dialog v-model="detailVisible" title="留言详情" width="680px" :close-on-click-modal="false" destroy-on-close>
      <div class="detail-body" v-if="detailMsg">
        <div class="detail-section">
          <div class="detail-label">访客信息</div>
          <div class="detail-card">
            <div class="detail-user">
              <el-avatar :size="48" :src="detailMsg.avatar">
                {{ detailMsg.nickname?.charAt(0)?.toUpperCase() || '?' }}
              </el-avatar>
              <div class="detail-user-info">
                <span class="detail-nickname">{{ detailMsg.nickname }}</span>
                <span class="detail-time">{{ detailMsg.createTime?.replace('T', ' ') }}</span>
              </div>
              <el-tag :type="statusType(detailMsg.status)" size="small" effect="light">{{ statusLabel(detailMsg.status) }}</el-tag>
            </div>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">留言内容</div>
          <div class="detail-card detail-content">{{ detailMsg.content }}</div>
        </div>
        <div class="detail-section" v-if="detailMsg.replies && detailMsg.replies.length > 0">
          <div class="detail-label">回复记录 ({{ detailMsg.replies.length }})</div>
          <div class="detail-card">
            <div v-for="reply in detailMsg.replies" :key="reply.id" class="detail-reply-item">
              <el-avatar :size="28" :src="reply.avatar" class="detail-reply-avatar">
                {{ reply.nickname?.charAt(0)?.toUpperCase() || '?' }}
              </el-avatar>
              <div class="detail-reply-body">
                <div class="detail-reply-header">
                  <span class="detail-reply-nickname">{{ reply.nickname }}</span>
                  <span class="detail-reply-time">{{ reply.createTime?.replace('T', ' ') }}</span>
                </div>
                <div class="detail-reply-content">{{ reply.content }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- ========== 回复弹窗 ========== -->
    <el-dialog v-model="replyVisible" title="回复留言" width="560px" :close-on-click-modal="false" destroy-on-close>
      <div class="reply-dialog-body" v-if="replyMsg">
        <div class="reply-original">
          <div class="reply-original-label">原留言</div>
          <div class="reply-original-card">
            <div class="reply-original-header">
              <el-avatar :size="32" :src="replyMsg.avatar">
                {{ replyMsg.nickname?.charAt(0)?.toUpperCase() || '?' }}
              </el-avatar>
              <span class="reply-original-nickname">{{ replyMsg.nickname }}</span>
              <span class="reply-original-time">{{ replyMsg.createTime?.replace('T', ' ') }}</span>
            </div>
            <div class="reply-original-content">{{ replyMsg.content }}</div>
          </div>
        </div>
        <div class="reply-editor">
          <div class="reply-editor-label">回复内容</div>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="5"
            placeholder="输入回复内容..."
            maxlength="1000"
            show-word-limit
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replySubmitting" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import {
  Search, Refresh, View, ChatLineSquare, Select, CloseBold, Delete
} from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedIds = ref([])
const dateRange = ref([])
const tableRef = ref(null)

// 详情弹窗
const detailVisible = ref(false)
const detailMsg = ref(null)

// 回复弹窗
const replyVisible = ref(false)
const replyMsg = ref(null)
const replyContent = ref('')
const replySubmitting = ref(false)

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: 'all',
  startDate: '',
  endDate: '',
})

const statusLabel = (s) => {
  const map = { pending: '待审核', approved: '已通过', rejected: '已驳回', deleted: '已删除' }
  return map[s] || s || '未知'
}

const statusType = (s) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger', deleted: 'info' }
  return map[s] || 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    } else {
      params.startDate = ''
      params.endDate = ''
    }
    const res = await request.get('/message/admin/page', { params })
    if (res.data.code === 200) {
      tableData.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取留言列表失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.page = 1
  fetchData()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(s => s.id)
}

// ---------- 查看详情 ----------
const viewDetail = async (row) => {
  try {
    const res = await request.get(`/message/admin/${row.id}`)
    if (res.data.code === 200) {
      detailMsg.value = res.data.data
      detailVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

// ---------- 回复 ----------
const openReply = (row) => {
  replyMsg.value = row
  replyContent.value = ''
  replyVisible.value = true
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    return ElMessage.warning('请输入回复内容')
  }
  replySubmitting.value = true
  try {
    const res = await request.post('/message/admin/reply', {
      content: replyContent.value,
      parentId: replyMsg.value.id,
      replyTo: replyMsg.value.nickname,
    })
    if (res.data.code === 200) {
      ElMessage.success('回复成功')
      replyVisible.value = false
      fetchData()
    }
  } catch (e) {
    ElMessage.error('回复失败')
  } finally {
    replySubmitting.value = false
  }
}

// ---------- 审核 ----------
const approve = async (row) => {
  try {
    const res = await request.put(`/message/admin/status/${row.id}?status=approved`)
    if (res.data.code === 200) {
      ElMessage.success('已通过')
      fetchData()
    }
  } catch (e) { ElMessage.error('操作失败') }
}

const reject = async (row) => {
  try {
    const res = await request.put(`/message/admin/status/${row.id}?status=rejected`)
    if (res.data.code === 200) {
      ElMessage.success('已驳回')
      fetchData()
    }
  } catch (e) { ElMessage.error('操作失败') }
}

// ---------- 删除 ----------
const deleteMsg = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.nickname}」的留言吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await request.delete(`/message/delete/${row.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除')
      fetchData()
    }
  } catch (e) { /* cancelled */ }
}

// ---------- 批量操作 ----------
const batchApprove = async () => {
  await doBatch('approve', '批量通过')
}
const batchReject = async () => {
  await doBatch('reject', '批量驳回')
}
const batchDelete = async () => {
  await doBatch('delete', '批量删除')
}

const doBatch = async (action, label) => {
  try {
    await ElMessageBox.confirm(
      `确定要${label}选中的 ${selectedIds.value.length} 条留言吗？`,
      `${label}确认`,
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await request.post('/message/admin/batch', { ids: selectedIds.value, action })
    if (res.data.code === 200) {
      ElMessage.success(`${label}成功`)
      selectedIds.value = []
      fetchData()
    }
  } catch (e) { /* cancelled */ }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.message-management {
  min-height: calc(100vh - 120px);
}

/* ===== Filter Bar ===== */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}
.search-input { width: 240px; }
.filter-select { width: 120px; }
.date-picker { width: 240px; }

/* ===== Batch Bar ===== */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px 16px;
  background: #f8f6fc;
  border-radius: 8px;
}
.batch-info {
  font-size: 0.85rem;
  color: #6c5fa0;
  font-weight: 500;
}

/* ===== Table ===== */
.table-card {
  border-radius: 12px;
  padding-bottom: 4px;
}

.nickname-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nickname-avatar {
  flex-shrink: 0;
}
.admin-tag-mini {
  font-size: 0.65rem;
  padding: 0 4px;
  height: 18px;
  line-height: 18px;
}

.content-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.content-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.replied-tag {
  flex-shrink: 0;
  font-size: 0.68rem;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
}

.time-text {
  font-size: 0.85rem;
  color: #888;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

/* ===== Detail Dialog ===== */
.detail-body {
  max-height: 60vh;
  overflow-y: auto;
}
.detail-section {
  margin-bottom: 20px;
}
.detail-label {
  font-size: 0.85rem;
  color: #999;
  margin-bottom: 8px;
  font-weight: 500;
}
.detail-card {
  background: #fafafc;
  border-radius: 10px;
  padding: 16px;
}
.detail-content {
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}
.detail-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.detail-user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}
.detail-nickname {
  font-weight: 600;
  color: #333;
}
.detail-time {
  font-size: 0.82rem;
  color: #999;
}

/* detail replies */
.detail-reply-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.detail-reply-item:last-child { border-bottom: none; }
.detail-reply-avatar { flex-shrink: 0; }
.detail-reply-body { flex: 1; }
.detail-reply-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.detail-reply-nickname {
  font-weight: 600;
  font-size: 0.88rem;
  color: #333;
}
.detail-reply-time {
  font-size: 0.78rem;
  color: #999;
}
.detail-reply-content {
  color: #555;
  line-height: 1.7;
  font-size: 0.9rem;
}

/* ===== Reply Dialog ===== */
.reply-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.reply-original-label,
.reply-editor-label {
  font-size: 0.82rem;
  color: #999;
  margin-bottom: 8px;
  font-weight: 500;
}
.reply-original-card {
  background: #fafafc;
  border-radius: 10px;
  padding: 14px;
  border: 1px solid #f0f0f0;
}
.reply-original-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.reply-original-nickname {
  font-weight: 600;
  color: #333;
  flex: 1;
}
.reply-original-time {
  font-size: 0.8rem;
  color: #999;
}
.reply-original-content {
  color: #555;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== Dark mode ===== */
html.dark .detail-card,
html.dark .reply-original-card {
  background: #1a1a20;
  color: #c8c4cc;
}
html.dark .detail-content,
html.dark .reply-original-content,
html.dark .detail-reply-content {
  color: #c8c4cc;
}
html.dark .detail-nickname,
html.dark .reply-original-nickname,
html.dark .detail-reply-nickname {
  color: #ece8e4;
}
html.dark .detail-reply-item {
  border-bottom-color: #24242c;
}
html.dark .batch-bar {
  background: #1a1a20;
}
</style>

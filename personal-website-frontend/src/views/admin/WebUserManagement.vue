<template>
  <div class="webuser-page">
    <el-card shadow="never" class="main-card">
      <!-- 说明 -->
      <div class="intro">
        <el-icon><InfoFilled /></el-icon>
        <span>
          访客用「形象 + 昵称」直接同步身份，<b>昵称即唯一标识</b>。
          同名即同一身份，因此这里按昵称管理：可禁言、改名（纠正冒充）、删除。
        </span>
      </div>

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="total">共 {{ total }} 个身份</span>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="keyword"
            placeholder="搜索昵称 / IP"
            clearable
            style="width: 220px"
            @keyup.enter="loadUsers(1)"
            @clear="loadUsers(1)"
          />
          <el-button type="primary" @click="loadUsers(1)">搜索</el-button>
          <el-button @click="loadUsers(pager.page)">刷新</el-button>
        </div>
      </div>

      <!-- 身份表 -->
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="身份" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-cell-avatar">
                <img v-if="avatarUrl(row.avatar)" :src="avatarUrl(row.avatar)" alt="" />
                <template v-else>{{ (row.nickname || '?').charAt(0) }}</template>
              </div>
              <div class="user-cell-text">
                <span class="user-cell-name">{{ row.nickname }}</span>
                <span class="user-cell-username">ID {{ row.id }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="messageCount" label="留言" width="76" align="center" />
        <el-table-column prop="loginCount" label="同步次数" width="90" align="center" />
        <el-table-column prop="createTime" label="首次同步" width="160" />
        <el-table-column prop="lastLoginTime" label="最近同步" width="160">
          <template #default="{ row }">{{ row.lastLoginTime || '—' }}</template>
        </el-table-column>
        <el-table-column prop="lastLoginIp" label="最近 IP" width="128">
          <template #default="{ row }">{{ row.lastLoginIp || '—' }}</template>
        </el-table-column>
        <el-table-column label="设备" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ deviceOf(row.lastLoginUa) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : 'danger'" size="small">
              {{ row.status === 'normal' ? '正常' : '已禁言' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 'normal' ? 'warning' : 'success'"
              text
              @click="toggleBan(row)"
            >
              {{ row.status === 'normal' ? '禁言' : '解禁' }}
            </el-button>
            <el-button size="small" type="primary" text @click="openRename(row)">改名</el-button>
            <el-button size="small" type="danger" text @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有访客同步过身份" />
        </template>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pager.pageSize"
          :current-page="pager.page"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 改名弹窗 -->
    <el-dialog v-model="renameDialog.show" title="修改昵称" width="440px">
      <p class="dialog-tip">
        原昵称：<b>{{ renameDialog.user?.nickname }}</b><br />
        改名后该身份的登录令牌会失效，需要用新昵称重新同步；历史留言保留（留言里存的是当时昵称快照）。
      </p>
      <el-input
        v-model="renameDialog.nickname"
        placeholder="新昵称（2-20 个字符）"
        maxlength="20"
        show-word-limit
      />
      <template #footer>
        <el-button @click="renameDialog.show = false">取消</el-button>
        <el-button type="primary" :loading="renameDialog.saving" @click="confirmRename">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { avatarUrl } from '../../utils/avatar'

const users = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const pager = reactive({ page: 1, pageSize: 10 })

const loadUsers = async (page = 1) => {
  pager.page = page
  loading.value = true
  try {
    const res = await request.get('/webuser/page', {
      params: { page, pageSize: pager.pageSize, keyword: keyword.value || undefined }
    })
    if (res.data.code === 200) {
      users.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 从 UA 粗略识别设备，便于核对同一昵称是否被多人使用
const deviceOf = (ua) => {
  if (!ua) return '—'
  const s = ua.toLowerCase()
  const os = s.includes('windows') ? 'Windows'
    : s.includes('mac os') ? 'macOS'
    : s.includes('android') ? 'Android'
    : s.includes('iphone') || s.includes('ipad') ? 'iOS'
    : s.includes('linux') ? 'Linux' : '未知系统'
  const br = s.includes('edg/') ? 'Edge'
    : s.includes('chrome') ? 'Chrome'
    : s.includes('firefox') ? 'Firefox'
    : s.includes('safari') ? 'Safari' : '其他浏览器'
  return `${os} · ${br}`
}

const toggleBan = async (row) => {
  const ban = row.status === 'normal'
  try {
    if (ban) {
      await ElMessageBox.confirm(
        `禁言后「${row.nickname}」将无法同步身份和留言，确定继续？`,
        '禁言确认',
        { confirmButtonText: '禁言', cancelButtonText: '取消', type: 'warning' }
      )
    }
    const res = await request.post('/webuser/status', { id: row.id, status: ban ? 'banned' : 'normal' })
    if (res.data.code === 200) {
      ElMessage.success(res.data.message)
      await loadUsers(pager.page)
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch { /* 取消或失败 */ }
}

const renameDialog = reactive({ show: false, user: null, nickname: '', saving: false })

const openRename = (row) => {
  renameDialog.user = row
  renameDialog.nickname = row.nickname
  renameDialog.show = true
}

const confirmRename = async () => {
  const name = renameDialog.nickname.trim()
  if (name.length < 2 || name.length > 20) {
    ElMessage.warning('昵称需 2-20 个字符')
    return
  }
  renameDialog.saving = true
  try {
    const res = await request.post('/webuser/rename', { id: renameDialog.user.id, nickname: name })
    if (res.data.code === 200) {
      ElMessage.success(res.data.message)
      renameDialog.show = false
      await loadUsers(pager.page)
    } else {
      ElMessage.error(res.data.message || '改名失败')
    }
  } catch {
    ElMessage.error('改名失败')
  } finally {
    renameDialog.saving = false
  }
}

const removeUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除后「${row.nickname}」无法恢复；其 ${row.messageCount} 条留言会保留但转为匿名。确定删除？`,
      '删除身份',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error' }
    )
    const res = await request.delete(`/webuser/${row.id}`)
    if (res.data.code === 200) {
      ElMessage.success(res.data.message)
      await loadUsers(pager.page)
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch { /* 取消或失败 */ }
}

onMounted(() => loadUsers())
</script>

<style scoped>
.webuser-page { display: flex; flex-direction: column; gap: 16px; }
.main-card { border-radius: 12px; }

.intro {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 18px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f8fb;
  color: #6b7488;
  font-size: 0.82rem;
  line-height: 1.7;
}
.intro b { color: #303133; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left .total { font-size: 0.9rem; color: #666; }
.toolbar-right { display: flex; gap: 10px; }

.user-cell { display: flex; align-items: center; gap: 10px; }
.user-cell-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: #eef1f7;
  color: #6b7488;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  flex-shrink: 0;
}
.user-cell-avatar img { width: 100%; height: 100%; object-fit: cover; }
.user-cell-text { display: flex; flex-direction: column; }
.user-cell-name { font-size: 0.9rem; font-weight: 600; color: #303133; }
.user-cell-username { font-size: 0.72rem; color: #909399; }

.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.dialog-tip { margin: 0 0 14px; font-size: 0.85rem; color: #606266; line-height: 1.8; }
.dialog-tip b { color: #303133; }
</style>

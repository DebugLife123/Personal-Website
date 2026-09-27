<template>
  <div class="webuser-page">
    <el-card shadow="never" class="main-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="total">共 {{ total }} 位注册用户</span>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名 / 昵称 / 邮箱"
            clearable
            style="width: 240px"
            @keyup.enter="loadUsers(1)"
            @clear="loadUsers(1)"
          />
          <el-button type="primary" @click="loadUsers(1)">搜索</el-button>
          <el-button @click="loadUsers(pager.page)">刷新</el-button>
        </div>
      </div>

      <!-- 用户表 -->
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-cell-avatar">{{ (row.nickname || row.username || '?').charAt(0).toUpperCase() }}</div>
              <div class="user-cell-text">
                <span class="user-cell-name">{{ row.nickname || row.username }}</span>
                <span class="user-cell-username">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column prop="messageCount" label="留言数" width="90" align="center" />
        <el-table-column prop="createTime" label="注册时间" width="165" />
        <el-table-column prop="lastLoginTime" label="最近登录" width="165">
          <template #default="{ row }">{{ row.lastLoginTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : 'danger'" size="small">
              {{ row.status === 'normal' ? '正常' : '已禁言' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 'normal' ? 'warning' : 'success'"
              text
              @click="toggleBan(row)"
            >
              {{ row.status === 'normal' ? '禁言' : '解禁' }}
            </el-button>
            <el-button size="small" type="primary" text @click="openReset(row)">重置密码</el-button>
            <el-button size="small" type="danger" text @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有注册用户" />
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

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetDialog.show" title="重置用户密码" width="420px">
      <p class="reset-tip">
        将为用户 <b>@{{ resetDialog.user?.username }}</b>（{{ resetDialog.user?.nickname }}）设置新密码，
        该用户的所有登录状态将立即失效。
      </p>
      <el-input
        v-model="resetDialog.password"
        type="password"
        placeholder="新密码（至少 6 位）"
        show-password
        maxlength="64"
      />
      <template #footer>
        <el-button @click="resetDialog.show = false">取消</el-button>
        <el-button type="primary" :loading="resetDialog.saving" @click="confirmReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

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

const toggleBan = async (row) => {
  const ban = row.status === 'normal'
  try {
    if (ban) {
      await ElMessageBox.confirm(
        `禁言后 @${row.username} 将无法登录和留言，确定继续？`,
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

const resetDialog = reactive({ show: false, user: null, password: '', saving: false })
const openReset = (row) => {
  resetDialog.user = row
  resetDialog.password = ''
  resetDialog.show = true
}
const confirmReset = async () => {
  if (!resetDialog.password || resetDialog.password.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  resetDialog.saving = true
  try {
    const res = await request.post('/webuser/resetPassword', {
      id: resetDialog.user.id,
      newPassword: resetDialog.password
    })
    if (res.data.code === 200) {
      ElMessage.success('已重置，请告知用户新密码')
      resetDialog.show = false
    } else {
      ElMessage.error(res.data.message || '重置失败')
    }
  } catch {
    ElMessage.error('重置失败')
  } finally {
    resetDialog.saving = false
  }
}

const removeUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除后 @${row.username} 无法恢复；其 ${row.messageCount} 条留言会保留但转为匿名。确定删除？`,
      '删除用户',
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
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  font-weight: 700;
  flex-shrink: 0;
}
.user-cell-text { display: flex; flex-direction: column; }
.user-cell-name { font-size: 0.9rem; font-weight: 600; color: #303133; }
.user-cell-username { font-size: 0.75rem; color: #909399; }

.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.reset-tip { margin: 0 0 14px; font-size: 0.88rem; color: #555; line-height: 1.7; }
</style>

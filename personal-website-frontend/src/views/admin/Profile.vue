<template>
  <div class="profile-page" v-loading="loading">
    <div class="page-heading">
      <div>
        <h2>个人资料</h2>
        <p>维护公开简历的基本信息、教育经历、实习经历和项目经历。</p>
      </div>
      <el-button type="primary" :loading="savingProfile" @click="saveProfile">
        <el-icon><Check /></el-icon>
        保存基本资料
      </el-button>
    </div>

    <el-card shadow="never" class="profile-card">
      <template #header><span class="card-title">基本资料</span></template>
      <el-form :model="profile" label-width="90px" class="profile-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8"><el-form-item label="姓名"><el-input v-model="profile.name" placeholder="请输入姓名" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8"><el-form-item label="邮箱"><el-input v-model="profile.email" placeholder="请输入邮箱" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8"><el-form-item label="电话"><el-input v-model="profile.phone" placeholder="请输入电话" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8"><el-form-item label="地址"><el-input v-model="profile.address" placeholder="请输入所在地" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="16"><el-form-item label="头像 URL"><el-input v-model="profile.avatar" placeholder="请输入头像图片地址" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="个人简介"><el-input v-model="profile.intro" type="textarea" :rows="2" placeholder="一句话介绍自己" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="技能"><el-input v-model="profile.skill" placeholder="多个技能用逗号分隔，如 JavaScript, Vue, SpringBoot" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="奖项"><el-input v-model="profile.award" type="textarea" :rows="2" placeholder="请输入奖项或证书，可留空" /></el-form-item></el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="profile-card entries-card">
      <template #header>
        <div class="entries-header">
          <div>
            <span class="card-title">经历管理</span>
            <span class="entry-count">共 {{ entries.length }} 条</span>
          </div>
          <el-button type="primary" plain @click="openCreateDialog"><el-icon><Plus /></el-icon>新增经历</el-button>
        </div>
      </template>

      <el-tabs v-model="activeType" @tab-change="handleTypeChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="`${tab.label} (${countByType(tab.value)})`" :name="tab.value" />
      </el-tabs>

      <el-table :data="visibleEntries" stripe empty-text="暂无经历，点击右上角新增">
        <el-table-column prop="title" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="subtitle" label="专业 / 职位 / 角色" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.subtitle || '—' }}</template>
        </el-table-column>
        <el-table-column prop="timeRange" label="时间" width="180"><template #default="{ row }">{{ row.timeRange || '—' }}</template></el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip><template #default="{ row }">{{ row.description || '—' }}</template></el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="150" fixed="right" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeEntry(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑经历' : '新增经历'" width="560px" destroy-on-close>
      <el-form ref="entryFormRef" :model="entryForm" :rules="entryRules" label-width="92px">
        <el-form-item label="经历类型" prop="type"><el-select v-model="entryForm.type" class="full-width"><el-option v-for="tab in tabs" :key="tab.value" :label="tab.label" :value="tab.value" /></el-select></el-form-item>
        <el-form-item label="名称" prop="title"><el-input v-model="entryForm.title" placeholder="学校 / 公司 / 项目名称" /></el-form-item>
        <el-form-item label="专业 / 职位 / 角色"><el-input v-model="entryForm.subtitle" placeholder="如：软件工程 / 算法工程师助理 / 全栈开发" /></el-form-item>
        <el-form-item label="时间"><el-input v-model="entryForm.timeRange" placeholder="如：2023.09 ~ 至今" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="entryForm.description" type="textarea" :rows="4" placeholder="补充经历详情" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="entryForm.sort" :min="0" :max="999" controls-position="right" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="savingEntry" @click="submitEntry">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Plus } from '@element-plus/icons-vue'
import request from '../../utils/request'

const tabs = [
  { label: '教育经历', value: 'education' },
  { label: '实习 / 工作', value: 'work' },
  { label: '项目经历', value: 'project' }
]
const loading = ref(false)
const savingProfile = ref(false)
const savingEntry = ref(false)
const activeType = ref('education')
const entries = ref([])
const profile = reactive({ id: 1, name: '', avatar: '', email: '', phone: '', address: '', intro: '', skill: '', award: '' })
const dialogVisible = ref(false)
const editingId = ref(null)
const entryFormRef = ref()
const entryForm = reactive({ type: 'education', title: '', subtitle: '', timeRange: '', description: '', sort: 0 })
const entryRules = {
  type: [{ required: true, message: '请选择经历类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}
const visibleEntries = computed(() => entries.value.filter(item => item.type === activeType.value))

const countByType = type => entries.value.filter(item => item.type === type).length

async function loadData() {
  loading.value = true
  try {
    const [profileRes, entriesRes] = await Promise.all([request.get('/resume/get'), request.get('/resume/entries')])
    if (profileRes.data.code === 200 && profileRes.data.data) Object.assign(profile, profileRes.data.data)
    if (entriesRes.data.code === 200) entries.value = entriesRes.data.data || []
  } catch (error) {
    console.error(error)
    ElMessage.error('资料加载失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!profile.name?.trim()) return ElMessage.warning('姓名不能为空')
  savingProfile.value = true
  try {
    const res = await request.post('/resume/update', { ...profile })
    if (res.data.code === 200) ElMessage.success('基本资料已保存')
    else ElMessage.error(res.data.message || '保存失败')
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败，请重试')
  } finally {
    savingProfile.value = false
  }
}

function resetEntryForm() {
  Object.assign(entryForm, { type: activeType.value, title: '', subtitle: '', timeRange: '', description: '', sort: 0 })
  editingId.value = null
  entryFormRef.value?.clearValidate()
}
function openCreateDialog() { resetEntryForm(); dialogVisible.value = true }
function openEditDialog(row) { Object.assign(entryForm, row); editingId.value = row.id; dialogVisible.value = true }
function handleTypeChange() { /* tab state is enough; retained for clarity */ }

async function submitEntry() {
  if (!entryFormRef.value) return
  await entryFormRef.value.validate(async valid => {
    if (!valid) return
    savingEntry.value = true
    try {
      const payload = { type: entryForm.type, title: entryForm.title.trim(), subtitle: entryForm.subtitle, timeRange: entryForm.timeRange, description: entryForm.description, sort: entryForm.sort }
      const res = editingId.value
        ? await request.put(`/resume/entries/${editingId.value}`, payload)
        : await request.post('/resume/entries', payload)
      if (res.data.code !== 200) return ElMessage.error(res.data.message || '保存失败')
      ElMessage.success(editingId.value ? '经历已更新' : '经历已新增')
      dialogVisible.value = false
      await loadData()
    } catch (error) {
      console.error(error)
      ElMessage.error('保存经历失败，请重试')
    } finally {
      savingEntry.value = false
    }
  })
}

async function removeEntry(row) {
  try {
    await ElMessageBox.confirm(`确定删除“${row.title}”吗？删除后不可恢复。`, '删除确认', { type: 'warning' })
    const res = await request.delete(`/resume/entries/${row.id}`)
    if (res.data.code !== 200) return ElMessage.error(res.data.message || '删除失败')
    ElMessage.success('经历已删除')
    await loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') console.error(error)
  }
}

onMounted(loadData)
</script>

<style scoped>
.profile-page { padding: 24px; max-width: 1500px; margin: 0 auto; }
.page-heading, .entries-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-heading { margin-bottom: 20px; }
.page-heading h2 { margin: 0 0 8px; font-size: 24px; color: var(--el-text-color-primary); }
.page-heading p { margin: 0; color: var(--el-text-color-secondary); }
.profile-card { margin-bottom: 20px; border-radius: 12px; }
.card-title { font-weight: 700; color: var(--el-text-color-primary); }
.profile-form { padding-top: 10px; }
.entry-count { color: var(--el-text-color-secondary); font-size: 13px; margin-left: 10px; }
.full-width { width: 100%; }
@media (max-width: 768px) { .profile-page { padding: 14px; } .page-heading { align-items: flex-start; flex-direction: column; } .page-heading .el-button { align-self: stretch; } .entries-header { align-items: flex-start; flex-direction: column; } }
</style>

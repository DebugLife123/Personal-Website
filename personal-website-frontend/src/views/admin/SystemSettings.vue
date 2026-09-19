<template>
  <div class="settings-page">
    <el-row :gutter="16">
      <!-- 站点设置 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="set-card">
          <template #header><span class="card-title">站点设置</span></template>
          <el-form :model="form" label-width="96px" v-loading="loading">
            <el-form-item label="站点标题"><el-input v-model="form.site_title" /></el-form-item>
            <el-form-item label="站点简介"><el-input v-model="form.site_description" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="备案号"><el-input v-model="form.icp" placeholder="如：湘ICP备xxxx号" /></el-form-item>
            <el-form-item label="页脚文案"><el-input v-model="form.footer_text" /></el-form-item>
            <el-form-item label="音乐自动播放">
              <el-switch v-model="autoplay" active-text="开" inactive-text="关" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 修改密码 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="set-card">
          <template #header><span class="card-title">修改密码</span></template>
          <el-form :model="pwd" label-width="96px">
            <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" /></el-form-item>
            <el-form-item label="确认新密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="changing" @click="changePwd">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const form = ref({ site_title: '', site_description: '', icp: '', footer_text: '', music_autoplay: 'false' })
const autoplay = ref(false)
const loading = ref(false)
const saving = ref(false)

const pwd = ref({ oldPassword: '', newPassword: '', confirm: '' })
const changing = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/setting/all')
    if (res.data.code === 200) {
      form.value = { ...form.value, ...res.data.data }
      autoplay.value = form.value.music_autoplay === 'true'
    }
  } catch { ElMessage.error('读取设置失败') } finally { loading.value = false }
}

const save = async () => {
  saving.value = true
  try {
    form.value.music_autoplay = autoplay.value ? 'true' : 'false'
    const res = await request.post('/setting/save', form.value)
    if (res.data.code === 200) ElMessage.success('设置已保存')
    else ElMessage.error(res.data.message || '保存失败')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const changePwd = async () => {
  if (!pwd.value.oldPassword || !pwd.value.newPassword) { ElMessage.warning('请填写完整'); return }
  if (pwd.value.newPassword.length < 6) { ElMessage.warning('新密码至少 6 位'); return }
  if (pwd.value.newPassword !== pwd.value.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  changing.value = true
  try {
    const res = await request.post('/user/changePassword', { oldPassword: pwd.value.oldPassword, newPassword: pwd.value.newPassword })
    if (res.data.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('auth')
      setTimeout(() => { location.href = '/login' }, 800)
    } else ElMessage.error(res.data.message || '修改失败')
  } catch { ElMessage.error('修改失败') } finally { changing.value = false }
}

onMounted(load)
</script>

<style scoped>
.settings-page { padding: 4px; }
.set-card { border-radius: 10px; margin-bottom: 16px; }
.card-title { font-weight: 600; color: #333; }
</style>

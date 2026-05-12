<template>
  <div class="music-management">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-input
          v-model="query.keyword"
          placeholder="搜索歌曲名/歌手/专辑..."
          :prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="search"
          @clear="search"
        />
      </div>
      <div class="filter-right">
        <el-button type="primary" :icon="Plus" @click="showAddDialog">添加音乐</el-button>
      </div>
    </div>

    <!-- 批量操作 -->
    <div class="batch-bar" v-if="selectedIds.length > 0">
      <span class="batch-info">已选择 {{ selectedIds.length }} 项</span>
      <el-button size="small" @click="batchDelete">批量删除</el-button>
      <el-button size="small" @click="batchEnable(true)">批量启用</el-button>
      <el-button size="small" @click="batchEnable(false)">批量禁用</el-button>
    </div>

    <!-- 音乐表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#fafafa', color: '#555', fontWeight: 600 }"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column type="index" label="#" width="48" align="center" />
        <el-table-column label="封面" width="72" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.cover"
              :src="row.cover"
              class="cover-img"
              fit="cover"
              lazy
            >
              <template #error>
                <div class="cover-placeholder"><el-icon :size="16"><Headset /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-placeholder"><el-icon :size="16"><Headset /></el-icon></div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="歌曲名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="artist" label="歌手" width="130" show-overflow-tooltip />
        <el-table-column prop="album" label="专辑" width="130" show-overflow-tooltip />
        <el-table-column label="时长" width="76" align="center">
          <template #default="{ row }">
            <span class="duration-text">{{ formatDuration(row.duration) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="true"
              :inactive-value="false"
              active-color="#6c5fa0"
              inactive-color="#ddd"
              size="small"
              @change="toggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" :icon="VideoPlay" @click="previewMusic(row)">播放</el-button>
            <el-button text size="small" :icon="Edit" @click="editMusic(row)">编辑</el-button>
            <el-button text size="small" type="danger" :icon="Delete" @click="deleteMusic(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" :image-size="60" description="暂无音乐数据" />

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

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑音乐' : '添加音乐'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        label-position="left"
        class="music-form"
      >
        <el-form-item label="歌曲名" prop="name">
          <el-input v-model="form.name" placeholder="请输入歌曲名" />
        </el-form-item>
        <el-form-item label="歌手" prop="artist">
          <el-input v-model="form.artist" placeholder="请输入歌手名" />
        </el-form-item>
        <el-form-item label="专辑">
          <el-input v-model="form.album" placeholder="请输入专辑名（可选）" />
        </el-form-item>

        <!-- 封面图 -->
        <el-form-item label="封面图">
          <div class="upload-area">
            <el-upload
              :action="uploadImageUrl"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
              :on-error="handleCoverError"
              :before-upload="beforeImageUpload"
              accept="image/*"
            >
              <div v-if="form.cover" class="upload-preview">
                <img :src="form.cover" />
                <div class="upload-overlay"><el-icon><Refresh /></el-icon></div>
              </div>
              <div v-else class="upload-trigger">
                <el-icon :size="20"><Plus /></el-icon>
              </div>
            </el-upload>
            <el-input v-model="form.cover" placeholder="或输入封面图URL" clearable class="url-input" />
          </div>
        </el-form-item>

        <!-- 音频文件 -->
        <el-form-item label="音频文件" prop="url">
          <div class="upload-area">
            <el-upload
              :action="uploadAudioUrl"
              :show-file-list="false"
              :on-success="handleAudioSuccess"
              :on-error="handleAudioError"
              :before-upload="beforeAudioUpload"
              accept="audio/*"
            >
              <el-button type="primary" plain :icon="Upload">上传音频</el-button>
            </el-upload>
            <span class="audio-name" v-if="form.url">{{ audioFileName }}</span>
          </div>
          <div class="audio-url-row">
            <el-input v-model="form.url" placeholder="或输入音频URL" clearable />
            <el-button v-if="form.url" :icon="VideoPlay" text @click="testPlayAudio" />
          </div>
        </el-form-item>

        <el-form-item label="时长">
          <div class="duration-input">
            <el-input-number v-model="durationMin" :min="0" :max="99" size="small" controls-position="right" class="dur-input" />
            <span class="dur-sep">:</span>
            <el-input-number v-model="durationSec" :min="0" :max="59" size="small" controls-position="right" class="dur-input" />
            <span class="dur-hint">分 : 秒</span>
          </div>
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="true" :inactive-value="false" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEditing ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 播放预览对话框 -->
    <el-dialog v-model="previewVisible" title="音乐预览" width="420px" :close-on-click-modal="false">
      <div class="preview-body" v-if="previewMusicData">
        <div class="preview-cover">
          <el-image
            v-if="previewMusicData.cover"
            :src="previewMusicData.cover"
            fit="cover"
            class="preview-img"
          >
            <template #error><div class="preview-img-placeholder"><el-icon :size="32"><Headset /></el-icon></div></template>
          </el-image>
          <div v-else class="preview-img-placeholder"><el-icon :size="32"><Headset /></el-icon></div>
        </div>
        <div class="preview-info">
          <div class="preview-title">{{ previewMusicData.name }}</div>
          <div class="preview-artist">{{ previewMusicData.artist || '未知歌手' }}</div>
        </div>
        <audio
          ref="audioRef"
          :src="previewMusicData.url"
          controls
          class="preview-player"
          @loadedmetadata="onAudioLoaded"
        ></audio>
        <div class="preview-duration">时长：{{ formatDuration(previewMusicData.duration) }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import {
  Search, Plus, VideoPlay, Edit, Delete,
  Headset, Refresh, Upload
} from '@element-plus/icons-vue'

const uploadImageUrl = 'http://localhost:8080/api/upload/image'
const uploadAudioUrl = 'http://localhost:8080/api/upload/audio'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedIds = ref([])
const dialogVisible = ref(false)
const previewVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const audioRef = ref(null)
const previewMusicData = ref(null)

const durationMin = ref(0)
const durationSec = ref(0)

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
})

const form = reactive({
  name: '',
  artist: '',
  album: '',
  cover: '',
  url: '',
  duration: 0,
  enabled: true,
})

const rules = {
  name: [{ required: true, message: '请输入歌曲名', trigger: 'blur' }],
  artist: [{ required: true, message: '请输入歌手', trigger: 'blur' }],
  url: [{ required: true, message: '请上传音频文件或输入URL', trigger: 'blur' }],
}

const audioFileName = computed(() => {
  if (!form.url) return ''
  const parts = form.url.split('/')
  return parts[parts.length - 1]
})

const formatDuration = (seconds) => {
  if (!seconds && seconds !== 0) return '--'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const syncDurationFields = () => {
  const totalSec = durationMin.value * 60 + durationSec.value
  form.duration = totalSec
}

watch(durationMin, syncDurationFields)
watch(durationSec, syncDurationFields)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/music/page', { params: { ...query } })
    if (res.data.code === 200) {
      tableData.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取音乐列表失败')
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

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 首音乐吗？`, '批量删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
    const res = await request.post('/music/batchDelete', selectedIds.value)
    if (res.data.code === 200) {
      ElMessage.success('批量删除成功')
      selectedIds.value = []
      fetchData()
    }
  } catch (e) { /* ignore */ }
}

const batchEnable = async (enabled) => {
  try {
    await ElMessageBox.confirm(
      `确定要${enabled ? '启用' : '禁用'}选中的 ${selectedIds.value.length} 首音乐吗？`,
      '批量操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await request.put('/music/batchStatus', {
      ids: selectedIds.value,
      enabled,
    })
    if (res.data.code === 200) {
      ElMessage.success(`批量${enabled ? '启用' : '禁用'}成功`)
      selectedIds.value = []
      fetchData()
    }
  } catch (e) { /* ignore */ }
}

const toggleEnabled = async (row) => {
  try {
    await request.put('/music/update', { id: row.id, enabled: row.enabled })
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.error('操作失败')
  }
}

const resetForm = () => {
  form.name = ''
  form.artist = ''
  form.album = ''
  form.cover = ''
  form.url = ''
  form.duration = 0
  form.enabled = true
  durationMin.value = 0
  durationSec.value = 0
  editingId.value = null
  isEditing.value = false
}

const showAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const editMusic = (row) => {
  isEditing.value = true
  editingId.value = row.id
  form.name = row.name || ''
  form.artist = row.artist || ''
  form.album = row.album || ''
  form.cover = row.cover || ''
  form.url = row.url || ''
  form.enabled = row.enabled !== false
  durationMin.value = Math.floor((row.duration || 0) / 60)
  durationSec.value = (row.duration || 0) % 60
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    syncDurationFields()
    let res
    if (isEditing.value) {
      res = await request.put('/music/update', { ...form, id: editingId.value })
    } else {
      res = await request.post('/music/add', form)
    }
    if (res.data.code === 200) {
      ElMessage.success(isEditing.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请检查网络')
  } finally {
    submitting.value = false
  }
}

const deleteMusic = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除《${row.name}》吗？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
    const res = await request.delete(`/music/delete/${row.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除')
      fetchData()
    }
  } catch (e) { /* ignore */ }
}

const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) { ElMessage.error('只能上传图片'); return false }
  if (file.size > 5 * 1024 * 1024) { ElMessage.error('图片大小不能超过 5MB'); return false }
  return true
}

const handleCoverSuccess = (res) => {
  if (res.code === 200) {
    form.cover = res.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(res.message || '封面上传失败')
  }
}

const handleCoverError = () => {
  ElMessage.error('封面上传失败，请重试')
}

const beforeAudioUpload = (file) => {
  const isAudio = file.type.startsWith('audio/')
  if (!isAudio) { ElMessage.error('只能上传音频文件'); return false }
  if (file.size > 50 * 1024 * 1024) { ElMessage.error('音频大小不能超过 50MB'); return false }
  return true
}

const handleAudioSuccess = (res) => {
  if (res.code === 200) {
    form.url = res.data
    ElMessage.success('音频上传成功')
    // 创建临时 Audio 解析时长，不依赖模板中的 <audio> 元素
    const tempAudio = new Audio(res.data)
    tempAudio.addEventListener('loadedmetadata', () => {
      const d = Math.round(tempAudio.duration)
      if (d > 0 && !isNaN(d)) {
        durationMin.value = Math.floor(d / 60)
        durationSec.value = d % 60
      }
      // 释放临时 Audio 资源
      tempAudio.removeAttribute('src')
      tempAudio.load()
    }, { once: true })
  } else {
    ElMessage.error(res.message || '音频上传失败')
  }
}

const handleAudioError = () => {
  ElMessage.error('音频上传失败，请重试')
}

const testPlayAudio = () => {
  if (audioRef.value && form.url) {
    audioRef.value.src = form.url
    audioRef.value.play().catch(() => {})
  }
}

const previewMusic = (row) => {
  previewMusicData.value = row
  previewVisible.value = true
}

const onAudioLoaded = () => {
  // Handled by browser
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.music-management {
  min-height: calc(100vh - 120px);
}

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

.search-input {
  width: 280px;
}

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

.table-card {
  border-radius: 12px;
  padding-bottom: 4px;
}

.cover-img {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  display: block;
}

.cover-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e8e4f0, #d4cce6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c5fa0;
}

.duration-text {
  font-size: 0.85rem;
  color: #888;
  font-variant-numeric: tabular-nums;
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

.music-form {
  padding-right: 20px;
}

.upload-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.upload-preview {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border: 1px solid #e0e0e0;
  cursor: pointer;
  flex-shrink: 0;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}

.upload-preview:hover .upload-overlay {
  opacity: 1;
}

.upload-trigger {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f5f2fa, #ece8f5);
  border: 2px dashed #c8bedc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a7db5;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.upload-trigger:hover {
  border-color: #6c5fa0;
  color: #6c5fa0;
  background: linear-gradient(135deg, #f0ecf6, #e8e4f0);
}

.url-input {
  flex: 1;
  min-width: 150px;
}

.audio-name {
  font-size: 0.85rem;
  color: #666;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audio-url-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  width: 100%;
}

.duration-input {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dur-input {
  width: 100px;
}

.dur-sep {
  font-size: 1.1rem;
  font-weight: 700;
  color: #555;
  padding: 0 2px;
}

.dur-hint {
  font-size: 0.8rem;
  color: #999;
  margin-left: 6px;
}

.preview-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
}

.preview-cover {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-img-placeholder {
  width: 180px;
  height: 180px;
  background: linear-gradient(135deg, #e8e4f0, #c4b8d9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c5fa0;
}

.preview-info {
  text-align: center;
}

.preview-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.preview-artist {
  font-size: 0.9rem;
  color: #888;
  margin-top: 4px;
}

.preview-player {
  width: 100%;
  height: 44px;
}

.preview-duration {
  font-size: 0.85rem;
  color: #999;
}

@media (max-width: 768px) {
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-input { width: 100%; }
}
</style>

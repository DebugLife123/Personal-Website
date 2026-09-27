<template>
  <div class="gallery-admin">
    <el-card shadow="never" class="main-card">
      <!-- 说明 -->
      <div class="intro">
        <el-icon><InfoFilled /></el-icon>
        <span>
          勾选「设为轮播」的照片会出现在首页顶部轮播（最多 {{ HERO_MAX }} 张，按排序值排列）。
          当前已设 <b>{{ heroCount }}</b> 张。
        </span>
      </div>

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="total">共 {{ total }} 张照片</span>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="keyword"
            placeholder="搜索标题 / 简介 / 地点"
            clearable
            style="width: 230px"
            @keyup.enter="load(1)"
            @clear="load(1)"
          />
          <el-select v-model="statusFilter" style="width: 130px" @change="load(1)">
            <el-option label="全部" value="all" />
            <el-option label="显示中" value="visible" />
            <el-option label="已隐藏" value="hidden" />
            <el-option label="首页轮播" value="hero" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增照片</el-button>
        </div>
      </div>

      <!-- 列表 -->
      <el-table :data="photos" v-loading="loading" stripe>
        <el-table-column label="预览" width="120">
          <template #default="{ row }">
            <div class="thumb" @click="preview(row)">
              <img :src="row.url" alt="" />
              <span v-if="row.hero" class="thumb-hero">轮播</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标题 / 简介" min-width="280">
          <template #default="{ row }">
            <div class="info-cell">
              <span class="info-title">{{ row.title || '（未填标题）' }}</span>
              <span class="info-desc">{{ row.description || '——' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="地点 / 时间" min-width="160">
          <template #default="{ row }">
            <span class="meta-line">{{ row.location || '—' }}</span>
            <span class="meta-line muted">{{ row.shotTime || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'visible' ? 'success' : 'info'" size="small">
              {{ row.status === 'visible' ? '显示中' : '已隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="165" />
        <el-table-column label="操作" width="286" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.hero ? 'success' : 'primary'"
              text
              @click="toggleHero(row)"
            >
              {{ row.hero ? '取消轮播' : '设为轮播' }}
            </el-button>
            <el-button
              size="small"
              :type="row.status === 'visible' ? 'info' : 'success'"
              text
              @click="toggleStatus(row)"
            >
              {{ row.status === 'visible' ? '隐藏' : '显示' }}
            </el-button>
            <el-button size="small" type="primary" text @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" text @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有照片，点右上角新增" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pager.pageSize"
          :current-page="pager.page"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialog.show" :title="dialog.id ? '编辑照片' : '新增照片'" width="620px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="照片" required>
          <div class="upload-area">
            <el-upload
              :action="uploadImageUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="onUploaded"
              :on-error="onUploadError"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <div v-if="form.url" class="upload-preview">
                <img :src="form.url" alt="" />
                <div class="upload-overlay"><el-icon><Refresh /></el-icon></div>
              </div>
              <div v-else class="upload-trigger">
                <el-icon :size="22"><Plus /></el-icon>
                <span>选择图片</span>
              </div>
            </el-upload>
            <el-input v-model="form.url" placeholder="或直接粘贴图片地址" clearable />
            <p class="hint">支持 jpg / png / gif / webp，单张不超过 10MB</p>
          </div>
        </el-form-item>

        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="可选，留空则只显示简介" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="这张照片背后的小故事……"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="拍摄地">
          <el-input v-model="form.location" placeholder="可选，如：湖南 · 南岳" maxlength="100" />
        </el-form-item>

        <el-form-item label="拍摄时间">
          <el-input v-model="form.shotTime" placeholder="可选，如：2025-10-01 或 去年秋天" maxlength="50" />
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
          <span class="hint inline">数值越小越靠前</span>
        </el-form-item>

        <el-form-item label="显示">
          <el-switch v-model="visible" active-text="在图库中显示" />
        </el-form-item>

        <el-form-item label="首页轮播">
          <el-switch v-model="heroSwitch" active-text="加入首页顶部轮播" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.show = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, InfoFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { uploadUrl as buildUploadUrl, authHeaders } from '../../utils/api'

/** 与后端 GalleryController.HERO_MAX 保持一致 */
const HERO_MAX = 8

const photos = ref([])
const total = ref(0)
const heroCount = ref(0)
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('all')
const pager = reactive({ page: 1, pageSize: 10 })

const uploadImageUrl = buildUploadUrl('image')
const uploadHeaders = computed(() => authHeaders())

const form = reactive({
  id: null, title: '', description: '', url: '',
  location: '', shotTime: '', sort: 0, status: 'visible', hero: false
})
const visible = ref(true)
const heroSwitch = ref(false)
const dialog = reactive({ show: false, id: null })

const load = async (page = 1) => {
  pager.page = page
  loading.value = true
  try {
    const res = await request.get('/gallery/admin/page', {
      params: {
        page,
        pageSize: pager.pageSize,
        keyword: keyword.value || undefined,
        status: statusFilter.value
      }
    })
    if (res.data.code === 200) {
      photos.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
    const cnt = await request.get('/gallery/admin/heroCount').catch(() => null)
    if (cnt?.data?.code === 200) heroCount.value = cnt.data.data
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 设为 / 取消首页轮播图
const toggleHero = async (row) => {
  const next = !row.hero
  if (next && heroCount.value >= HERO_MAX) {
    ElMessage.warning(`首页轮播最多 ${HERO_MAX} 张，请先取消其他照片`)
    return
  }
  try {
    const res = await request.put(`/gallery/admin/hero/${row.id}?hero=${next}`)
    if (res.data.code === 200) {
      ElMessage.success(res.data.message)
      await load(pager.page)
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null, title: '', description: '', url: '',
    location: '', shotTime: '', sort: 0, status: 'visible', hero: false
  })
  visible.value = true
  heroSwitch.value = false
}

const openCreate = () => {
  resetForm()
  dialog.id = null
  dialog.show = true
}

const openEdit = (row) => {
  Object.assign(form, {
    id: row.id,
    title: row.title || '',
    description: row.description || '',
    url: row.url || '',
    location: row.location || '',
    shotTime: row.shotTime || '',
    sort: row.sort ?? 0,
    status: row.status || 'visible',
    hero: !!row.hero
  })
  visible.value = form.status !== 'hidden'
  heroSwitch.value = !!row.hero
  dialog.id = row.id
  dialog.show = true
}

const save = async () => {
  if (!form.url) { ElMessage.warning('请先上传照片或填写图片地址'); return }
  // 新增时若勾了轮播，先做数量校验
  if (!form.id && heroSwitch.value && heroCount.value >= HERO_MAX) {
    ElMessage.warning(`首页轮播最多 ${HERO_MAX} 张，请先取消其他照片`)
    return
  }
  saving.value = true
  form.status = visible.value ? 'visible' : 'hidden'
  form.hero = heroSwitch.value
  try {
    const payload = { ...form }
    const res = form.id
      ? await request.put('/gallery/update', payload)
      : await request.post('/gallery/add', payload)
    if (res.data.code === 200) {
      ElMessage.success(res.data.message || '已保存')
      dialog.show = false
      await load(pager.page)
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const next = row.status === 'visible' ? 'hidden' : 'visible'
  try {
    const res = await request.put(`/gallery/admin/status/${row.id}?status=${next}`)
    if (res.data.code === 200) {
      ElMessage.success(res.data.message)
      await load(pager.page)
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm('删除后不可恢复（上传的图片文件仍保留在服务器上）。确定删除？', '删除照片', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    const res = await request.delete(`/gallery/delete/${row.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除')
      await load(pager.page)
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch { /* 取消或失败 */ }
}

const preview = (row) => { window.open(row.url, '_blank') }

const beforeUpload = (file) => {
  const isImage = file.type?.startsWith('image/')
  const under10M = file.size / 1024 / 1024 < 10
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!under10M) { ElMessage.error('图片不能超过 10MB'); return false }
  return true
}

const onUploaded = (res) => {
  if (res.code === 200) {
    form.url = res.data
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const onUploadError = () => ElMessage.error('上传失败，请重试')

onMounted(() => load())
</script>

<style scoped>
.gallery-admin { display: flex; flex-direction: column; gap: 16px; }
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
.toolbar-right { display: flex; gap: 10px; flex-wrap: wrap; }

.thumb {
  position: relative;
  width: 84px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #f4f5f8;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }

.thumb-hero {
  position: absolute;
  left: 5px;
  bottom: 5px;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(103, 194, 58, 0.94);
  color: #fff;
  font-size: 0.64rem;
  letter-spacing: 0.5px;
}

.info-cell { display: flex; flex-direction: column; gap: 3px; }
.info-title { font-size: 0.9rem; font-weight: 600; color: #303133; }
.info-desc {
  font-size: 0.78rem;
  color: #909399;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-line { display: block; font-size: 0.82rem; color: #606266; }
.meta-line.muted { color: #909399; font-size: 0.76rem; }

.pager { display: flex; justify-content: flex-end; margin-top: 16px; }

.upload-area { display: flex; flex-direction: column; gap: 10px; width: 100%; }
.upload-preview {
  position: relative;
  width: 168px;
  height: 120px;
  border-radius: 10px;
  overflow: hidden;
}
.upload-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.upload-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.42);
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}
.upload-preview:hover .upload-overlay { opacity: 1; }
.upload-trigger {
  width: 168px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  color: #909399;
  font-size: 0.8rem;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.upload-trigger:hover { border-color: #409eff; color: #409eff; }

.hint { margin: 0; font-size: 0.74rem; color: #a8abb2; }
.hint.inline { margin-left: 10px; }
</style>

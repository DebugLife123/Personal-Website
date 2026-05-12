<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Edit, Delete, Plus, Link, Calendar,
  Folder, Refresh, View, Share
} from '@element-plus/icons-vue'
import { isAdmin } from '../utils/auth'

const projectList = ref([])
const categories = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('')
const pageLoaded = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const editingId = ref(null)

const form = ref({
  title: '',
  description: '',
  techStack: '',
  githubUrl: '',
  imgUrl: '',
  previewUrl: '',
  category: '',
  status: '',
  sort: 0
})

const coverGradients = [
  'linear-gradient(135deg, #9e92b0 0%, #8a8eaa 100%)',
  'linear-gradient(135deg, #b8aec8 0%, #a0a4ba 100%)',
  'linear-gradient(135deg, #a898b8 0%, #8e9ab0 100%)',
  'linear-gradient(135deg, #c4b8d0 0%, #a8b0c4 100%)',
  'linear-gradient(135deg, #b0a4c0 0%, #98a0b8 100%)',
  'linear-gradient(135deg, #a49cb8 0%, #8e94ac 100%)',
]

// 状态配置
const statusConfig = {
  '已完成': { type: 'success', color: '#67c23a' },
  '开发中': { type: 'warning', color: '#e6a23c' },
  '学习中': { type: 'primary', color: '#409eff' },
}
const defaultStatus = { type: 'info', color: '#909399' }

// 详情弹窗
const detailVisible = ref(false)
const detailProject = ref(null)

const openDetail = (project) => {
  detailProject.value = project
  detailVisible.value = true
}

const formatDate = (dt) => {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const filteredProjects = computed(() => {
  let list = projectList.value
  if (activeCategory.value) {
    list = list.filter(p => p.category === activeCategory.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(p =>
      p.title?.toLowerCase().includes(kw) ||
      p.description?.toLowerCase().includes(kw) ||
      p.techStack?.toLowerCase().includes(kw)
    )
  }
  return list
})

const fetchProjects = async () => {
  loading.value = true
  try {
    const res = await request.get('/project/list')
    if (res.data.code === 200) projectList.value = res.data.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await request.get('/project/categories')
    if (res.data.code === 200) categories.value = res.data.data
  } catch (e) {}
}

const openGithub = (url) => {
  if (url) window.open(url, '_blank')
}

const openPreview = (url) => {
  if (url) window.open(url, '_blank')
}

const resetFilter = () => {
  activeCategory.value = ''
  searchKeyword.value = ''
}

const openAdd = () => {
  editingId.value = null
  dialogTitle.value = '添加项目'
  form.value = { title: '', description: '', techStack: '', githubUrl: '', imgUrl: '', previewUrl: '', category: '', status: '', sort: 0 }
  dialogVisible.value = true
}

const openEdit = (project, e) => {
  e.stopPropagation()
  editingId.value = project.id
  dialogTitle.value = '编辑项目'
  form.value = {
    title: project.title,
    description: project.description || '',
    techStack: project.techStack || '',
    githubUrl: project.githubUrl || '',
    imgUrl: project.imgUrl || '',
    previewUrl: project.previewUrl || '',
    category: project.category || '',
    status: project.status || '',
    sort: project.sort ?? 0
  }
  dialogVisible.value = true
}

const submitProject = async () => {
  if (!form.value.title.trim()) {
    return ElMessage.warning('请输入项目名称')
  }
  submitting.value = true
  try {
    const data = { ...form.value }
    if (editingId.value) {
      data.id = editingId.value
      await request.put('/project/update', data)
      ElMessage.success('更新成功')
    } else {
      await request.post('/project/add', data)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await Promise.all([fetchProjects(), fetchCategories()])
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const deleteProject = async (project, e) => {
  e.stopPropagation()
  try {
    await ElMessageBox.confirm(
      `确定要删除「${project.title}」吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await request.delete(`/project/delete/${project.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除')
      await Promise.all([fetchProjects(), fetchCategories()])
    }
  } catch (e) {}
}

onMounted(async () => {
  await Promise.all([fetchProjects(), fetchCategories()])
  setTimeout(() => { pageLoaded.value = true }, 80)
})
</script>

<template>
  <div class="projects-page" :class="{ 'page-loaded': pageLoaded }">
    <!-- 统一顶部栏 -->
    <div class="top-card">
      <div class="top-card-header">
        <div class="top-card-text">
          <h1 class="top-title">项目展示</h1>
          <p class="top-subtitle">一些我做过的项目</p>
        </div>
      </div>

      <!-- 工具栏：搜索 + 分类标签 + 按钮 -->
      <div class="top-card-toolbar">
        <div class="toolbar-left-group">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索项目..."
            :prefix-icon="Search"
            clearable
            class="search-input"
            @clear="resetFilter"
          />
          <div class="category-filters">
            <span
              class="filter-pill"
              :class="{ active: !activeCategory }"
              @click="activeCategory = ''"
            >全部</span>
            <span
              v-for="cat in categories"
              :key="cat"
              class="filter-pill"
              :class="{ active: activeCategory === cat }"
              @click="activeCategory = cat"
            >{{ cat }}</span>
          </div>
        </div>
        <div class="toolbar-right-group">
          <el-button text :icon="Refresh" @click="resetFilter" v-if="activeCategory || searchKeyword" class="reset-btn">重置</el-button>
          <el-button v-if="isAdmin" type="primary" :icon="Plus" class="add-btn" @click="openAdd">添加项目</el-button>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content-body">
      <div v-if="loading" class="loading-wrapper">
        <el-skeleton :rows="3" animated />
      </div>

      <el-row v-else :gutter="16" class="project-grid">
        <el-col
          v-for="(project, index) in filteredProjects"
          :key="project.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          class="project-col"
        >
          <div
            class="project-card"
            :style="{ '--i': index }"
            @click="openDetail(project)"
          >
            <div class="card-img" @click.stop="openDetail(project)">
              <img v-if="project.imgUrl" :src="project.imgUrl" alt="" />
              <div v-else class="card-img-fallback" :style="{ background: coverGradients[index % coverGradients.length] }">
                <span class="fallback-icon">{{ project.title?.charAt(0) || 'P' }}</span>
              </div>
              <div class="card-img-overlay">
                <el-icon :size="28"><Link /></el-icon>
              </div>
            </div>
            <div class="card-body">
              <div class="card-top">
                <h3 class="card-title">{{ project.title }}</h3>
                <div class="card-top-right">
                  <span
                    v-if="project.status"
                    class="status-badge"
                    :style="{ background: (statusConfig[project.status] || defaultStatus).color }"
                  >{{ project.status }}</span>
                </div>
              </div>
              <p class="card-desc">{{ project.description }}</p>
              <div class="tech-stack" v-if="project.techStack">
                <span class="tech-chip" v-for="tech in project.techStack.split(',')" :key="tech">
                  {{ tech.trim() }}
                </span>
              </div>
              <div class="card-footer">
                <span class="card-date">
                  <el-icon :size="13"><Calendar /></el-icon>
                  {{ formatDate(project.createTime) }}
                </span>
                <div class="card-actions" v-if="isAdmin">
                  <el-tooltip content="编辑" placement="top">
                    <el-button class="action-icon-btn" size="small" :icon="Edit" circle @click.stop="(e) => openEdit(project, e)" />
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button class="action-icon-btn delete-btn-action" size="small" :icon="Delete" circle @click.stop="(e) => deleteProject(project, e)" />
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <el-col v-if="filteredProjects.length === 0 && !loading" :span="24">
          <div class="empty-state">
            <el-empty :description="searchKeyword || activeCategory ? '没有匹配的项目' : '暂无项目'" />
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- ===== 项目详情弹窗 ===== -->
    <el-dialog
      v-model="detailVisible"
      :title="detailProject?.title || '项目详情'"
      width="760px"
      top="5vh"
      :close-on-click-modal="true"
      class="detail-dialog"
    >
      <div v-if="detailProject" class="detail-body">
        <!-- 封面头图 -->
        <div class="detail-hero" v-if="detailProject.imgUrl">
          <img :src="detailProject.imgUrl" alt="" />
          <div class="detail-hero-overlay">
            <div class="detail-hero-meta">
              <span
                v-if="detailProject.status"
                class="hero-badge"
                :style="{ background: (statusConfig[detailProject.status] || defaultStatus).color }"
              >{{ detailProject.status }}</span>
              <span v-if="detailProject.category" class="hero-badge hero-badge-cat">{{ detailProject.category }}</span>
            </div>
          </div>
        </div>

        <!-- 无封面时的占位 -->
        <div v-else class="detail-hero detail-hero-fallback"
          :style="{ background: coverGradients[detailProject.id % coverGradients.length] }">
          <div class="detail-hero-placeholder">
            <span class="detail-hero-icon">{{ detailProject.title?.charAt(0) || 'P' }}</span>
            <div class="detail-hero-meta">
              <span
                v-if="detailProject.status"
                class="hero-badge"
                :style="{ background: (statusConfig[detailProject.status] || defaultStatus).color }"
              >{{ detailProject.status }}</span>
              <span v-if="detailProject.category" class="hero-badge hero-badge-cat">{{ detailProject.category }}</span>
            </div>
          </div>
        </div>

        <!-- 标题区域 -->
        <div class="detail-title-area">
          <h2 class="detail-project-title">{{ detailProject.title }}</h2>
          <div class="detail-title-meta">
            <span class="detail-meta-item">
              <el-icon :size="14"><Calendar /></el-icon>
              {{ formatDate(detailProject.createTime) }}
            </span>
            <span class="detail-meta-item" v-if="detailProject.sort !== undefined && detailProject.sort !== null">
              <el-icon :size="14"><Folder /></el-icon>
              优先级: {{ detailProject.sort }}
            </span>
            <span class="detail-meta-item" v-if="detailProject._views">
              <el-icon :size="14"><View /></el-icon>
              {{ detailProject._views }} 次浏览
            </span>
          </div>
        </div>

        <el-divider class="detail-divider" />

        <!-- 项目介绍 -->
        <div class="detail-section">
          <h4 class="detail-section-title">
            <span class="section-title-dot"></span>
            项目介绍
          </h4>
          <div class="detail-desc-content">
            <p>{{ detailProject.description || '暂无描述' }}</p>
          </div>
        </div>

        <!-- 技术栈 -->
        <div class="detail-section" v-if="detailProject.techStack">
          <h4 class="detail-section-title">
            <span class="section-title-dot"></span>
            技术栈
          </h4>
          <div class="detail-tech-stack">
            <span class="detail-tech-tag" v-for="tech in detailProject.techStack.split(',')" :key="tech">
              {{ tech.trim() }}
            </span>
          </div>
        </div>

        <el-divider class="detail-divider" />

        <!-- 链接 -->
        <div class="detail-links-row">
          <el-button
            v-if="detailProject.githubUrl"
            type="primary"
            plain
            :icon="Link"
            @click="openGithub(detailProject.githubUrl)"
            class="detail-link-btn"
          >
            <span>GitHub 仓库</span>
            <small class="link-hint">{{ detailProject.githubUrl.replace(/^https?:\/\//, '').substring(0, 30) }}{{ detailProject.githubUrl.replace(/^https?:\/\//, '').length > 30 ? '...' : '' }}</small>
          </el-button>
          <el-button
            v-if="detailProject.previewUrl"
            type="success"
            plain
            :icon="Share"
            @click="openPreview(detailProject.previewUrl)"
            class="detail-link-btn"
          >
            <span>在线预览</span>
            <small class="link-hint">{{ detailProject.previewUrl.replace(/^https?:\/\//, '').substring(0, 30) }}{{ detailProject.previewUrl.replace(/^https?:\/\//, '').length > 30 ? '...' : '' }}</small>
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- ===== 添加/编辑对话框 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      :close-on-click-modal="false"
      @closed="dialogVisible = false"
    >
      <el-form label-width="80px" class="project-form">
        <el-form-item label="项目名称">
          <el-input v-model="form.title" placeholder="项目名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-row :gutter="15">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-input v-model="form.category" placeholder="如: 前端" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="选择状态" clearable style="width:100%">
                <el-option label="已完成" value="已完成" />
                <el-option label="开发中" value="开发中" />
                <el-option label="学习中" value="学习中" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目描述" />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="form.techStack" placeholder="逗号分隔，如: Vue, SpringBoot, MySQL" />
        </el-form-item>
        <el-row :gutter="15">
          <el-col :span="12">
            <el-form-item label="GitHub">
              <el-input v-model="form.githubUrl" placeholder="GitHub 链接" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预览地址">
              <el-input v-model="form.previewUrl" placeholder="在线预览链接" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图">
          <el-input v-model="form.imgUrl" placeholder="图片 URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProject">
          {{ editingId ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.projects-page {
  min-height: 100vh;
  padding-bottom: 60px;
  background: linear-gradient(135deg, #f4f5f8 0%, #eae8ee 100%);
  opacity: 0;
  transition: opacity 0.5s ease;
}
.projects-page.page-loaded { opacity: 1; }

/* ===== 统一顶部栏 ===== */
.top-card {
  max-width: 1320px;
  margin: 28px auto 0;
  padding: 0 24px;
  position: relative;
  z-index: 2;
}
.top-card-header {
  background: linear-gradient(135deg, #9e92b0 0%, #8a8eaa 100%);
  border-radius: 12px 12px 0 0;
  padding: 34px 28px 24px;
  text-align: center;
  color: white;
}
.top-title {
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 0 0 6px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.12);
}
.top-subtitle {
  font-size: 0.95rem;
  opacity: 0.8;
  letter-spacing: 2px;
  font-weight: 300;
  margin: 0;
}

/* ===== 工具栏（搜索 + 分类标签 + 按钮） ===== */
.top-card-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  background: #fff;
  padding: 14px 24px;
  border-radius: 0 0 12px 12px;
  flex-wrap: wrap;
}
.toolbar-left-group {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  flex-wrap: wrap;
}
.toolbar-right-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input { width: 200px; }
.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1.5px solid #e4e4ea;
  box-shadow: none !important;
  transition: border-color 0.25s, box-shadow 0.25s;
  background: #fafafc;
}
.search-input :deep(.el-input__wrapper:hover) { border-color: #c8c4d4; }
.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #9e92b0;
  box-shadow: 0 0 0 3px rgba(158,146,176,0.12) !important;
  background: #fff;
}

/* ===== 分类筛选药丸标签 ===== */
.category-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.filter-pill {
  font-size: 0.82rem;
  padding: 4px 14px;
  border-radius: 20px;
  background: #f0eef5;
  color: #7a7a8a;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  user-select: none;
}
.filter-pill:hover {
  background: #e4e0ec;
  color: #5a5a6a;
}
.filter-pill.active {
  background: #9e92b0;
  color: #fff;
  font-weight: 600;
}

.add-btn {
  border-radius: 8px;
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  border: none;
  padding: 8px 18px;
  font-weight: 600;
  transition: transform 0.2s, box-shadow 0.25s;
}
.add-btn:hover {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 4px 14px rgba(158,146,176,0.35);
  background: linear-gradient(135deg, #8e82a0, #7a7e9a);
}
.add-btn:active { transform: scale(0.97); }

.reset-btn { margin-left: 0; }

/* ===== 内容区 ===== */
.content-body {
  max-width: 1320px;
  margin: 0 auto;
  padding: 24px 24px 0;
}
.loading-wrapper {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04), 0 2px 8px rgba(0,0,0,0.04);
}
.project-grid {
  min-height: 200px;
  display: flex;
  flex-wrap: wrap;
}
.project-col { margin-bottom: 16px; }

/* ===== 项目卡片 ===== */
.project-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04), 0 2px 8px rgba(0,0,0,0.04);
  transition: transform 0.35s cubic-bezier(0.25, 0.1, 0.25, 1), box-shadow 0.35s ease;
  display: flex;
  flex-direction: column;
  height: 100%;
  cursor: pointer;
  opacity: 0;
  transform: translateY(18px);
}
.page-loaded .project-card {
  animation: cardFadeIn 0.45s ease forwards;
  animation-delay: calc(var(--i) * 0.07s);
}
@keyframes cardFadeIn {
  to { opacity: 1; transform: translateY(0); }
}
.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06), 0 8px 24px rgba(0,0,0,0.08);
}

/* ===== 卡片封面 ===== */
.card-img {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  cursor: pointer;
  background: #eee;
}
.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
}
.project-card:hover .card-img img { transform: scale(1.06); }
.card-img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: filter 0.3s;
}
.project-card:hover .card-img-fallback { filter: brightness(1.08); }
.fallback-icon {
  font-size: 2.4rem;
  font-weight: 700;
  color: rgba(255,255,255,0.7);
  letter-spacing: 2px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.1);
}
.card-img-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.2);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  backdrop-filter: blur(0);
}
.card-img:hover .card-img-overlay {
  opacity: 1;
  backdrop-filter: blur(2px);
}

/* ===== 卡片主体 ===== */
.card-body {
  padding: 16px 18px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}
.card-top-right {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  align-items: center;
}
.card-title {
  font-size: 1.08rem;
  font-weight: 700;
  color: #2d2d3a;
  margin: 0;
  line-height: 1.35;
  flex: 1;
}

/* ===== 状态标签 ===== */
.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 100px;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
}

.card-desc {
  color: #777;
  font-size: 0.85rem;
  line-height: 1.6;
  margin: 0 0 12px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 技术栈 ===== */
.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.tech-chip {
  font-size: 0.72rem;
  background: #f0eef5;
  color: #7a7a8a;
  padding: 2px 10px;
  border-radius: 12px;
  transition: background 0.2s, color 0.2s, transform 0.2s;
  cursor: default;
}
.tech-chip:hover {
  background: #9e92b0;
  color: #fff;
  transform: translateY(-1px);
}

/* ===== 卡片底部 ===== */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f0eef4;
}
.card-date {
  font-size: 0.76rem;
  color: #aaa;
  display: flex;
  align-items: center;
  gap: 4px;
}
.card-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}
.project-card:hover .card-actions { opacity: 1; }
.action-icon-btn {
  background: #f5f5f5 !important;
  border: none !important;
  color: #888 !important;
  transition: background 0.2s, color 0.2s, transform 0.15s !important;
}
.action-icon-btn:hover {
  color: #409eff !important;
  background: #ecf5ff !important;
  transform: scale(1.08);
}
.delete-btn-action:hover {
  color: #e74c3c !important;
  background: #fef0f0 !important;
}

/* ===== 空态 ===== */
.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px 0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04), 0 2px 8px rgba(0,0,0,0.04);
}

/* ===== 项目详情弹窗 ===== */
.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.detail-dialog :deep(.el-dialog__header) {
  display: none;
}
.detail-body {
  overflow: hidden;
}

/* ---- Hero 封面 ---- */
.detail-hero {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #eee;
}
.detail-hero img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.detail-hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(transparent 50%, rgba(0,0,0,0.6));
  display: flex;
  align-items: flex-end;
  padding: 20px 24px;
}
.detail-hero-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-hero-placeholder {
  text-align: center;
}
.detail-hero-icon {
  font-size: 4rem;
  font-weight: 700;
  color: rgba(255,255,255,0.6);
  display: block;
  margin-bottom: 12px;
}
.detail-hero-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.hero-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 100px;
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.hero-badge-cat {
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(4px);
}

/* ---- 标题区域 ---- */
.detail-title-area {
  padding: 28px 28px 0;
}
.detail-project-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 12px;
  line-height: 1.35;
}
.detail-title-meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}
.detail-meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.82rem;
  color: #999;
}

/* ---- 分割线 ---- */
.detail-divider {
  margin: 20px 28px;
}

/* ---- 章节 ---- */
.detail-section {
  padding: 0 28px 4px;
}
.detail-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1rem;
  font-weight: 700;
  color: #333;
  margin: 0 0 14px;
}
.section-title-dot {
  display: inline-block;
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: linear-gradient(180deg, #9e92b0, #8a8eaa);
}
.detail-desc-content {
  background: #f8f7fa;
  border-radius: 10px;
  padding: 18px 20px;
}
.detail-desc-content p {
  color: #555;
  font-size: 0.92rem;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

/* ---- 技术栈 ---- */
.detail-tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-tech-tag {
  font-size: 0.8rem;
  padding: 6px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f0eef5, #e8e5f0);
  color: #5a4e70;
  font-weight: 500;
  transition: all 0.2s;
  cursor: default;
}
.detail-tech-tag:hover {
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  color: #fff;
  transform: translateY(-1px);
}

/* ---- 链接按钮 ---- */
.detail-links-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  padding: 0 28px 24px;
}
.detail-link-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  height: auto;
  padding: 12px 20px;
  border-radius: 10px;
  line-height: 1.4;
  min-width: 160px;
  flex: 1;
}
.detail-link-btn span {
  font-size: 0.9rem;
  font-weight: 600;
}
.link-hint {
  font-size: 0.7rem;
  opacity: 0.6;
  font-weight: 400;
  margin-top: 2px;
  word-break: break-all;
}

/* ===== Dialog 编辑表单 ===== */
.project-form .el-input-number .el-input__wrapper { padding-left: 10px; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .top-card-header { padding: 26px 20px 20px; }
  .top-title { font-size: 1.5rem; }
  .top-card-toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-left-group { flex-direction: column; align-items: stretch; }
  .toolbar-right-group { justify-content: flex-end; }
  .search-input { width: 100%; }
  .category-filters { justify-content: center; }
  .content-body { padding: 16px 16px 0; }
  .top-card { padding: 0 16px; }
  .card-actions { opacity: 1; }
  .project-card { animation: cardFadeIn 0.45s ease forwards; }
  .detail-dialog :deep(.el-dialog) { width: 92vw !important; }
  .detail-body { padding: 16px; }
  .detail-meta { gap: 6px; }
  .detail-date { margin-left: 0; width: 100%; }
  .project-form .el-row { flex-direction: column; }
  .project-form .el-row .el-col { width: 100%; }
}

</style>

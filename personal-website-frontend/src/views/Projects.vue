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
    <!-- ===== 卷首：档案页眉 ===== -->
    <header class="page-head">
      <p class="head-kicker">PROJECTS · SELECTED WORKS</p>
      <h1 class="head-title">项目<span class="head-title-accent">集</span></h1>
      <p class="head-sub">从想法到落地，这里收着几件拿得出手的作品</p>
      <div class="head-meta">
        <span class="head-meta-item">
          已收录 <em>{{ filteredProjects.length }}</em> 件
          <template v-if="activeCategory || searchKeyword">· 已筛选</template>
        </span>
        <label class="head-search">
          <el-icon :size="13"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="检索项目…"
            class="head-search-input"
          />
          <button v-if="searchKeyword" class="head-search-clear" @click="resetFilter">×</button>
        </label>
        <button v-if="isAdmin" class="head-add" @click="openAdd">
          <el-icon :size="13"><Plus /></el-icon> 新建项目
        </button>
      </div>
    </header>

    <!-- ===== 分类横档 ===== -->
    <nav class="shelf-nav" v-if="categories.length">
      <button
        class="shelf-tab"
        :class="{ active: !activeCategory }"
        @click="activeCategory = ''"
      >全部</button>
      <button
        v-for="cat in categories"
        :key="cat"
        class="shelf-tab"
        :class="{ active: activeCategory === cat }"
        @click="activeCategory = cat"
      >{{ cat }}</button>
      <button
        v-if="activeCategory || searchKeyword"
        class="shelf-tab shelf-reset"
        @click="resetFilter"
      ><el-icon :size="12"><Refresh /></el-icon> 重置</button>
    </nav>

    <!-- ===== 内容区 ===== -->
    <div class="content-body">
      <div v-if="loading" class="loading-wrapper">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else class="project-grid">
        <article
          v-for="(project, index) in filteredProjects"
          :key="project.id"
          class="project-card"
          :style="{ '--i': index }"
          @click="openDetail(project)"
        >
          <div class="card-img">
            <img v-if="project.imgUrl" :src="project.imgUrl" alt="" loading="lazy" />
            <div v-else class="card-img-fallback">
              <span class="fallback-icon">{{ project.title?.charAt(0) || 'P' }}</span>
            </div>
            <span class="card-serial">#{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="card-actions" v-if="isAdmin">
              <el-tooltip content="编辑" placement="top">
                <el-button class="action-icon-btn" size="small" :icon="Edit" circle @click.stop="(e) => openEdit(project, e)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button class="action-icon-btn delete-btn-action" size="small" :icon="Delete" circle @click.stop="(e) => deleteProject(project, e)" />
              </el-tooltip>
            </div>
          </div>
          <div class="card-body">
            <div class="card-top">
              <h3 class="card-title">{{ project.title }}</h3>
              <span
                v-if="project.status"
                class="status-badge"
                :data-status="project.status"
              >{{ project.status }}</span>
            </div>
            <p class="card-desc">{{ project.description }}</p>
            <div class="tech-stack" v-if="project.techStack">
              <span class="tech-chip" v-for="tech in project.techStack.split(',')" :key="tech">
                {{ tech.trim() }}
              </span>
            </div>
            <div class="card-footer">
              <span class="card-date">
                <el-icon :size="12"><Calendar /></el-icon>
                {{ formatDate(project.createTime) }}
              </span>
              <span class="card-more">查看详情 →</span>
            </div>
          </div>
        </article>

        <div v-if="filteredProjects.length === 0 && !loading" class="empty-state">
          <p class="empty-mark"><el-icon :size="40"><Folder /></el-icon></p>
          <p class="empty-text">{{ searchKeyword || activeCategory ? '没有符合条件的项目，换个条件试试' : '暂无项目' }}</p>
          <button v-if="searchKeyword || activeCategory" class="empty-reset" @click="resetFilter">清空筛选</button>
        </div>
      </div>
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
                :data-status="detailProject.status"
              >{{ detailProject.status }}</span>
              <span v-if="detailProject.category" class="hero-badge hero-badge-cat">{{ detailProject.category }}</span>
            </div>
          </div>
        </div>

        <!-- 无封面时的占位 -->
        <div v-else class="detail-hero detail-hero-fallback">
          <div class="detail-hero-placeholder">
            <span class="detail-hero-icon">{{ detailProject.title?.charAt(0) || 'P' }}</span>
            <div class="detail-hero-meta">
              <span
                v-if="detailProject.status"
                class="hero-badge"
                :data-status="detailProject.status"
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
/* ========== 设计令牌：与全站一致的冷调商务色系 ========== */
.projects-page {
  --page: #f4f5f7;
  --card: #ffffff;
  --card-soft: #fafbfd;
  --ink: #1a2233;
  --ink-2: #5c6675;
  --ink-3: #98a1b3;
  --line: #e9ecf2;
  --line-2: #dde2ea;
  --accent: #3d6ee0;
  --accent-deep: #2f59c2;
  --accent-soft: #eef3fd;
  --cta: #1d2534;
  --cta-hover: #2b3648;
  --ease: cubic-bezier(0.22, 1, 0.36, 1);
  --shadow-1: 0 1px 2px rgba(16, 24, 40, 0.04), 0 1px 3px rgba(16, 24, 40, 0.03);
  --shadow-2: 0 14px 34px -14px rgba(16, 24, 40, 0.18);

  min-height: 100vh;
  padding-bottom: 90px;
  background: var(--page);
  color: var(--ink);
  opacity: 0;
  transition: opacity 0.4s ease;
}
.projects-page.page-loaded { opacity: 1; }

/* ========== 页眉 ========== */
.page-head {
  max-width: 1240px;
  margin: 0 auto;
  padding: 108px 28px 0;
}
.head-kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--accent);
  margin: 0 0 14px;
}
.head-kicker::before {
  content: '';
  width: 22px;
  height: 2px;
  border-radius: 2px;
  background: var(--accent);
}
.head-title {
  font-size: clamp(2rem, 4vw, 2.7rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin: 0 0 10px;
  color: var(--ink);
}
.head-title-accent { color: var(--accent); }
.head-sub {
  font-size: 0.98rem;
  color: var(--ink-2);
  margin: 0 0 30px;
}
.head-meta {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}
.head-meta-item {
  font-size: 0.86rem;
  color: var(--ink-2);
}
.head-meta-item em {
  font-style: normal;
  font-weight: 700;
  color: var(--accent);
  margin: 0 2px;
}

/* 检索框 */
.head-search {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 12px;
  background: var(--card);
  border: 1px solid var(--line-2);
  border-radius: 9px;
  color: var(--ink-3);
  transition: border-color 0.2s, box-shadow 0.2s, color 0.2s;
}
.head-search:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(61, 110, 224, 0.12);
  color: var(--accent);
}
.head-search-input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.86rem;
  color: var(--ink);
  width: 150px;
}
.head-search-input::placeholder { color: var(--ink-3); }
.head-search-clear {
  border: none;
  background: none;
  color: var(--ink-3);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  padding: 0;
  transition: color 0.2s;
}
.head-search-clear:hover { color: var(--accent); }

.head-add {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 9px;
  background: var(--cta);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s, box-shadow 0.25s;
}
.head-add:hover {
  background: var(--cta-hover);
  transform: translateY(-1px);
  box-shadow: 0 10px 22px -10px rgba(29, 37, 52, 0.55);
}
.head-add:active { transform: translateY(0) scale(0.985); }

/* ========== 分类筛选 ========== */
.shelf-nav {
  max-width: 1240px;
  margin: 24px auto 0;
  padding: 0 28px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.shelf-tab {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 0.88rem;
  color: var(--ink-2);
  padding: 8px 14px;
  border-radius: 9px;
  transition: color 0.2s, background 0.2s, transform 0.15s;
}
.shelf-tab:hover { color: var(--ink); background: rgba(29, 37, 52, 0.045); }
.shelf-tab.active {
  color: var(--accent-deep);
  background: var(--accent-soft);
  font-weight: 600;
}
.shelf-reset {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--ink-3);
  margin-left: 4px;
}
.shelf-reset:hover { color: var(--accent); background: var(--accent-soft); }

/* ===== 内容区 ===== */
.content-body {
  max-width: 1240px;
  margin: 0 auto;
  padding: 26px 28px 0;
}
.loading-wrapper {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 40px;
}
.project-grid {
  min-height: 200px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(310px, 1fr));
  gap: 22px;
}

/* ===== 项目卡片 ===== */
.project-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-1);
  opacity: 0;
  transform: translateY(14px);
  transition:
    transform 0.35s var(--ease),
    box-shadow 0.35s var(--ease),
    border-color 0.25s;
}
.page-loaded .project-card {
  animation: cardFadeIn 0.55s var(--ease) forwards;
  animation-delay: calc(var(--i) * 0.05s);
}
@keyframes cardFadeIn {
  to { opacity: 1; transform: translateY(0); }
}
.project-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-2);
  border-color: var(--line-2);
}

/* ===== 卡片封面 ===== */
.card-img {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #eef0f4;
}
.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s var(--ease);
}
.project-card:hover .card-img img { transform: scale(1.04); }
.card-img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f7f8fb 0%, #eceff5 100%);
}
.fallback-icon {
  font-size: 2.6rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #c9d0dc;
}
/* 编号 */
.card-serial {
  position: absolute;
  top: 12px;
  left: 12px;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--ink-2);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
  border-radius: 7px;
  padding: 3px 8px;
}

/* ===== 管理员操作（封面上，悬停显现） ===== */
.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.2s, transform 0.2s var(--ease);
  z-index: 3;
}
.project-card:hover .card-actions {
  opacity: 1;
  transform: translateY(0);
}
.action-icon-btn {
  background: rgba(255, 255, 255, 0.94) !important;
  border: none !important;
  color: var(--ink-2) !important;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.14);
  transition: background 0.2s, color 0.2s, transform 0.15s !important;
}
.action-icon-btn:hover {
  color: var(--accent) !important;
  transform: scale(1.06);
}
.delete-btn-action:hover { color: #e2554f !important; }

/* ===== 卡片主体 ===== */
.card-body {
  padding: 18px 20px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}
.card-title {
  font-size: 1.08rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
  line-height: 1.45;
  flex: 1;
  transition: color 0.2s;
}
.project-card:hover .card-title { color: var(--accent); }

/* ===== 状态标签 ===== */
.status-badge {
  flex-shrink: 0;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 7px;
  background: #f2f4f8;
  color: var(--ink-2);
  white-space: nowrap;
  margin-top: 2px;
}
.status-badge[data-status='已完成'] { background: #e8f6ee; color: #188a4d; }
.status-badge[data-status='开发中'] { background: #fdf3e6; color: #b3701a; }
.status-badge[data-status='学习中'] { background: var(--accent-soft); color: var(--accent-deep); }

.card-desc {
  color: var(--ink-2);
  font-size: 0.86rem;
  line-height: 1.7;
  margin: 0 0 14px;
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
  margin-bottom: 14px;
}
.tech-chip {
  font-size: 0.72rem;
  color: var(--ink-2);
  background: #f4f6fa;
  border-radius: 6px;
  padding: 3px 9px;
  cursor: default;
  transition: background 0.2s, color 0.2s;
}
.project-card:hover .tech-chip {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

/* ===== 卡片底部 ===== */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}
.card-date {
  font-size: 0.78rem;
  color: var(--ink-3);
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.card-more {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ink-3);
  white-space: nowrap;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.25s var(--ease), transform 0.25s var(--ease), color 0.2s;
}
.project-card:hover .card-more {
  opacity: 1;
  transform: translateX(0);
  color: var(--accent);
}

/* ===== 空态 ===== */
.empty-state {
  grid-column: 1 / -1;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 64px 20px;
  text-align: center;
}
.empty-mark {
  color: #ccd3de;
  margin: 0 0 12px;
  line-height: 1;
}
.empty-text {
  color: var(--ink-3);
  font-size: 0.9rem;
  margin: 0 0 20px;
}
.empty-reset {
  border: 1px solid var(--line-2);
  background: var(--card);
  color: var(--ink-2);
  font-size: 0.82rem;
  padding: 8px 18px;
  border-radius: 9px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.empty-reset:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

/* ===== 项目详情弹窗 ===== */
.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.detail-dialog :deep(.el-dialog__header) {
  display: none;
}
.detail-dialog :deep(.el-dialog) {
  border-radius: 16px;
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
  background: #eef0f4;
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
  background: linear-gradient(transparent 55%, rgba(16, 24, 40, 0.55));
  display: flex;
  align-items: flex-end;
  padding: 20px 24px;
}
.detail-hero-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f7f8fb 0%, #eceff5 100%);
}
.detail-hero-placeholder {
  text-align: center;
}
.detail-hero-icon {
  font-size: 3.2rem;
  font-weight: 800;
  color: #c9d0dc;
  display: block;
  margin-bottom: 12px;
}
.detail-hero-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}
.detail-hero-overlay .detail-hero-meta { justify-content: flex-start; }
.hero-badge {
  display: inline-block;
  padding: 4px 11px;
  font-size: 0.72rem;
  font-weight: 600;
  white-space: nowrap;
  border-radius: 8px;
  background: #f2f4f8;
  color: var(--ink-2);
}
.hero-badge[data-status='已完成'] { background: #e8f6ee; color: #188a4d; }
.hero-badge[data-status='开发中'] { background: #fdf3e6; color: #b3701a; }
.hero-badge[data-status='学习中'] { background: var(--accent-soft); color: var(--accent-deep); }
.hero-badge-cat { background: rgba(255, 255, 255, 0.94); color: var(--ink-2); }

/* ---- 标题区域 ---- */
.detail-title-area {
  padding: 26px 28px 0;
}
.detail-project-title {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--ink);
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
  font-size: 0.8rem;
  color: var(--ink-3);
}

/* ---- 分割线 ---- */
.detail-divider {
  margin: 20px 28px;
  border-color: var(--line);
}

/* ---- 章节 ---- */
.detail-section {
  padding: 0 28px 4px;
}
.detail-section-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 14px;
}
.section-title-dot {
  display: inline-block;
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: var(--accent);
}
.detail-desc-content {
  background: var(--card-soft);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 18px 20px;
}
.detail-desc-content p {
  color: var(--ink-2);
  font-size: 0.92rem;
  line-height: 1.85;
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
  padding: 5px 14px;
  border-radius: 8px;
  background: #f4f6fa;
  color: var(--ink-2);
  transition: background 0.2s, color 0.2s;
  cursor: default;
}
.detail-tech-tag:hover {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

/* ---- 链接按钮 ---- */
.detail-links-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  padding: 0 28px 28px;
}
.detail-link-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  height: auto;
  padding: 12px 20px;
  border-radius: 12px;
  line-height: 1.4;
  min-width: 160px;
  flex: 1;
  border: 1px solid var(--line-2);
  background: var(--card);
  color: var(--ink);
  transition: border-color 0.22s, background 0.22s, color 0.22s, transform 0.18s;
}
.detail-link-btn:hover {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent-deep);
  transform: translateY(-1px);
}
.detail-link-btn span {
  font-size: 0.9rem;
  font-weight: 600;
}
.link-hint {
  font-size: 0.72rem;
  opacity: 0.6;
  font-weight: 400;
  margin-top: 3px;
  word-break: break-all;
}

/* ===== Dialog 编辑表单 ===== */
.project-form .el-input-number .el-input__wrapper { padding-left: 10px; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .page-head { padding: 88px 20px 0; }
  .head-sub { margin-bottom: 24px; }
  .head-meta { gap: 14px; }
  .head-add { margin-left: 0; }
  .shelf-nav { padding: 0 20px; }
  .content-body { padding: 22px 20px 0; }
  .project-grid { grid-template-columns: 1fr; gap: 18px; }
  .card-actions { opacity: 1; transform: none; }
  .card-more { display: none; }
  .project-card { animation: cardFadeIn 0.45s var(--ease) forwards; }
  .detail-dialog :deep(.el-dialog) { width: 92vw !important; }
  .project-form .el-row { flex-direction: column; }
  .project-form .el-row .el-col { width: 100%; }
}

/* 尊重减弱动效 */
@media (prefers-reduced-motion: reduce) {
  .projects-page { transition: none; opacity: 1; }
  .page-loaded .project-card { animation: none; opacity: 1; transform: none; }
  .project-card { transition: border-color 0.2s; }
}

</style>

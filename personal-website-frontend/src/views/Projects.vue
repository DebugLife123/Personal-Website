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
      <p class="head-kicker">W O R K S · 手 作 档 案</p>
      <h1 class="head-title">项目<span class="head-title-accent">集</span></h1>
      <p class="head-sub">—— 从想法到落地，这里收着几件得意的作品</p>
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
          <el-icon :size="13"><Plus /></el-icon> 登记新项目
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
            <span class="card-serial">Nº {{ String(index + 1).padStart(2, '0') }}</span>
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
        </article>

        <div v-if="filteredProjects.length === 0 && !loading" class="empty-state">
          <p class="empty-mark">空</p>
          <p class="empty-text">{{ searchKeyword || activeCategory ? '这一格架子还是空的，换个条件试试' : '架子还没摆上东西' }}</p>
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
/* ========== 纸墨档案室 · 设计令牌 ========== */
.projects-page {
  --paper: #f0ece1;
  --paper-raised: #faf7ee;
  --ink: #2b2722;
  --ink-2: #645d51;
  --ink-3: #948c7b;
  --line: #ddd5c3;
  --line-strong: #c2b89d;
  --vermilion: #b23e22;
  --vermilion-deep: #93321a;
  --serif: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
  --mono: 'JetBrains Mono', 'Cascadia Mono', Consolas, 'Courier New', monospace;
  --sans: -apple-system, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;

  min-height: 100vh;
  padding-bottom: 120px;
  color: var(--ink);
  background:
    radial-gradient(1200px 400px at 50% -120px, rgba(255, 253, 246, 0.8), transparent 70%),
    var(--paper);
  opacity: 0;
  transition: opacity 0.5s ease;
  overflow-x: clip;
}
/* 纸面颗粒 */
.projects-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 30;
  opacity: 0.55;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)' opacity='0.045'/%3E%3C/svg%3E");
}
.projects-page.page-loaded { opacity: 1; }

/* ========== 卷首：页眉 ========== */
.page-head {
  position: relative;
  max-width: 1160px;
  margin: 0 auto;
  padding: 78px 28px 30px;
  z-index: 1;
}
.head-kicker {
  font-family: var(--mono);
  font-size: 0.72rem;
  font-weight: 500;
  letter-spacing: 0.32em;
  color: var(--ink-3);
  margin: 0 0 20px;
}
.head-kicker::before {
  content: '';
  display: inline-block;
  width: 34px;
  height: 1px;
  background: var(--line-strong);
  vertical-align: middle;
  margin-right: 14px;
  transform: translateY(-2px);
}
.head-title {
  font-family: var(--serif);
  font-size: clamp(3.2rem, 7.5vw, 5.4rem);
  font-weight: 900;
  line-height: 1.05;
  margin: 0 0 14px;
  color: var(--ink);
  text-wrap: balance;
}
.head-title-accent { color: var(--vermilion); }
.head-sub {
  font-family: var(--serif);
  font-size: 1rem;
  color: var(--ink-2);
  margin: 0 0 34px;
}
.head-meta {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}
.head-meta-item {
  font-size: 0.85rem;
  color: var(--ink-2);
}
.head-meta-item em {
  font-family: var(--mono);
  font-style: normal;
  font-weight: 600;
  color: var(--vermilion);
  margin: 0 2px;
}

/* 检索（墨线输入） */
.head-search {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--line-strong);
  padding: 4px 2px;
  color: var(--ink-3);
  transition: border-color 0.25s;
}
.head-search:focus-within { border-bottom-color: var(--vermilion); color: var(--vermilion); }
.head-search-input {
  border: none;
  background: transparent;
  outline: none;
  font-family: var(--sans);
  font-size: 0.86rem;
  color: var(--ink);
  width: 150px;
  padding: 2px 0;
}
.head-search-input::placeholder { color: var(--ink-3); }
.head-search-clear {
  border: none;
  background: none;
  color: var(--ink-3);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  padding: 0 2px;
}
.head-search-clear:hover { color: var(--vermilion); }

.head-add {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  border: 1px solid var(--vermilion);
  background: transparent;
  color: var(--vermilion);
  font-family: var(--sans);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  padding: 7px 18px;
  cursor: pointer;
  transition: background 0.22s, color 0.22s, transform 0.15s;
}
.head-add:hover {
  background: var(--vermilion);
  color: var(--paper-raised);
  transform: translateY(-1px);
}
.head-add:active { transform: translateY(0); }

/* ========== 分类横档 ========== */
.shelf-nav {
  position: relative;
  z-index: 1;
  max-width: 1160px;
  margin: 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: flex-end;
  gap: 30px;
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
  scrollbar-width: none;
}
.shelf-nav::-webkit-scrollbar { display: none; }
.shelf-tab {
  position: relative;
  border: none;
  background: none;
  font-family: var(--sans);
  font-size: 0.92rem;
  color: var(--ink-3);
  padding: 12px 2px;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.22s;
}
.shelf-tab::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: -1px;
  height: 2px;
  background: var(--vermilion);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s cubic-bezier(0.65, 0, 0.35, 1);
}
.shelf-tab:hover { color: var(--ink); }
.shelf-tab.active {
  color: var(--vermilion);
  font-weight: 600;
}
.shelf-tab.active::after { transform: scaleX(1); }
.shelf-reset {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--ink-3);
}
.shelf-reset:hover { color: var(--vermilion); }

/* ===== 内容区 ===== */
.content-body {
  position: relative;
  z-index: 1;
  max-width: 1160px;
  margin: 0 auto;
  padding: 36px 28px 0;
}
.loading-wrapper {
  background: var(--paper-raised);
  border: 1px solid var(--line);
  padding: 40px;
}
.project-grid {
  min-height: 200px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 22px;
}

/* ===== 项目档卡 ===== */
.project-card {
  position: relative;
  background: var(--paper-raised);
  border: 1px solid var(--line);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  opacity: 0;
  transform: translateY(18px);
  box-shadow: 0 1px 0 rgba(43, 39, 34, 0.02);
  transition:
    transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.3s,
    box-shadow 0.3s;
}
.page-loaded .project-card {
  animation: cardFadeIn 0.5s cubic-bezier(0.22, 1, 0.36, 1) forwards;
  animation-delay: calc(var(--i) * 0.06s);
}
@keyframes cardFadeIn {
  to { opacity: 1; transform: translateY(0); }
}
.project-card:hover {
  transform: translateY(-5px);
  border-color: var(--ink);
  box-shadow: 0 16px 34px -14px rgba(43, 39, 34, 0.28);
}
/* 左上角朱批标记 */
.project-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 0; height: 0;
  border-style: solid;
  border-width: 26px 26px 0 0;
  border-color: var(--vermilion) transparent transparent transparent;
  opacity: 0;
  transition: opacity 0.25s;
  z-index: 3;
}
.project-card:hover::before { opacity: 1; }

/* ===== 卡片封面 ===== */
.card-img {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #e8e2d2;
  border-bottom: 1px solid var(--line);
}
.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(0.82) contrast(1.02);
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1), filter 0.4s;
}
.project-card:hover .card-img img {
  transform: scale(1.05);
  filter: saturate(1);
}
/* 无封面：斜纹纸 + 大号题字 */
.card-img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background:
    repeating-linear-gradient(-45deg, transparent 0 12px, rgba(43, 39, 34, 0.035) 12px 13px),
    linear-gradient(160deg, #f3eee1 0%, #e9e2cf 100%);
}
.fallback-icon {
  font-family: var(--serif);
  font-size: 4rem;
  font-weight: 900;
  color: rgba(43, 39, 34, 0.16);
  line-height: 1;
}
/* 编号 */
.card-serial {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-family: var(--mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--ink-2);
  background: var(--paper-raised);
  border: 1px solid var(--line);
  padding: 2px 8px;
}

/* ===== 卡片主体 ===== */
.card-body {
  padding: 18px 20px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 9px;
}
.card-title {
  font-family: var(--serif);
  font-size: 1.18rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
  line-height: 1.4;
  flex: 1;
  transition: color 0.2s;
}
.project-card:hover .card-title { color: var(--vermilion); }

/* ===== 状态小印 ===== */
.status-badge {
  flex-shrink: 0;
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  padding: 3px 9px;
  border: 1px solid var(--ink-3);
  color: var(--ink-2);
  white-space: nowrap;
  margin-top: 3px;
}
.status-badge[data-status='已完成'] {
  border-color: var(--vermilion);
  color: var(--vermilion);
}
.status-badge[data-status='开发中'] { border-style: dashed; }
.status-badge[data-status='学习中'] { opacity: 0.75; }

.card-desc {
  color: var(--ink-2);
  font-size: 0.85rem;
  line-height: 1.75;
  margin: 0 0 12px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 技术栈 · 斜杠清单 ===== */
.tech-stack {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 3px;
  margin-bottom: 14px;
}
.tech-chip {
  font-family: var(--mono);
  font-size: 0.7rem;
  color: var(--ink-3);
  letter-spacing: 0.02em;
  cursor: default;
  transition: color 0.2s;
}
.tech-chip + .tech-chip::before {
  content: '/';
  color: var(--line-strong);
  margin: 0 7px;
}
.project-card:hover .tech-chip { color: var(--ink-2); }

/* ===== 卡片底部 ===== */
.card-footer {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 11px;
  border-top: 1px solid var(--line);
}
.card-date {
  font-family: var(--mono);
  font-size: 0.72rem;
  color: var(--ink-3);
  display: flex;
  align-items: center;
  gap: 5px;
}
.card-more {
  font-size: 0.76rem;
  color: var(--ink-3);
  letter-spacing: 0.1em;
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity 0.25s, transform 0.25s, color 0.25s;
}
.project-card:hover .card-more {
  opacity: 1;
  transform: translateX(0);
  color: var(--vermilion);
}
.card-actions {
  position: absolute;
  right: 0;
  bottom: 7px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s;
  background: var(--paper-raised);
  padding-left: 6px;
}
.project-card:hover .card-actions { opacity: 1; }
.action-icon-btn {
  background: transparent !important;
  border: 1px solid var(--line) !important;
  color: var(--ink-3) !important;
  transition: border-color 0.2s, color 0.2s, transform 0.15s !important;
}
.action-icon-btn:hover {
  color: var(--ink) !important;
  border-color: var(--ink) !important;
  transform: scale(1.08);
}
.delete-btn-action:hover {
  color: var(--vermilion) !important;
  border-color: var(--vermilion) !important;
}

/* ===== 空态 ===== */
.empty-state {
  grid-column: 1 / -1;
  background: var(--paper-raised);
  border: 1px dashed var(--line-strong);
  padding: 70px 20px;
  text-align: center;
}
.empty-mark {
  font-family: var(--serif);
  font-size: 3.4rem;
  font-weight: 900;
  color: var(--line-strong);
  margin: 0 0 10px;
  line-height: 1;
}
.empty-text {
  color: var(--ink-3);
  font-size: 0.9rem;
  margin: 0 0 20px;
}
.empty-reset {
  border: 1px solid var(--ink-3);
  background: none;
  color: var(--ink-2);
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  padding: 7px 20px;
  cursor: pointer;
  transition: all 0.22s;
}
.empty-reset:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
}

/* ===== 项目详情弹窗 ===== */
.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.detail-dialog :deep(.el-dialog__header) {
  display: none;
}
.detail-dialog :deep(.el-dialog) {
  border-radius: 2px;
  border: 1px solid var(--line-strong);
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
  background: #e8e2d2;
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
  background: linear-gradient(transparent 55%, rgba(30, 26, 20, 0.62));
  display: flex;
  align-items: flex-end;
  padding: 20px 24px;
}
.detail-hero-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    repeating-linear-gradient(-45deg, transparent 0 14px, rgba(43, 39, 34, 0.04) 14px 15px),
    linear-gradient(160deg, #f3eee1 0%, #e6dec8 100%);
}
.detail-hero-placeholder {
  text-align: center;
}
.detail-hero-icon {
  font-family: var(--serif);
  font-size: 4.6rem;
  font-weight: 900;
  color: rgba(43, 39, 34, 0.18);
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
  padding: 3px 12px;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  white-space: nowrap;
  border: 1px solid var(--ink-3);
  color: var(--ink-2);
  background: var(--paper-raised);
}
.hero-badge[data-status='已完成'] {
  border-color: var(--vermilion);
  color: var(--vermilion);
}
.hero-badge-cat {
  border-color: var(--line-strong);
}

/* ---- 标题区域 ---- */
.detail-title-area {
  padding: 28px 28px 0;
}
.detail-project-title {
  font-family: var(--serif);
  font-size: 1.7rem;
  font-weight: 900;
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
  font-family: var(--mono);
  font-size: 0.76rem;
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
  gap: 10px;
  font-family: var(--serif);
  font-size: 1.02rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 14px;
}
.section-title-dot {
  display: inline-block;
  width: 4px;
  height: 17px;
  background: var(--vermilion);
}
.detail-desc-content {
  background: rgba(43, 39, 34, 0.035);
  border: 1px solid var(--line);
  padding: 18px 20px;
}
.detail-desc-content p {
  color: var(--ink-2);
  font-size: 0.92rem;
  line-height: 1.9;
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
  font-family: var(--mono);
  font-size: 0.76rem;
  padding: 5px 14px;
  border: 1px solid var(--line-strong);
  color: var(--ink-2);
  transition: all 0.2s;
  cursor: default;
}
.detail-tech-tag:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
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
  border-radius: 2px;
  line-height: 1.4;
  min-width: 160px;
  flex: 1;
  border: 1px solid var(--line-strong);
  background: var(--paper-raised);
  color: var(--ink);
  transition: border-color 0.22s, color 0.22s, transform 0.18s;
}
.detail-link-btn:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
  transform: translateY(-1px);
  background: var(--paper-raised);
}
.detail-link-btn span {
  font-size: 0.9rem;
  font-weight: 600;
}
.link-hint {
  font-family: var(--mono);
  font-size: 0.68rem;
  opacity: 0.55;
  font-weight: 400;
  margin-top: 2px;
  word-break: break-all;
}

/* ===== Dialog 编辑表单 ===== */
.project-form .el-input-number .el-input__wrapper { padding-left: 10px; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .page-head { padding: 56px 20px 22px; }
  .head-sub { margin-bottom: 26px; }
  .head-meta { gap: 16px; }
  .head-add { margin-left: 0; }
  .shelf-nav { padding: 0 20px; gap: 22px; }
  .content-body { padding: 26px 20px 0; }
  .project-grid { grid-template-columns: 1fr; gap: 18px; }
  .card-actions { opacity: 1; }
  .card-more { display: none; }
  .project-card { animation: cardFadeIn 0.45s ease forwards; }
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

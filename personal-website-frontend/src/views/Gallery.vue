<template>
  <div class="gallery-page" :class="{ 'page-loaded': pageLoaded }">
    <!-- ===== 页眉 ===== -->
    <div class="gallery-masthead">
      <p class="mast-kicker">GALLERY · MOMENTS</p>
      <h1 class="mast-title">图<span class="mast-title-accent">库</span></h1>
      <p class="mast-sub">随手拍下的光与影，都放在这里</p>
    </div>

    <div class="gallery-body">
      <!-- 工具行 -->
      <div class="tool-row">
        <div class="tool-left">
          <span class="count-num">{{ photos.length }}</span>
          <span class="count-label">张照片</span>
        </div>
        <div class="tool-right">
          <el-input
            v-model="keyword"
            class="gallery-search"
            placeholder="搜索标题 / 简介 / 地点"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 骨架 -->
      <div v-if="loading" class="photo-grid">
        <div v-for="i in 6" :key="i" class="photo-skeleton"></div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filtered.length === 0" class="gallery-empty">
        <el-icon :size="38"><Picture /></el-icon>
        <p class="empty-text">{{ keyword ? '没有匹配的照片' : '图库还空着，等主人上传' }}</p>
        <button v-if="keyword" class="empty-reset" @click="keyword = ''">清除搜索</button>
      </div>

      <!-- 照片网格 -->
      <transition-group v-else name="photo-in" tag="div" class="photo-grid">
        <article
          v-for="(photo, i) in filtered"
          :key="photo.id"
          class="photo-card"
          :style="{ transitionDelay: Math.min(i, 12) * 35 + 'ms' }"
          @click="openViewer(photo)"
        >
          <div class="photo-frame">
            <img :src="photo.url" :alt="photo.title || photo.description" loading="lazy" />
            <div class="photo-veil"></div>
            <span class="photo-index">#{{ String(i + 1).padStart(2, '0') }}</span>
          </div>
          <div class="photo-meta">
            <h3 v-if="photo.title" class="photo-title">{{ photo.title }}</h3>
            <p class="photo-desc">{{ photo.description || '——' }}</p>
            <div class="photo-tags" v-if="photo.location || photo.shotTime">
              <span v-if="photo.location" class="photo-tag">
                <el-icon :size="11"><LocationInformation /></el-icon>{{ photo.location }}
              </span>
              <span v-if="photo.shotTime" class="photo-tag">
                <el-icon :size="11"><Calendar /></el-icon>{{ photo.shotTime }}
              </span>
            </div>
          </div>
        </article>
      </transition-group>
    </div>

    <!-- ===== 大图查看 ===== -->
    <transition name="viewer-fade">
      <div v-if="viewer.open" class="viewer" @click.self="closeViewer">
        <button class="viewer-close" @click="closeViewer" title="关闭 (Esc)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>

        <button v-if="filtered.length > 1" class="viewer-nav prev" @click.stop="step(-1)" title="上一张">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </button>
        <button v-if="filtered.length > 1" class="viewer-nav next" @click.stop="step(1)" title="下一张">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>

        <div class="viewer-panel" @click.stop>
          <div class="viewer-image">
            <img :src="viewer.photo?.url" :alt="viewer.photo?.title || viewer.photo?.description" />
          </div>
          <div class="viewer-side">
            <h2 v-if="viewer.photo?.title" class="viewer-title">{{ viewer.photo.title }}</h2>
            <p class="viewer-desc">{{ viewer.photo?.description || '这张照片还没有写简介' }}</p>
            <div class="viewer-tags">
              <span v-if="viewer.photo?.location" class="photo-tag">
                <el-icon :size="11"><LocationInformation /></el-icon>{{ viewer.photo.location }}
              </span>
              <span v-if="viewer.photo?.shotTime" class="photo-tag">
                <el-icon :size="11"><Calendar /></el-icon>{{ viewer.photo.shotTime }}
              </span>
            </div>
            <p class="viewer-count">{{ viewer.index + 1 }} / {{ filtered.length }}</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import request from '../utils/request'
import { Search, Picture, Calendar, LocationInformation } from '@element-plus/icons-vue'

const photos = ref([])
const loading = ref(false)
const pageLoaded = ref(false)
const keyword = ref('')

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return photos.value
  return photos.value.filter((p) =>
    [p.title, p.description, p.location]
      .filter(Boolean)
      .some((t) => String(t).toLowerCase().includes(kw))
  )
})

const fetchPhotos = async () => {
  loading.value = true
  try {
    const res = await request.get('/gallery/list')
    if (res.data.code === 200) photos.value = res.data.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// ==================== 大图查看 ====================
const viewer = ref({ open: false, index: 0, photo: null })

const openViewer = (photo) => {
  viewer.value = { open: true, index: filtered.value.findIndex((p) => p.id === photo.id), photo }
  document.body.style.overflow = 'hidden'
}

const closeViewer = () => {
  viewer.value = { open: false, index: 0, photo: null }
  document.body.style.overflow = ''
}

const step = (delta) => {
  const total = filtered.value.length
  if (!total) return
  const next = (viewer.value.index + delta + total) % total
  viewer.value = { open: true, index: next, photo: filtered.value[next] }
}

const onKey = (e) => {
  if (!viewer.value.open) return
  if (e.key === 'Escape') closeViewer()
  else if (e.key === 'ArrowLeft') step(-1)
  else if (e.key === 'ArrowRight') step(1)
}

onMounted(async () => {
  await fetchPhotos()
  setTimeout(() => { pageLoaded.value = true }, 80)
  window.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>

<style scoped>
/* ============================================================
   图库 · 与项目/留言页同一套商务风令牌
   ============================================================ */
.gallery-page {
  --page: #f4f5f7;
  --card: #ffffff;
  --card-soft: #fafbfd;
  --ink: #1a2233;
  --ink-2: #5c6675;
  --ink-3: #98a1b3;
  --line: #e9ecf2;
  --line-2: #dde2ea;
  --accent: #3d6ee0;
  --accent-soft: #eef3fd;
  --shadow-1: 0 1px 2px rgba(16, 24, 40, 0.04), 0 1px 3px rgba(16, 24, 40, 0.03);
  --shadow-2: 0 14px 34px -14px rgba(16, 24, 40, 0.18);
  --ease: cubic-bezier(0.22, 1, 0.36, 1);

  min-height: 100vh;
  padding-bottom: 72px;
  background: var(--page);
  color: var(--ink);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Helvetica Neue",
    "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ---------- 页眉 ---------- */
.gallery-masthead {
  max-width: 1240px;
  margin: 0 auto;
  padding: 128px 28px 26px;
}

.mast-kicker {
  margin: 0 0 10px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 3px;
  color: var(--accent);
  opacity: 0;
  animation: rise 0.6s var(--ease) 0.05s forwards;
}

.mast-title {
  margin: 0 0 12px;
  font-size: 2.6rem;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--ink);
  opacity: 0;
  animation: rise 0.6s var(--ease) 0.12s forwards;
}

.mast-title-accent { color: var(--accent); }

.mast-sub {
  margin: 0;
  font-size: 0.92rem;
  color: var(--ink-2);
  letter-spacing: 0.4px;
  opacity: 0;
  animation: rise 0.6s var(--ease) 0.2s forwards;
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ---------- 主体 ---------- */
.gallery-body {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 28px;
}

.tool-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
  flex-wrap: wrap;
}

.tool-left { display: flex; align-items: baseline; gap: 6px; }
.count-num { font-size: 1.5rem; font-weight: 800; color: var(--ink); }
.count-label { font-size: 0.82rem; color: var(--ink-3); letter-spacing: 0.5px; }

.gallery-search { width: 260px; }
.gallery-search :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: var(--card);
  box-shadow: 0 0 0 1px var(--line) inset;
}
.gallery-search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px var(--accent-soft);
}

/* ---------- 网格 ---------- */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 22px;
}

.photo-skeleton {
  height: 268px;
  border-radius: 14px;
  background: linear-gradient(100deg, var(--card) 30%, #eef1f6 50%, var(--card) 70%);
  background-size: 220% 100%;
  animation: shimmer 1.3s infinite linear;
}

@keyframes shimmer {
  from { background-position: 180% 0; }
  to { background-position: -60% 0; }
}

.photo-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-1);
  opacity: 0;
  transform: translateY(14px);
  transition: transform 0.42s var(--ease), box-shadow 0.42s var(--ease), border-color 0.3s;
}

.page-loaded .photo-card {
  opacity: 1;
  transform: translateY(0);
}

.photo-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-2);
  border-color: var(--line-2);
}

.photo-frame {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: linear-gradient(135deg, #f7f8fb, #eceff5);
}

.photo-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.7s var(--ease);
}

.photo-card:hover .photo-frame img { transform: scale(1.05); }

.photo-veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(16, 24, 40, 0) 45%, rgba(16, 24, 40, 0.5) 100%);
  opacity: 0;
  transition: opacity 0.35s var(--ease);
}

.photo-card:hover .photo-veil { opacity: 1; }

.photo-index {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(6px);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--ink-2);
}

.photo-meta { padding: 15px 17px 17px; }

.photo-title {
  margin: 0 0 6px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.2px;
}

.photo-desc {
  margin: 0;
  font-size: 0.84rem;
  line-height: 1.65;
  color: var(--ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.photo-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }

.photo-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--card-soft);
  border: 1px solid var(--line);
  font-size: 0.7rem;
  color: var(--ink-3);
}

/* ---------- 空状态 ---------- */
.gallery-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 90px 20px;
  border-radius: 14px;
  background: var(--card);
  border: 1px dashed var(--line-2);
  color: var(--ink-3);
}

.empty-text { margin: 0; font-size: 0.88rem; }

.empty-reset {
  padding: 6px 16px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: transparent;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 0.8rem;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}

.empty-reset:hover { border-color: var(--accent); color: var(--accent); }

/* ---------- 列表进入动画 ---------- */
.photo-in-enter-active { transition: opacity 0.45s var(--ease), transform 0.45s var(--ease); }
.photo-in-enter-from { opacity: 0; transform: translateY(16px); }
.photo-in-leave-active { display: none; }

/* ============================================================
   大图查看
   ============================================================ */
.viewer {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: rgba(8, 12, 20, 0.78);
  backdrop-filter: blur(10px);
}

.viewer-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  width: min(1180px, 100%);
  max-height: calc(100vh - 80px);
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 40px 90px -30px rgba(0, 0, 0, 0.7);
  animation: viewer-in 0.36s var(--ease) both;
}

@keyframes viewer-in {
  from { opacity: 0; transform: translateY(14px) scale(0.99); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.viewer-image {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d1119;
  min-height: 320px;
}

.viewer-image img {
  max-width: 100%;
  max-height: calc(100vh - 80px);
  object-fit: contain;
  display: block;
}

.viewer-side {
  padding: 28px 26px;
  overflow-y: auto;
  border-left: 1px solid var(--line);
}

.viewer-title {
  margin: 0 0 12px;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--ink);
}

.viewer-desc {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.85;
  color: var(--ink-2);
  white-space: pre-wrap;
}

.viewer-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }

.viewer-count {
  margin: 22px 0 0;
  font-size: 0.72rem;
  letter-spacing: 1.4px;
  color: var(--ink-3);
}

.viewer-close {
  position: absolute;
  top: 26px;
  right: 30px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, transform 0.2s;
}

.viewer-close:hover { background: rgba(255, 255, 255, 0.22); transform: rotate(90deg); }

.viewer-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, transform 0.2s;
}

.viewer-nav:hover { background: rgba(255, 255, 255, 0.24); }
.viewer-nav.prev { left: 26px; }
.viewer-nav.next { right: 26px; }

.viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity 0.28s ease; }
.viewer-fade-enter-from, .viewer-fade-leave-to { opacity: 0; }

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .gallery-masthead { padding: 104px 20px 20px; }
  .mast-title { font-size: 2.1rem; }
  .gallery-body { padding: 0 20px; }
  .photo-grid { grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
  .viewer { padding: 18px; }
  .viewer-panel { grid-template-columns: 1fr; max-height: calc(100vh - 36px); }
  .viewer-image { min-height: 200px; }
  .viewer-side { border-left: none; border-top: 1px solid var(--line); padding: 20px; }
  .viewer-nav.prev { left: 8px; }
  .viewer-nav.next { right: 8px; }
  .gallery-search { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  .photo-card, .mast-kicker, .mast-title, .mast-sub { animation: none; opacity: 1; transform: none; }
  .photo-frame img, .photo-card { transition: none; }
}
</style>

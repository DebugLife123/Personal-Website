<template>
  <div class="home-page">
    <!-- ============================================================
         ① 顶部轮播
         换图：把图片放到 public/images/ 或上传到图库，
         也可以直接在下方 HERO_SLIDES 里写死 image 地址
         ============================================================ -->
    <section class="hero" @mouseenter="pauseHero" @mouseleave="resumeHero">
      <div class="hero-slides">
        <div
          v-for="(s, i) in slides"
          :key="i"
          class="hero-slide"
          :class="{ active: i === heroIndex, prev: i === prevIndex }"
          :style="{ backgroundImage: `url(${s.image})` }"
        ></div>
      </div>
      <div class="hero-veil"></div>
      <div class="hero-grain"></div>

      <!-- 中间区域的硬编码文案（每张图可单独覆盖） -->
      <div class="hero-content" :key="heroIndex">
        <p class="hero-kicker">{{ current.kicker }}</p>
        <h1 class="hero-title">{{ current.title }}</h1>
        <p class="hero-sub">{{ current.sub }}</p>
        <div class="hero-actions">
          <button class="hero-btn primary" @click="router.push('/gallery')">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="5" width="18" height="14" rx="3" /><circle cx="9" cy="10" r="1.6" /><path d="M4 17l4.5-4.5 3.5 3.5 3-3L20 17" />
            </svg>
            逛逛照片墙
          </button>
          <button class="hero-btn ghost" @click="scrollTo('blog')">读点文章</button>
        </div>
      </div>

      <!-- 轮播控制 -->
      <div v-if="slides.length > 1" class="hero-controls">
        <button class="hero-arrow" @click="goSlide(heroIndex - 1)" aria-label="上一张">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </button>
        <div class="hero-dots">
          <button
            v-for="(s, i) in slides"
            :key="i"
            class="hero-dot"
            :class="{ active: i === heroIndex }"
            @click="goSlide(i)"
            :aria-label="`第 ${i + 1} 张`"
          >
            <span class="dot-fill" :style="i === heroIndex && !heroPaused ? { animationDuration: HERO_INTERVAL + 'ms' } : null"></span>
          </button>
        </div>
        <button class="hero-arrow" @click="goSlide(heroIndex + 1)" aria-label="下一张">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>
      </div>

      <div class="scroll-hint">
        <span>向下滚动</span>
        <i></i>
      </div>
    </section>

    <!-- ============================================================
         ② 个人卡（9:16）+ 图库入口
         ============================================================ -->
    <section class="profile-section reveal">
      <div class="profile-grid">
        <!-- 竖屏个人卡 -->
        <aside class="id-card">
          <div class="id-avatar">
            <img :src="AVATAR" alt="yu翔" />
          </div>
          <h2 class="nickname">yu翔</h2>
          <p class="motto">初出茅庐 <span>|</span> 科班码农 <span>|</span> 拾枝者</p>

          <div class="id-divider"></div>

          <div class="social-grid">
            <button class="social-btn" @click="router.push('/blog')">
              <el-icon><Notebook /></el-icon><span>博客</span>
            </button>
            <button class="social-btn" @click="router.push('/resume')">
              <el-icon><Document /></el-icon><span>简历</span>
            </button>
            <button class="social-btn" @click="openExternal('https://github.com/DebugLife123')">
              <img src="https://api.iconify.design/mdi:github.svg" width="16" height="16" />
              <span>GitHub</span>
            </button>
            <button class="social-btn" @click="openExternal('https://leetcode.cn/u/festive-lichtermaninv/')">
              <img src="https://api.iconify.design/simple-icons:leetcode.svg" width="16" height="16" />
              <span>LeetCode</span>
            </button>
          </div>

          <p class="id-foot">© 2025 - {{ new Date().getFullYear() }}</p>
        </aside>

        <!-- 图库入口 -->
        <div class="gallery-entry" @click="router.push('/gallery')">
          <div class="ge-head">
            <div>
              <p class="ge-kicker">GALLERY</p>
              <h3 class="ge-title">我的图库</h3>
              <p class="ge-sub">随手拍下的光与影，都在这里飘着</p>
            </div>
            <span class="ge-arrow">
              进入照片墙
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h13M13 6l6 6-6 6" />
              </svg>
            </span>
          </div>

          <div class="ge-thumbs" :class="{ empty: !galleryThumbs.length }">
            <template v-if="galleryThumbs.length">
              <div v-for="(p, i) in galleryThumbs" :key="p.id" class="ge-thumb" :class="`t${i}`">
                <img :src="p.url" :alt="p.title || p.description || '照片'" loading="lazy" />
              </div>
            </template>
            <div v-else class="ge-empty">
              <el-icon :size="26"><Picture /></el-icon>
              <span>图库还空着，去后台上传几张</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============================================================
         ③ 最新文章
         ============================================================ -->
    <section id="blog" class="blog-section reveal">
      <div class="section-head">
        <div>
          <p class="section-kicker">BLOG</p>
          <h2 class="section-title">最新文章</h2>
        </div>
        <button class="section-more" @click="router.push('/blog')">
          查看全部
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h13M13 6l6 6-6 6" />
          </svg>
        </button>
      </div>

      <div v-if="articles.length" class="blog-grid">
        <article
          v-for="a in articles"
          :key="a.id"
          class="blog-card"
          @click="router.push(`/article/${a.id}`)"
        >
          <div class="blog-cover">
            <img v-if="a.cover" :src="a.cover" :alt="a.title" loading="lazy" />
            <div v-else class="cover-fallback">{{ (a.title || '文').charAt(0) }}</div>
            <span v-if="a.category" class="blog-tag">{{ a.category }}</span>
          </div>
          <div class="blog-body">
            <h3 class="blog-title">{{ a.title }}</h3>
            <p class="blog-summary">{{ a.summary || '——' }}</p>
            <div class="blog-meta">
              <span>{{ (a.createTime || '').slice(0, 10) }}</span>
              <span class="dot-sep">·</span>
              <span>{{ a.views || 0 }} 阅读</span>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="blog-empty">
        <p>还没有发布的文章</p>
      </div>
    </section>

    <!-- ============================================================
         ④ 作品集入口
         ============================================================ -->
    <section class="works-cta reveal" @click="router.push('/projects')">
      <div class="works-glow"></div>
      <div class="works-inner">
        <p class="works-kicker">PORTFOLIO</p>
        <h2 class="works-title">想看看我做过什么？</h2>
        <p class="works-sub">
          <template v-if="projectCount">{{ projectCount }} 个项目，从想法到落地都记在里面</template>
          <template v-else>从想法到落地，都记在里面</template>
        </p>
        <span class="works-btn">
          浏览作品集
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h13M13 6l6 6-6 6" />
          </svg>
        </span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { Notebook, Document, Picture } from '@element-plus/icons-vue'

const router = useRouter()

// ============================================================
// 首页配置：改这里就能换图 / 换文案
// ============================================================
const AVATAR = 'https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500'

/** 轮播文案（每张图默认都用这段；想单张不同，就在 HERO_SLIDES 里写 title/sub） */
const HERO_TEXT = {
  kicker: '拾枝者的自留地',
  title: '把走过的路，都留在这里',
  sub: '记录 · 构建 · 分享 —— 一个还在长大的个人站点'
}

/** 手动指定轮播图（留空则自动使用图库照片，图库为空时用内置星空图） */
const HERO_SLIDES = []

/** 是否用照片自带的标题/简介作为轮播文案（默认 false：始终用上面的硬编码文案） */
const HERO_USE_PHOTO_CAPTION = false

/** 图库为空时的兜底背景 */
const HERO_FALLBACK = '/images/login-bg.jpg'

const HERO_INTERVAL = 6000

// ============================================================
// 轮播
// ============================================================
const galleryPhotos = ref([])
const heroIndex = ref(0)
const prevIndex = ref(-1)
const heroPaused = ref(false)
let heroTimer = null

const slides = computed(() => {
  if (HERO_SLIDES.length) {
    return HERO_SLIDES.map((s) => ({
      image: s.image,
      kicker: s.kicker || HERO_TEXT.kicker,
      title: s.title || HERO_TEXT.title,
      sub: s.sub || HERO_TEXT.sub
    }))
  }
  if (galleryPhotos.value.length) {
    return galleryPhotos.value.slice(0, 5).map((p) => ({
      image: p.url,
      kicker: (HERO_USE_PHOTO_CAPTION && p.location) || HERO_TEXT.kicker,
      title: (HERO_USE_PHOTO_CAPTION && p.title) || HERO_TEXT.title,
      sub: (HERO_USE_PHOTO_CAPTION && p.description) || HERO_TEXT.sub
    }))
  }
  return [{ image: HERO_FALLBACK, ...HERO_TEXT }]
})

const current = computed(() => slides.value[heroIndex.value] || slides.value[0] || HERO_TEXT)

const goSlide = (i) => {
  const total = slides.value.length
  if (total < 2) return
  prevIndex.value = heroIndex.value
  heroIndex.value = (i + total) % total
  restartHeroTimer()
}

const restartHeroTimer = () => {
  clearInterval(heroTimer)
  if (heroPaused.value || slides.value.length < 2) return
  heroTimer = setInterval(() => {
    prevIndex.value = heroIndex.value
    heroIndex.value = (heroIndex.value + 1) % slides.value.length
  }, HERO_INTERVAL)
}

const pauseHero = () => { heroPaused.value = true; clearInterval(heroTimer) }
const resumeHero = () => { heroPaused.value = false; restartHeroTimer() }

// ============================================================
// 数据
// ============================================================
const galleryThumbs = ref([])
const articles = ref([])
const projectCount = ref(0)

const loadData = async () => {
  const [galleryRes, articleRes, projectRes] = await Promise.all([
    request.get('/gallery/list').catch(() => null),
    request.get('/article/page', { params: { page: 1, pageSize: 3, status: '已发布' } }).catch(() => null),
    request.get('/project/list').catch(() => null)
  ])

  if (galleryRes?.data?.code === 200) {
    galleryPhotos.value = (galleryRes.data.data || []).filter((p) => p.url)
    galleryThumbs.value = galleryPhotos.value.slice(0, 4)
  }
  if (articleRes?.data?.code === 200) {
    articles.value = (articleRes.data.data?.records || []).slice(0, 3)
  }
  if (projectRes?.data?.code === 200) {
    projectCount.value = (projectRes.data.data || []).length
  }
}

const openExternal = (url) => window.open(url, '_blank')

const scrollTo = (id) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ============================================================
// 滚动进场
// ============================================================
let observer = null
const setupReveal = () => {
  const els = document.querySelectorAll('.home-page .reveal')
  if (!('IntersectionObserver' in window)) {
    els.forEach((el) => el.classList.add('in'))
    return
  }
  observer = new IntersectionObserver((entries) => {
    entries.forEach((e) => {
      if (e.isIntersecting) {
        e.target.classList.add('in')
        observer.unobserve(e.target)
      }
    })
  }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' })
  els.forEach((el) => observer.observe(el))
}

const onVisibility = () => {
  if (document.hidden) clearInterval(heroTimer)
  else restartHeroTimer()
}

onMounted(async () => {
  await loadData()
  await nextTick()
  setupReveal()
  restartHeroTimer()
  document.addEventListener('visibilitychange', onVisibility)
})

onBeforeUnmount(() => {
  clearInterval(heroTimer)
  observer?.disconnect()
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
/* ============================================================
   首页 · 与全站同一套商务风令牌
   ============================================================ */
.home-page {
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
  --shadow-2: 0 16px 40px -18px rgba(16, 24, 40, 0.22);
  --ease: cubic-bezier(0.22, 1, 0.36, 1);

  min-height: 100vh;
  background: var(--page);
  color: var(--ink);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Helvetica Neue",
    "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow-x: hidden;
}

/* ============================================================
   ① 轮播
   ============================================================ */
.hero {
  position: relative;
  height: 100vh;
  min-height: 620px;
  max-height: 1000px;
  overflow: hidden;
  background: #0b1020;
}

.hero-slides { position: absolute; inset: 0; }

.hero-slide {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0;
  transform: scale(1.06);
  transition: opacity 1.1s var(--ease), transform 7s linear;
  will-change: opacity, transform;
}

.hero-slide.active { opacity: 1; transform: scale(1); }

/* 交叉淡出时旧图保持可见，避免闪黑 */
.hero-slide.prev { opacity: 0; transform: scale(1.03); transition: opacity 1.1s var(--ease); }

.hero-veil {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(8, 12, 22, 0.78) 0%, rgba(8, 12, 22, 0.46) 38%, rgba(8, 12, 22, 0.9) 100%),
    /* 中央压暗，保证文字在任何图片上都清晰 */
    radial-gradient(ellipse 66% 52% at 50% 50%, rgba(6, 10, 18, 0.62) 0%, rgba(6, 10, 18, 0.32) 55%, rgba(6, 10, 18, 0.66) 100%);
}

.hero-grain {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background-image: radial-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 3px 3px;
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 2;
  height: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 65px 28px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
}

.hero-kicker {
  margin: 0 0 16px;
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 5px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
  animation: heroUp 0.9s var(--ease) both;
}

.hero-title {
  margin: 0 0 18px;
  font-size: clamp(2rem, 5.2vw, 3.6rem);
  font-weight: 800;
  line-height: 1.22;
  letter-spacing: 2px;
  text-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
  animation: heroUp 0.9s var(--ease) 0.08s both;
}

.hero-sub {
  margin: 0 0 34px;
  font-size: clamp(0.86rem, 1.5vw, 1.02rem);
  letter-spacing: 1.5px;
  color: rgba(255, 255, 255, 0.66);
  animation: heroUp 0.9s var(--ease) 0.16s both;
}

@keyframes heroUp {
  from { opacity: 0; transform: translateY(22px); filter: blur(6px); }
  to { opacity: 1; transform: translateY(0); filter: blur(0); }
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
  animation: heroUp 0.9s var(--ease) 0.24s both;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 26px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 0.9rem;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  transition: transform 0.28s var(--ease), background 0.28s, border-color 0.28s, box-shadow 0.28s;
}

.hero-btn.primary {
  border: none;
  background: #fff;
  color: #101827;
  box-shadow: 0 18px 40px -18px rgba(255, 255, 255, 0.6);
}

.hero-btn.primary:hover { transform: translateY(-3px); box-shadow: 0 22px 46px -16px rgba(255, 255, 255, 0.7); }

.hero-btn.ghost {
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  color: #fff;
}

.hero-btn.ghost:hover {
  transform: translateY(-3px);
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.6);
}

/* 控制条 */
.hero-controls {
  position: absolute;
  left: 50%;
  bottom: 74px;
  transform: translateX(-50%);
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-arrow {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.24);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: background 0.25s, transform 0.25s, border-color 0.25s;
}

.hero-arrow:hover { background: rgba(255, 255, 255, 0.2); border-color: rgba(255, 255, 255, 0.5); transform: scale(1.06); }

.hero-dots { display: flex; align-items: center; gap: 9px; }

.hero-dot {
  position: relative;
  width: 30px;
  height: 3px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.28);
  overflow: hidden;
  cursor: pointer;
  transition: width 0.35s var(--ease), background 0.3s;
}

.hero-dot.active { width: 52px; background: rgba(255, 255, 255, 0.3); }

.dot-fill {
  position: absolute;
  inset: 0;
  transform-origin: left center;
  transform: scaleX(0);
  background: #fff;
}

.hero-dot.active .dot-fill { animation: dotFill linear forwards; }

@keyframes dotFill {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

.scroll-hint {
  position: absolute;
  left: 50%;
  bottom: 26px;
  transform: translateX(-50%);
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  font-size: 0.68rem;
  letter-spacing: 3px;
  color: rgba(255, 255, 255, 0.5);
}

.scroll-hint i {
  width: 1px;
  height: 26px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0));
  animation: hintMove 1.9s ease-in-out infinite;
}

@keyframes hintMove {
  0%, 100% { opacity: 0.3; transform: translateY(-4px); }
  50% { opacity: 1; transform: translateY(4px); }
}

/* ============================================================
   ② 个人卡 + 图库入口
   ============================================================ */
.profile-section {
  max-width: 1240px;
  margin: 0 auto;
  padding: 64px 28px 0;
}

.profile-grid {
  display: grid;
  grid-template-columns: 288px minmax(0, 1fr);
  gap: 26px;
  align-items: stretch;
}

/* 9:16 竖屏个人卡 */
.id-card {
  aspect-ratio: 9 / 16;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 24px;
  border-radius: 18px;
  background: var(--card);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-1);
  text-align: center;
  transition: transform 0.4s var(--ease), box-shadow 0.4s var(--ease);
}

.id-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-2); }

.id-avatar {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 20px;
  border: 3px solid var(--card);
  box-shadow: 0 10px 30px -12px rgba(16, 24, 40, 0.35);
}

.id-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }

.nickname {
  margin: 0 0 8px;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--ink);
}

.motto {
  margin: 0;
  font-size: 0.78rem;
  line-height: 1.7;
  color: var(--ink-2);
}

.motto span { color: var(--line-2); margin: 0 5px; }

.id-divider {
  width: 40px;
  height: 2px;
  margin: 24px 0;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--accent), #8fb4ff);
}

.social-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
  width: 100%;
}

.social-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--card-soft);
  color: var(--ink-2);
  font-family: inherit;
  font-size: 0.78rem;
  cursor: pointer;
  transition: transform 0.25s var(--ease), border-color 0.25s, color 0.25s, background 0.25s;
}

.social-btn img { display: block; }

.social-btn:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.id-foot {
  margin: 26px 0 0;
  font-size: 0.66rem;
  letter-spacing: 1.6px;
  color: var(--ink-3);
}

/* 图库入口 */
.gallery-entry {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 32px 34px;
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #1b2740 0%, #101827 55%, #17203a 100%);
  box-shadow: var(--shadow-1);
  transition: transform 0.42s var(--ease), box-shadow 0.42s var(--ease);
}

.gallery-entry::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(560px 320px at 12% 8%, rgba(96, 140, 255, 0.28), transparent 62%),
    radial-gradient(420px 300px at 88% 92%, rgba(150, 110, 240, 0.22), transparent 62%);
  pointer-events: none;
}

.gallery-entry:hover { transform: translateY(-4px); box-shadow: var(--shadow-2); }

.ge-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.ge-kicker {
  margin: 0 0 8px;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.5);
}

.ge-title { margin: 0 0 8px; font-size: 1.5rem; font-weight: 800; letter-spacing: 1px; }
.ge-sub { margin: 0; font-size: 0.8rem; color: rgba(255, 255, 255, 0.6); }

.ge-arrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 8px 15px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.1);
  font-size: 0.76rem;
  white-space: nowrap;
  transition: background 0.28s, transform 0.28s, border-color 0.28s;
}

.gallery-entry:hover .ge-arrow {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateX(3px);
}

.ge-thumbs {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-items: start;
  gap: 12px;
  margin-top: 30px;
}

.ge-thumb {
  aspect-ratio: 4 / 3;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.05);
}

.ge-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.6s var(--ease);
}

.gallery-entry:hover .ge-thumb img { transform: scale(1.07); }

.ge-thumbs.empty { display: block; }

.ge-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 46px 0;
  border-radius: 12px;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.8rem;
}

/* ============================================================
   ③ 最新文章
   ============================================================ */
.blog-section {
  max-width: 1240px;
  margin: 0 auto;
  padding: 78px 28px 0;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 26px;
}

.section-kicker {
  margin: 0 0 8px;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 4px;
  color: var(--accent);
}

.section-title { margin: 0; font-size: 1.7rem; font-weight: 800; letter-spacing: 1px; color: var(--ink); }

.section-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--line-2);
  background: var(--card);
  color: var(--ink-2);
  font-family: inherit;
  font-size: 0.78rem;
  cursor: pointer;
  transition: border-color 0.25s, color 0.25s, transform 0.25s;
}

.section-more:hover { border-color: var(--accent); color: var(--accent); transform: translateX(2px); }

.blog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 22px;
}

.blog-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  overflow: hidden;
  background: var(--card);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-1);
  cursor: pointer;
  transition: transform 0.4s var(--ease), box-shadow 0.4s var(--ease), border-color 0.3s;
}

.blog-card:hover { transform: translateY(-5px); box-shadow: var(--shadow-2); border-color: var(--line-2); }

.blog-cover {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: linear-gradient(135deg, var(--card-soft), var(--line));
}

.blog-cover img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform 0.7s var(--ease); }

.blog-card:hover .blog-cover img { transform: scale(1.05); }

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.2rem;
  font-weight: 800;
  letter-spacing: 2px;
  color: rgba(61, 110, 224, 0.35);
  background:
    radial-gradient(420px 200px at 22% 12%, rgba(96, 140, 255, 0.14), transparent 62%),
    radial-gradient(360px 200px at 88% 92%, rgba(150, 110, 240, 0.12), transparent 62%),
    linear-gradient(135deg, var(--card-soft), var(--line));
}

.blog-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--ink-2);
}

.blog-body { padding: 18px 19px 20px; }

.blog-title {
  margin: 0 0 9px;
  font-size: 1.02rem;
  font-weight: 700;
  line-height: 1.45;
  color: var(--ink);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.blog-summary {
  margin: 0 0 14px;
  font-size: 0.82rem;
  line-height: 1.7;
  color: var(--ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.blog-meta { display: flex; align-items: center; gap: 7px; font-size: 0.72rem; color: var(--ink-3); }
.dot-sep { color: var(--line-2); }

.blog-empty {
  padding: 60px 0;
  text-align: center;
  color: var(--ink-3);
  font-size: 0.86rem;
  border-radius: 14px;
  border: 1px dashed var(--line-2);
  background: var(--card);
}

/* ============================================================
   ④ 作品集入口
   ============================================================ */
.works-cta {
  position: relative;
  max-width: 1240px;
  margin: 78px auto 0;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(120deg, #131c30 0%, #0d1424 55%, #1a2440 100%);
  transition: transform 0.42s var(--ease), box-shadow 0.42s var(--ease);
}

.works-cta:hover { transform: translateY(-4px); box-shadow: var(--shadow-2); }

.works-glow {
  position: absolute;
  inset: -40% -10% auto -10%;
  height: 200%;
  background:
    radial-gradient(600px 320px at 78% 18%, rgba(96, 140, 255, 0.3), transparent 62%),
    radial-gradient(520px 300px at 12% 88%, rgba(160, 110, 240, 0.22), transparent 62%);
  pointer-events: none;
  transition: transform 0.8s var(--ease);
}

.works-cta:hover .works-glow { transform: translateX(-3%); }

.works-inner {
  position: relative;
  padding: 54px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.works-kicker {
  margin: 0 0 12px;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 4.5px;
  color: rgba(255, 255, 255, 0.5);
}

.works-title { margin: 0 0 12px; font-size: clamp(1.4rem, 3vw, 2rem); font-weight: 800; letter-spacing: 1px; }

.works-sub { margin: 0 0 26px; font-size: 0.86rem; color: rgba(255, 255, 255, 0.6); }

.works-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 26px;
  border-radius: 999px;
  background: #fff;
  color: #101827;
  font-size: 0.88rem;
  font-weight: 600;
  letter-spacing: 1px;
  transition: transform 0.28s var(--ease), box-shadow 0.28s;
}

.works-cta:hover .works-btn {
  transform: translateY(-2px);
  box-shadow: 0 20px 44px -18px rgba(255, 255, 255, 0.6);
}

/* 底部留白 */
.home-page::after {
  content: '';
  display: block;
  height: 84px;
}

/* ============================================================
   滚动进场
   ============================================================ */
.reveal {
  opacity: 0;
  transform: translateY(26px);
  transition: opacity 0.8s var(--ease), transform 0.8s var(--ease);
}

.reveal.in { opacity: 1; transform: translateY(0); }

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 1000px) {
  .profile-grid { grid-template-columns: 240px minmax(0, 1fr); }
  .ge-thumbs { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 760px) {
  .hero { height: 88vh; min-height: 520px; }
  .hero-controls { bottom: 62px; }
  .profile-section { padding: 42px 18px 0; }
  .profile-grid { grid-template-columns: 1fr; }
  .id-card {
    aspect-ratio: auto;
    max-width: 300px;
    margin: 0 auto;
  }
  .gallery-entry { padding: 26px 22px; }
  .ge-head { flex-direction: column; }
  .blog-section { padding: 56px 18px 0; }
  .blog-grid { grid-template-columns: 1fr; }
  .works-cta { margin: 56px 18px 0; }
  .works-inner { padding: 40px 22px; }
  .section-title { font-size: 1.4rem; }
}

@media (prefers-reduced-motion: reduce) {
  .hero-slide { transition: opacity 0.4s ease; transform: none; }
  .reveal { opacity: 1; transform: none; transition: none; }
  .hero-kicker, .hero-title, .hero-sub, .hero-actions { animation: none; }
  .scroll-hint i { animation: none; }
}
</style>

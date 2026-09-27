<template>
  <div class="login-page" ref="pageRef" :class="{ 'bg-in': bgIn, 'card-in': cardIn }">
    <!-- 星空背景：public/images/login-bg.jpg（缺失时回退到内置星空渐变） -->
    <div class="bg-layer" :class="{ 'has-image': bgOk }" :style="bgOk ? { backgroundImage: `url(${BG_IMAGE})` } : null"></div>
    <div class="bg-veil"></div>
    <canvas ref="canvasRef" class="particle-canvas"></canvas>

    <!-- ① 入场加载动画 -->
    <transition name="loader-fade">
      <div v-if="booting" class="boot-loader">
        <div class="boot-mark">
          <span class="boot-ring"></span>
          <span class="boot-letter">Y</span>
        </div>
        <div class="boot-bar"><i :style="{ width: bootProgress + '%' }"></i></div>
        <p class="boot-text">正在进入 · 拾枝者的自留地</p>
      </div>
    </transition>

    <!-- 右上角圆形按钮 -->
    <div class="page-tools">
      <button class="tool-btn" :title="isDarkMode ? '切换到亮色' : '切换到暗色'" @click="handleThemeToggle">
        <svg v-if="isDarkMode" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
        </svg>
        <svg v-else viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
        </svg>
      </button>
      <button class="tool-btn" title="以游客身份浏览" @click="enterAsGuest">
        <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="8" r="3.4" />
          <path d="M5 20c0-3.3 3.1-5.4 7-5.4s7 2.1 7 5.4" />
        </svg>
      </button>
    </div>

    <!-- ③ 登录卡片（从天而降后落定） -->
    <div class="shell-drop">
      <div class="shell-shadow"></div>
      <div class="login-shell">
      <!-- 左：插画区 -->
      <div class="shell-visual">
        <div class="visual-brand">yu翔<span>拾枝者的自留地</span></div>
        <svg class="visual-art" viewBox="0 0 420 300" fill="none" aria-hidden="true">
          <!-- 地面投影 -->
          <ellipse cx="175" cy="266" rx="150" ry="14" fill="#000" opacity="0.05" />
          <!-- 紫色高个 -->
          <rect x="150" y="66" width="70" height="196" rx="14" fill="#8b5cf6" />
          <circle cx="172" cy="104" r="5" fill="#111" />
          <circle cx="198" cy="104" r="5" fill="#111" />
          <!-- 黑色方块 -->
          <rect x="224" y="120" width="76" height="142" rx="16" fill="#1f2937" />
          <circle cx="248" cy="158" r="5" fill="#fff" />
          <circle cx="276" cy="158" r="5" fill="#fff" />
          <!-- 黄色圆角块 -->
          <rect x="300" y="140" width="96" height="122" rx="20" fill="#facc15" />
          <circle cx="330" cy="180" r="5" fill="#111" />
          <circle cx="364" cy="180" r="5" fill="#111" />
          <line x1="332" y1="206" x2="362" y2="206" stroke="#111" stroke-width="4" stroke-linecap="round" />
          <!-- 橙色圆顶 -->
          <path d="M40 262a78 78 0 0 1 156 0z" fill="#f97316" />
          <circle cx="100" cy="216" r="5" fill="#111" />
          <circle cx="136" cy="216" r="5" fill="#111" />
          <path d="M104 236q14 12 28 0" stroke="#111" stroke-width="4" stroke-linecap="round" />
          <!-- 光标 -->
          <path d="M300 232l22 44 7-17 17-6z" fill="#111" stroke="#fff" stroke-width="3" stroke-linejoin="round" />
        </svg>
      </div>

      <!-- 右：表单区 -->
      <div class="shell-form">
        <transition name="fade-swap" mode="out-in">
          <!-- ===================== 登录 ===================== -->
          <div class="form-pane" v-if="page === 'login'" key="login">
            <h1 class="form-title">欢迎来访！</h1>
            <p class="form-sub">选择你的身份并输入用户名</p>

            <!-- 身份选择 -->
            <div class="pick-row">
              <button
                v-for="opt in identityOptions"
                :key="opt.value"
                class="pick-item"
                :class="{ active: loginAs === opt.value }"
                @click="switchLoginAs(opt.value)"
              >
                <img :src="opt.url" :alt="opt.label" />
                <span>{{ opt.label }}</span>
              </button>
            </div>

            <label class="field-label">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="8" r="3.4" /><path d="M5 20c0-3.3 3.1-5.4 7-5.4s7 2.1 7 5.4" />
              </svg>
              用户名
            </label>
            <input
              ref="usernameRef"
              v-model="form.username"
              class="field-input"
              type="text"
              placeholder="请输入用户名"
              autocomplete="off"
              @keyup.enter="focusPassword"
            />

            <label class="field-label">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4" y="10" width="16" height="10" rx="3" /><path d="M8 10V7a4 4 0 0 1 8 0v3" />
              </svg>
              密码
            </label>
            <input
              ref="passwordRef"
              v-model="form.password"
              class="field-input"
              type="password"
              placeholder="请输入密码"
              autocomplete="off"
              @keyup.enter="handleLogin"
            />

            <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>

            <button class="cta-btn" :class="{ loading: submitting }" :disabled="submitting" @click="handleLogin">
              <span v-if="submitting" class="btn-spinner"></span>
              <template v-else>
                <span>同步身份并登录</span>
                <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M5 12h13M13 6l6 6-6 6" />
                </svg>
              </template>
            </button>

            <p class="form-foot">
              还没有账号？<button class="link-btn" @click="switchPage('register')">立即注册</button>
            </p>
          </div>

          <!-- ===================== 注册 ===================== -->
          <div class="form-pane" v-else key="register">
            <h1 class="form-title">创建账号</h1>
            <p class="form-sub">选个形象，起个名字，即刻留言</p>

            <!-- 形象选择 -->
            <div class="pick-row">
              <button
                v-for="a in PRESET_AVATARS"
                :key="a.key"
                class="pick-item"
                :class="{ active: reg.avatar === a.key }"
                @click="reg.avatar = a.key"
              >
                <img :src="a.url" alt="形象" />
              </button>
              <button class="pick-item pick-add" title="随机换一组" @click="shuffleAvatars">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                  <path d="M6 12h12M12 6v12" />
                </svg>
              </button>
            </div>

            <label class="field-label">用户名</label>
            <input
              v-model="reg.username"
              class="field-input"
              type="text"
              placeholder="3-20 位字母、数字或下划线"
              autocomplete="off"
              maxlength="20"
            />

            <div class="field-grid">
              <div>
                <label class="field-label">昵称（可选）</label>
                <input v-model="reg.nickname" class="field-input" type="text" placeholder="默认同用户名" autocomplete="off" maxlength="20" />
              </div>
              <div>
                <label class="field-label">邮箱（可选）</label>
                <input v-model="reg.email" class="field-input" type="email" placeholder="用于找回账号" autocomplete="off" />
              </div>
            </div>

            <div class="field-grid">
              <div>
                <label class="field-label">密码</label>
                <input v-model="reg.password" class="field-input" type="password" placeholder="至少 6 位" autocomplete="new-password" maxlength="64" />
              </div>
              <div>
                <label class="field-label">确认密码</label>
                <input v-model="reg.password2" class="field-input" type="password" placeholder="再输入一次" autocomplete="new-password" maxlength="64" @keyup.enter="handleRegister" />
              </div>
            </div>

            <label class="field-label">安全验证</label>
            <div class="captcha-row">
              <div class="captcha-question" @click="refreshCaptcha" title="点击换一题">
                <span>{{ captchaQuestion || '点击获取题目' }}</span>
              </div>
              <input
                v-model="reg.captchaAnswer"
                class="field-input"
                type="text"
                placeholder="填写答案"
                autocomplete="off"
                @keyup.enter="handleRegister"
              />
            </div>

            <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>

            <button class="cta-btn" :class="{ loading: submitting }" :disabled="submitting" @click="handleRegister">
              <span v-if="submitting" class="btn-spinner"></span>
              <template v-else>
                <span>创建并登录</span>
                <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M5 12h13M13 6l6 6-6 6" />
                </svg>
              </template>
            </button>

            <p class="form-foot">
              已有账号？<button class="link-btn" @click="switchPage('login')">返回登录</button>
            </p>
          </div>
        </transition>

        <p class="shell-foot">yu翔 · 个人品牌网站</p>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { PRESET_AVATARS } from '../utils/avatar'
import { adminLogin, userLogin, userRegister, guestLogin } from '../utils/auth'
import { getTheme, setTheme } from '../utils/theme'

const router = useRouter()
const page = ref('login')
const loginAs = ref('user')
const submitting = ref(false)
const errorMsg = ref('')
const usernameRef = ref(null)
const passwordRef = ref(null)

const form = reactive({ username: '', password: '' })
const reg = reactive({
  username: '', nickname: '', password: '', password2: '',
  email: '', captchaId: '', captchaAnswer: '',
  avatar: PRESET_AVATARS[0].key
})
const captchaQuestion = ref('')

// 登录身份选项（沿用既有「用户 / 管理员」双身份逻辑）
const identityOptions = computed(() => [
  { value: 'user', label: '用户', url: PRESET_AVATARS[4].url },
  { value: 'admin', label: '管理员', url: PRESET_AVATARS[3].url },
])

// ==================== 背景图与主题 ====================
// 替换背景：把图片放到 personal-website-frontend/public/images/login-bg.jpg
// 线上可直接覆盖 /opt/personal-website/web/images/login-bg.jpg，无需重新构建
const BG_IMAGE = '/images/login-bg.jpg'
const bgOk = ref(true)
const isDarkMode = ref(getTheme() === 'dark')

// ==================== 入场动效（加载 → 背景 → 卡片降临）====================
const booting = ref(true)      // ① 加载动画
const bootProgress = ref(0)    // 加载进度条
const bgIn = ref(false)        // ② 背景图浮现
const cardIn = ref(false)      // ③ 卡片从天而降

const REDUCED_MOTION = typeof window !== 'undefined'
  && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const wait = (ms) => new Promise((r) => setTimeout(r, ms))

let bgResolve
const bgLoaded = new Promise((r) => { bgResolve = r })

// 探测背景图是否存在，缺失则回退到内置星空；同时作为入场时序的「就绪」信号
const probeBg = () => {
  const img = new Image()
  const finish = (ok) => { bgOk.value = ok; bgResolve() }
  img.onload = () => finish(true)
  img.onerror = () => finish(false)
  img.src = BG_IMAGE
  setTimeout(() => bgResolve(), 1800) // 兜底：网络慢也不让入场卡住
}

const runIntro = async () => {
  if (REDUCED_MOTION) {
    booting.value = false; bootProgress.value = 100
    bgIn.value = true; cardIn.value = true
    return
  }
  // 同一会话内再次进入：跳过加载层，只保留背景浮现 + 卡片降临
  const played = sessionStorage.getItem('loginIntroPlayed') === '1'
  try { sessionStorage.setItem('loginIntroPlayed', '1') } catch { /* 隐私模式忽略 */ }
  if (played) {
    booting.value = false
    bootProgress.value = 100
    bgIn.value = true
    await wait(140)
    cardIn.value = true
    return
  }
  // 安全兜底：无论加载/入场时序出什么问题，4 秒后必须呈现可交互的最终状态
  setTimeout(() => {
    if (!cardIn.value) {
      booting.value = false
      bootProgress.value = 100
      bgIn.value = true
      cardIn.value = true
    }
  }, 4000)

  // 进度条爬升（约 760ms），与背景图加载并行
  const tick = setInterval(() => {
    bootProgress.value = Math.min(96, bootProgress.value + 6 + Math.random() * 12)
  }, 60)

  await Promise.all([bgLoaded, wait(640)])
  clearInterval(tick)
  bootProgress.value = 100

  await wait(160)
  booting.value = false       // ① 加载层淡出
  await wait(100)
  bgIn.value = true           // ② 背景图由模糊放大到清晰
  await wait(430)
  cardIn.value = true         // ③ 卡片从天而降 + 落地回弹
}

const handleThemeToggle = () => {
  const next = isDarkMode.value ? 'light' : 'dark'
  setTheme(next)
  isDarkMode.value = next === 'dark'
}

// ==================== Canvas 粒子系统 ====================
const pageRef = ref(null)
const canvasRef = ref(null)

const PARTICLE_COUNT = 110
const CONNECT_DIST = 140
const MOUSE_RADIUS = 220
const MOUSE_ATTRACT = 0.07

let canvas, ctx, particles, mouse, animId
let w, h

class Particle {
  constructor() {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.ox = this.x
    this.oy = this.y
    this.vx = 0
    this.vy = 0
    this.r = 1.0 + Math.random() * 1.6
    this.baseOpacity = 0.1 + Math.random() * 0.2
    this.opacity = this.baseOpacity
  }

  update() {
    const dx = mouse.x - this.x
    const dy = mouse.y - this.y
    const dist = Math.sqrt(dx * dx + dy * dy)

    if (dist < MOUSE_RADIUS) {
      const force = (MOUSE_RADIUS - dist) / MOUSE_RADIUS
      const fx = (dx / dist) * force * MOUSE_ATTRACT
      const fy = (dy / dist) * force * MOUSE_ATTRACT
      this.vx += fx
      this.vy += fy
      this.opacity = Math.min(this.baseOpacity + force * 0.4, 0.6)
    } else {
      this.opacity += (this.baseOpacity - this.opacity) * 0.04
    }

    this.vx += (this.ox - this.x) * 0.003
    this.vy += (this.oy - this.y) * 0.003
    this.vx *= 0.94
    this.vy *= 0.94

    this.x += this.vx
    this.y += this.vy
  }

  draw() {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(200,215,255,${this.opacity})`
    ctx.fill()
  }
}

function initParticles() {
  particles = Array.from({ length: PARTICLE_COUNT }, () => new Particle())
}

function drawLines() {
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const a = particles[i]
      const b = particles[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < CONNECT_DIST) {
        const avgOpacity = (a.opacity + b.opacity) / 2
        const alpha = (1 - dist / CONNECT_DIST) * 0.04 + avgOpacity * 0.08
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.strokeStyle = `rgba(180,200,255,${alpha})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }
}

function animate() {
  ctx.clearRect(0, 0, w, h)
  for (const p of particles) p.update()
  drawLines()
  for (const p of particles) p.draw()
  animId = requestAnimationFrame(animate)
}

function resize() {
  const el = pageRef.value
  if (!el) return
  w = el.offsetWidth
  h = el.offsetHeight
  canvas.width = w
  canvas.height = h
}

function onMouseMove(e) {
  mouse.x = e.clientX
  mouse.y = e.clientY
}

function onMouseLeave() {
  mouse.x = -9999
  mouse.y = -9999
}

onMounted(() => {
  probeBg()
  runIntro()
  canvas = canvasRef.value
  ctx = canvas.getContext('2d')
  mouse = { x: -9999, y: -9999 }
  resize()
  initParticles()
  animate()
  window.addEventListener('resize', resize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseleave', onMouseLeave)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseleave', onMouseLeave)
})

// ==================== 业务逻辑 ====================
const shuffleAvatars = () => {
  const pool = PRESET_AVATARS.filter((a) => a.key !== reg.avatar)
  reg.avatar = pool[Math.floor(Math.random() * pool.length)].key
}

const switchPage = (target) => {
  page.value = target
  errorMsg.value = ''
  if (target === 'login') nextTick(() => { usernameRef.value?.focus() })
  else { reg.captchaAnswer = ''; refreshCaptcha() }
}

const switchLoginAs = (target) => {
  loginAs.value = target
  errorMsg.value = ''
  nextTick(() => { usernameRef.value?.focus() })
}

const focusPassword = () => { passwordRef.value?.focus() }

const handleLogin = async () => {
  if (!form.username.trim() || !form.password.trim()) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  errorMsg.value = ''
  submitting.value = true
  try {
    const fn = loginAs.value === 'admin' ? adminLogin : userLogin
    const result = await fn(form.username.trim(), form.password)
    if (result.success) {
      router.push(loginAs.value === 'admin' ? '/admin' : '/')
    } else {
      errorMsg.value = result.message
    }
  } finally { submitting.value = false }
}

const refreshCaptcha = async () => {
  reg.captchaAnswer = ''
  try {
    const res = await request.get('/webuser/captcha')
    if (res.data.code === 200) {
      reg.captchaId = res.data.data.captchaId
      captchaQuestion.value = res.data.data.question
    } else {
      captchaQuestion.value = ''
      errorMsg.value = res.data.message || '获取验证码失败'
    }
  } catch {
    captchaQuestion.value = ''
    errorMsg.value = '获取验证码失败，请重试'
  }
}

const handleRegister = async () => {
  errorMsg.value = ''
  const u = reg.username.trim()
  if (!u || u.length < 3 || u.length > 20) { errorMsg.value = '用户名需 3-20 位'; return }
  if (!/^[a-zA-Z0-9_]+$/.test(u)) { errorMsg.value = '用户名仅限字母、数字和下划线'; return }
  if (!reg.password || reg.password.length < 6) { errorMsg.value = '密码至少 6 位'; return }
  if (reg.password !== reg.password2) { errorMsg.value = '两次密码输入不一致'; return }
  if (!reg.captchaId) { errorMsg.value = '请先点击验证码题目获取'; return }
  if (!reg.captchaAnswer.trim()) { errorMsg.value = '请输入验证码答案'; return }

  submitting.value = true
  try {
    const result = await userRegister({
      username: u,
      password: reg.password,
      nickname: reg.nickname.trim(),
      email: reg.email.trim(),
      avatar: reg.avatar,
      captchaId: reg.captchaId,
      captchaAnswer: reg.captchaAnswer.trim()
    })
    if (result.success) {
      router.push('/')
    } else {
      errorMsg.value = result.message
      refreshCaptcha() // 无论验证码对错，都让题目刷新
    }
  } finally { submitting.value = false }
}

const enterAsGuest = () => { guestLogin(); router.push('/') }

// 外部页面（如留言板）跳转时预选 Tab
const presetTab = sessionStorage.getItem('loginTab')
if (presetTab === 'register' || presetTab === 'login') {
  page.value = presetTab
  sessionStorage.removeItem('loginTab')
  if (presetTab === 'register') refreshCaptcha()
}
</script>

<style scoped>
/* ============================================================
   登录页 · 星空分栏卡片
   左：插画区（浅灰）  右：表单区（白）
   背景图：public/images/login-bg.jpg，缺失时用内置星空
   ============================================================ */

.login-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  overflow: hidden;
  background: #070b16;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Helvetica Neue",
    "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ---------- 背景层 ---------- */
.bg-layer {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transform: scale(1.04);
  transition: opacity 0.6s ease;
  /* 无图时的内置星空 */
  background-image:
    radial-gradient(1200px 600px at 18% 12%, rgba(84, 60, 160, 0.5) 0%, transparent 60%),
    radial-gradient(900px 500px at 82% 78%, rgba(24, 92, 150, 0.45) 0%, transparent 62%),
    radial-gradient(700px 420px at 62% 22%, rgba(180, 70, 160, 0.22) 0%, transparent 60%),
    radial-gradient(1.5px 1.5px at 12% 24%, rgba(255, 255, 255, 0.9), transparent 100%),
    radial-gradient(1.2px 1.2px at 32% 68%, rgba(255, 255, 255, 0.75), transparent 100%),
    radial-gradient(1.6px 1.6px at 58% 16%, rgba(255, 255, 255, 0.85), transparent 100%),
    radial-gradient(1.2px 1.2px at 74% 52%, rgba(255, 255, 255, 0.7), transparent 100%),
    radial-gradient(1.4px 1.4px at 88% 30%, rgba(255, 255, 255, 0.8), transparent 100%),
    radial-gradient(1.2px 1.2px at 44% 88%, rgba(255, 255, 255, 0.7), transparent 100%),
    radial-gradient(1.5px 1.5px at 22% 46%, rgba(255, 255, 255, 0.6), transparent 100%),
    radial-gradient(1.3px 1.3px at 68% 76%, rgba(255, 255, 255, 0.65), transparent 100%),
    linear-gradient(160deg, #0a1024 0%, #0d1730 45%, #0a1020 100%);
}
/* 有背景图时淡出内置星空，避免叠加发脏 */
.bg-layer.has-image {
  background-image: none;
}

.bg-veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(4, 8, 18, 0.28) 0%, rgba(4, 8, 18, 0.42) 100%);
  pointer-events: none;
}

/* ---------- ① 入场加载层 ---------- */
.boot-loader {
  position: absolute;
  inset: 0;
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 22px;
  background: radial-gradient(600px 400px at 50% 42%, #101a33 0%, #05070f 70%);
}

.boot-mark {
  position: relative;
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.boot-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: #7ba3f5;
  border-right-color: rgba(123, 163, 245, 0.5);
  animation: boot-spin 0.9s linear infinite;
}

@keyframes boot-spin { to { transform: rotate(360deg); } }

.boot-letter {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.92);
  animation: boot-pulse 1.4s ease-in-out infinite;
}

@keyframes boot-pulse {
  0%, 100% { opacity: 0.75; transform: scale(0.96); }
  50% { opacity: 1; transform: scale(1.04); }
}

.boot-bar {
  width: 168px;
  height: 2px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.boot-bar i {
  display: block;
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, #4f7ff0, #8fb4ff);
  transition: width 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.boot-text {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 3px;
  color: rgba(255, 255, 255, 0.34);
}

.loader-fade-leave-active { transition: opacity 0.5s ease; }
.loader-fade-leave-to { opacity: 0; }

/* ---------- ② 背景图浮现（初始隐藏） ---------- */
.bg-layer {
  opacity: 0;
  transform: scale(1.16);
  filter: blur(16px) saturate(0.7);
}

.bg-veil,
.particle-canvas,
.page-tools {
  opacity: 0;
}

.login-page.bg-in .bg-layer {
  opacity: 1;
  transform: scale(1.04);
  filter: blur(0) saturate(1);
  transition: opacity 1.1s ease, transform 1.6s cubic-bezier(0.22, 1, 0.36, 1), filter 1.1s ease;
}

.login-page.bg-in .bg-veil {
  opacity: 1;
  transition: opacity 1.1s ease 0.1s;
}

.login-page.bg-in .particle-canvas,
.login-page.bg-in .page-tools {
  opacity: 1;
  transition: opacity 0.9s ease 0.35s;
}

/* ---------- ③ 卡片从天而降 ---------- */
.shell-drop {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(1000px, 100%);
}

.shell-shadow {
  position: absolute;
  left: 10%;
  right: 10%;
  bottom: -22px;
  height: 44px;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(0, 0, 0, 0.6), transparent 70%);
  opacity: 0;
  filter: blur(9px);
}

.login-page.card-in .shell-shadow {
  animation: shadow-land 1.05s ease-out both;
}

@keyframes shadow-land {
  0% { opacity: 0; transform: scale(0.72); }
  55% { opacity: 0.85; transform: scale(1.05); }
  100% { opacity: 0.5; transform: scale(1); }
}

.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

/* ---------- 右上角按钮 ---------- */
.page-tools {
  position: fixed;
  top: 22px;
  right: 24px;
  z-index: 5;
  display: flex;
  gap: 10px;
}

.tool-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: background 0.2s, transform 0.2s, color 0.2s;
}

.tool-btn:hover {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  transform: translateY(-1px);
}

/* ============================================================
   分栏卡片
   ============================================================ */
.login-shell {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  width: 100%;
  min-height: 588px;
  border-radius: 26px;
  overflow: hidden;
  background: #fff;
  box-shadow:
    0 40px 90px -30px rgba(0, 0, 0, 0.6),
    0 2px 8px rgba(0, 0, 0, 0.16);
  /* 入场前藏在视口上方，等 card-in 触发后落下 */
  opacity: 0;
  transform: translateY(-115vh);
}

.login-page.card-in .login-shell {
  animation: card-drop 1.15s cubic-bezier(0.32, 0.72, 0.28, 1) both;
}

/* 从天而降 + 落地两次回弹 */
@keyframes card-drop {
  0%   { opacity: 0; transform: translateY(-115vh) scale(0.94); }
  12%  { opacity: 1; }
  58%  { opacity: 1; transform: translateY(0) scale(1); }
  70%  { transform: translateY(-16px) scale(1.004); }
  82%  { transform: translateY(0) scale(1); }
  90%  { transform: translateY(-6px); }
  95%  { transform: translateY(0); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

/* ---------- 左：插画 ---------- */
.shell-visual {
  position: relative;
  background: linear-gradient(170deg, #f6f6f8 0%, #eef0f4 100%);
  overflow: hidden;
}

.visual-brand {
  position: absolute;
  top: 34px;
  left: 38px;
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: 1px;
  color: #1a2233;
}

.visual-brand span {
  display: block;
  margin-top: 6px;
  font-size: 0.74rem;
  font-weight: 400;
  letter-spacing: 2px;
  color: #98a1b3;
}

.visual-art {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  width: 88%;
  max-width: 420px;
  animation: art-in 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.1s both;
}

@keyframes art-in {
  from { opacity: 0; transform: translate(-50%, 26px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}

/* ---------- 右：表单 ---------- */
.shell-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 44px 52px 30px;
  position: relative;
}

.form-pane {
  width: 100%;
  max-width: 380px;
  margin: 0 auto;
}

.form-title {
  margin: 0 0 9px;
  font-size: 1.86rem;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #12192b;
}

.form-sub {
  margin: 0 0 26px;
  font-size: 0.83rem;
  color: #98a1b3;
  letter-spacing: 0.3px;
}

/* 形象 / 身份选择行 */
.pick-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.pick-row:has(.pick-item span) { gap: 16px; }

.pick-item {
  position: relative;
  width: 54px;
  height: 54px;
  padding: 0;
  border-radius: 50%;
  border: 1px solid #e6e9f0;
  background: #fff;
  cursor: pointer;
  /* 注意：不能用 overflow:hidden，否则下方文字标签会被裁掉 */
  overflow: visible;
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.22s, border-color 0.22s;
}

.pick-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 50%;
}

.pick-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px -8px rgba(16, 24, 40, 0.3);
}

.pick-item.active {
  border-color: #12192b;
  box-shadow: 0 0 0 2px #12192b, 0 8px 18px -8px rgba(16, 24, 40, 0.35);
}

/* 登录身份项带文字 */
.pick-row .pick-item span {
  position: absolute;
  bottom: -21px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.72rem;
  color: #98a1b3;
  white-space: nowrap;
}

.pick-row:has(.pick-item span) {
  margin-bottom: 34px;
}

.pick-item.active span { color: #12192b; font-weight: 600; }

.pick-add {
  border-style: dashed;
  color: #b6bccb;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfd;
}
.pick-add:hover { color: #12192b; border-color: #cfd5e2; }

/* 字段 */
.field-label {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0 0 7px;
  font-size: 0.74rem;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: #6b7488;
}

.field-input {
  width: 100%;
  height: 48px;
  margin-bottom: 17px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: #f4f5f8;
  color: #12192b;
  font-size: 0.92rem;
  font-family: inherit;
  outline: none;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.field-input::placeholder { color: #b6bccb; }

.field-input:focus {
  background: #fff;
  border-color: #12192b;
  box-shadow: 0 0 0 3px rgba(18, 25, 43, 0.08);
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field-grid .field-input { margin-bottom: 16px; }

/* 验证码 */
.captcha-row {
  display: flex;
  gap: 10px;
  margin-bottom: 4px;
}

.captcha-question {
  flex-shrink: 0;
  min-width: 116px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-radius: 11px;
  background: #f4f5f8;
  border: 1px dashed #d6dbe6;
  color: #12192b;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s, border-color 0.2s;
}

.captcha-question:hover { background: #eef1f7; border-color: #c3cad9; }

.captcha-row .field-input { margin-bottom: 0; }

/* 错误提示 */
.error-text {
  margin: 12px 0 0;
  font-size: 0.78rem;
  color: #d93b3b;
  text-align: center;
}

/* CTA */
.cta-btn {
  width: 100%;
  height: 50px;
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: none;
  border-radius: 999px;
  background: #12192b;
  color: #fff;
  font-size: 0.92rem;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 1px;
  cursor: pointer;
  transition: background 0.22s, transform 0.22s, box-shadow 0.22s;
}

.cta-btn svg { transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1); }

.cta-btn:hover {
  background: #1d2740;
  box-shadow: 0 14px 30px -14px rgba(18, 25, 43, 0.7);
  transform: translateY(-1px);
}

.cta-btn:hover svg { transform: translateX(4px); }

.cta-btn:active { transform: translateY(0); }

.cta-btn:disabled { cursor: not-allowed; opacity: 0.6; }

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* 底部链接 */
.form-foot {
  margin: 18px 0 0;
  text-align: center;
  font-size: 0.8rem;
  color: #98a1b3;
}

.link-btn {
  padding: 0;
  border: none;
  background: none;
  color: #12192b;
  font-family: inherit;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
  text-decoration-color: #ccd2de;
  transition: text-decoration-color 0.2s;
}

.link-btn:hover { text-decoration-color: #12192b; }

.shell-foot {
  position: absolute;
  bottom: 18px;
  left: 0;
  right: 0;
  margin: 0;
  text-align: center;
  font-size: 0.68rem;
  letter-spacing: 1.6px;
  color: #c3cad9;
}

/* 面板切换动画 */
.fade-swap-enter-active, .fade-swap-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.fade-swap-enter-from { opacity: 0; transform: translateY(8px); }
.fade-swap-leave-to { opacity: 0; transform: translateY(-8px); }

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .shell-drop { width: min(460px, 100%); }
  .login-shell {
    grid-template-columns: 1fr;
    width: 100%;
    min-height: 0;
  }
  .shell-visual { display: none; }
  .shell-form { padding: 40px 32px 44px; }
  .shell-foot { position: static; margin-top: 22px; }
}

@media (max-width: 420px) {
  .login-page { padding: 24px 14px; }
  .shell-form { padding: 32px 22px 36px; }
  .field-grid { grid-template-columns: 1fr; gap: 0; }
}

/* 关闭动效偏好：直接呈现最终状态 */
@media (prefers-reduced-motion: reduce) {
  .login-shell, .visual-art, .shell-shadow { animation: none !important; }
  .login-shell { opacity: 1; transform: none; }
  .shell-shadow { opacity: 0.5; }
  .bg-layer { opacity: 1; transform: scale(1.04); filter: none; }
  .bg-veil, .page-tools { opacity: 1; }
  .particle-canvas { display: none; }
}
</style>

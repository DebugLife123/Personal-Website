<template>
  <div class="login-page" ref="pageRef">
    <canvas ref="canvasRef" class="particle-canvas"></canvas>

    <div class="login-card">
      <!-- 品牌 -->
      <div class="card-brand">
        <span class="brand-icon">Y</span>
      </div>
      <h1 class="card-title">{{ pageTitle }}</h1>
      <p class="card-sub">yu翔 的个人网站</p>

      <!-- 顶端切换：登录 / 注册 -->
      <div class="mode-tabs">
        <div class="mode-tab" :class="{ active: page === 'login' }" @click="switchPage('login')">登录</div>
        <div class="mode-tab" :class="{ active: page === 'register' }" @click="switchPage('register')">注册</div>
        <span class="tab-ink" :style="{ transform: page === 'register' ? 'translateX(100%)' : 'translateX(0)' }"></span>
      </div>

      <!-- ===================== 登录页 ===================== -->
      <div class="form-area" v-if="page === 'login'">
        <!-- 身份选择：用户 / 管理员 -->
        <div class="identity-row">
          <div class="identity-item" :class="{ active: loginAs === 'user' }" @click="switchLoginAs('user')">
            <div class="identity-avatar guest-avatar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14c-4.418 0-8 1.79-8 4v1h16v-1c0-2.21-3.582-4-8-4z" />
              </svg>
            </div>
            <span class="identity-label">用户</span>
          </div>
          <div class="identity-item" :class="{ active: loginAs === 'admin' }" @click="switchLoginAs('admin')">
            <div class="identity-avatar">
              <img src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" />
            </div>
            <span class="identity-label">管理员</span>
          </div>
        </div>

        <input
          ref="usernameRef"
          v-model="form.username"
          class="field-input"
          type="text"
          placeholder="用户名"
          autocomplete="off"
          @keyup.enter="focusPassword"
        />
        <input
          ref="passwordRef"
          v-model="form.password"
          class="field-input"
          type="password"
          placeholder="密码"
          autocomplete="off"
          @keyup.enter="handleLogin"
        />
        <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
        <button class="submit-btn" :class="{ loading: submitting }" :disabled="submitting" @click="handleLogin">
          <span v-if="submitting" class="btn-spinner"></span>
          <span v-else>登录</span>
        </button>
      </div>

      <!-- ===================== 注册页 ===================== -->
      <div class="form-area" v-else>
        <input
          v-model="reg.username"
          class="field-input"
          type="text"
          placeholder="用户名（3-20 位字母、数字或下划线）"
          autocomplete="off"
          maxlength="20"
        />
        <input
          v-model="reg.nickname"
          class="field-input"
          type="text"
          placeholder="昵称（可选，默认与用户名相同）"
          autocomplete="off"
          maxlength="20"
        />
        <input
          v-model="reg.password"
          class="field-input"
          type="password"
          placeholder="密码（至少 6 位）"
          autocomplete="new-password"
          maxlength="64"
        />
        <input
          v-model="reg.password2"
          class="field-input"
          type="password"
          placeholder="确认密码"
          autocomplete="new-password"
          maxlength="64"
          @keyup.enter="handleRegister"
        />
        <input
          v-model="reg.email"
          class="field-input"
          type="email"
          placeholder="邮箱（可选）"
          autocomplete="off"
        />
        <!-- 算术验证码 -->
        <div class="captcha-row">
          <div class="captcha-question" @click="refreshCaptcha" title="点击换一题">
            <span>{{ captchaQuestion || '点击获取题目' }}</span>
          </div>
          <input
            v-model="reg.captchaAnswer"
            class="field-input captcha-input"
            type="text"
            placeholder="答案"
            autocomplete="off"
            @keyup.enter="handleRegister"
          />
        </div>
        <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
        <button class="submit-btn" :class="{ loading: submitting }" :disabled="submitting" @click="handleRegister">
          <span v-if="submitting" class="btn-spinner"></span>
          <span v-else>注册并登录</span>
        </button>
        <p class="reg-hint">注册即视为同意文明留言；本站数据仅用于个人站点展示</p>
      </div>

      <!-- 游客进入：始终在底部 -->
      <div class="guest-section" v-if="page === 'login'">
        <p class="guest-hint">以游客身份浏览，仅可查看内容</p>
        <button class="guest-btn" @click="enterAsGuest">游客登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { adminLogin, userLogin, userRegister, guestLogin } from '../utils/auth'

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
  email: '', captchaId: '', captchaAnswer: ''
})
const captchaQuestion = ref('')

const pageTitle = computed(() => page.value === 'login' ? '欢迎回来' : '创建账号')

// ==================== Canvas 粒子系统 ====================
const pageRef = ref(null)
const canvasRef = ref(null)

const PARTICLE_COUNT = 130
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
    this.baseOpacity = 0.12 + Math.random() * 0.22
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
    ctx.fillStyle = `rgba(180,170,210,${this.opacity})`
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
        const alpha = (1 - dist / CONNECT_DIST) * 0.05 + avgOpacity * 0.1
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.strokeStyle = `rgba(160,150,200,${alpha})`
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
   极简高级感登录页 — 深空渐变 + 粒子 Canvas + 磨砂玻璃卡片
   ============================================================ */

/* ---------- 页面基底 ---------- */
.login-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(38, 46, 78, 0.55) 0%, transparent 60%),
    radial-gradient(ellipse at 50% 100%, rgba(20, 26, 40, 0.6) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 40%, rgba(34, 44, 72, 0.28) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 60%, rgba(30, 38, 62, 0.22) 0%, transparent 50%),
    linear-gradient(180deg, #0e1424 0%, #101828 32%, #10182b 60%, #0c1220 100%);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Helvetica Neue",
    "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ---------- Canvas ---------- */
.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* ============================================================
   磨砂玻璃卡片
   ============================================================ */
.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 52px 48px 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(22, 28, 46, 0.45);
  backdrop-filter: blur(32px);
  -webkit-backdrop-filter: blur(32px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 28px;
  box-shadow:
    0 2px 40px rgba(0, 0, 0, 0.3),
    0 0 0 0.5px rgba(255, 255, 255, 0.04) inset;
}

/* ---------- 品牌 ---------- */
.card-brand { margin-bottom: 24px; }
.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.55);
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  user-select: none;
}

/* ---------- 顶端 Tab：登录 / 注册 ---------- */
.mode-tabs {
  position: relative;
  display: flex;
  gap: 0;
  margin-bottom: 26px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 4px;
  width: 100%;
}
.mode-tab {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  border-radius: 9px;
  position: relative;
  z-index: 1;
  transition: color 0.25s;
  user-select: none;
}
.mode-tab.active { color: #fff; }
.tab-ink {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: rgba(255, 255, 255, 0.12);
  border-radius: 9px;
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

/* ---------- 验证码行 ---------- */
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: stretch;
  margin-bottom: 14px;
}
.captcha-question {
  flex-shrink: 0;
  min-width: 118px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.98rem;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s, border-color 0.2s;
}
.captcha-question:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.18);
}
.captcha-input { flex: 1; margin-bottom: 0 !important; }

/* ---------- 标题 ---------- */
.card-title {
  margin: 0 0 6px;
  font-size: 1.3rem;
  font-weight: 500;
  letter-spacing: 3px;
  color: rgba(255, 255, 255, 0.82);
}
.card-sub {
  margin: 0 0 40px;
  font-size: 0.85rem;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.28);
}

/* ---------- 身份切换 ---------- */
.identity-row {
  display: flex;
  gap: 44px;
  margin-bottom: 36px;
}
.identity-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  opacity: 0.28;
  transition: opacity 0.3s;
}
.identity-item.active { opacity: 1; }
.identity-item:hover { opacity: 0.6; }
.identity-item.active:hover { opacity: 1; }

.identity-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  transition: box-shadow 0.3s;
}
.identity-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.identity-item.active .identity-avatar {
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.guest-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.3);
}
.guest-avatar svg {
  width: 22px;
  height: 22px;
}
.identity-item.active .guest-avatar {
  color: rgba(255, 255, 255, 0.5);
}

.identity-label {
  font-size: 0.76rem;
  letter-spacing: 1.5px;
  color: rgba(255, 255, 255, 0.5);
}

/* ---------- 表单（含淡入淡出过渡） ---------- */
.form-area {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 10px;
}

.form-fade-enter-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.form-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.form-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.form-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.field-input {
  width: 100%;
  height: 46px;
  margin-bottom: 12px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 0.88rem;
  letter-spacing: 0.5px;
  outline: none;
  transition: border-color 0.3s, background 0.3s;
  box-sizing: border-box;
}
.field-input::placeholder {
  color: rgba(255, 255, 255, 0.2);
}
.field-input:focus {
  border-color: rgba(160, 150, 210, 0.35);
  background: rgba(255, 255, 255, 0.06);
}

.error-text {
  width: 100%;
  margin: 0 0 10px;
  font-size: 0.8rem;
  color: rgba(240, 110, 110, 0.65);
  text-align: center;
}

/* ---------- 登录按钮 ---------- */
.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: none;
  border-radius: 10px;
  background: rgba(120, 135, 175, 0.22);
  color: rgba(255, 255, 255, 0.65);
  font-size: 0.9rem;
  font-weight: 500;
  letter-spacing: 4px;
  cursor: pointer;
  transition: background 0.3s, color 0.3s;
}
.submit-btn:hover {
  background: rgba(130, 145, 185, 0.3);
  color: rgba(255, 255, 255, 0.82);
}
.submit-btn:active {
  background: rgba(110, 125, 165, 0.18);
}
.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.submit-btn.loading { pointer-events: none; }

.reg-hint {
  margin: 14px 0 0;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.28);
  letter-spacing: 0.3px;
  text-align: center;
}

.btn-spinner {
  display: inline-block;
  width: 15px;
  height: 15px;
  border: 2px solid rgba(255,255,255,0.15);
  border-top-color: rgba(255,255,255,0.4);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ---------- 游客登录（底部） ---------- */
.guest-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 6px;
  padding-top: 30px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.guest-hint {
  margin: 0 0 18px;
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.2);
  letter-spacing: 0.5px;
}
.guest-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.9rem;
  font-weight: 500;
  letter-spacing: 4px;
  cursor: pointer;
  transition: background 0.3s, color 0.3s;
}
.guest-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
}
.guest-btn:active {
  background: rgba(255, 255, 255, 0.03);
}
</style>

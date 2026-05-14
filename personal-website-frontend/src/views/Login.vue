<template>
  <div class="login-page" ref="pageRef">
    <canvas ref="canvasRef" class="particle-canvas"></canvas>

    <div class="login-card">
      <!-- 品牌 -->
      <div class="card-brand">
        <span class="brand-icon">Y</span>
      </div>
      <h1 class="card-title">欢迎回来</h1>
      <p class="card-sub">yu翔 的个人网站</p>

      <!-- 身份选择：管理员 / 用户 -->
      <div class="identity-row">
        <div
          class="identity-item"
          :class="{ active: mode === 'admin' }"
          @click="switchMode('admin')"
        >
          <div class="identity-avatar">
            <img src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" />
          </div>
          <span class="identity-label">管理员</span>
        </div>
        <div
          class="identity-item"
          :class="{ active: mode === 'user' }"
          @click="switchMode('user')"
        >
          <div class="identity-avatar guest-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14c-4.418 0-8 1.79-8 4v1h16v-1c0-2.21-3.582-4-8-4z" />
            </svg>
          </div>
          <span class="identity-label">用户</span>
        </div>
      </div>

      <!-- 登录表单：管理员 / 用户 选中时显示 -->
      <transition name="form-fade">
        <div class="form-area" v-if="mode === 'admin' || mode === 'user'">
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
          <button
            class="submit-btn"
            :class="{ loading: submitting }"
            :disabled="submitting"
            @click="handleLogin"
          >
            <span v-if="submitting" class="btn-spinner"></span>
            <span v-else>登录</span>
          </button>
        </div>
      </transition>

      <!-- 游客登录：始终在底部 -->
      <div class="guest-section">
        <p class="guest-hint">以游客身份浏览，仅可查看内容</p>
        <button class="guest-btn" @click="enterAsGuest">游客登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { login, guestLogin } from '../utils/auth'

const router = useRouter()
const mode = ref('guest')
const submitting = ref(false)
const errorMsg = ref('')
const usernameRef = ref(null)
const passwordRef = ref(null)

const form = reactive({ username: '', password: '' })

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
const switchMode = (target) => {
  mode.value = target
  errorMsg.value = ''
  form.username = ''
  form.password = ''
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
    const result = await login(form.username, form.password)
    if (result.success) { router.push('/') }
    else { errorMsg.value = result.message }
  } finally { submitting.value = false }
}

const enterAsGuest = () => { guestLogin(); router.push('/') }
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
    radial-gradient(ellipse at 50% 0%, rgba(60, 55, 90, 0.5) 0%, transparent 60%),
    radial-gradient(ellipse at 50% 100%, rgba(40, 45, 70, 0.35) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 40%, rgba(50, 45, 80, 0.25) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 60%, rgba(45, 50, 75, 0.2) 0%, transparent 50%),
    linear-gradient(180deg, #12101c 0%, #161522 30%, #141320 60%, #11101a 100%);
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
  background: rgba(30, 28, 50, 0.45);
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

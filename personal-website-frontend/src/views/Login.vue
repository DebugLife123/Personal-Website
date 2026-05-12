<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
    </div>

    <!-- 粒子光斑 -->
    <div class="particle-layer">
      <div
        v-for="p in particles"
        :key="p.id"
        class="particle"
        :style="{
          width: p.size + 'px',
          height: p.size + 'px',
          left: p.x + '%',
          top: p.y + '%',
          '--drift-x': p.dx + 'px',
          '--drift-y': p.dy + 'px',
          '--duration': p.duration + 's',
          '--delay': p.delay + 's',
          opacity: p.opacity,
          background: p.color,
        }"
      ></div>
    </div>

    <!-- 噪点纹理 -->
    <div class="noise-overlay"></div>

    <div class="login-container">
      <div class="login-card">
        <!-- 头部 -->
        <div class="login-header">
          <div class="logo-area">
            <span class="logo-letter">Y</span>
          </div>
          <h1 class="login-title">欢迎回来</h1>
          <p class="login-subtitle">yu翔 的个人网站</p>
        </div>

        <!-- 登录表单 -->
        <div class="login-body" v-if="!isLoggingIn">
          <!-- 头像预览 -->
          <div class="avatar-row">
            <div
              class="avatar-preview"
              :class="{ active: mode === 'admin', clicking: clickingAdmin }"
              @click="switchToAdmin"
            >
              <el-avatar :size="48" src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" />
              <span class="avatar-label">管理员</span>
            </div>
            <div
              class="avatar-preview"
              :class="{ active: mode === 'guest', clicking: clickingGuest }"
              @click="switchToGuest"
            >
              <el-avatar :size="48" icon="UserFilled" style="background: #8a8eaa" />
              <span class="avatar-label">访客</span>
            </div>
          </div>

          <el-form @submit.prevent="handleLogin">
            <!-- 管理员登录表单 -->
            <template v-if="mode === 'admin'">
              <div class="input-group">
                <el-input
                  v-model="form.username"
                  placeholder="用户名"
                  size="large"
                  class="login-input"
                  :prefix-icon="User"
                  @keyup.enter="handleLogin"
                />
              </div>
              <div class="input-group">
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="密码"
                  size="large"
                  class="login-input"
                  :prefix-icon="Lock"
                  show-password
                  @keyup.enter="handleLogin"
                />
              </div>
              <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="submitting"
                @click="handleLogin"
              >登 录</el-button>
            </template>

            <!-- 游客模式 -->
            <template v-if="mode === 'guest'">
              <div class="guest-info">
                <el-icon :size="20"><View /></el-icon>
                <span>以游客身份浏览，仅可查看内容</span>
              </div>
              <el-button
                type="primary"
                size="large"
                class="login-btn guest-btn"
                @click="enterAsGuest"
              >游客登录</el-button>
            </template>
          </el-form>
        </div>

        <!-- 底部切换 -->
        <div class="login-footer">
          <span v-if="mode === 'admin'" class="switch-link" @click="mode = 'guest'">
            以游客身份进入
            <el-icon><ArrowRight /></el-icon>
          </span>
          <span v-else class="switch-link" @click="mode = 'admin'">
            管理员登录
            <el-icon><ArrowRight /></el-icon>
          </span>
        </div>
      </div>

      <!-- 底部版权 -->
      <p class="copyright">© 2025-2026 yu翔. All rights reserved.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, View, ArrowRight } from '@element-plus/icons-vue'
import { login, guestLogin, checkAuth } from '../utils/auth'

const router = useRouter()
const mode = ref('guest')
const submitting = ref(false)
const errorMsg = ref('')
const isLoggingIn = ref(false)
const clickingAdmin = ref(false)
const clickingGuest = ref(false)

// 生成粒子光斑数据
const particles = ref([])
const generateParticles = () => {
  const colors = [
    'radial-gradient(circle, rgba(158,146,176,0.4), transparent)',
    'radial-gradient(circle, rgba(108,95,160,0.3), transparent)',
    'radial-gradient(circle, rgba(200,196,204,0.25), transparent)',
    'radial-gradient(circle, rgba(130,120,160,0.35), transparent)',
  ]
  const list = []
  for (let i = 0; i < 30; i++) {
    list.push({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: 2 + Math.random() * 5,
      dx: (Math.random() - 0.5) * 60,
      dy: (Math.random() - 0.5) * 60,
      duration: 8 + Math.random() * 14,
      delay: Math.random() * 10,
      opacity: 0.06 + Math.random() * 0.1,
      color: colors[i % colors.length],
    })
  }
  particles.value = list
}

onMounted(() => {
  generateParticles()
})

const form = reactive({
  username: '',
  password: ''
})

const switchToAdmin = () => {
  mode.value = 'admin'
  // 点击反馈动画
  clickingAdmin.value = true
  setTimeout(() => { clickingAdmin.value = false }, 400)
  // 自动聚焦用户名输入框
  setTimeout(() => {
    const input = document.querySelector('.login-input .el-input__inner')
    if (input) input.focus()
  }, 100)
}

const handleLogin = async () => {
  if (!form.username.trim() || !form.password.trim()) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  errorMsg.value = ''
  submitting.value = true
  try {
    const result = await login(form.username, form.password)
    if (result.success) {
      router.push('/')
    } else {
      errorMsg.value = result.message
    }
  } finally {
    submitting.value = false
  }
}

const switchToGuest = () => {
  mode.value = 'guest'
  clickingGuest.value = true
  setTimeout(() => { clickingGuest.value = false }, 400)
}

const enterAsGuest = () => {
  guestLogin()
  router.push('/')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    /* 网格线 */
    linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.025) 1px, transparent 1px),
    /* 主渐变 */
    radial-gradient(ellipse at 20% 30%, rgba(60, 40, 120, 0.25) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 70%, rgba(30, 80, 160, 0.2) 0%, transparent 50%),
    linear-gradient(135deg, #0a0a12 0%, #0f0d1a 40%, #0e1628 70%, #0a0a12 100%);
  background-size:
    48px 48px,
    48px 48px,
    100% 100%,
    100% 100%,
    100% 100%;
  position: relative;
  overflow: hidden;
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* ---- 神经网络光斑（霓虹光斑） ---- */
.bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.shape {
  position: absolute;
  border-radius: 50%;
}
.shape-1 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(130, 80, 255, 0.2), transparent 70%);
  filter: blur(50px);
  top: -80px; left: -60px;
  animation: neonFloat1 10s ease-in-out infinite;
}
.shape-2 {
  width: 250px; height: 250px;
  background: radial-gradient(circle, rgba(0, 180, 255, 0.18), transparent 70%);
  filter: blur(45px);
  bottom: -60px; right: -40px;
  animation: neonFloat2 8s ease-in-out infinite;
}
.shape-3 {
  width: 180px; height: 180px;
  background: radial-gradient(circle, rgba(200, 100, 255, 0.15), transparent 70%);
  filter: blur(40px);
  top: 55%; left: 10%;
  animation: neonFloat3 12s ease-in-out infinite;
}
.shape-4 {
  width: 200px; height: 200px;
  background: radial-gradient(circle, rgba(0, 200, 255, 0.12), transparent 70%);
  filter: blur(50px);
  bottom: 20%; right: 15%;
  animation: neonFloat4 9s ease-in-out infinite;
}
@keyframes neonFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, 30px) scale(1.05); }
  66% { transform: translate(-20px, 50px) scale(0.95); }
}
@keyframes neonFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-30px, -40px) scale(1.08); }
  66% { transform: translate(20px, -20px) scale(0.95); }
}
@keyframes neonFloat3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(60px, -30px) scale(1.1); }
}
@keyframes neonFloat4 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-40px, 40px) scale(1.06); }
}

/* ---- 粒子光斑 ---- */
.particle-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.particle {
  position: absolute;
  border-radius: 50%;
  will-change: transform;
  animation: particleDrift var(--duration) ease-in-out infinite;
  animation-delay: var(--delay);
  backface-visibility: hidden;
}
@keyframes particleDrift {
  0%, 100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(var(--drift-x), calc(var(--drift-y) * 0.5));
  }
  50% {
    transform: translate(calc(var(--drift-x) * 0.5), var(--drift-y));
  }
  75% {
    transform: translate(var(--drift-x), calc(var(--drift-y) * -0.3));
  }
}

/* ---- 噪点纹理 ---- */
.noise-overlay {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.1;
}
.noise-overlay::after {
  content: '';
  position: absolute;
  inset: -50%;
  width: 200%;
  height: 200%;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-repeat: repeat;
  background-size: 256px 256px;
  animation: noiseShift 0.5s steps(4) infinite;
}
@keyframes noiseShift {
  0% { transform: translate(0, 0); }
  25% { transform: translate(-5%, -5%); }
  50% { transform: translate(-10%, 0); }
  75% { transform: translate(-5%, 5%); }
}

/* ---- 登录容器 ---- */
.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-card {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 48px 36px 32px;
  box-shadow: 0 8px 60px rgba(0, 0, 0, 0.4);
}

/* ---- 头部 ---- */
.login-header {
  text-align: center;
  margin-bottom: 36px;
}
.logo-area {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #9e92b0, #6c5fa0);
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(108, 95, 160, 0.3);
}
.logo-letter {
  font-size: 2rem;
  font-weight: 800;
  color: #fff;
  letter-spacing: 1px;
}
.login-title {
  color: #fff;
  font-size: 1.6rem;
  font-weight: 700;
  margin: 0 0 6px;
}
.login-subtitle {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.9rem;
  margin: 0;
  letter-spacing: 1px;
}

/* ---- 模式切换头像行 ---- */
.avatar-row {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 28px;
}
.avatar-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  opacity: 0.45;
  transition: opacity 0.3s, transform 0.3s;
}

.avatar-preview.clicking {
  animation: avatarBounce 0.4s ease;
}

@keyframes avatarBounce {
  0% { transform: scale(1); }
  30% { transform: scale(1.12); }
  60% { transform: scale(0.95); }
  100% { transform: scale(1); }
}
.avatar-preview.active {
  opacity: 1;
}
.avatar-preview :deep(.el-avatar) {
  border: 2px solid transparent;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.avatar-preview.active :deep(.el-avatar) {
  border-color: #9e92b0;
  box-shadow: 0 0 0 3px rgba(158, 146, 176, 0.25);
}
.avatar-label {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}
.avatar-preview.active .avatar-label {
  color: #c8c4cc;
}

/* ---- 表单 ---- */
.input-group {
  margin-bottom: 16px;
}
.login-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: none !important;
  padding: 4px 16px;
  transition: border-color 0.3s, background 0.3s;
}
.login-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(158, 146, 176, 0.4);
}
.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: #9e92b0;
  background: rgba(255, 255, 255, 0.08);
}
.login-input :deep(.el-input__inner) {
  color: #ece8e4;
  font-size: 0.95rem;
}
.login-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}
.login-input :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.3);
  margin-right: 8px;
}

.error-msg {
  color: #f56c6c;
  font-size: 0.82rem;
  margin-bottom: 14px;
  text-align: center;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #9e92b0, #7a6ea0);
  border: none;
  transition: transform 0.2s, box-shadow 0.3s;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(108, 95, 160, 0.35);
}
.login-btn:active {
  transform: scale(0.98);
}
.guest-btn {
  background: linear-gradient(135deg, #6c7a9a, #5a6a8a);
}
.guest-btn:hover {
  box-shadow: 0 6px 24px rgba(90, 106, 138, 0.35);
}

/* ---- 游客信息 ---- */
.guest-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.85rem;
  margin-bottom: 20px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 10px;
}

/* ---- 底部 ---- */
.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
.switch-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.85rem;
  cursor: pointer;
  transition: color 0.2s;
}
.switch-link:hover {
  color: #c8c4cc;
}

.copyright {
  text-align: center;
  color: rgba(255, 255, 255, 0.2);
  font-size: 0.75rem;
  margin-top: 24px;
}
</style>

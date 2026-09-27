<template>
  <div class="admin-login-page">
    <div class="bg-layer"></div>
    <div class="bg-veil"></div>

    <div class="admin-card">
      <div class="card-head">
        <span class="brand-mark">Y</span>
        <div>
          <h1 class="card-title">管理后台</h1>
          <p class="card-sub">仅站长本人使用</p>
        </div>
      </div>

      <label class="field-label">管理员账号</label>
      <input
        ref="usernameRef"
        v-model="form.username"
        class="field-input"
        type="text"
        placeholder="账号"
        autocomplete="off"
        @keyup.enter="focusPassword"
      />

      <label class="field-label">密码</label>
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

      <button class="cta-btn" :class="{ loading: submitting }" :disabled="submitting" @click="handleLogin">
        <span v-if="submitting" class="btn-spinner"></span>
        <span v-else>登录后台</span>
      </button>

      <p class="card-foot">
        <button class="link-btn" @click="router.push('/login')">← 返回访客入口</button>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin } from '../../utils/auth'

const router = useRouter()
const submitting = ref(false)
const errorMsg = ref('')
const usernameRef = ref(null)
const passwordRef = ref(null)
const form = reactive({ username: '', password: '' })

const focusPassword = () => { passwordRef.value?.focus() }

const handleLogin = async () => {
  if (!form.username.trim() || !form.password) {
    errorMsg.value = '请输入账号和密码'
    return
  }
  errorMsg.value = ''
  submitting.value = true
  try {
    const result = await adminLogin(form.username.trim(), form.password)
    if (result.success) router.push('/admin')
    else errorMsg.value = result.message
  } finally { submitting.value = false }
}

onMounted(() => nextTick(() => usernameRef.value?.focus()))
</script>

<style scoped>
/* 管理员入口：与访客入口（星空分栏卡片）刻意区分——更小、更克制、更安静 */
.admin-login-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  overflow: hidden;
  background: #070b16;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Helvetica Neue",
    "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
}

.bg-layer {
  position: absolute;
  inset: 0;
  background-image: url('/images/login-bg.jpg');
  background-size: cover;
  background-position: center;
  filter: blur(2px) saturate(0.85);
  transform: scale(1.05);
}

.bg-veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(4, 8, 18, 0.62) 0%, rgba(4, 8, 18, 0.78) 100%);
}

.admin-card {
  position: relative;
  z-index: 1;
  width: min(392px, 100%);
  padding: 34px 34px 26px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 30px 70px -28px rgba(0, 0, 0, 0.7);
  animation: card-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(14px) scale(0.99); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #12192b;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.02rem;
  font-weight: 800;
  flex-shrink: 0;
}

.card-title {
  margin: 0 0 3px;
  font-size: 1.12rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #12192b;
}

.card-sub {
  margin: 0;
  font-size: 0.76rem;
  color: #98a1b3;
  letter-spacing: 0.6px;
}

.field-label {
  display: block;
  margin: 0 0 7px;
  font-size: 0.74rem;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: #6b7488;
}

.field-input {
  width: 100%;
  height: 44px;
  margin-bottom: 16px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: #f4f5f8;
  color: #12192b;
  font-size: 0.9rem;
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

.error-text {
  margin: 0 0 4px;
  font-size: 0.78rem;
  color: #d93b3b;
  text-align: center;
}

.cta-btn {
  width: 100%;
  height: 46px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 10px;
  background: #12192b;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 1px;
  cursor: pointer;
  transition: background 0.22s, transform 0.22s;
}

.cta-btn:hover { background: #1d2740; transform: translateY(-1px); }
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

.card-foot {
  margin: 18px 0 0;
  text-align: center;
}

.link-btn {
  padding: 0;
  border: none;
  background: none;
  color: #98a1b3;
  font-family: inherit;
  font-size: 0.78rem;
  cursor: pointer;
  transition: color 0.2s;
}

.link-btn:hover { color: #12192b; }
</style>

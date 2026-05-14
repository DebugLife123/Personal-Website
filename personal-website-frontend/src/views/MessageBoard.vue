<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTodayStatistic, getTotalStatistic } from '../utils/statistic'
import {
  Edit, DataLine,
  Star, StarFilled, Delete, ChatDotSquare,
  ChatLineSquare
} from '@element-plus/icons-vue'
import { isAdmin } from '../utils/auth'

const messages = ref([])


// 留言表单
const form = ref({
  content: '',
  isMarkdown: true
})

// 回复表单
const replyForm = ref({
  content: '',
  parentId: null,
  replyTo: ''
})
const showReplyForm = ref(false)
const currentReplyMsg = ref(null)

// 加载状态
const loading = ref(false)
const submitting = ref(false)
const replySubmitting = ref(false)
const pageLoaded = ref(false)

const hoveredMsg = ref(null)

// 记录已点赞的留言 ID（本地状态，刷新重置）
const likedIds = ref(new Set())
const isLiked = (id) => likedIds.value.has(id)

// 区分自己的留言
const isOwnMessage = (msg) => msg.adminPost

const fetchMessages = async () => {
  loading.value = true
  try {
    const res = await request.get('/message/list')
    if (res.data.code === 200) messages.value = res.data.data
  } catch (e) {
    console.error(e)
    ElMessage.error('获取留言失败')
  } finally {
    loading.value = false
  }
}

const submitMessage = async () => {
  if (!form.value.content.trim()) {
    return ElMessage.warning('请输入留言内容')
  }
  submitting.value = true
  try {
    const res = await request.post('/message/add', { ...form.value, isAdmin: isAdmin.value })
    if (res.data.code === 200) {
      ElMessage.success('留言成功！')
      form.value.content = ''
      await fetchMessages()
    }
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const toggleLike = async (msg) => {
  const already = isLiked(msg.id)
  try {
    const res = await request.post(`/message/${already ? 'unlike' : 'like'}/${msg.id}`)
    if (res.data.code === 200) {
      if (already) {
        likedIds.value.delete(msg.id)
        likedIds.value = new Set(likedIds.value)
        msg.likes = Math.max(0, (msg.likes || 0) - 1)
      } else {
        likedIds.value.add(msg.id)
        likedIds.value = new Set(likedIds.value)
        msg.likes = (msg.likes || 0) + 1
      }
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const togglePin = async (msg) => {
  try {
    const res = await request.post(`/message/pin/${msg.id}`)
    if (res.data.code === 200) {
      msg.isPinned = !msg.isPinned
      await fetchMessages()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteMessage = async (msg) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${msg.nickname}」的留言吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await request.delete(`/message/delete/${msg.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除')
      await fetchMessages()
    }
  } catch (e) {}
}

  const openReplyForm = (msg) => {
    currentReplyMsg.value = msg
    replyForm.value = {
      content: '',
      parentId: msg.id,
      replyTo: msg.nickname
    }
    showReplyForm.value = true
  }

const submitReply = async () => {
  if (!replyForm.value.content.trim()) {
    return ElMessage.warning('请输入回复内容')
  }
  replySubmitting.value = true
  try {
    const res = await request.post('/message/add', { ...replyForm.value, isAdmin: isAdmin.value })
    if (res.data.code === 200) {
      ElMessage.success('回复成功！')
      showReplyForm.value = false
      currentReplyMsg.value = null
      await fetchMessages()
    }
  } catch (e) {
    ElMessage.error('回复失败')
  } finally {
    replySubmitting.value = false
  }
}

const cancelReply = () => {
  showReplyForm.value = false
  currentReplyMsg.value = null
  replyForm.value = { content: '', parentId: null, replyTo: '' }
}

// 站点统计
const todayStats = ref({ pageViews: 0, uniqueVisitors: 0, articleReads: 0 })
const totalStats = ref({ pageViews: 0, uniqueVisitors: 0, articleReads: 0 })
const fetchSiteStats = async () => {
  const today = await getTodayStatistic()
  const total = await getTotalStatistic()
  if (today) todayStats.value = today
  if (total) totalStats.value = total
}

onMounted(async () => {
  await Promise.all([fetchMessages(), fetchSiteStats()])
  setTimeout(() => { pageLoaded.value = true }, 100)
})
</script>

<template>
  <div class="message-page" :class="{ 'page-loaded': pageLoaded }">
    <!-- 低饱和紫灰渐变横幅 -->
    <div class="header-banner">
      <div class="banner-overlay"></div>
      <div class="banner-content">
        <div class="banner-icon">💬</div>
        <h1 class="banner-title">留言板</h1>
        <p class="banner-subtitle">每一句话都值得被听见</p>
      </div>
    </div>

    <div class="main-body">
      <el-row :gutter="32" justify="center">
        <el-col :xs="24" :sm="24" :md="16" :lg="15">

          <!-- 写留言卡片 -->
          <transition name="fade-up">
            <div class="post-card">
              <div class="post-card-header">
                <el-icon :size="18"><Edit /></el-icon>
                <span>说点什么</span>
              </div>
              <div class="post-card-body">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="4"
                  placeholder="写下你想说的..."
                  class="soft-input"
                  maxlength="1000"
                  show-word-limit
                />
                <div class="post-footer">
                  <div class="post-options">
                    <el-checkbox v-model="form.isMarkdown">Markdown</el-checkbox>
                  </div>
                  <el-button
                    type="primary"
                    :loading="submitting"
                    @click="submitMessage"
                    class="post-btn"
                  >
                    发布留言
                  </el-button>
                </div>
              </div>
            </div>
          </transition>

          <!-- 留言统计 -->
          <div class="message-bar">
            <span class="message-count">
              <el-icon><ChatDotSquare /></el-icon>
              共 <strong>{{ messages.length }}</strong> 条留言
            </span>
            <span class="bar-line"></span>
          </div>

          <!-- 加载骨架 -->
          <div v-if="loading" class="skeleton-list">
            <div v-for="i in 3" :key="i" class="skeleton-card">
              <div class="skeleton-avatar"></div>
              <div class="skeleton-lines">
                <div class="skeleton-line w-30"></div>
                <div class="skeleton-line w-60"></div>
                <div class="skeleton-line w-80"></div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <p>还没有留言，快来写下第一条吧</p>
          </div>

          <!-- 留言列表 -->
          <transition-group name="msg-list" tag="div" class="message-list">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-card"
              :class="{
                'is-pinned': msg.isPinned,
                'is-own': isOwnMessage(msg),
                'is-hovered': hoveredMsg === msg.id
              }"
              @mouseenter="hoveredMsg = msg.id"
              @mouseleave="hoveredMsg = null"
            >
              <!-- 置顶标签 -->
              <div v-if="msg.isPinned" class="pinned-badge">
                <el-icon :size="12"><StarFilled /></el-icon> 置顶
              </div>

              <div class="msg-header">
                <el-avatar :size="42" :src="msg.avatar" class="msg-avatar" :class="{ 'own-avatar': isOwnMessage(msg) }">
                  {{ msg.nickname?.charAt(0)?.toUpperCase() || '?' }}
                </el-avatar>
                <div class="msg-author">
                  <span class="msg-nickname">{{ msg.nickname }} <span v-if="isOwnMessage(msg)" class="self-tag">管理员</span></span>
                  <span class="msg-time">{{ msg.createTime }}</span>
                </div>
              </div>

              <div class="msg-content">{{ msg.content }}</div>

              <!-- 操作按钮：hover 显示 -->
              <div class="msg-actions" :class="{ 'actions-visible': hoveredMsg === msg.id }">
                <button class="action-btn like-btn" :class="{ liked: isLiked(msg.id) }" @click="toggleLike(msg)">
                  <span>👍</span>
                  <span>{{ msg.likes || 0 }}</span>
                </button>
                <button class="action-btn reply-btn" @click="openReplyForm(msg)">
                  <el-icon :size="14"><ChatLineSquare /></el-icon>
                  <span>回复</span>
                </button>
                <button v-if="isAdmin"
                  class="action-btn pin-btn"
                  :class="{ pinned: msg.isPinned }"
                  @click="togglePin(msg)"
                  :title="msg.isPinned ? '取消置顶' : '置顶'"
                >
                  <el-icon :size="14"><Star /></el-icon>
                </button>
                <button v-if="isAdmin" class="action-btn delete-btn" @click="deleteMessage(msg)">
                  <el-icon :size="14"><Delete /></el-icon>
                </button>
              </div>

              <!-- 回复列表 -->
              <div v-if="msg.replies && msg.replies.length > 0" class="replies-section">
                <div class="replies-divider">
                  <span class="divider-line"></span>
                  <span class="divider-text">回复 {{ msg.replies.length }}</span>
                  <span class="divider-line"></span>
                </div>
                <div class="replies-list">
                  <div v-for="reply in msg.replies" :key="reply.id" class="reply-item" :class="{ 'reply-own': isOwnMessage(reply) }">
                    <div class="reply-header">
                      <el-avatar :size="26" :src="reply.avatar" class="reply-avatar" :class="{ 'reply-avatar-own': isOwnMessage(reply) }">
                        {{ reply.nickname?.charAt(0)?.toUpperCase() || '?' }}
                      </el-avatar>
                      <span class="reply-nickname">{{ reply.nickname }}</span>
                      <span class="reply-target" v-if="reply.replyTo">→ {{ reply.replyTo }}</span>
                      <span class="reply-time">{{ reply.createTime }}</span>
                    </div>
                    <div class="reply-content">{{ reply.content }}</div>
                    <div class="reply-actions-bar">
                      <button class="action-btn like-btn" :class="{ liked: isLiked(reply.id) }" @click="toggleLike(reply)">
                        <span>👍</span>
                        <span>{{ reply.likes || 0 }}</span>
                      </button>
                      <button v-if="isAdmin" class="action-btn delete-btn" @click="deleteMessage(reply)">
                        <el-icon :size="13"><Delete /></el-icon>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 回复表单 -->
              <transition name="slide-down">
                <div v-if="showReplyForm && currentReplyMsg && currentReplyMsg.id === msg.id" class="reply-form">
                  <div class="reply-form-header">回复 {{ replyForm.replyTo }}</div>
                  <el-input
                    v-model="replyForm.content"
                    type="textarea"
                    :rows="3"
                    placeholder="写下你的回复..."
                    class="soft-input reply-form-input"
                    maxlength="500"
                    show-word-limit
                  />
                  <div class="reply-form-actions">
                    <el-button size="small" @click="cancelReply">取消</el-button>
                    <el-button size="small" type="primary" :loading="replySubmitting" class="reply-submit-btn" @click="submitReply">
                      提交回复
                    </el-button>
                  </div>
                </div>
              </transition>
            </div>
          </transition-group>
        </el-col>

        <!-- 右侧栏 -->
        <el-col :xs="0" :sm="0" :md="7" :lg="6" class="sidebar">
          <div class="sidebar-inner">
            <!-- 个人信息 -->
            <div class="side-card">
              <div class="side-card-body profile-body">
                <el-avatar :size="68" src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" class="profile-avatar" />
                <h3 class="profile-name">yu翔</h3>
                <p class="profile-desc">初出茅庐 · 科班码农 · 拾枝者</p>
                <div class="profile-divider"></div>
                <div class="profile-stats">
                  <div class="profile-stat">
                    <span class="profile-stat-num">{{ messages.length }}</span>
                    <span class="profile-stat-label">留言</span>
                  </div>
                  <div class="profile-stat">
                    <span class="profile-stat-num">12</span>
                    <span class="profile-stat-label">文章</span>
                  </div>
                  <div class="profile-stat">
                    <span class="profile-stat-num">3</span>
                    <span class="profile-stat-label">分类</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 站点统计 -->
            <div class="side-card">
              <div class="side-card-header">
                <el-icon :size="16"><DataLine /></el-icon>
                <span>站点统计</span>
              </div>
              <div class="side-card-body">
                <div class="stat-item">
                  <span class="stat-key">今日浏览</span>
                  <span class="stat-val">{{ todayStats.pageViews }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-key">今日访客</span>
                  <span class="stat-val">{{ todayStats.uniqueVisitors }}</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <span class="stat-key">总访问量</span>
                  <span class="stat-val highlight">{{ totalStats.pageViews }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
/* ===== 色彩系统 — 低饱和紫灰 ===== */
:root {
  --primary: #9e92b0;
  --primary-dark: #8a7f9e;
  --primary-light: #f0eef5;
  --primary-soft: #e8e5f0;
  --accent: #b8aec8;
  --gray-bg: #f5f6f8;
  --gray-card: #ffffff;
  --text-primary: #3d3d4a;
  --text-secondary: #7a7a8a;
  --text-muted: #9a9aaa;
  --shadow-sm: 0 2px 12px rgba(0,0,0,0.04);
  --shadow-md: 0 4px 20px rgba(0,0,0,0.06);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.08);
  --radius: 14px;
  --radius-lg: 18px;
}

/* ===== Page ===== */
.message-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f4f5f8 0%, #eae8ee 100%);
  opacity: 0;
  transition: opacity 0.6s ease;
}
.message-page.page-loaded {
  opacity: 1;
}

/* ===== Header Banner — 低饱和紫灰 ===== */
.header-banner {
  position: relative;
  height: 340px;
  padding-top: 65px;
  background: linear-gradient(135deg, #9e92b0 0%, #8a8eaa 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
.banner-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(255,255,255,0.12) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 50%),
    radial-gradient(circle at 50% 80%, rgba(255,255,255,0.06) 0%, transparent 50%);
  pointer-events: none;
}
.banner-content {
  position: relative;
  text-align: center;
  color: white;
  animation: bannerIn 0.8s ease-out;
}
@keyframes bannerIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
.banner-icon {
  font-size: 3rem;
  margin-bottom: 8px;
  animation: float 3s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.banner-title {
  font-size: 2.6rem;
  font-weight: 700;
  letter-spacing: 5px;
  margin-bottom: 6px;
  text-shadow: 0 2px 16px rgba(0,0,0,0.15);
}
.banner-subtitle {
  font-size: 1rem;
  opacity: 0.8;
  letter-spacing: 3px;
  font-weight: 300;
}

/* ===== Main Body ===== */
.main-body {
  max-width: 1320px;
  margin: -50px auto 0;
  padding: 0 24px 70px;
  position: relative;
  z-index: 2;
}

/* ===== Post Card ===== */
.post-card {
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  margin-bottom: 36px;
  transition: box-shadow 0.3s;
}
.post-card:hover {
  box-shadow: var(--shadow-lg);
}
.post-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  background: linear-gradient(135deg, #9e92b0 0%, #8a8eaa 100%);
  color: white;
  font-weight: 600;
  font-size: 0.95rem;
  letter-spacing: 1px;
}
.post-card-body {
  padding: 22px 24px 24px;
}

/* Soft Inputs — 无边框/细线风格 */
.soft-input :deep(.el-textarea__inner),
.soft-input :deep(.el-input__wrapper) {
  border: 1px solid #e8e8ee;
  border-radius: 10px;
  background: #fafafc;
  box-shadow: none !important;
  transition: border-color 0.25s, box-shadow 0.25s;
}
.soft-input :deep(.el-textarea__inner:hover),
.soft-input :deep(.el-input__wrapper:hover) {
  border-color: #c8c4d4;
}
.soft-input :deep(.el-textarea__inner:focus),
.soft-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(158,146,176,0.1) !important;
  background: #fff;
}
.soft-input :deep(.el-textarea__inner) {
  padding: 12px 14px;
  font-size: 0.95rem;
  line-height: 1.6;
  resize: vertical;
}
.soft-input :deep(.el-textarea__inner:focus) {
  background: #fff;
}
.post-textarea :deep(.el-textarea__inner) {
  min-height: 100px;
}

.post-inputs {
  display: flex;
  gap: 14px;
  margin-top: 16px;
}
.post-input { flex: 1; }
.post-input :deep(.el-input__wrapper) {
  padding-left: 8px;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 18px;
}
.post-options {
  display: flex;
  gap: 16px;
}
.post-btn {
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  border: none;
  border-radius: 10px;
  padding: 10px 30px;
  font-weight: 600;
  letter-spacing: 1px;
  transition: transform 0.2s, box-shadow 0.25s;
}
.post-btn:hover {
  transform: scale(1.04);
  box-shadow: 0 6px 20px rgba(158,146,176,0.35);
}
.post-btn:active {
  transform: scale(0.97);
}

/* ===== Message Bar ===== */
.message-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}
.message-count {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 0.88rem;
  white-space: nowrap;
}
.message-count strong {
  color: var(--primary);
  font-size: 1.05rem;
}
.bar-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #d8d4e0, transparent);
}

/* ===== Skeletons ===== */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.skeleton-card {
  display: flex;
  gap: 16px;
  padding: 22px;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}
.skeleton-avatar {
  width: 42px; height: 42px;
  border-radius: 50%;
  background: linear-gradient(90deg, #eee 25%, #f5f5f8 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}
.skeleton-lines { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.skeleton-line {
  height: 14px; border-radius: 7px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f8 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.w-30 { width: 30%; }
.w-60 { width: 60%; }
.w-80 { width: 80%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== Empty State ===== */
.empty-state {
  text-align: center;
  padding: 70px 20px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.empty-icon { font-size: 3.5rem; margin-bottom: 14px; animation: float 3s ease-in-out infinite; }
.empty-state p { color: var(--text-muted); font-size: 0.95rem; }

/* ===== Message List ===== */
.message-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== Message Card — 聊天卡片样式 ===== */
.message-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 22px 24px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  position: relative;
}
.message-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 自己的留言（管理员）— 浅紫背景 */
.message-card.is-own {
  background: linear-gradient(135deg, #f8f6fc 0%, #f4f2f8 100%);
  border: 1px solid #e8e4f0;
}
.message-card.is-own:hover {
  background: linear-gradient(135deg, #f4f0fa 0%, #efeaf6 100%);
}

/* 访客留言 — 浅灰 */
.message-card:not(.is-own) {
  background: #fff;
}

/* 置顶留言 */
.message-card.is-pinned {
  border: 1.5px solid var(--primary);
  background: linear-gradient(135deg, #f8f6fc 0%, #fff 100%);
}
.message-card.is-pinned.is-own {
  border-color: var(--primary-dark);
}

.pinned-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  color: white;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 12px;
  border-radius: 20px;
  margin-bottom: 14px;
}

/* ===== Message Header ===== */
.msg-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.msg-avatar {
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  color: white;
  font-weight: 700;
  font-size: 1rem;
  flex-shrink: 0;
}
.own-avatar {
  background: linear-gradient(135deg, #8a7f9e, #9e92b0);
}
.msg-author {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.msg-nickname {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.92rem;
  display: flex;
  align-items: center;
  gap: 6px;
}
.self-tag {
  font-size: 0.65rem;
  background: var(--primary-soft);
  color: var(--primary-dark);
  padding: 1px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.msg-time {
  font-size: 0.76rem;
  color: var(--text-muted);
}

/* ===== Message Content ===== */
.msg-content {
  color: var(--text-primary);
  line-height: 1.7;
  font-size: 0.93rem;
  margin-bottom: 14px;
  word-break: break-word;
}

/* ===== Message Actions — 默认隐藏，hover 显示 ===== */
.msg-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  opacity: 0;
  transition: opacity 0.2s;
}
.actions-visible,
.message-card:hover .msg-actions {
  opacity: 1;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 11px;
  border: none;
  border-radius: 18px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 0.78rem;
  transition: all 0.2s;
}
.action-btn:hover {
  background: var(--gray-bg);
}
.like-btn:hover, .like-btn.liked {
  background: #fde8e8;
  color: #d9534f;
}
.like-btn.liked {
  font-weight: 600;
}
.reply-btn:hover {
  background: #edeaf2;
  color: var(--primary-dark);
}
.pin-btn:hover, .pin-btn.pinned {
  background: #f5f0e0;
  color: #b8953a;
}
.delete-btn:hover {
  background: #fde8e8;
  color: #d9534f;
}

/* ===== Replies ===== */
.replies-section {
  margin-top: 16px;
}
.replies-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.divider-line { flex: 1; height: 1px; background: #e8e8ee; }
.divider-text { font-size: 0.76rem; color: var(--text-muted); white-space: nowrap; }
.replies-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.reply-item {
  padding: 12px 14px;
  background: var(--gray-bg);
  border-radius: 12px;
  border-left: 3px solid #ccc;
  transition: background 0.2s;
}
.reply-item:hover { background: #eef0f4; }
.reply-own {
  border-left-color: var(--primary);
  background: #f4f0fa;
}
.reply-own:hover {
  background: #efeaf6;
}
.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.reply-avatar {
  background: #bbb;
  color: white;
  font-weight: 600;
  font-size: 0.75rem;
  flex-shrink: 0;
}
.reply-avatar-own {
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
}
.reply-nickname {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.82rem;
}
.reply-target {
  color: var(--primary);
  font-size: 0.76rem;
  background: var(--primary-light);
  padding: 1px 8px;
  border-radius: 10px;
}
.reply-time {
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-left: auto;
}
.reply-content {
  color: var(--text-secondary);
  font-size: 0.88rem;
  line-height: 1.6;
}
.reply-actions-bar {
  margin-top: 6px;
  display: flex;
  gap: 4px;
}

/* ===== Reply Form ===== */
.reply-form {
  margin-top: 14px;
  padding: 16px 18px;
  background: var(--gray-bg);
  border-radius: var(--radius);
  border: 1px solid #e4e2ea;
}
.reply-form-header {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--primary-dark);
  margin-bottom: 12px;
}
.reply-form-input { margin-bottom: 10px; }
.reply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}
.reply-submit-btn {
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  border: none;
}
.reply-submit-btn:hover {
  background: linear-gradient(135deg, #8e82a0, #7a7e9a);
}

/* ===== Sidebar ===== */
.sidebar-inner {
  position: sticky;
  top: 90px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* Side Card — 统一风格 */
.side-card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: box-shadow 0.3s;
}
.side-card:hover {
  box-shadow: var(--shadow-md);
}
.side-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.88rem;
  border-bottom: 1px solid #f0eef4;
}
.side-card-body {
  padding: 20px;
}

/* Profile */
.profile-body { text-align: center; }
.profile-avatar {
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  color: white;
  font-weight: 700;
  font-size: 1.4rem;
  border: 3px solid #fff;
  box-shadow: 0 4px 14px rgba(0,0,0,0.06);
}
.profile-name {
  margin: 14px 0 2px;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-primary);
}
.profile-desc {
  font-size: 0.82rem;
  color: var(--text-muted);
  margin: 0;
}
.profile-divider {
  width: 36px;
  height: 3px;
  background: linear-gradient(90deg, #9e92b0, #8a8eaa);
  border-radius: 3px;
  margin: 14px auto;
}
.profile-stats {
  display: flex;
  justify-content: space-around;
}
.profile-stat {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.profile-stat-num {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--primary);
  transition: color 0.2s, transform 0.2s;
  cursor: default;
}
.profile-stat-num:hover {
  color: var(--primary-dark);
  transform: scale(1.1);
}
.profile-stat-label {
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* Stats rows */
.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.stat-item:last-of-type {
  margin-bottom: 0;
}
.stat-key {
  font-size: 0.84rem;
  color: var(--text-muted);
}
.stat-val {
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--primary);
  transition: color 0.2s, transform 0.2s;
  cursor: default;
}
.stat-val:hover {
  color: var(--primary-dark);
  transform: scale(1.08);
}
.stat-val.highlight {
  font-size: 1rem;
  color: var(--primary-dark);
}
.stat-divider {
  height: 1px;
  background: #f0eef4;
  margin: 10px 0;
}

/* ===== Transitions ===== */
.fade-up-enter-active { transition: all 0.4s ease-out; }
.fade-up-enter-from { opacity: 0; transform: translateY(20px); }

.msg-list-enter-active { transition: all 0.4s ease-out; }
.msg-list-enter-from { opacity: 0; transform: translateY(30px); }
.msg-list-move { transition: transform 0.4s ease; }

.slide-down-enter-active,
.slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from,
.slide-down-leave-to { opacity: 0; transform: translateY(-10px); }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .header-banner { height: 240px; }
  .banner-title { font-size: 1.7rem; letter-spacing: 3px; }
  .banner-subtitle { font-size: 0.85rem; }
  .post-inputs { flex-direction: column; }
  .sidebar { display: none; }
  .main-body { margin-top: -30px; padding: 0 14px 50px; }
  .message-card { padding: 18px; }
  .msg-actions { opacity: 1; }
}

</style>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTodayStatistic, getTotalStatistic } from '../utils/statistic'
import { DataLine, ChatDotSquare } from '@element-plus/icons-vue'
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

// 记录已点赞的留言 ID（本地状态，刷新重置）
const likedIds = ref(new Set())
const isLiked = (id) => likedIds.value.has(id)

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
    const res = await request.post('/message/add', { ...form.value })
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
    const res = await request.post('/message/add', { ...replyForm.value })
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

// 侧栏档案：真实的文章数 / 分类数
const articleCount = ref(0)
const categoryCount = ref(0)
const fetchArchiveStats = async () => {
  try {
    const [a, c] = await Promise.all([
      request.get('/article/page', { params: { page: 1, pageSize: 1, status: '已发布' } }),
      request.get('/article/categories')
    ])
    if (a.data.code === 200) {
      articleCount.value = a.data.data.total ?? (a.data.data.records?.length || 0)
    }
    if (c.data.code === 200) {
      categoryCount.value = (c.data.data || []).length
    }
  } catch (e) {}
}

onMounted(async () => {
  await Promise.all([fetchMessages(), fetchSiteStats(), fetchArchiveStats()])
  setTimeout(() => { pageLoaded.value = true }, 100)
})
</script>

<template>
  <div class="message-page" :class="{ 'page-loaded': pageLoaded }">
    <!-- ===== 横幅 ===== -->
    <div class="hero-banner">
      <div class="banner-content">
        <h1 class="banner-title">留言板</h1>
        <p class="banner-subtitle">来都来了，写两句再走</p>
      </div>
    </div>

    <div class="main-body">
      <div class="main-col">

          <!-- 写留言 -->
          <transition name="fade-up">
            <div class="post-card">
              <div class="post-card-body">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="4"
                  placeholder="路过此地，不妨留个脚印……"
                  class="soft-input"
                  maxlength="1000"
                  show-word-limit
                />
                <div class="post-footer">
                  <div class="post-options">
                    <el-checkbox v-model="form.isMarkdown">Markdown</el-checkbox>
                  </div>
                  <button
                    class="post-btn"
                    :disabled="submitting"
                    @click="submitMessage"
                  >
                    {{ submitting ? '发布中…' : '发布留言' }}
                  </button>
                </div>
              </div>
            </div>
          </transition>

          <!-- 列表头 -->
          <div class="list-head">
            <h2 class="list-title">全部留言</h2>
            <span class="list-count">{{ messages.length }}</span>
            <span class="list-line"></span>
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
            <p class="empty-mark"><el-icon :size="40"><ChatDotSquare /></el-icon></p>
            <p class="empty-text">还没有留言，来说两句吧</p>
          </div>

          <!-- 留言列表 -->
          <transition-group name="msg-list" tag="div" class="message-list">
            <article
              v-for="(msg, i) in messages"
              :key="msg.id"
              class="message-card"
              :class="{ 'is-pinned': msg.isPinned }"
            >
              <span class="msg-index">#{{ String(messages.length - i).padStart(2, '0') }}</span>
              <span v-if="msg.isPinned" class="pin-mark">置顶</span>

                <header class="entry-head">
                  <span class="entry-avatar">
                    <img v-if="msg.avatar" :src="msg.avatar" alt="" />
                    <template v-else>{{ msg.nickname?.charAt(0)?.toUpperCase() || '访' }}</template>
                  </span>
                  <div class="entry-who">
                    <span class="entry-nick">{{ msg.nickname }}
                      <span v-if="msg.adminPost" class="tag-admin">站长</span>
                    </span>
                    <span class="entry-time">{{ msg.createTime }}</span>
                  </div>
                  <div class="entry-actions">
                    <button class="act like-act" :class="{ liked: isLiked(msg.id) }" @click="toggleLike(msg)" :title="isLiked(msg.id) ? '取消点赞' : '点赞'">
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                      <span>{{ msg.likes || 0 }}</span>
                    </button>
                    <button class="act reply-act" @click="openReplyForm(msg)">
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                      <span>回复</span>
                    </button>
                    <button v-if="isAdmin"
                      class="act pin-act"
                      :class="{ active: msg.isPinned }"
                      @click="togglePin(msg)"
                      :title="msg.isPinned ? '取消置顶' : '置顶'"
                    >
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 2-2H7a2 2 0 0 0 2 2h1z" transform="translate(0 1)"/></svg>
                    </button>
                    <button v-if="isAdmin" class="act del-act" @click="deleteMessage(msg)" title="删除">
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                    </button>
                  </div>
                </header>

                <p class="entry-content">{{ msg.content }}</p>

                <!-- 回复列表 -->
                <div v-if="msg.replies && msg.replies.length > 0" class="entry-replies">
                  <div v-for="reply in msg.replies" :key="reply.id" class="reply-item" :class="{ 'reply-admin': reply.adminPost }">
                    <div class="reply-head">
                      <span class="reply-nick">{{ reply.nickname }}</span>
                      <span v-if="reply.adminPost" class="tag-admin">站长</span>
                      <span class="reply-target" v-if="reply.replyTo">▸ {{ reply.replyTo }}</span>
                      <span class="reply-time">{{ reply.createTime }}</span>
                      <span class="reply-acts">
                        <button class="act like-act" :class="{ liked: isLiked(reply.id) }" @click="toggleLike(reply)" title="点赞">
                          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                          <span>{{ reply.likes || 0 }}</span>
                        </button>
                        <button v-if="isAdmin" class="act del-act" @click="deleteMessage(reply)" title="删除">
                          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                        </button>
                      </span>
                    </div>
                    <p class="reply-content">{{ reply.content }}</p>
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
                      placeholder="回一句……"
                      class="soft-input reply-form-input"
                      maxlength="500"
                      show-word-limit
                    />
                    <div class="reply-form-actions">
                      <button class="ghost-btn" @click="cancelReply">取消</button>
                      <button class="post-btn post-btn-sm" :disabled="replySubmitting" @click="submitReply">
                        {{ replySubmitting ? '回复中…' : '回复' }}
                      </button>
                    </div>
                  </div>
                </transition>
            </article>
          </transition-group>
      </div>

      <!-- 右侧栏 -->
      <aside class="sidebar">
        <div class="sidebar-inner">
          <!-- 站长名片 -->
          <div class="side-card side-profile">
            <div class="profile-avatar">
              <img src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" alt="yu翔" />
            </div>
            <h3 class="profile-name">yu翔</h3>
            <p class="profile-desc">初出茅庐 · 科班码农 · 拾枝者</p>
            <div class="profile-stats">
              <div class="profile-stat">
                <span class="profile-stat-num">{{ messages.length }}</span>
                <span class="profile-stat-label">留言</span>
              </div>
              <div class="profile-stat">
                <span class="profile-stat-num">{{ articleCount }}</span>
                <span class="profile-stat-label">文章</span>
              </div>
              <div class="profile-stat">
                <span class="profile-stat-num">{{ categoryCount }}</span>
                <span class="profile-stat-label">分类</span>
              </div>
            </div>
          </div>

          <!-- 站点统计 -->
          <div class="side-card">
            <div class="side-card-title">
              <el-icon :size="15"><DataLine /></el-icon>
              站点统计
            </div>
            <dl class="stat-rows">
              <div class="stat-row">
                <dt>今日浏览</dt>
                <dd>{{ todayStats.pageViews }}</dd>
              </div>
              <div class="stat-row">
                <dt>今日访客</dt>
                <dd>{{ todayStats.uniqueVisitors }}</dd>
              </div>
              <div class="stat-row stat-total">
                <dt>累计访问</dt>
                <dd>{{ totalStats.pageViews }}</dd>
              </div>
            </dl>
          </div>

          <!-- 小注 -->
          <p class="side-note">留字即缘分，<br />每一条我都会认真看。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
/* ========== 设计令牌：与全站一致的冷调商务色系 ========== */
.message-page {
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
  --danger: #e2554f;
  --danger-soft: #fcf0ef;
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
.message-page.page-loaded { opacity: 1; }

/* ===== 横幅（与博客页一致的通栏英雄区） ===== */
.hero-banner {
  position: relative;
  height: 300px;
  padding-top: 65px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  overflow: hidden;
  background:
    radial-gradient(700px 300px at 16% 18%, rgba(61, 110, 224, 0.3), transparent 60%),
    radial-gradient(520px 260px at 86% 78%, rgba(61, 110, 224, 0.16), transparent 60%),
    linear-gradient(135deg, #232d42 0%, #1a2233 58%, #202a3e 100%);
}
.banner-content {
  animation: bannerIn 0.55s var(--ease);
}
@keyframes bannerIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
.banner-title {
  font-size: 2.4rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  margin: 0 0 10px;
}
.banner-subtitle {
  font-size: 0.98rem;
  opacity: 0.78;
  margin: 0;
}

/* ===== 主体布局 ===== */
.main-body {
  max-width: 1240px;
  margin: 40px auto 0;
  padding: 0 28px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 28px;
  align-items: start;
}
.main-col { min-width: 0; }

/* ===== 写留言 ===== */
.post-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow-1);
  margin-bottom: 30px;
  transition: box-shadow 0.3s var(--ease);
}
.post-card:focus-within { box-shadow: var(--shadow-2); }
.post-card-body { padding: 20px 22px 16px; }

.soft-input :deep(.el-textarea__inner) {
  border: 1px solid var(--line-2);
  border-radius: 10px;
  background: var(--card-soft);
  box-shadow: none !important;
  color: var(--ink);
  padding: 14px 16px;
  font-size: 0.94rem;
  line-height: 1.8;
  resize: vertical;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}
.soft-input :deep(.el-textarea__inner:hover) { border-color: #c6cddb; }
.soft-input :deep(.el-textarea__inner:focus) {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(61, 110, 224, 0.12) !important;
  background: var(--card);
}
.soft-input :deep(.el-input__count) {
  background: transparent;
  font-size: 0.7rem;
  color: var(--ink-3);
  right: 10px;
  bottom: 6px;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
.post-options :deep(.el-checkbox__label) {
  font-size: 0.78rem;
  color: var(--ink-3);
}
.post-options :deep(.el-checkbox__inner) { border-radius: 4px; }
.post-options :deep(.el-checkbox.is-checked .el-checkbox__inner) {
  background: var(--accent);
  border-color: var(--accent);
}
.post-btn {
  border: none;
  border-radius: 9px;
  background: var(--accent);
  color: #fff;
  font-size: 0.86rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  padding: 9px 28px;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s, box-shadow 0.25s, opacity 0.2s;
}
.post-btn:hover {
  background: var(--accent-deep);
  transform: translateY(-1px);
  box-shadow: 0 10px 22px -10px rgba(61, 110, 224, 0.55);
}
.post-btn:active { transform: translateY(0) scale(0.985); }
.post-btn:disabled { opacity: 0.55; cursor: not-allowed; transform: none; box-shadow: none; }
.post-btn-sm { padding: 7px 22px; font-size: 0.8rem; }
.ghost-btn {
  border: 1px solid var(--line-2);
  border-radius: 9px;
  background: var(--card);
  color: var(--ink-2);
  font-size: 0.8rem;
  padding: 7px 16px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.ghost-btn:hover { border-color: #c6cddb; color: var(--ink); background: var(--card-soft); }

/* ===== 列表头 ===== */
.list-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}
.list-title {
  font-size: 1.12rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
}
.list-count {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--accent-deep);
  background: var(--accent-soft);
  border-radius: 999px;
  padding: 2px 10px;
}
.list-line { flex: 1; height: 1px; background: var(--line-2); }

/* ===== 骨架 ===== */
.skeleton-list { display: flex; flex-direction: column; gap: 18px; }
.skeleton-card {
  display: flex;
  gap: 16px;
  padding: 24px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
}
.skeleton-avatar {
  width: 42px; height: 42px;
  border-radius: 50%;
  background: linear-gradient(90deg, #eef0f4 25%, #f7f8fb 50%, #eef0f4 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}
.skeleton-lines { flex: 1; display: flex; flex-direction: column; gap: 12px; padding-top: 4px; }
.skeleton-line {
  height: 13px;
  border-radius: 6px;
  background: linear-gradient(90deg, #eef0f4 25%, #f7f8fb 50%, #eef0f4 75%);
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

/* ===== 空态 ===== */
.empty-state {
  background: var(--card);
  border: 1px dashed var(--line-2);
  border-radius: 14px;
  padding: 64px 20px;
  text-align: center;
}
.empty-mark {
  color: #ccd3de;
  margin: 0 0 10px;
  line-height: 1;
}
.empty-text { color: var(--ink-3); font-size: 0.9rem; margin: 0; }

/* ===== 留言卡片 ===== */
.message-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.message-card {
  position: relative;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 22px 24px 16px;
  box-shadow: var(--shadow-1);
  transition:
    transform 0.3s var(--ease),
    box-shadow 0.3s var(--ease),
    border-color 0.25s;
}
.message-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-2);
  border-color: var(--line-2);
}
/* 置顶：淡蓝底 + 角落旗帜 */
.message-card.is-pinned {
  border-color: rgba(61, 110, 224, 0.32);
  background: linear-gradient(105deg, var(--accent-soft) 0%, var(--card) 36%);
}
.pin-mark {
  position: absolute;
  top: 0;
  right: 0;
  background: var(--accent);
  color: #fff;
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  padding: 5px 12px;
  border-radius: 0 13px 0 10px;
}
.message-card.is-pinned .msg-index { display: none; }

/* 序号（克制的小标记） */
.msg-index {
  position: absolute;
  top: 20px;
  right: 22px;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #c3c9d6;
  transition: color 0.2s;
}
.message-card:hover .msg-index { color: var(--accent); }

/* 条目头 */
.entry-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.entry-avatar {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #2a3346;
  color: #fff;
  font-weight: 600;
  font-size: 0.98rem;
  overflow: hidden;
}
.entry-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.entry-who {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.entry-nick {
  font-weight: 600;
  font-size: 0.94rem;
  color: var(--ink);
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag-admin {
  font-size: 0.66rem;
  font-weight: 600;
  color: var(--accent-deep);
  background: var(--accent-soft);
  padding: 2px 8px;
  border-radius: 6px;
  white-space: nowrap;
}
.entry-time {
  font-size: 0.76rem;
  color: var(--ink-3);
}

/* 操作（hover / 键盘聚焦显现） */
.entry-actions {
  margin-left: auto;
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.message-card:hover .entry-actions,
.message-card:focus-within .entry-actions { opacity: 1; }
.act {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--ink-3);
  font-size: 0.75rem;
  border-radius: 8px;
  padding: 5px 9px;
  transition: color 0.18s, background 0.18s, transform 0.15s;
}
.act:hover { color: var(--ink); background: #f2f4f8; }
.act:active { transform: scale(0.94); }
.like-act:hover { color: var(--accent); background: var(--accent-soft); }
.like-act.liked { color: var(--accent); }
.like-act.liked svg {
  fill: var(--accent);
  stroke: var(--accent);
  animation: likePop 0.32s var(--ease);
}
@keyframes likePop {
  0% { transform: scale(1); }
  50% { transform: scale(1.4); }
  100% { transform: scale(1); }
}
.pin-act:hover { color: var(--accent); background: var(--accent-soft); }
.pin-act.active { color: var(--accent); }
.pin-act.active svg { fill: var(--accent); stroke: var(--accent); }
.del-act:hover { color: var(--danger); background: var(--danger-soft); }

/* 条目正文 */
.entry-content {
  font-size: 0.93rem;
  line-height: 1.8;
  color: var(--ink-2);
  margin: 0 0 12px;
  word-break: break-word;
  white-space: pre-wrap;
}

/* ===== 回复 ===== */
.entry-replies {
  margin-top: 14px;
  padding-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.reply-item {
  background: var(--card-soft);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 12px 14px;
  transition: border-color 0.2s, background 0.2s;
}
.reply-item:hover { border-color: var(--line-2); background: #f7f9fc; }
.reply-admin { border-left: 3px solid var(--accent); }
.reply-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.reply-nick {
  font-weight: 600;
  font-size: 0.82rem;
  color: var(--ink);
}
.reply-target {
  font-size: 0.72rem;
  color: var(--accent-deep);
  background: var(--accent-soft);
  padding: 1px 8px;
  border-radius: 6px;
}
.reply-time {
  font-size: 0.7rem;
  color: var(--ink-3);
}
.reply-acts {
  margin-left: auto;
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.reply-item:hover .reply-acts { opacity: 1; }
.reply-content {
  font-size: 0.86rem;
  line-height: 1.7;
  color: var(--ink-2);
  margin: 4px 0 0;
}

/* ===== 回复表单 ===== */
.reply-form {
  margin-top: 14px;
  background: var(--card-soft);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px 12px;
}
.reply-form-header {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--accent-deep);
  margin-bottom: 10px;
}
.reply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

/* ===== 右侧栏 ===== */
.sidebar-inner {
  position: sticky;
  top: 88px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.side-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow-1);
  padding: 24px 22px;
  transition: box-shadow 0.3s var(--ease), transform 0.3s var(--ease);
}
.side-card:hover {
  box-shadow: var(--shadow-2);
  transform: translateY(-2px);
}

/* 站长名片 */
.side-profile { text-align: center; }
.profile-avatar {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 14px;
  border: 3px solid #fff;
  box-shadow: 0 3px 12px rgba(16, 24, 40, 0.14);
}
.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.profile-name {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 4px;
}
.profile-desc {
  font-size: 0.8rem;
  color: var(--ink-3);
  margin: 0 0 16px;
}
.profile-stats {
  display: flex;
  border-top: 1px solid var(--line);
  padding-top: 16px;
}
.profile-stat {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.profile-stat + .profile-stat::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 1px;
  background: var(--line);
}
.profile-stat-num {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--ink);
}
.profile-stat-label {
  font-size: 0.72rem;
  color: var(--ink-3);
}

/* 站点统计 */
.side-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--ink);
  padding-bottom: 12px;
  margin-bottom: 6px;
  border-bottom: 1px solid var(--line);
}
.side-card-title .el-icon { color: var(--accent); }
.stat-rows { margin: 0; }
.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
}
.stat-row:last-child { border-bottom: none; }
.stat-row dt { font-size: 0.84rem; color: var(--ink-3); }
.stat-row dd {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--ink);
}
.stat-total dd { color: var(--accent-deep); font-size: 1rem; }

/* 小注 */
.side-note {
  font-size: 0.8rem;
  color: var(--ink-3);
  line-height: 1.9;
  text-align: center;
  margin: 0;
}

/* ===== Transitions ===== */
.fade-up-enter-active { transition: all 0.4s ease-out; }
.fade-up-enter-from { opacity: 0; transform: translateY(20px); }

.msg-list-enter-active { transition: all 0.4s var(--ease); }
.msg-list-enter-from { opacity: 0; transform: translateY(24px); }
.msg-list-move { transition: transform 0.4s var(--ease); }

.slide-down-enter-active,
.slide-down-leave-active { transition: all 0.3s var(--ease); }
.slide-down-enter-from,
.slide-down-leave-to { opacity: 0; transform: translateY(-10px); }

/* ===== Responsive ===== */
@media (max-width: 960px) {
  .main-body { grid-template-columns: minmax(0, 1fr); }
  .sidebar { display: none; }
}
@media (max-width: 768px) {
  .hero-banner { height: 220px; }
  .banner-title { font-size: 1.7rem; }
  .main-body { margin-top: 28px; padding: 0 16px; gap: 0; }
  .message-card { padding: 18px 18px 12px; }
  .msg-index { top: 16px; right: 16px; }
  .entry-actions { opacity: 1; }
  .reply-acts { opacity: 1; }
  .post-footer { flex-direction: column; align-items: stretch; gap: 12px; }
  .post-options { order: 2; }
}

@media (prefers-reduced-motion: reduce) {
  .message-page { transition: none; opacity: 1; }
  .message-card { transition: border-color 0.2s; }
  .like-act.liked svg { animation: none; }
}

</style>

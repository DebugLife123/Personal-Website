<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTodayStatistic, getTotalStatistic } from '../utils/statistic'
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

// 留言计数角标（朱色印章）
const stampSvg = (n) =>
  `data:image/svg+xml;utf8,${encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="74" height="74" viewBox="0 0 74 74">
      <rect x="4" y="4" width="66" height="66" fill="none" stroke="#b23e22" stroke-width="2.6"/>
      <rect x="9" y="9" width="56" height="56" fill="none" stroke="#b23e22" stroke-width="1"/>
      <text x="37" y="46" font-family="'Noto Serif SC','SimSun',serif" font-size="34" font-weight="900" fill="#b23e22" text-anchor="middle">${n}</text>
    </svg>`
  )}`

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
    <!-- ===== 卷首 ===== -->
    <header class="page-head">
      <p class="head-kicker">G U E S T B O O K · 来 客 留 名 处</p>
      <h1 class="head-title">留言<span class="head-title-accent">板</span></h1>
      <p class="head-sub">—— 来都来了，写两句再走</p>
      <div class="head-meta">
        <span class="head-meta-item">共 <em>{{ messages.length }}</em> 条留字</span>
      </div>
    </header>

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
                    {{ submitting ? '盖章中…' : '盖戳留言' }}
                  </button>
                </div>
              </div>
            </div>
          </transition>

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
            <p class="empty-mark">空</p>
            <p class="empty-text">册子还没人动笔，等你来开个头</p>
          </div>

          <!-- 留言册 -->
          <transition-group name="msg-list" tag="div" class="message-list">
            <article
              v-for="(msg, i) in messages"
              :key="msg.id"
              class="msg-ledger-item"
              :class="{ 'is-pinned': msg.isPinned }"
            >
              <!-- 左侧：朱印编号 + 装订线 -->
              <div class="ledger-rail" aria-hidden="true">
                <div class="stamp-box">
                  <img :src="stampSvg(i + 1)" :alt="'第' + (i + 1) + '条'" class="stamp-img" loading="lazy" />
                </div>
                <span class="rail-line"></span>
              </div>

              <div class="entry-card">
                <span v-if="msg.isPinned" class="pin-mark">置顶</span>

                <header class="entry-head">
                  <span class="entry-avatar" :class="{ 'has-img': !!msg.avatar }">
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
                      <button class="ghost-btn" @click="cancelReply">作罢</button>
                      <button class="post-btn post-btn-sm" :disabled="replySubmitting" @click="submitReply">
                        {{ replySubmitting ? '盖章中…' : '盖戳' }}
                      </button>
                    </div>
                  </div>
                </transition>
              </div>
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

          <!-- 站点小账 -->
          <div class="side-card">
            <div class="side-card-title">站点小账</div>
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
/* ========== 纸墨留言册 · 设计令牌 ========== */
.message-page {
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

  min-height: 100vh;
  padding-bottom: 110px;
  color: var(--ink);
  background:
    radial-gradient(1200px 400px at 50% -120px, rgba(255, 253, 246, 0.8), transparent 70%),
    var(--paper);
  opacity: 0;
  transition: opacity 0.5s ease;
  overflow-x: clip;
}
/* 纸面颗粒 */
.message-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.55;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)' opacity='0.045'/%3E%3C/svg%3E");
}
.message-page.page-loaded { opacity: 1; }

/* ===== 卷首 ===== */
.page-head {
  position: relative;
  max-width: 1100px;
  margin: 0 auto;
  padding: 78px 28px 0;
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
  font-size: clamp(3rem, 7vw, 5rem);
  font-weight: 900;
  line-height: 1.05;
  margin: 0 0 14px;
  color: var(--ink);
}
.head-title-accent { color: var(--vermilion); }
.head-sub {
  font-family: var(--serif);
  font-size: 1rem;
  color: var(--ink-2);
  margin: 0 0 30px;
}
.head-meta {
  display: flex;
  align-items: center;
  gap: 22px;
  padding-bottom: 26px;
  border-bottom: 1px solid var(--line);
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

/* ===== 主体布局 ===== */
.main-body {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 36px auto 0;
  padding: 0 28px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  gap: 44px;
  align-items: start;
}
.main-col { min-width: 0; }

/* ===== 写留言 ===== */
.post-card {
  background: var(--paper-raised);
  border: 1px solid var(--line);
  margin-bottom: 46px;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.post-card:hover {
  border-color: var(--line-strong);
  box-shadow: 0 12px 32px -20px rgba(43, 39, 34, 0.45);
}
.post-card-body { padding: 20px 22px 16px; }

.soft-input :deep(.el-textarea__inner) {
  border: 1px solid var(--line);
  border-radius: 2px;
  background: transparent;
  box-shadow: none !important;
  color: var(--ink);
  padding: 14px 16px;
  font-size: 0.94rem;
  line-height: 1.8;
  resize: vertical;
  transition: border-color 0.25s;
}
.soft-input :deep(.el-textarea__inner:hover) { border-color: var(--line-strong); }
.soft-input :deep(.el-textarea__inner:focus) { border-color: var(--vermilion); }
.soft-input :deep(.el-input__count) {
  background: transparent;
  font-family: var(--mono);
  font-size: 0.68rem;
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
  font-family: var(--mono);
  font-size: 0.78rem;
  color: var(--ink-3);
}
.post-options :deep(.el-checkbox__inner) { border-radius: 0; }
.post-options :deep(.el-checkbox.is-checked .el-checkbox__inner) {
  background: var(--vermilion);
  border-color: var(--vermilion);
}
.post-btn {
  border: 1px solid var(--vermilion);
  background: var(--vermilion);
  color: var(--paper-raised);
  font-size: 0.86rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  padding: 9px 30px;
  cursor: pointer;
  transition: background 0.22s, transform 0.15s, opacity 0.2s;
}
.post-btn:hover { background: var(--vermilion-deep); }
.post-btn:active { transform: translateY(1px); }
.post-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.post-btn-sm { padding: 7px 22px; font-size: 0.8rem; }
.ghost-btn {
  border: 1px solid var(--line-strong);
  background: none;
  color: var(--ink-2);
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  padding: 7px 16px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.ghost-btn:hover { border-color: var(--ink); color: var(--ink); }

/* ===== 骨架 ===== */
.skeleton-list { display: flex; flex-direction: column; }
.skeleton-card {
  display: flex;
  gap: 16px;
  padding: 26px 4px;
  border-top: 1px solid var(--line);
}
.skeleton-avatar {
  width: 40px; height: 40px;
  border-radius: 2px;
  background: linear-gradient(90deg, #e6dfcd 25%, #f2ecdc 50%, #e6dfcd 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}
.skeleton-lines { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.skeleton-line {
  height: 13px;
  border-radius: 2px;
  background: linear-gradient(90deg, #e6dfcd 25%, #f2ecdc 50%, #e6dfcd 75%);
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
  border: 1px dashed var(--line-strong);
  padding: 70px 20px;
  text-align: center;
  background: rgba(250, 247, 238, 0.55);
}
.empty-mark {
  font-family: var(--serif);
  font-size: 3.2rem;
  font-weight: 900;
  color: var(--line-strong);
  margin: 0 0 10px;
  line-height: 1;
}
.empty-text { color: var(--ink-3); font-size: 0.9rem; margin: 0; }

/* ===== 留言册 · 账页条目 ===== */
.message-list { display: flex; flex-direction: column; }
.msg-ledger-item {
  position: relative;
  display: flex;
  gap: 22px;
  padding: 26px 0 30px;
  border-top: 1px solid var(--line);
}
.ledger-rail {
  width: 46px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
/* 朱印编号（sticky 跟随） */
.stamp-box {
  position: sticky;
  top: 92px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stamp-img {
  width: 44px;
  height: 44px;
  transform: rotate(-5deg);
  mix-blend-mode: multiply;
  opacity: 0.9;
  filter: saturate(0.92);
}
.rail-line {
  flex: 1;
  width: 1px;
  margin-top: 14px;
  background: repeating-linear-gradient(
    to bottom,
    var(--line-strong) 0 5px,
    transparent 5px 11px
  );
}

/* 条目卡 */
.entry-card {
  position: relative;
  flex: 1;
  min-width: 0;
  background: var(--paper-raised);
  border: 1px solid var(--line);
  padding: 20px 24px 14px;
  transition: border-color 0.25s, box-shadow 0.25s, transform 0.25s;
}
.entry-card:hover {
  border-color: var(--line-strong);
  box-shadow: 0 12px 30px -18px rgba(43, 39, 34, 0.5);
  transform: translateY(-2px);
}
.msg-ledger-item.is-pinned .entry-card {
  border-left: 3px solid var(--vermilion);
  background: linear-gradient(105deg, rgba(178, 62, 34, 0.05), var(--paper-raised) 38%);
}
.pin-mark {
  position: absolute;
  top: 0;
  right: 16px;
  background: var(--vermilion);
  color: var(--paper-raised);
  font-size: 0.66rem;
  font-weight: 600;
  letter-spacing: 0.22em;
  padding: 4px 9px 5px 11px;
}

/* 条目头 */
.entry-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.entry-avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line-strong);
  background: var(--ink);
  color: var(--paper-raised);
  font-family: var(--serif);
  font-weight: 700;
  font-size: 1.05rem;
  overflow: hidden;
}
.entry-avatar.has-img { border: none; background: none; }
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
  font-size: 0.95rem;
  color: var(--ink);
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag-admin {
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  border: 1px solid var(--vermilion);
  color: var(--vermilion);
  padding: 1px 7px;
  white-space: nowrap;
}
.entry-time {
  font-family: var(--mono);
  font-size: 0.68rem;
  color: var(--ink-3);
}

/* 操作（hover 显现，键盘聚焦也可用） */
.entry-actions {
  margin-left: auto;
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.entry-card:hover .entry-actions,
.entry-card:focus-within .entry-actions { opacity: 1; }
.act {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 0.75rem;
  padding: 5px 9px;
  transition: color 0.18s, background 0.18s;
}
.act:hover { color: var(--ink); background: rgba(43, 39, 34, 0.05); }
.like-act:hover, .like-act.liked { color: var(--vermilion); }
.like-act.liked svg { fill: var(--vermilion); stroke: var(--vermilion); }
.pin-act.active { color: var(--vermilion); }
.del-act:hover { color: var(--vermilion); }

/* 条目正文 */
.entry-content {
  font-size: 0.94rem;
  line-height: 1.9;
  color: var(--ink-2);
  margin: 0 0 12px;
  word-break: break-word;
  white-space: pre-wrap;
}

/* ===== 回复 ===== */
.entry-replies {
  margin-top: 14px;
  border-top: 1px dashed var(--line);
  padding-top: 13px;
  display: flex;
  flex-direction: column;
  gap: 13px;
}
.reply-item {
  position: relative;
  padding: 2px 0 2px 18px;
  border-left: 1px solid var(--line-strong);
}
.reply-item::before {
  content: '';
  position: absolute;
  left: -3.5px;
  top: 13px;
  width: 6px;
  height: 6px;
  background: var(--paper-raised);
  border: 1px solid var(--line-strong);
}
.reply-admin { border-left-color: var(--vermilion); }
.reply-admin::before { border-color: var(--vermilion); }
.reply-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.reply-nick {
  font-weight: 600;
  font-size: 0.84rem;
  color: var(--ink);
}
.reply-target {
  font-family: var(--mono);
  font-size: 0.68rem;
  color: var(--ink-3);
}
.reply-time {
  font-family: var(--mono);
  font-size: 0.66rem;
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
  font-size: 0.87rem;
  line-height: 1.8;
  color: var(--ink-2);
  margin: 3px 0 0;
}

/* ===== 回复表单 ===== */
.reply-form {
  margin-top: 14px;
  border: 1px solid var(--line);
  background: rgba(43, 39, 34, 0.025);
  padding: 14px 16px 12px;
}
.reply-form-header {
  font-family: var(--serif);
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--vermilion);
  margin-bottom: 10px;
}
.reply-form-header::before { content: '✎  '; }
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
  background: var(--paper-raised);
  border: 1px solid var(--line);
  padding: 26px 24px;
  transition: border-color 0.25s;
}
.side-card:hover { border-color: var(--line-strong); }

/* 站长名片 */
.side-profile { text-align: center; }
.profile-avatar {
  width: 80px;
  height: 80px;
  margin: 0 auto 14px;
  border: 1px solid var(--line-strong);
  background: var(--paper-raised);
  padding: 4px;
}
.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.profile-name {
  font-family: var(--serif);
  font-size: 1.3rem;
  font-weight: 900;
  color: var(--ink);
  margin: 0 0 4px;
}
.profile-desc {
  font-size: 0.78rem;
  color: var(--ink-3);
  margin: 0 0 18px;
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
  font-family: var(--mono);
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--ink);
}
.profile-stat-label {
  font-size: 0.68rem;
  letter-spacing: 0.18em;
  color: var(--ink-3);
}

/* 站点小账 */
.side-card-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-family: var(--serif);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
  padding-bottom: 12px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--line);
}
.side-card-title::before {
  content: '';
  width: 8px;
  height: 8px;
  background: var(--vermilion);
}
.stat-rows { margin: 0; }
.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
}
.stat-row:last-child { border-bottom: none; }
.stat-row dt { font-size: 0.82rem; color: var(--ink-3); }
.stat-row dd {
  margin: 0;
  font-family: var(--mono);
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--ink);
}
.stat-total dd { color: var(--vermilion); font-size: 1.05rem; }

/* 小注 */
.side-note {
  font-family: var(--serif);
  font-size: 0.8rem;
  color: var(--ink-3);
  line-height: 2;
  text-align: center;
  margin: 2px 0 0;
}

/* ===== Transitions ===== */
.fade-up-enter-active { transition: all 0.4s ease-out; }
.fade-up-enter-from { opacity: 0; transform: translateY(20px); }

.msg-list-enter-active { transition: all 0.4s ease-out; }
.msg-list-enter-from { opacity: 0; transform: translateY(26px); }
.msg-list-move { transition: transform 0.4s ease; }

.slide-down-enter-active,
.slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from,
.slide-down-leave-to { opacity: 0; transform: translateY(-10px); }

/* ===== Responsive ===== */
@media (max-width: 960px) {
  .main-body {
    grid-template-columns: minmax(0, 1fr);
  }
  .sidebar { display: none; }
}
@media (max-width: 768px) {
  .page-head { padding: 54px 20px 0; }
  .head-meta { padding-bottom: 20px; }
  .main-body { margin-top: 28px; padding: 0 16px; gap: 0; }
  .msg-ledger-item { gap: 12px; padding: 20px 0 24px; }
  .ledger-rail { width: 30px; }
  .stamp-box { width: 29px; height: 29px; top: 84px; }
  .stamp-img { width: 29px; height: 29px; }
  .rail-line { display: none; }
  .entry-card { padding: 16px 16px 12px; }
  .entry-actions { opacity: 1; }
  .reply-acts { opacity: 1; }
  .post-footer { flex-direction: column; align-items: stretch; gap: 12px; }
  .post-options { order: 2; }
  .post-btn { order: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .message-page { transition: none; opacity: 1; }
  .entry-card { transition: border-color 0.2s; }
}

</style>

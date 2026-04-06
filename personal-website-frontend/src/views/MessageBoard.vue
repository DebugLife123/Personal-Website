<script setup>
import { ref, onMounted,onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { 
  HomeFilled, Notebook, Box, Link, ChatLineRound, UserFilled, Promotion, 
  VideoPlay, DArrowRight, Moon, Search, Edit, Postcard, Timer, Connection, DataLine 
} from '@element-plus/icons-vue'
const router = useRouter()

// 1. 数据定义
const messages = ref([])
const form = ref({
  nickname: '',
  email: '',
  code: '',
  content: '',
  isPrivate: false,
  isMarkdown: true
})

// 导航数据
const navLinks = ref([
  { name: '主页', icon: HomeFilled, path: '/' },
  { name: '博客', icon: Notebook, path: '/blog' },
  { name: '归档', icon: Box, path: '/archives' },
  { name: '友链', icon: Link, path: '/friends' },
  { name: '留言', icon: ChatLineRound, path: '/messages' },
  { name: '关于', icon: UserFilled, path: '/about' },
  { name: '开往', icon: Promotion, path: '/travelling' },
])

// 获取留言列表
const fetchMessages = async () => {
  try {
    const res = await request.get('/message/list')
    if (res.data.code === 200) messages.value = res.data.data
  } catch (e) { console.error(e) }
}

// 提交留言
const submitMessage = async () => {
  if (!form.value.nickname || !form.value.content) {
    return ElMessage.warning('请填写昵称和内容')
  }
  const res = await request.post('/message/add', form.value)
  if (res.data.code === 200) {
    ElMessage.success('留言成功！')
    form.value.content = ''
    fetchMessages()
  }
}
// --- 复制过来的音乐播放逻辑 ---
const isPlaying = ref(false)
const currentTrackIndex = ref(0)
const playlist = [
  { name: '泡沫', url: '/music/G.E.M. 邓紫棋 - 泡沫.mp3',cover: 'https://img2.baidu.com/it/u=3284921046,1018788791&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800' },
  { name: '母系社会', url: '/music/母系社会-张惠妹.mp3',cover: 'https://img2.baidu.com/it/u=3284921046,1018788791&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800' }
]

// 创建音频实例
const audio = new Audio(playlist[currentTrackIndex.value].url)

// 播放/暂停切换
const togglePlay = () => {
  if (isPlaying.value) {
    audio.pause()
  } else {
    audio.play().catch(e => console.error("播放失败:", e))
  }
  isPlaying.value = !isPlaying.value
}

// 下一首
const nextTrack = () => {
  currentTrackIndex.value = (currentTrackIndex.value + 1) % playlist.length
  audio.src = playlist[currentTrackIndex.value].url
  if (isPlaying.value) {
    audio.play()
  }
}

// 自动切歌
audio.onended = () => nextTrack()

onUnmounted(() => {
  audio.pause()
})

onMounted(fetchMessages)
</script>

<template>
  <div class="message-page">
    <div class="header-banner">
      <div class="nav-overlay">
        <div class="nav-container">
          <div class="logo">FeiTwnd's Blog</div>
          <div class="nav-menu">
            <div v-for="item in navLinks" :key="item.name" class="nav-item" @click="router.push(item.path)">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.name }}</span>
            </div>
          </div>
          <div class="nav-right">
  <span class="music-status" @click="togglePlay" style="cursor: pointer;">
    <el-icon>
      <component :is="isPlaying ? VideoPause : VideoPlay" />
    </el-icon> 
    <span>{{ playlist[currentTrackIndex]?.name }}</span>
  </span>

  <el-icon @click="togglePlay" style="cursor: pointer;">
    <component :is="isPlaying ? VideoPause : VideoPlay" />
  </el-icon>

  <el-icon @click="nextTrack" style="cursor: pointer;">
    <DArrowRight />
  </el-icon>
  
  <el-icon><Moon /></el-icon>
  <el-icon><Search /></el-icon>
</div>
        </div>
      </div>
      <div class="banner-title">
        <h1>留言板</h1>
        <p>说点什么吧</p>
      </div>
    </div>

    <div class="main-body">
      <el-row :gutter="25" justify="center">
        <el-col :xs="24" :sm="24" :md="16" :lg="15">
          
          <el-card class="editor-card" shadow="never">
            <template #header>
              <div class="editor-header"><el-icon><Edit /></el-icon> 写留言</div>
            </template>
            
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="5"
              placeholder="写点什么..."
              class="custom-textarea"
            />
            
            <div class="editor-inputs">
              <el-input v-model="form.nickname" placeholder="昵称 *" class="input-item">
                <template #prefix><el-icon><UserFilled /></el-icon></template>
              </el-input>
              <el-input v-model="form.email" placeholder="邮箱/QQ号" class="input-item">
                <template #prefix><el-icon><Postcard /></el-icon></template>
              </el-input>
              <el-input v-model="form.code" placeholder="验证码" class="input-item code-input">
                <template #prefix><el-icon><Timer /></el-icon></template>
                <template #suffix><span class="code-refresh">↻</span></template>
              </el-input>
            </div>
            
            <div class="editor-footer">
              <div class="options">
                <el-checkbox v-model="form.isPrivate">悄悄话</el-checkbox>
                <el-checkbox>邮件提醒</el-checkbox>
                <el-checkbox v-model="form.isMarkdown">Markdown</el-checkbox>
              </div>
              <div class="actions">
                <span class="emoji-btn">☺</span>
                <el-button type="info" class="submit-btn" @click="submitMessage">留言</el-button>
              </div>
            </div>
          </el-card>

          <div class="message-stats">共 {{ messages.length }} 条留言</div>
          
          <div class="message-list">
            <div v-for="msg in messages" :key="msg.id" class="message-item">
              <el-avatar :size="45" src="https://img1.baidu.com/it/u=1611598392,1593784950&fm=253&app=138&f=JPEG?w=500&h=500" />
              <div class="msg-main">
                <div class="msg-info">
                  <span class="nickname">{{ msg.nickname }}</span>
                  <span class="location">📍 湖南-衡阳</span>
                  <span class="device">💻 Chrome</span>
                  <span class="time">{{ msg.createTime }}</span>
                </div>
                <div class="msg-content">{{ msg.content }}</div>
              </div>
            </div>
          </div>

        </el-col>

        <el-col :xs="0" :sm="0" :md="7" :lg="6">
          <el-card class="profile-card" shadow="never">
            <div class="profile-header">
              <el-avatar :size="80" src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" />
              <h3>yu翔</h3>
              <p>初出茅庐 | 科班码农 | 拾枝者</p>
              <p class="loc">📍 湖南-衡阳</p>
            </div>
            <div class="profile-stats">
              <div class="item"><span>12</span>文章</div>
              <div class="item"><span>3</span>分类</div>
              <div class="item"><span>15</span>标签</div>
            </div>
            <div class="profile-social">
              <el-icon><Connection /></el-icon> <el-icon><Postcard /></el-icon> <el-icon><Connection /></el-icon>
            </div>
          </el-card>

          <el-card class="stat-card" shadow="never">
            <template #header><strong><el-icon><DataLine /></el-icon> 站点统计</strong></template>
            <div class="stat-row"><span>在线访客</span><span>69</span></div>
            <div class="stat-row"><span>今日浏览</span><span>28</span></div>
            <div class="stat-row"><span>总访问量</span><span>5765</span></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.message-page { background-color: #f6f8fa; min-height: 100vh; }

/* 1. Header Banner */
.header-banner {
  height: 400px;
  background: url('https://img2.baidu.com/it/u=3284921046,1018788791&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800') center/cover;
  position: relative;
  color: white;
}
.nav-overlay { background: rgba(0,0,0,0.2); padding: 15px 0; }
.nav-container { max-width: 1400px; margin: 0 auto; display: flex; align-items: center; padding: 0 40px; }
.logo { font-size: 1.2rem; font-weight: bold; flex: 0 0 auto; margin-right: 40px; }
.nav-menu { flex: 1; display: flex; gap: 20px; }
.nav-item { display: flex; align-items: center; gap: 6px; cursor: pointer; font-size: 0.95rem; opacity: 0.9; }
.nav-item:hover { opacity: 1; }
.nav-right { display: flex; align-items: center; gap: 15px; }
.music-status { font-size: 0.8rem; background: rgba(255,255,255,0.1); padding: 4px 10px; border-radius: 20px; }

.banner-title { text-align: center; margin-top: 60px; }
.banner-title h1 { font-size: 2.5rem; letter-spacing: 4px; margin-bottom: 10px; }

/* 2. Main Body */
.main-body { max-width: 1350px; margin: -50px auto 0; padding: 0 20px 50px; }

/* 写留言卡片 */
.editor-card { border-radius: 10px; border: none; margin-bottom: 30px; }
.editor-header { font-weight: bold; color: #444; }
.custom-textarea :deep(.el-textarea__inner) { border: 1px solid #eee; background: #fafafa; border-radius: 8px; margin-bottom: 15px; }

.editor-inputs { display: flex; gap: 15px; margin-bottom: 15px; }
.input-item { flex: 1; }
.code-refresh { cursor: pointer; color: #999; }

.editor-footer { display: flex; justify-content: space-between; align-items: center; }
.actions { display: flex; align-items: center; gap: 20px; }
.emoji-btn { font-size: 1.2rem; cursor: pointer; color: #999; }
.submit-btn { background: #333; color: white; border: none; padding: 0 30px; border-radius: 5px; }

/* 留言列表 */
.message-stats { padding-left: 5px; color: #999; font-size: 0.9rem; margin-bottom: 20px; }
.message-item { background: white; padding: 25px; border-radius: 8px; display: flex; gap: 15px; margin-bottom: 15px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); }
.msg-info { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.nickname { font-weight: bold; color: #555; }
.location, .device, .time { color: #aaa; font-size: 0.8rem; }
.msg-content { color: #666; line-height: 1.6; }

/* 侧边栏 */
.profile-card { text-align: center; border: none; margin-bottom: 25px; border-radius: 10px; }
.profile-header h3 { margin: 15px 0 5px; }
.profile-header p { color: #888; font-size: 0.85rem; }
.profile-header .loc { margin-top: 8px; color: #aaa; }
.profile-stats { display: flex; justify-content: space-around; margin: 25px 0; border-top: 1px solid #f0f0f0; padding-top: 20px; }
.profile-stats .item span { display: block; font-weight: bold; font-size: 1.2rem; }
.profile-stats .item { font-size: 0.85rem; color: #999; }
.profile-social { display: flex; justify-content: center; gap: 20px; color: #aaa; font-size: 1.2rem; }

.stat-card { border: none; border-radius: 10px; }
.stat-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 0.9rem; color: #666; }
/* 音乐播放器外层包裹 */
.music-capsule-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 胶囊主体 */
.music-capsule {
  display: flex;
  align-items: center;
  padding: 4px 20px 4px 4px; /* 左侧留小一点给图片，右侧留多一点给文字 */
  background: rgba(255, 255, 255, 0.15); /* 半透明背景 */
  backdrop-filter: blur(10px); /* 毛玻璃 */
  border: 1px solid rgba(255, 255, 255, 0.3); /* 细微边框 */
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}

.music-capsule:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.02);
}

/* 封面图片 */
.music-cover {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  border: 1.5px solid rgba(255, 255, 255, 0.5);
  margin-right: 12px;
}

.music-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 播放时的旋转动画（可选） */
.rotating {
  animation: rotate 8s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 歌曲名称 */
.music-name {
  font-size: 1rem;
  font-weight: 500;
  color: white;
  letter-spacing: 0.5px;
}

/* 控制按钮组 */
.music-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.control-icon {
  font-size: 1.5rem;
  color: white;
  cursor: pointer;
  transition: opacity 0.2s;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}

.control-icon:hover {
  opacity: 0.8;
}
</style>
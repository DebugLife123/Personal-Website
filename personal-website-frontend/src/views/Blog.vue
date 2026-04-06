<script setup>
import { useRouter } from 'vue-router'
import { ref, onMounted, onUnmounted } from 'vue' 
import request from '../utils/request' 
// 【修改】增加了 VideoPause 图标的引入
import { 
  HomeFilled, Notebook, Box, Link, ChatLineRound, UserFilled, Promotion, 
  Calendar, View, VideoPlay, VideoPause, DArrowRight, Moon, Search, DataLine 
} from '@element-plus/icons-vue'

const router = useRouter()

// 导航链接配置
const navLinks = ref([
  { name: '主页', icon: HomeFilled, path: '/' },
  { name: '博客', icon: Notebook, path: '/blog' },
  { name: '归档', icon: Box, path: '/archives' },
  { name: '友链', icon: Link, path: '/friends' },
  { name: '留言', icon: ChatLineRound, path: '/messages' },
  { name: '关于', icon: UserFilled, path: '/about' },
  { name: '开往', icon: Promotion, path: '/travelling' },
])

// 文章列表数据
const articleList = ref([])
onMounted(async () => {
  try {
    const res = await request.get('/article/list')
    if (res.data.code === 200) {
      articleList.value = res.data.data
    }
  } catch (error) {
    console.error("获取文章列表失败:", error)
  }
})

const goToDetail = (id) => {
  router.push(`/article/${id}`)
}

// ==================================================
// 🎵 音乐播放逻辑 (核心修复区)
// ==================================================
const isPlaying = ref(false)
const currentTrackIndex = ref(0)

// 【重要修改】去掉了路径中的 /public。
// Vite 规范：public 文件夹下的资源直接通过 / 访问。
const playlist = [
  { name: '泡沫', url: '/music/G.E.M. 邓紫棋 - 泡沫.mp3',cover: 'https://img2.baidu.com/it/u=587348950,570052171&fm=253&fmt=auto&app=120&f=JPEG?w=667&h=500' },
  { name: '母系社会', url: '/music/母系社会 - 张惠妹.mp3',cover: 'https://img2.baidu.com/it/u=3284921046,1018788791&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800' }
]

// 创建音频实例
const audio = new Audio(playlist[currentTrackIndex.value].url)

// 播放/暂停切换
const togglePlay = () => {
  if (isPlaying.value) {
    audio.pause()
  } else {
    // 捕获播放错误（如路径不对或浏览器拦截）
    audio.play().catch(e => {
      console.error("播放失败，请检查文件名是否正确:", e)
    })
  }
  isPlaying.value = !isPlaying.value
}

// 下一首
const nextTrack = () => {
  currentTrackIndex.value = (currentTrackIndex.value + 1) % playlist.length
  audio.src = playlist[currentTrackIndex.value].url
  // 如果当前是播放状态，切歌后自动播放
  if (isPlaying.value) {
    audio.play()
  } else {
    // 如果原本是暂停，切歌后也保持暂停，但 UI 状态要对
    isPlaying.value = false 
  }
}

// 自动切歌：当歌曲播放结束
audio.onended = () => {
  nextTrack()
}

onUnmounted(() => {
  audio.pause()
})
</script>

<template>
  <div class="blog-container">
    
    <div class="fixed-nav">
      <div class="nav-content">
        <div class="logo-wrapper">
          <div class="logo" @click="router.push('/')">yu翔's Blog</div>
        </div>
        
        <div class="nav-links-wrapper">
          <template v-for="link in navLinks" :key="link.name">
            <el-button text class="nav-link" @click="router.push(link.path)">
              <el-icon><component :is="link.icon" /></el-icon> {{ link.name }}
            </el-button>
          </template>
        </div>
        
       <div class="func-icons-wrapper">
  <div class="music-capsule-wrapper" v-if="playlist.length > 0">
    <div class="music-capsule" @click="togglePlay">
      <div class="music-cover">
        <img :src="playlist[currentTrackIndex]?.cover" :class="{ 'rotating': isPlaying }" />
      </div>
      <span class="music-name">{{ playlist[currentTrackIndex]?.name || '未播放' }}</span>
    </div>

    <div class="music-controls">
      <el-icon class="control-icon" @click="togglePlay">
        <component :is="isPlaying ? VideoPause : VideoPlay" />
      </el-icon>
      <el-icon class="control-icon" @click="nextTrack">
        <DArrowRight />
      </el-icon>
    </div>
  </div>

  <span class="func-icon"><el-icon><Moon /></el-icon></span>
  <span class="func-icon search"><el-icon><Search /></el-icon></span>
</div>
      </div>
    </div>

    <div class="hero-banner">
      <div class="banner-content">
        <h1 class="banner-title">yu翔</h1>
        <p class="banner-subtitle">随便坐坐，看看我写的字 —— 些许技术、心得、生活日常和胡思乱想。</p>
      </div>
    </div>

    <div class="main-content">
      <el-row :gutter="25" justify="center">
        <el-col :xs="24" :sm="24" :md="16" :lg="15">
          <div class="article-list">
            <el-card 
                v-for="article in articleList" 
                :key="article.id" 
                class="article-card" 
                shadow="hover"
                :body-style="{ padding: '0px' }"
                @click="goToDetail(article.id)" 
                >
              <div class="card-inner">
                <div class="article-cover">
                  <img :src="article.cover" alt="cover" />
                </div>
                <div class="article-info">
                  <div class="info-top">
                    <div class="meta">
                      <el-tag size="small" type="primary" effect="light">{{ article.category }}</el-tag>
                      <span class="date"><el-icon><Calendar /></el-icon> {{ article.createTime }}</span>
                    </div>
                    <h2 class="title">{{ article.title }}</h2>
                    <p class="summary">{{ article.summary }}</p>
                  </div>
                  <div class="stats">
                    <span><el-icon><View /></el-icon> {{ article.views }} 阅读</span>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="8" :lg="6" class="sidebar">
          <el-card shadow="hover" class="author-card" :body-style="{ padding: '30px 20px' }">
            <div class="author-info">
              <el-avatar :size="85" src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" />
              <h3>yu翔</h3>
              <p>初出茅庐 | 科班码农 | 拾枝者</p>
              <div class="author-stats">
                <div class="stat-item"><span>12</span>文章</div>
                <div class="stat-item"><span>3</span>分类</div>
                <div class="stat-item"><span>15</span>标签</div>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="site-stats-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><DataLine /></el-icon> 站点统计</span>
              </div>
            </template>
            <div class="stat-row"><span>在线访客</span><span>57</span></div>
            <div class="stat-row"><span>今日浏览</span><span>18</span></div>
            <div class="stat-row"><span>总访问量</span><span>5755</span></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
/* 此处保持你原有的 CSS 不变，已省略... */
/* ==================================================
   侧边栏样式：个人信息卡片
   ================================================== */
.author-info {
  text-align: center; /* 核心：让头像和文字居中 */
}

.author-info h3 {
  margin: 15px 0 8px 0;
  font-size: 1.3rem;
  color: #333;
}

.author-info p {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 25px;
}

/* 关键：让 12文章、3分类 变成横向排列 */
.author-stats {
  display: flex;
  justify-content: space-between;
  text-align: center;
  padding: 0 10px;
}

.stat-item span {
  display: block; /* 让数字占一行 */
  font-size: 1.3rem;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 5px;
}

.stat-item {
  color: #888;
  font-size: 0.85rem;
}

/* ==================================================
   侧边栏样式：站点统计
   ================================================== */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  color: #333;
}

/* 关键：让“在线访客”和“数字”分居左右 */
.stat-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  color: #555;
  font-size: 0.9rem;
}

.stat-row:last-child {
  margin-bottom: 0;
}
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
.blog-container { background-color: #f4f5f7; min-height: 100vh; padding-bottom: 50px; }
.fixed-nav { position: fixed; top: 0; left: 0; width: 100%; height: 65px; z-index: 999; background: rgba(40, 40, 40, 0.4); backdrop-filter: saturate(180%) blur(20px); box-shadow: 0 1px 8px rgba(0, 0, 0, 0.1); color: white; }
.nav-content { max-width: 1400px; margin: 0 auto; height: 100%; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; }
.logo { font-size: 1.15rem; font-weight: bold; cursor: pointer; }
.nav-links-wrapper { flex: 1; display: flex; justify-content: center; gap: 5px; }
.nav-link { color: white; }
.func-icons-wrapper { display: flex; align-items: center; gap: 18px; }
.func-icon { cursor: pointer; }
.func-icon.status { display: flex; align-items: center; gap: 6px; font-size: 0.85rem; background-color: rgba(255,255,255,0.15); padding: 5px 12px; border-radius: 15px; }
.hero-banner { height: 400px; padding-top: 65px; background: linear-gradient(rgba(0,0,0,0.5), rgba(0,0,0,0.3)), url('https://img2.baidu.com/it/u=3284921046,1018788791&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800') center/cover; display: flex; flex-direction: column; justify-content: center; align-items: center; color: white; text-align: center; }
.banner-title { font-size: 3.5rem; font-weight: 800; }
.main-content { max-width: 1300px; margin: 0 auto; padding: 40px 20px; }
.article-list { display: flex; flex-direction: column; gap: 25px; }
.article-card { border-radius: 12px; overflow: hidden; }
.card-inner { display: flex; min-height: 220px; }
.article-cover { width: 42%; position: relative; }
.article-cover img { width: 100%; height: 100%; object-fit: cover; }
.article-info { width: 58%; padding: 30px; display: flex; flex-direction: column; justify-content: space-between; }
.title { font-size: 1.4rem; font-weight: bold; }
.sidebar { display: flex; flex-direction: column; gap: 25px; }

</style>
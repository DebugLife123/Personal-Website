<template>
  <div class="global-nav">
    <div class="nav-content">
      <div class="logo" @click="router.push('/')">yu翔</div>

      <div class="nav-links">
        <div
          v-for="link in navLinks"
          :key="link.path"
          class="nav-link"
          :class="{ active: $route.path === link.path }"
          @click="router.push(link.path)"
        >
          <el-icon><component :is="link.icon" /></el-icon>
          <span>{{ link.name }}</span>
        </div>
      </div>

      <div class="nav-right">
        <div class="music-capsule-wrapper" v-if="playlist.length">
          <div class="music-capsule" @click="togglePlay">
            <div class="music-cover">
              <img v-if="currentTrack?.cover" :src="currentTrack.cover" :class="{ rotating: isPlaying }" />
              <div v-else class="default-cover"><el-icon :size="16"><Headset /></el-icon></div>
            </div>
            <span class="music-name">{{ currentTrack?.name || '未播放' }}</span>
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

        <el-button v-if="isAdmin" class="admin-btn" text size="small" @click="router.push('/admin')">管理后台</el-button>
        <el-icon class="func-icon" @click="handleThemeToggle">
          <component :is="isDarkMode ? Sunny : Moon" />
        </el-icon>
        <el-button v-if="isLoggedIn" class="logout-btn" text size="small" @click="handleLogout">退出</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  HomeFilled, Notebook, Document, Box, ChatLineRound,
  VideoPlay, VideoPause, DArrowRight, Moon, Sunny, Headset
} from '@element-plus/icons-vue'
import musicPlayer from '../utils/musicPlayer'
import request from '../utils/request'
import { isLoggedIn, isAdmin, logout } from '../utils/auth'

const props = defineProps({
  isDarkMode: Boolean
})
const emit = defineEmits(['toggle-theme'])
const router = useRouter()

const navLinks = [
  { name: '主页', icon: HomeFilled, path: '/' },
  { name: '博客', icon: Notebook, path: '/blog' },
  { name: '简历', icon: Document, path: '/resume' },
  { name: '项目', icon: Box, path: '/projects' },
  { name: '留言', icon: ChatLineRound, path: '/messages' },
]

const isPlaying = ref(false)
const currentTrack = ref(null)
const playlist = ref([])

const handleThemeToggle = () => {
  emit('toggle-theme')
}

const togglePlay = () => {
  isPlaying.value = musicPlayer.togglePlay()
}

const nextTrack = () => {
  currentTrack.value = musicPlayer.nextTrack()
  isPlaying.value = musicPlayer.getIsPlaying()
}

const handleLogout = () => {
  logout()
  router.push('/login')
}

const fetchPlaylist = async () => {
  try {
    const res = await request.get('/music/enabled')
    if (res.data.code === 200) {
      const tracks = (res.data.data || []).map(t => ({
        name: t.artist ? `${t.name} - ${t.artist}` : t.name,
        url: t.url,
        cover: t.cover || '',
      }))
      playlist.value = tracks
      if (tracks.length > 0) {
        musicPlayer.init(tracks)
        currentTrack.value = musicPlayer.getCurrentTrack()
        isPlaying.value = musicPlayer.getIsPlaying()
      }
    }
  } catch (e) {
    console.error('获取音乐列表失败', e)
  }
}

onMounted(() => {
  fetchPlaylist()
})
</script>

<style scoped>
.global-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 65px;
  z-index: 999;
  background: rgba(40, 40, 40, 0.4);
  backdrop-filter: saturate(180%) blur(20px);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.1);
  color: white;
}

.nav-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.logo {
  font-size: 1.15rem;
  font-weight: bold;
  cursor: pointer;
  flex-shrink: 0;
}

.nav-links {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 5px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 0.95rem;
  border-radius: 8px;
  transition: background 0.2s;
  color: rgba(255, 255, 255, 0.85);
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-link.active {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-shrink: 0;
}

.func-icon {
  cursor: pointer;
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.85);
  transition: color 0.2s, transform 0.3s ease;
}

.func-icon:hover {
  color: white;
  transform: rotate(30deg);
}

.logout-btn {
  color: rgba(255, 255, 255, 0.6) !important;
  font-size: 0.82rem;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  border-radius: 8px;
  padding: 4px 14px;
  transition: all 0.2s;
}
.logout-btn:hover {
  color: white !important;
  border-color: rgba(255, 255, 255, 0.35) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

.admin-btn {
  color: rgba(255, 255, 255, 0.85) !important;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
}

/* Music capsule */
.music-capsule-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.music-capsule {
  display: flex;
  align-items: center;
  padding: 4px 16px 4px 4px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}

.music-capsule:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.02);
}

.music-cover {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  border: 1.5px solid rgba(255, 255, 255, 0.5);
  margin-right: 10px;
  flex-shrink: 0;
}

.music-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.default-cover {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #d4cce6, #b8a8d4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.rotating {
  animation: rotate 8s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.music-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: white;
  letter-spacing: 0.5px;
  white-space: nowrap;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.music-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-icon {
  font-size: 1.3rem;
  color: white;
  cursor: pointer;
  transition: opacity 0.2s;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.control-icon:hover {
  opacity: 0.8;
}

</style>

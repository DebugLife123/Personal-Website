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

        <!-- 登录用户：头像 + 下拉 -->
        <div v-if="isLoggedIn && !isAdmin" class="user-chip" @click="toggleUserMenu" ref="userChipRef">
          <div class="user-chip-avatar">
            <img v-if="userAvatar" :src="userAvatar" />
            <div v-else class="user-chip-initial">{{ userInitial }}</div>
          </div>
          <span class="user-chip-name">{{ displayName }}</span>
          <el-icon class="user-chip-caret"><ArrowDown /></el-icon>
          <transition name="menu-fade">
            <div v-if="userMenuOpen" class="user-menu" @click.stop>
              <div class="user-menu-item" @click="goProfile">
                <el-icon><User /></el-icon>
                <span>我的资料</span>
              </div>
              <div class="user-menu-divider"></div>
              <div class="user-menu-item danger" @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </div>
            </div>
          </transition>
        </div>

        <!-- 管理员直接退出 -->
        <el-button v-else-if="isAdmin" class="logout-btn" text size="small" @click="handleLogout">退出</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import {
  HomeFilled, Notebook, Document, Box, ChatLineRound,
  VideoPlay, VideoPause, DArrowRight, Moon, Sunny, Headset,
  ArrowDown, User, SwitchButton, Picture
} from '@element-plus/icons-vue'
import musicPlayer from '../utils/musicPlayer'
import request from '../utils/request'
import { mediaUrl } from '../utils/api'
import { isLoggedIn, isAdmin, currentUser, logout } from '../utils/auth'
import { avatarUrl } from '../utils/avatar'

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
  { name: '图库', icon: Picture, path: '/gallery' },
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
  userMenuOpen.value = false
  router.push('/login')
}

// 用户菜单
const userMenuOpen = ref(false)
const userChipRef = ref(null)
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '')
const userAvatar = computed(() => avatarUrl(currentUser.value?.avatar))
const userInitial = computed(() => (displayName.value || 'U').charAt(0).toUpperCase())
const toggleUserMenu = () => { userMenuOpen.value = !userMenuOpen.value }
const goProfile = () => { userMenuOpen.value = false; router.push('/messages') }

const onClickOutside = (e) => {
  if (userChipRef.value && !userChipRef.value.contains(e.target)) {
    userMenuOpen.value = false
  }
}

const fetchPlaylist = async () => {
  try {
    const res = await request.get('/music/enabled')
    if (res.data.code === 200) {
      const tracks = (res.data.data || []).map(t => ({
        name: t.artist ? `${t.name} - ${t.artist}` : t.name,
        // mediaUrl：兼容历史数据里写死的本机地址（如 http://localhost:8080/uploads/...）
        url: mediaUrl(t.url),
        cover: mediaUrl(t.cover) || '',
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
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
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

/* 用户芯片 + 下拉 */
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(6px);
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  user-select: none;
}
.user-chip:hover { background: rgba(255, 255, 255, 0.16); }
.user-chip-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.user-chip-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.user-chip-initial {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 700;
}
.user-chip-name {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.9);
  max-width: 88px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-chip-caret { font-size: 0.7rem; color: rgba(255, 255, 255, 0.5); }

.user-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 148px;
  background: rgba(28, 28, 35, 0.95);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
  z-index: 1000;
}
.user-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.user-menu-item:hover { background: rgba(255, 255, 255, 0.08); color: white; }
.user-menu-item.danger { color: #ff8a8a; }
.user-menu-item.danger:hover { background: rgba(255, 138, 138, 0.1); color: #ffb3b3; }
.user-menu-divider { height: 1px; background: rgba(255, 255, 255, 0.06); margin: 4px 0; }
.menu-fade-enter-active, .menu-fade-leave-active { transition: opacity 0.18s, transform 0.18s; }
.menu-fade-enter-from, .menu-fade-leave-to { opacity: 0; transform: translateY(-6px); }

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

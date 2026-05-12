// 音乐播放器管理工具

class MusicPlayer {
  constructor() {
    this.audio = null
    this.playlist = []
    this.currentTrackIndex = 0
    this.isPlaying = false
  }

  init(playlist) {
    // 彻底停止之前的播放，防止新旧 Audio 同时播放
    this.destroy()

    this.playlist = playlist
    if (playlist.length > 0) {
      this.audio = new Audio(playlist[0].url)
      this.setupEventListeners()
    }
  }

  setupEventListeners() {
    if (this.audio) {
      this.audio.onended = () => {
        this.nextTrack()
      }

      this.audio.onerror = (error) => {
        console.error('音频播放错误:', error)
      }
    }
  }

  togglePlay() {
    if (!this.audio) return

    if (this.isPlaying) {
      this.audio.pause()
    } else {
      this.audio.play().catch(e => {
        console.error('播放失败:', e)
      })
    }
    this.isPlaying = !this.isPlaying
    return this.isPlaying
  }

  nextTrack() {
    if (!this.audio || this.playlist.length === 0) return

    // 先停止当前播放，彻底卸载当前音频
    this.audio.pause()
    this.audio.removeAttribute('src')
    this.audio.load()

    // 切换到下一首
    this.currentTrackIndex = (this.currentTrackIndex + 1) % this.playlist.length

    // 设置新音频源
    this.audio.src = this.playlist[this.currentTrackIndex].url
    this.audio.load()

    // 如果当前是播放状态，切歌后自动播放
    if (this.isPlaying) {
      this.audio.play().catch(e => {
        console.error('播放失败:', e)
      })
    }

    return this.playlist[this.currentTrackIndex]
  }

  getCurrentTrack() {
    if (this.playlist.length === 0) return null
    return this.playlist[this.currentTrackIndex]
  }

  getIsPlaying() {
    return this.isPlaying
  }

  pause() {
    if (this.audio && this.isPlaying) {
      this.audio.pause()
      this.isPlaying = false
    }
  }

  play() {
    if (this.audio && !this.isPlaying) {
      this.audio.play().catch(e => {
        console.error('播放失败:', e)
      })
      this.isPlaying = true
    }
  }

  // 彻底销毁 Audio 实例，阻止新旧实例同时播放
  destroy() {
    if (this.audio) {
      this.audio.pause()
      this.audio.removeAttribute('src')
      this.audio.load()
      this.audio.onended = null
      this.audio.onerror = null
      this.audio = null
    }
    this.isPlaying = false
    this.currentTrackIndex = 0
  }
}

// 导出单例
const musicPlayer = new MusicPlayer()
export default musicPlayer

<template>
  <div class="home-wrapper">
    <div class="main-card">
      
      <div class="avatar-section">
        <el-avatar :size="150" src="https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500" border />
      </div>

      <div class="info-section">
        <h1 class="nickname">yu翔</h1>
        <p class="motto">
          初出茅庐 <span>|</span> 科班码农 <span>|</span> 拾枝者 · 欢迎来到我的主页
        </p>
      </div>

      <div class="social-grid">
        <el-button class="social-btn" icon="Edit" @click="$router.push('/blog')">博客</el-button>
        <el-button class="social-btn" icon="Document" @click="$router.push('/resume')">简历</el-button>
        <el-button class="social-btn" icon="Message">邮箱</el-button>
       <el-button class="social-btn" @click="openExternal('https://github.com/DebugLife123')">
    <template #icon>
      <img src="https://api.iconify.design/mdi:github.svg" width="18" height="18" />
    </template>
    GitHub
  </el-button>
       <el-button class="social-btn" @click="openExternal('https://space.bilibili.com/3494370628078296?spm_id_from=333.1007.0.0')">
    <template #icon>
      <img src="https://api.iconify.design/tabler:brand-bilibili.svg" width="18" height="18" />
    </template>
    Bilibili
  </el-button>
       <el-button class="social-btn" @click="openExternal('https://leetcode.cn/u/festive-lichtermaninv/')">
    <template #icon>
      <img src="https://api.iconify.design/simple-icons:leetcode.svg" width="18" height="18" />
    </template>
    LeetCode
  </el-button>
      </div>

      <div class="card-footer">
        <p>豫公网安备XXXXXXXX号 | 豫ICP备XXXXXXXX号-1 | © 2025-2026 yu翔.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// 打开外部链接的方法
const openExternal = (url) => {
  window.open(url, '_blank');
}
</script>

<style scoped>
.home-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f8f9fa;
  padding: 20px;
  box-sizing: border-box; /* 防止边距撑破全屏 */
}

.main-card {
  background: white;
  width: 100%;
  max-width: 900px;
  padding: 60px 40px 20px 40px;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  text-align: center;
}

.avatar-section { margin-bottom: 30px; }
.el-avatar { border: 4px solid #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.nickname { font-size: 3rem; font-weight: 800; margin-bottom: 15px; color: #1a1a1a; }
.motto { color: #666; font-size: 1.1rem; margin-bottom: 40px; }
.motto span { color: #ddd; margin: 0 10px; }

.social-grid { display: flex; justify-content: center; flex-wrap: wrap; gap: 15px; margin-bottom: 60px; }
.social-btn { padding: 20px 30px; border-radius: 12px; font-size: 1rem; transition: all 0.3s; }
.social-btn:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.card-footer { font-size: 0.8rem; color: #999; border-top: 1px solid #f0f0f0; padding-top: 20px; }

@media (max-width: 768px) {
  .nickname { font-size: 2rem; }
  .social-grid { display: grid; grid-template-columns: 1fr 1fr; }
  .main-card { padding: 40px 20px; }
}
</style>
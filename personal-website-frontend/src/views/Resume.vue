<script setup>
import { useRouter } from 'vue-router'

// 获取路由实例，用于返回首页
const router = useRouter()

const goBack = () => {
  router.push('/')
}
</script>

<template>
  <div class="resume-wrapper">
    
    <div class="top-bar">
      <el-button icon="ArrowLeft" text @click="goBack">返回首页</el-button>
    </div>

    <el-row justify="center">
      <el-col :xs="24" :sm="22" :md="18" :lg="14">
        
        <div class="resume-header">
          <h1 class="name">yu翔</h1>
          <div class="contact-bar">
            <el-button round size="small" icon="Message">kiuwoba@qq.com</el-button>
            <el-button round size="small" icon="User">yu翔</el-button>
            <el-button round size="small" icon="Link">yu翔.cc</el-button>
          </div>
          <el-divider />
        </div>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><School /></el-icon> 教育经历</h3>
          <el-timeline>
            <el-timeline-item placement="top" :hollow="true">
              <el-card shadow="never" class="content-card">
                <div class="card-header">
                  <div class="org-info">
                    <el-avatar :size="40" src="https://img1.baidu.com/it/u=460186796,737045498&fm=253&app=120&f=JPEG?w=814&h=800" />
                    <div class="text">
                      <h4>南华大学</h4>
                      <span>软件工程</span>
                    </div>
                  </div>
                  <span class="date">2023.09 ~ 至今</span>
                </div>
                <p class="desc">大三在读</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Briefcase /></el-icon> 实习 & 工作经历</h3>
          <el-timeline>
            <el-timeline-item placement="top" :hollow="true">
              <el-card shadow="never" class="content-card">
                <div class="card-header">
                  <h4>西安青砧智果科技有限公司</h4>
                  <span class="date">2026.01 ~ 2026.02</span>
                </div>
                <div class="sub-title">算法工程师助理</div>
                <p class="desc">算法开发与优化</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Files /></el-icon> 项目经历</h3>
          <el-timeline>
            <el-timeline-item placement="top" :hollow="true">
              <el-card shadow="never" class="content-card">
                <div class="card-header">
                  <h4>七锡源集</h4>
                  <span class="date">2025.12 ~ 2026.01</span>
                </div>
                <div class="sub-title">任务看板系统</div>
                <p class="desc">基于Vue3 + SpringBoot的轻量级任务管理看板系统</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Tools /></el-icon> 技术栈</h3>
          <div class="skill-list">
            <el-card shadow="never" class="skill-card">
              <div class="skill-info">
                <img src="https://img.icons8.com/color/48/000000/javascript--v1.png" class="tech-logo" />
                <div class="text">
                  <h4>JavaScript</h4>
                  <p>熟练使用js，掌握es6+</p>
                </div>
              </div>
            </el-card>
            
            <el-card shadow="never" class="skill-card">
              <div class="skill-info">
                <img src="https://img.icons8.com/color/48/000000/vue-js.png" class="tech-logo" />
                <div class="text">
                  <h4>Vue</h4>
                  <p>渐进式js框架，熟练使用</p>
                </div>
              </div>
            </el-card>
          </div>
        </section>

      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.resume-wrapper { padding: 15px 20px; background-color: #fff; min-height: 100vh; }
.top-bar { margin-bottom: 20px; }

/* Header 样式 */
.resume-header { text-align: center; margin-bottom: 40px; }
.name { font-size: 2.5rem; font-weight: 800; margin-bottom: 20px; color: #1a1a1a; }
.contact-bar { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; }

/* 章节标题 */
.section-title { display: flex; align-items: center; gap: 10px; font-size: 1.2rem; margin-bottom: 20px; color: #333; }

/* 卡片样式优化 */
.content-card { border-radius: 12px; border: 1px solid #f0f0f0; background-color: #fafafa; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.org-info { display: flex; align-items: center; gap: 15px; }
.org-info h4 { margin: 0; font-size: 1.1rem; color: #333; }
.date { color: #999; font-size: 0.9rem; }
.sub-title { font-weight: bold; margin-bottom: 5px; color: #333; font-size: 0.95rem; }
.desc { color: #666; font-size: 0.95rem; margin: 0; }

/* 技术栈专属样式 */
.skill-list { display: flex; flex-direction: column; gap: 15px; }
.skill-card { border-radius: 12px; border: 1px solid #f0f0f0; }
.skill-info { display: flex; align-items: center; gap: 20px; }
.tech-logo { width: 48px; height: 48px; object-fit: contain; }
.skill-info h4 { margin: 0 0 5px 0; font-size: 1.1rem; color: #333; }
.skill-info p { margin: 0; color: #666; font-size: 0.9rem; }

/* 响应式调整 */
@media (max-width: 768px) {
  .card-header { flex-direction: column; gap: 5px; }
}
</style>
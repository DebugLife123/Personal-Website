<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { Calendar, View, ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 1. 响应式数据定义
const article = ref({
  title: '加载中...',
  content: '',
  cover: '',
  createTime: '',
  views: 0
})

const titles = ref([]) // 存储提取出来的目录索引

// 2. 获取文章详情数据
const fetchArticle = async () => {
  const id = route.params.id
  try {
    const res = await request.get(`/article/get?id=${id}`)
    if (res.data.code === 200) {
      article.value = res.data.data
      
      // 数据加载后，等待 DOM 渲染完成再提取目录
      nextTick(() => {
        extractTitles()
      })
    }
  } catch (error) {
    console.error("获取文章详情失败:", error)
  }
}

// 3. 提取 Markdown 中的标题生成目录
const extractTitles = () => {
  // 选择 markdown 预览区域内的所有标题标签 (h1, h2, h3)
  const anchors = document.querySelectorAll('.v-md-editor-preview h1, .v-md-editor-preview h2, .v-md-editor-preview h3')
  
  // 将 NodeList 转为数组并过滤掉空标题
  const _titles = Array.from(anchors).filter((title) => !!title.innerText.trim())
  
  if (!_titles.length) {
    titles.value = []
    return
  }

  // 映射为目录对象：包含标题文本、行索引（用于跳转）、缩进层级
  titles.value = _titles.map((el) => ({
    title: el.innerText,
    lineIndex: el.getAttribute('data-v-md-line'), // v-md-editor 特有的属性
    indent: parseInt(el.tagName.substring(1))    // 根据 h1/h2/h3 确定缩进
  }))
}

// 4. 目录点击跳转函数
const handleAnchorClick = (anchor) => {
  const { lineIndex } = anchor
  // 根据 lineIndex 找到正文中对应的 DOM 元素
  const heading = document.querySelector(`.v-md-editor-preview [data-v-md-line="${lineIndex}"]`)
  if (heading) {
    // 平滑滚动到目标位置
    heading.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(() => {
  fetchArticle()
  window.scrollTo(0, 0) // 切换页面时自动回到顶部
})
</script>

<template>
  <div class="article-detail-container">
    
    <div 
      class="article-banner" 
      :style="{ backgroundImage: `url(${article.cover || 'https://via.placeholder.com/1920x600'})` }"
    >
      <div class="banner-mask">
        <el-button class="back-btn" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>

        <div class="header-content">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span class="meta-item">
              <el-icon><Calendar /></el-icon> {{ article.createTime }}
            </span>
            <el-divider direction="vertical" />
            <span class="meta-item">
              <el-icon><View /></el-icon> {{ article.views }} 阅读
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="main-wrapper">
      <el-row :gutter="30" justify="center">
        
        <el-col :xs="24" :sm="24" :md="17" :lg="14">
          <el-card class="content-card" shadow="never">
            <v-md-preview :text="article.content"></v-md-preview>
            
            <el-divider />
            <div class="article-footer">
              <p>© 著作权归作者所有，转载请联系作者</p>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="0" :sm="0" :md="6" :lg="5">
          <div class="sticky-sidebar">
            <el-card class="toc-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <strong>文章目录</strong>
                </div>
              </template>
              
              <div class="toc-list" v-if="titles.length > 0">
                <div 
                  v-for="anchor in titles" 
                  :key="anchor.title" 
                  class="toc-item"
                  :style="{ paddingLeft: `${(anchor.indent - 1) * 15}px` }"
                  @click="handleAnchorClick(anchor)"
                >
                  {{ anchor.title }}
                </div>
              </div>
              
              <el-empty v-else description="暂无目录" :image-size="60" />
            </el-card>
          </div>
        </el-col>

      </el-row>
    </div>
  </div>
</template>

<style scoped>
.article-detail-container {
  background-color: #f4f5f7;
  min-height: 100vh;
}

/* --- Banner 区域样式 --- */
.article-banner {
  height: 450px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.banner-mask {
  height: 100%;
  width: 100%;
  background: rgba(0, 0, 0, 0.45); /* 黑色半透明遮罩 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  padding: 0 20px;
}

.back-btn {
  position: absolute;
  top: 30px;
  left: 40px;
  background-color: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(5px);
}

.header-content { text-align: center; max-width: 850px; }
.title { font-size: 2.6rem; margin-bottom: 20px; font-weight: 800; text-shadow: 0 2px 10px rgba(0,0,0,0.3); }
.meta { display: flex; justify-content: center; align-items: center; gap: 15px; opacity: 0.9; }
.meta-item { display: flex; align-items: center; gap: 6px; }

/* --- 内容区域布局 --- */
.main-wrapper {
  max-width: 1400px;
  margin: -60px auto 0; /* 负边距让卡片“浮”在 Banner 上 */
  padding: 0 20px 60px;
  position: relative;
  z-index: 10;
}

.content-card {
  border-radius: 12px;
  border: none;
  padding: 10px 20px;
}

/* --- 侧边栏 & 目录样式 --- */
.sticky-sidebar {
  position: sticky;
  top: 90px; /* 避开可能存在的顶部导航栏 */
}

.toc-card { border-radius: 12px; border: none; }
.card-header { font-size: 1.1rem; color: #333; }

.toc-list { max-height: 70vh; overflow-y: auto; }

.toc-item {
  padding: 10px 0;
  color: #666;
  cursor: pointer;
  font-size: 0.92rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s;
}

.toc-item:hover {
  color: #409EFF;
  transform: translateX(5px); /* 悬浮时向右微调 */
}

.article-footer {
  text-align: center;
  color: #999;
  font-size: 0.85rem;
  padding: 30px 0;
}

/* --- 移动端自适应 --- */
@media (max-width: 768px) {
  .article-banner { height: 320px; }
  .title { font-size: 1.6rem; }
  .main-wrapper { margin-top: -30px; padding: 0 10px 30px; }
  .content-card { padding: 5px; }
  .back-btn { top: 20px; left: 20px; padding: 8px 12px; }
}
</style>
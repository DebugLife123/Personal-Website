<script setup>
import { useRouter } from 'vue-router'
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { getTodayStatistic, getTotalStatistic } from '../utils/statistic'
import {
  Calendar, View, Search, DataLine, CollectionTag, Folder, Delete, Edit
} from '@element-plus/icons-vue'
import { isAdmin } from '../utils/auth'

const router = useRouter()

// 文章列表数据
const articleList = ref([])
const categories = ref([])
const tags = ref([])
const currentCategory = ref('')
const currentTag = ref('')

// 发布对话框
const dialogVisible = ref(false)
const submitting = ref(false)

const articleForm = reactive({
  title: '',
  category: '',
  tags: '',
  cover: '',
  summary: '',
  content: ''
})

const resetForm = () => {
  articleForm.title = ''
  articleForm.category = ''
  articleForm.tags = ''
  articleForm.cover = ''
  articleForm.summary = ''
  articleForm.content = ''
}

// 提交文章
const submitArticle = async () => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!articleForm.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  submitting.value = true
  try {
    const res = await request.post('/article/add', articleForm)
    if (res.data.code === 200) {
      ElMessage.success('文章发布成功！')
      dialogVisible.value = false
      resetForm()
      await fetchArticles('/article/list')
      await fetchCategories()
      await fetchTags()
    } else {
      ElMessage.error(res.data.message || '发布失败')
    }
  } catch (error) {
    ElMessage.error('发布失败，请检查网络')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

// 删除文章
const deleteArticle = async (id, title) => {
  try {
    await ElMessageBox.confirm(`确定要删除《${title}》吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await request.delete(`/article/delete/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('文章已删除')
      await fetchArticles('/article/list')
      await fetchCategories()
      await fetchTags()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    // 用户取消删除，不做处理
  }
}

// 获取文章列表
const fetchArticles = async (url) => {
  try {
    const res = await request.get(url)
    if (res.data.code === 200) {
      articleList.value = res.data.data
    }
  } catch (error) {
    console.error("获取文章列表失败:", error)
  }
}

// 获取已发布的文章列表（供前端展示）
const fetchPublishedArticles = async () => {
  try {
    // 使用分页接口，只获取已发布的文章（设置大 pageSize 获取全部）
    const res = await request.get('/article/page', {
      params: { page: 1, pageSize: 999, status: '已发布' }
    })
    if (res.data.code === 200) {
      articleList.value = res.data.data.records || []
    }
  } catch (error) {
    console.error("获取文章列表失败:", error)
  }
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await request.get('/article/categories')
    if (res.data.code === 200) {
      categories.value = res.data.data
    }
  } catch (error) {
    console.error("获取分类列表失败:", error)
  }
}

// 获取标签列表
const fetchTags = async () => {
  try {
    const res = await request.get('/article/tags')
    if (res.data.code === 200) {
      tags.value = res.data.data
    }
  } catch (error) {
    console.error("获取标签列表失败:", error)
  }
}

// 用 page 接口查询已发布文章
const queryPublished = async (params = {}) => {
  const res = await request.get('/article/page', {
    params: { page: 1, pageSize: 999, status: '已发布', ...params }
  })
  if (res.data.code === 200) {
    articleList.value = res.data.data.records || []
  }
}

// 按分类筛选
const filterByCategory = async (category) => {
  currentCategory.value = category
  currentTag.value = ''
  searchKeyword.value = ''
  await queryPublished({ category })
}

// 按标签筛选
const filterByTag = async (tag) => {
  currentTag.value = tag
  currentCategory.value = ''
  searchKeyword.value = ''
  await queryPublished({ keyword: tag })
}

// 重置筛选
const resetFilter = async () => {
  currentCategory.value = ''
  currentTag.value = ''
  searchKeyword.value = ''
  await queryPublished()
}

// 搜索功能
const searchKeyword = ref('')
const isSearching = ref(false)

const searchArticles = async () => {
  isSearching.value = true
  try {
    if (searchKeyword.value.trim()) {
      await queryPublished({ keyword: searchKeyword.value })
    } else {
      await queryPublished()
    }
  } finally {
    isSearching.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    fetchPublishedArticles(),
    fetchCategories(),
    fetchTags(),
    fetchSiteStats()
  ])
})

// 站点统计数据
const todayStats = ref({ pageViews: 0, uniqueVisitors: 0, articleReads: 0 })
const totalStats = ref({ pageViews: 0, uniqueVisitors: 0, articleReads: 0 })

const fetchSiteStats = async () => {
  const today = await getTodayStatistic()
  const total = await getTotalStatistic()
  if (today) todayStats.value = today
  if (total) totalStats.value = total
}

const goToDetail = (id) => {
  router.push(`/article/${id}`)
}
</script>

<template>
  <div class="blog-container">

    <div class="hero-banner">
      <div class="banner-content">
        <h1 class="banner-title">yu翔</h1>
        <p class="banner-subtitle">随便坐坐，看看我写的字 —— 些许技术、心得、生活日常和胡思乱想。</p>
      </div>
    </div>

    <!-- 操作栏：搜索 + 写文章 -->
    <div class="toolbar">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文章..."
          :prefix-icon="Search"
          :loading="isSearching"
          clearable
          @clear="resetFilter"
          @keyup.enter="searchArticles"
        />
        <el-button type="primary" @click="searchArticles" :loading="isSearching">搜索</el-button>
      </div>
      <el-button v-if="isAdmin" type="success" :icon="Edit" @click="dialogVisible = true">写文章</el-button>
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
                >
              <div class="card-inner" @click="goToDetail(article.id)">
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
              <div class="card-actions" v-if="isAdmin">
                <el-tooltip content="删除文章" placement="top">
                  <el-button
                    class="delete-btn"
                    size="small"
                    :icon="Delete"
                    circle
                    @click.stop="deleteArticle(article.id, article.title)"
                  />
                </el-tooltip>
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
                <div class="stat-item"><span>{{ articleList.length }}</span>文章</div>
                <div class="stat-item"><span>{{ categories.length }}</span>分类</div>
                <div class="stat-item"><span>{{ tags.length }}</span>标签</div>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="category-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><Folder /></el-icon> 文章分类</span>
                <span class="reset-btn" v-if="currentCategory" @click="resetFilter">重置</span>
              </div>
            </template>
            <div class="category-list">
              <el-tag
                v-for="category in categories"
                :key="category"
                :type="currentCategory === category ? 'primary' : 'info'"
                effect="plain"
                @click="filterByCategory(category)"
                class="category-tag"
              >
                {{ category }}
              </el-tag>
            </div>
          </el-card>

          <el-card shadow="hover" class="tag-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><CollectionTag /></el-icon> 文章标签</span>
                <span class="reset-btn" v-if="currentTag" @click="resetFilter">重置</span>
              </div>
            </template>
            <div class="tag-list">
              <el-tag
                v-for="tag in tags"
                :key="tag"
                :type="currentTag === tag ? 'primary' : 'info'"
                effect="plain"
                @click="filterByTag(tag)"
                class="tag-item"
                size="small"
              >
                {{ tag }}
              </el-tag>
            </div>
          </el-card>

          <el-card shadow="hover" class="site-stats-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><DataLine /></el-icon> 站点统计</span>
              </div>
            </template>
            <div class="stat-row"><span>今日浏览</span><span>{{ todayStats.pageViews }}</span></div>
            <div class="stat-row"><span>今日访客</span><span>{{ todayStats.uniqueVisitors }}</span></div>
            <div class="stat-row"><span>总访问量</span><span>{{ totalStats.pageViews }}</span></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 发布文章对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="写文章"
      width="750px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form label-width="70px" class="article-form">
        <el-form-item label="标题">
          <el-input v-model="articleForm.title" placeholder="文章标题" maxlength="100" show-word-limit />
        </el-form-item>

        <el-row :gutter="15">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-input v-model="articleForm.category" placeholder="如: 前端" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标签">
              <el-input v-model="articleForm.tags" placeholder="逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="封面">
              <el-input v-model="articleForm.cover" placeholder="图片URL" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="摘要">
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            :rows="2"
            placeholder="文章摘要（可选）"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="内容">
          <el-input
            v-model="articleForm.content"
            type="textarea"
            :rows="12"
            placeholder="支持 Markdown 格式"
            class="content-textarea"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitArticle">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.blog-container { background-color: #f4f5f7; min-height: 100vh; padding-bottom: 50px; }

/* 操作栏 */
.toolbar {
  max-width: 1300px;
  margin: 25px auto 0;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.search-bar {
  display: flex;
  gap: 10px;
  flex: 1;
  max-width: 500px;
}

.hero-banner {
  height: 380px;
  padding-top: 65px;
  background: linear-gradient(rgba(0,0,0,0.55), rgba(0,0,0,0.35)), url('https://img2.baidu.com/it/u=3284921046,1018788791&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800') center/cover;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  text-align: center;
  position: relative;
}

.banner-title {
  font-size: 3.2rem;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 10px;
  text-shadow: 0 2px 20px rgba(0,0,0,0.3);
}
.banner-subtitle {
  font-size: 1.05rem;
  opacity: 0.85;
  max-width: 500px;
  line-height: 1.6;
}

.main-content { max-width: 1300px; margin: 0 auto; padding: 40px 20px; }
.article-list { display: flex; flex-direction: column; gap: 25px; }
.article-card {
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: transform 0.35s ease, box-shadow 0.35s ease;
}
.article-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(0,0,0,0.08) !important;
}
.card-inner { display: flex; min-height: 220px; }
.article-cover { width: 42%; position: relative; }
.article-cover img { width: 100%; height: 100%; object-fit: cover; }
.article-info { width: 58%; padding: 30px; display: flex; flex-direction: column; justify-content: space-between; }
.meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.date { font-size: 0.85rem; color: #999; display: flex; align-items: center; gap: 4px; }
.title { font-size: 1.4rem; font-weight: bold; margin: 0 0 10px 0; color: #1a1a1a; }
.summary { color: #666; font-size: 0.95rem; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.stats { color: #999; font-size: 0.85rem; display: flex; align-items: center; gap: 4px; }

/* 删除按钮 - hover 显示 */
.card-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 10;
}

.article-card:hover .card-actions {
  opacity: 1;
}

.delete-btn {
  background: rgba(0, 0, 0, 0.45) !important;
  border: none !important;
  color: white !important;
}

.delete-btn:hover {
  background: rgba(245, 108, 108, 0.8) !important;
}

.sidebar { display: flex; flex-direction: column; gap: 25px; }

/* 作者卡片 */
.author-info { text-align: center; }
.author-info h3 { margin: 15px 0 8px 0; font-size: 1.3rem; color: #333; }
.author-info p { color: #666; font-size: 0.9rem; margin-bottom: 25px; }
.author-stats { display: flex; justify-content: space-between; text-align: center; padding: 0 10px; }
.stat-item span { display: block; font-size: 1.3rem; font-weight: bold; color: #2c3e50; margin-bottom: 5px; }
.stat-item { color: #888; font-size: 0.85rem; }

/* 分类和标签 */
.category-card, .tag-card, .site-stats-card { margin-bottom: 20px; }
.category-list, .tag-list { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 15px; }
.category-tag, .tag-item { cursor: pointer; transition: all 0.3s ease; }
.category-tag:hover, .tag-item:hover { transform: translateY(-2px); }
.reset-btn { font-size: 0.8rem; color: #409eff; cursor: pointer; margin-left: auto; }
.reset-btn:hover { text-decoration: underline; }

.card-header { display: flex; align-items: center; gap: 8px; font-weight: bold; color: #333; }
.stat-row { display: flex; justify-content: space-between; margin-bottom: 15px; color: #555; font-size: 0.9rem; }
.stat-row:last-child { margin-bottom: 0; }

/* 发布文章对话框 */
.article-form .content-textarea :deep(textarea) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .banner-title { font-size: 2rem; }
  .hero-banner { height: 280px; }
  .card-inner { flex-direction: column; }
  .article-cover { width: 100%; height: 200px; }
  .article-info { width: 100%; padding: 20px; }
  .toolbar { flex-direction: column; align-items: stretch; }
  .search-bar { max-width: 100%; }
}

</style>

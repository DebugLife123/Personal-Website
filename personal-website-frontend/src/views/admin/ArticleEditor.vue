<template>
  <div class="article-editor">
    <!-- 顶部操作栏 -->
    <div class="editor-toolbar">
      <el-button text :icon="ArrowLeft" @click="goBack">返回列表</el-button>
      <span class="toolbar-title">{{ isEdit ? '编辑文章' : '新建文章' }}</span>
      <div class="toolbar-actions">
        <el-button @click="saveAsDraft" :loading="saving" :disabled="saving">存为草稿</el-button>
        <el-button type="primary" @click="publish" :loading="publishing" :disabled="publishing">发布</el-button>
      </div>
    </div>

    <!-- 编辑区 -->
    <div class="editor-body">
      <div class="editor-main">
        <el-input
          v-model="form.title"
          placeholder="请输入文章标题..."
          class="title-input"
          maxlength="100"
          show-word-limit
        />

        <div class="meta-row">
          <el-input v-model="form.category" placeholder="分类（如: 前端）" class="meta-input" />
          <el-input v-model="form.tags" placeholder="标签（逗号分隔）" class="meta-input" />
        </div>

        <!-- 封面图上传 -->
        <div class="cover-section">
          <div class="section-label">封面图</div>
          <div class="cover-upload-area">
            <el-upload
              :action="uploadUrl"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              accept="image/*"
              class="cover-uploader"
            >
              <div v-if="form.cover" class="cover-preview">
                <img :src="form.cover" />
                <div class="cover-overlay">
                  <el-icon :size="20"><Refresh /></el-icon>
                  <span>更换图片</span>
                </div>
              </div>
              <div v-else class="cover-placeholder">
                <el-icon :size="28"><Plus /></el-icon>
                <span>点击上传封面图</span>
              </div>
            </el-upload>
            <div class="cover-url-input">
              <span class="cover-url-label">或输入图片URL：</span>
              <el-input v-model="form.cover" placeholder="https://..." clearable />
            </div>
          </div>
        </div>

        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="2"
          placeholder="文章摘要（可选）"
          maxlength="300"
          show-word-limit
          class="summary-input"
        />

        <div class="section-label">文章内容</div>
        <MarkdownEditor ref="mdEditorRef" v-model="form.content" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import MarkdownEditor from '../../components/MarkdownEditor.vue'
import { ArrowLeft, Plus, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const uploadUrl = 'http://localhost:8080/api/upload/image'

const mdEditorRef = ref(null)
const isEdit = ref(false)
const articleId = ref(null)
const saving = ref(false)
const publishing = ref(false)

const form = reactive({
  title: '',
  category: '',
  tags: '',
  cover: '',
  summary: '',
  content: '',
  status: '已发布',
})


const goBack = () => {
  router.push('/admin/articles')
}

const loadArticle = async (id) => {
  try {
    const res = await request.get('/article/getById', { params: { id } })
    if (res.data.code === 200) {
      const data = res.data.data
      form.title = data.title || ''
      form.category = data.category || ''
      form.tags = data.tags || ''
      form.cover = data.cover || ''
      form.summary = data.summary || ''
      form.content = data.content || ''
      form.status = data.status || '已发布'
    } else {
      ElMessage.error('文章不存在')
      goBack()
    }
  } catch (e) {
    ElMessage.error('获取文章失败')
    goBack()
  }
}

const save = async (status) => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  const loading = status === '已发布' ? publishing : saving
  loading.value = true
  form.status = status

  try {
    let res
    if (isEdit.value) {
      res = await request.put('/article/update', { ...form, id: articleId.value })
    } else {
      res = await request.post('/article/add', form)
    }
    if (res.data.code === 200) {
      ElMessage.success(status === '已发布' ? '文章发布成功！' : '草稿已保存')
      // 清除 Markdown 编辑器的自动保存草稿
      mdEditorRef.value?.clearDraft()
      goBack()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请检查网络')
  } finally {
    loading.value = false
  }
}

const publish = () => save('已发布')
const saveAsDraft = () => save('草稿')

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    form.cover = res.data
    ElMessage.success('封面图上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('图片上传失败，请重试')
}

onMounted(() => {
  if (route.params.id) {
    isEdit.value = true
    articleId.value = Number(route.params.id)
    loadArticle(articleId.value)
  }
})
</script>

<style scoped>
.article-editor {
  min-height: calc(100vh - 120px);
}

/* 工具栏 */
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 12px;
  padding: 12px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.toolbar-title {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  flex: 1;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

/* 编辑区 */
.editor-body {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.editor-main {
  max-width: 900px;
  margin: 0 auto;
}

.title-input :deep(.el-input__inner) {
  font-size: 1.5rem;
  font-weight: 700;
  border: none;
  padding-left: 0;
  color: #333;
}

.title-input :deep(.el-input__inner::placeholder) {
  color: #ccc;
  font-weight: 400;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding-left: 0;
}

.meta-row {
  display: flex;
  gap: 12px;
  margin: 16px 0;
}

.meta-input {
  flex: 1;
}

/* 封面图上传 */
.cover-section {
  margin-bottom: 16px;
}

.cover-upload-area {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.cover-uploader {
  flex-shrink: 0;
}

.cover-placeholder,
.cover-preview {
  width: 200px;
  height: 112px;
  border-radius: 8px;
  border: 2px dashed #d0d0d0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  color: #aaa;
  font-size: 0.82rem;
  transition: border-color 0.2s, background 0.2s;
  position: relative;
  overflow: hidden;
}

.cover-placeholder:hover {
  border-color: #6c5fa0;
  color: #6c5fa0;
  background: #f8f6fc;
}

.cover-preview {
  border: 2px solid #e0e0e0;
  cursor: pointer;
  padding: 0;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #fff;
  font-size: 0.8rem;
  opacity: 0;
  transition: opacity 0.2s;
}

.cover-preview:hover .cover-overlay {
  opacity: 1;
}

.cover-url-input {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.cover-url-label {
  font-size: 0.82rem;
  color: #999;
  white-space: nowrap;
}

/* 摘要 */
.summary-input {
  margin-bottom: 20px;
}

.section-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #555;
  margin-bottom: 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .meta-row {
    flex-direction: column;
  }
  .editor-toolbar {
    flex-wrap: wrap;
  }
  .editor-body {
    padding: 16px;
  }
  .cover-upload-area {
    flex-direction: column;
  }
  .cover-url-input {
    flex-wrap: wrap;
  }
}
</style>

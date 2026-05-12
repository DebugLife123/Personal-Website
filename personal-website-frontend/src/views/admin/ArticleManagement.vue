<template>
  <div class="article-management">
    <!-- 顶部筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-input
          v-model="query.keyword"
          placeholder="搜索文章标题..."
          :prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.category" placeholder="全部分类" clearable class="filter-select" @change="search">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable class="filter-select" @change="search">
          <el-option label="已发布" value="已发布" />
          <el-option label="草稿" value="草稿" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="date-picker"
          @change="search"
        />
      </div>
      <div class="filter-right">
        <el-button type="primary" :icon="Plus" @click="createArticle">新增文章</el-button>
      </div>
    </div>

    <!-- 文章表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#fafafa', color: '#555', fontWeight: 600 }"
      >
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="title" label="文章标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类名称" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.category" size="small" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="160">
          <template #default="{ row }">
            <div class="tag-cell">
              <el-tag
                v-for="tag in (row.tags || '').split(',').filter(Boolean)"
                :key="tag"
                size="small"
                effect="plain"
                class="tag-item"
              >{{ tag.trim() }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="views" label="浏览量" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === '已发布' ? 'success' : 'info'"
              size="small"
              effect="light"
            >{{ row.status || '已发布' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" :icon="View" @click="viewArticle(row.id)">查看</el-button>
            <el-button text size="small" :icon="Edit" @click="editArticle(row.id)">编辑</el-button>
            <el-button text size="small" type="danger" :icon="Delete" @click="deleteArticle(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空数据 -->
      <el-empty v-if="!loading && tableData.length === 0" :image-size="60" description="暂无文章数据" />

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          small
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { Search, Plus, View, Edit, Delete } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const categories = ref([])
const dateRange = ref([])

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  category: '',
  status: '已发布',
  startDate: '',
  endDate: '',
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    } else {
      params.startDate = ''
      params.endDate = ''
    }
    const res = await request.get('/article/page', { params })
    if (res.data.code === 200) {
      tableData.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await request.get('/article/categories')
    if (res.data.code === 200) categories.value = res.data.data || []
  } catch (e) { /* ignore */ }
}

const search = () => {
  query.page = 1
  fetchData()
}

const createArticle = () => {
  router.push('/admin/articles/create')
}

const editArticle = (id) => {
  router.push(`/admin/articles/edit/${id}`)
}

const viewArticle = (id) => {
  window.open(`/article/${id}`, '_blank')
}

const deleteArticle = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除《${row.title}》吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await request.delete(`/article/delete/${row.id}`)
    if (res.data.code === 200) {
      ElMessage.success('文章已删除')
      fetchData()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) { /* cancelled */ }
}

onMounted(() => {
  fetchData()
  fetchCategories()
})
</script>

<style scoped>
.article-management {
  min-height: calc(100vh - 120px);
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}

.search-input {
  width: 220px;
}

.filter-select {
  width: 140px;
}

.date-picker {
  width: 240px;
}

/* 表格卡片 */
.table-card {
  border-radius: 12px;
  padding-bottom: 4px;
}

.tag-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-item {
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-text {
  font-size: 0.85rem;
  color: #888;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-input { width: 100%; }
  .filter-select { width: 100%; }
  .date-picker { width: 100%; }
}
</style>

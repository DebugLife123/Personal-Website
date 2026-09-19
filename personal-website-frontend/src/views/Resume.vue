<script setup>
import { useRouter } from 'vue-router'
import { ref, onMounted, computed } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Message, User, Link, School, Briefcase, Files, Tools,
  Edit, Check, Close, Trophy, Plus, Delete
} from '@element-plus/icons-vue'
import { isAdmin } from '../utils/auth'

// 获取路由实例，用于返回首页
const router = useRouter()

const goBack = () => {
  router.push('/')
}

// 打开外部链接的方法
const openExternal = (url) => {
  window.open(url, '_blank');
}

// 响应式数据
const resume = ref({
  name: 'yu翔',
  avatar: 'https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500',
  email: '2739267961@qq.com',
  phone: '',
  address: '湖南-衡阳',
  education: '南华大学 | 软件工程 | 2023.09 ~ 至今',
  workExperience: '西安青砧智果科技有限公司 | 算法工程师助理 | 2026.01 ~ 2026.02',
  projectExperience: '七锡源集 | 任务看板系统 | 2025.12 ~ 2026.01',
  skill: 'JavaScript, Vue, SpringBoot, MySQL',
  award: '',
  intro: '初出茅庐 | 科班码农 | 拾枝者'
})

// 经历编辑草稿：三类经历各自可以自由增删条目
const draftEntries = ref({ education: [], work: [], project: [] })

// 各类型的输入占位文案
const entryPlaceholders = {
  education: { title: '学校名称', subtitle: '专业', time: '时间，如 2023.09 ~ 至今', desc: '描述，如 大三在读' },
  work: { title: '公司名称', subtitle: '职位', time: '时间，如 2026.01 ~ 2026.02', desc: '工作内容描述' },
  project: { title: '项目名称', subtitle: '角色 / 职责', time: '时间，如 2025.12 ~ 2026.01', desc: '项目描述' }
}

const createDraft = (type, source = {}) => ({
  id: source.id ?? null,
  type,
  title: source.title ?? '',
  subtitle: source.subtitle ?? '',
  timeRange: source.timeRange ?? '',
  description: source.description ?? '',
  sort: source.sort ?? 0
})

// 用当前数据库数据初始化草稿
const resetDrafts = () => {
  draftEntries.value = {
    education: educationEntries.value.map(item => createDraft('education', item)),
    work: workEntries.value.map(item => createDraft('work', item)),
    project: projectEntries.value.map(item => createDraft('project', item))
  }
}

const addDraftEntry = (type) => {
  draftEntries.value[type].push(createDraft(type, { sort: draftEntries.value[type].length }))
}

const removeDraftEntry = (type, index) => {
  draftEntries.value[type].splice(index, 1)
}

// 技能列表（从 skill 字段解析）
const skillList = ref([])

// 结构化简历经历
const entries = ref([])
const entriesByType = (type) => entries.value.filter(item => item.type === type)
const educationEntries = computed(() => entriesByType('education'))
const workEntries = computed(() => entriesByType('work'))
const projectEntries = computed(() => entriesByType('project'))

const parseSkills = (skills) => {
  return (skills || '').split(',').map(s => s.trim()).filter(Boolean)
}

// 编辑模式
const isEditMode = ref(false)
const loading = ref(true)
const pageLoaded = ref(false)

// 获取个人信息
const fetchResume = async () => {
  loading.value = true
  try {
    const [res, entryRes] = await Promise.all([
      request.get('/resume/get'),
      request.get('/resume/entries')
    ])
    if (res.data.code === 200) {
      resume.value = res.data.data
      skillList.value = parseSkills(resume.value.skill)
    }
    if (entryRes.data.code === 200) {
      entries.value = entryRes.data.data || []
    }
  } catch (error) {
    console.error('获取个人信息失败:', error)
  } finally {
    loading.value = false
    setTimeout(() => { pageLoaded.value = true }, 50)
  }
}

// 保存：先提交基本信息，再按差异同步三类经历
const saving = ref(false)

const syncEntries = async () => {
  const originals = entries.value
  const originalByType = {
    education: originals.filter(i => i.type === 'education'),
    work: originals.filter(i => i.type === 'work'),
    project: originals.filter(i => i.type === 'project')
  }

  for (const type of ['education', 'work', 'project']) {
    const drafts = draftEntries.value[type]
    const original = originalByType[type]
    const keptIds = new Set(drafts.filter(d => d.id).map(d => d.id))

    // 删除：原有的但已不在草稿中的条目
    for (const item of original) {
      if (!keptIds.has(item.id)) {
        await request.delete(`/resume/entries/${item.id}`)
      }
    }

    // 新增 / 更新
    for (let index = 0; index < drafts.length; index++) {
      const draft = drafts[index]
      if (!draft.title.trim()) continue
      const payload = {
        type,
        title: draft.title.trim(),
        subtitle: draft.subtitle,
        timeRange: draft.timeRange,
        description: draft.description,
        sort: index
      }
      if (draft.id) {
        await request.put(`/resume/entries/${draft.id}`, payload)
      } else {
        await request.post('/resume/entries', payload)
      }
    }
  }
}

const saveResume = async () => {
  saving.value = true
  try {
    const res = await request.post('/resume/update', { ...resume.value })
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '基本信息保存失败')
      return
    }
    await syncEntries()
    ElMessage.success('个人信息更新成功！')
    isEditMode.value = false
    await fetchResume()
  } catch (error) {
    console.error('更新个人信息失败:', error)
    ElMessage.error('更新失败，请重试')
  } finally {
    saving.value = false
  }
}

// 取消编辑
const cancelEdit = () => {
  isEditMode.value = false
  fetchResume() // 重新获取数据，恢复到原始状态
}

// 进入编辑模式时解析字段
const enterEditMode = () => {
  skillList.value = parseSkills(resume.value.skill)
  resetDrafts()
  isEditMode.value = true
}

onMounted(() => {
  fetchResume()
})
</script>

<template>
  <div class="resume-wrapper" :class="{ 'page-loaded': pageLoaded }">

    <div class="top-bar">
      <el-button icon="ArrowLeft" text @click="goBack">返回首页</el-button>
      <div class="action-buttons">
        <template v-if="isAdmin">
          <el-button v-if="!isEditMode" type="primary" icon="Edit" @click="enterEditMode">编辑个人资料</el-button>
          <template v-else>
            <el-button type="success" icon="Check" :loading="saving" @click="saveResume">保存</el-button>
            <el-button type="info" icon="Close" @click="cancelEdit">取 消</el-button>
          </template>
        </template>
      </div>
    </div>

    <div v-if="loading" class="loading-skeleton">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else>
    <el-row justify="center">
      <el-col :xs="24" :sm="22" :md="18" :lg="14">
        
        <div class="resume-header">
          <div v-if="!isEditMode">
            <h1 class="name">{{ resume.name }}</h1>
            <div class="contact-bar">
              <el-button round size="small" icon="Message" @click="openExternal('mailto:' + resume.email)">{{ resume.email }}</el-button>
              <el-button round size="small" icon="User" @click="openExternal('https://github.com/DebugLife123')">DebugLife123</el-button>
              <el-button round size="small" icon="Link" @click="goBack">yu翔.cc</el-button>
            </div>
          </div>
          <div v-else class="edit-header">
            <el-input v-model="resume.name" placeholder="姓名" class="name-input" />
            <el-input v-model="resume.avatar" placeholder="头像 URL" class="intro-input" />
            <el-input v-model="resume.email" placeholder="邮箱" class="contact-input" />
            <el-input v-model="resume.phone" placeholder="电话" class="contact-input" />
            <el-input v-model="resume.address" placeholder="地址" class="contact-input" />
            <el-input v-model="resume.intro" placeholder="个人简介" class="intro-input" />
          </div>
          <el-divider />
        </div>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><School /></el-icon> 教育经历</h3>
          <div v-if="!isEditMode">
            <el-timeline v-if="educationEntries.length">
              <el-timeline-item v-for="item in educationEntries" :key="item.id" placement="top" :hollow="true">
                <el-card shadow="never" class="content-card">
                  <div class="card-header">
                    <div class="org-info">
                      <el-avatar :size="40" src="https://img1.baidu.com/it/u=460186796,737045498&fm=253&app=120&f=JPEG?w=814&h=800" />
                      <div class="text">
                        <h4>{{ item.title }}</h4>
                        <span>{{ item.subtitle || '' }}</span>
                      </div>
                    </div>
                    <span class="date">{{ item.timeRange || '' }}</span>
                  </div>
                  <p class="desc">{{ item.description || '暂无描述' }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无教育经历" :image-size="40" />
          </div>
          <div v-else class="entry-edit-list">
            <div v-for="(item, index) in draftEntries.education" :key="item.id ?? index" class="entry-edit-item">
              <div class="entry-edit-head">
                <span class="entry-index">{{ index + 1 }}</span>
                <el-button text type="danger" size="small" icon="Delete" @click="removeDraftEntry('education', index)">删除</el-button>
              </div>
              <el-input v-model="item.title" :placeholder="entryPlaceholders.education.title" class="section-edit-input" />
              <el-input v-model="item.subtitle" :placeholder="entryPlaceholders.education.subtitle" class="section-edit-input" />
              <el-input v-model="item.timeRange" :placeholder="entryPlaceholders.education.time" class="section-edit-input" />
              <el-input v-model="item.description" :placeholder="entryPlaceholders.education.desc" class="section-edit-input" />
            </div>
            <el-button class="entry-add-btn" plain type="primary" icon="Plus" @click="addDraftEntry('education')">添加教育经历</el-button>
          </div>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Briefcase /></el-icon> 实习 & 工作经历</h3>
          <div v-if="!isEditMode">
            <el-timeline v-if="workEntries.length">
              <el-timeline-item v-for="item in workEntries" :key="item.id" placement="top" :hollow="true">
                <el-card shadow="never" class="content-card">
                  <div class="card-header">
                    <h4>{{ item.title }}</h4>
                    <span class="date">{{ item.timeRange || '' }}</span>
                  </div>
                  <div class="sub-title">{{ item.subtitle || '' }}</div>
                  <p class="desc">{{ item.description || '暂无描述' }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无实习 / 工作经历" :image-size="40" />
          </div>
          <div v-else class="entry-edit-list">
            <div v-for="(item, index) in draftEntries.work" :key="item.id ?? index" class="entry-edit-item">
              <div class="entry-edit-head">
                <span class="entry-index">{{ index + 1 }}</span>
                <el-button text type="danger" size="small" icon="Delete" @click="removeDraftEntry('work', index)">删除</el-button>
              </div>
              <el-input v-model="item.title" :placeholder="entryPlaceholders.work.title" class="section-edit-input" />
              <el-input v-model="item.subtitle" :placeholder="entryPlaceholders.work.subtitle" class="section-edit-input" />
              <el-input v-model="item.timeRange" :placeholder="entryPlaceholders.work.time" class="section-edit-input" />
              <el-input v-model="item.description" :placeholder="entryPlaceholders.work.desc" class="section-edit-input" />
            </div>
            <el-button class="entry-add-btn" plain type="primary" icon="Plus" @click="addDraftEntry('work')">添加实习 / 工作经历</el-button>
          </div>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Files /></el-icon> 项目经历</h3>
          <div v-if="!isEditMode">
            <el-timeline v-if="projectEntries.length">
              <el-timeline-item v-for="item in projectEntries" :key="item.id" placement="top" :hollow="true">
                <el-card shadow="never" class="content-card">
                  <div class="card-header">
                    <h4>{{ item.title }}</h4>
                    <span class="date">{{ item.timeRange || '' }}</span>
                  </div>
                  <div class="sub-title">{{ item.subtitle || '' }}</div>
                  <p class="desc">{{ item.description || '暂无描述' }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无项目经历" :image-size="40" />
          </div>
          <div v-else class="entry-edit-list">
            <div v-for="(item, index) in draftEntries.project" :key="item.id ?? index" class="entry-edit-item">
              <div class="entry-edit-head">
                <span class="entry-index">{{ index + 1 }}</span>
                <el-button text type="danger" size="small" icon="Delete" @click="removeDraftEntry('project', index)">删除</el-button>
              </div>
              <el-input v-model="item.title" :placeholder="entryPlaceholders.project.title" class="section-edit-input" />
              <el-input v-model="item.subtitle" :placeholder="entryPlaceholders.project.subtitle" class="section-edit-input" />
              <el-input v-model="item.timeRange" :placeholder="entryPlaceholders.project.time" class="section-edit-input" />
              <el-input v-model="item.description" :placeholder="entryPlaceholders.project.desc" class="section-edit-input" />
            </div>
            <el-button class="entry-add-btn" plain type="primary" icon="Plus" @click="addDraftEntry('project')">添加项目经历</el-button>
          </div>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Tools /></el-icon> 技术栈</h3>
          <div v-if="!isEditMode" class="skill-panel">
            <div v-if="skillList.length" class="skill-cloud">
              <span
                v-for="(skill, index) in skillList"
                :key="skill"
                class="skill-chip"
                :style="{ '--chip-hue': (index * 47 + 232) % 360 }"
              >
                <span class="skill-dot"></span>
                <span class="skill-name">{{ skill }}</span>
              </span>
            </div>
            <el-empty v-else description="暂无技术栈" :image-size="40" />
            <p v-if="skillList.length" class="skill-summary">共 {{ skillList.length }} 项技术能力</p>
          </div>
          <div v-else class="section-edit-group">
            <el-input
              v-model="resume.skill"
              type="textarea"
              :rows="3"
              resize="vertical"
              placeholder="多个技术用逗号分隔，如: JavaScript, Vue, SpringBoot, MySQL"
              class="section-edit-input"
            />
            <p class="edit-hint">用逗号分隔，保存后会拆分为技术标签逐个展示</p>
          </div>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Trophy /></el-icon> 奖项</h3>
          <div v-if="!isEditMode">
            <el-card shadow="never" class="content-card">
              <p class="desc">{{ resume.award || '暂无奖项' }}</p>
            </el-card>
          </div>
          <el-input v-else v-model="resume.award" placeholder="奖项" class="section-input" />
        </section>

      </el-col>
    </el-row>
    </template>
  </div>
</template>

<style scoped>
.resume-wrapper {
  padding: 85px 20px 40px;
  background-color: #fff;
  min-height: 100vh;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 0.4s ease, transform 0.4s ease;
}
.resume-wrapper.page-loaded {
  opacity: 1;
  transform: translateY(0);
}

.loading-skeleton {
  max-width: 700px;
  margin: 40px auto;
  padding: 30px;
  background: #fafafa;
  border-radius: 14px;
}
.top-bar { margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
.action-buttons { display: flex; gap: 10px; }

/* 编辑模式样式 */
.edit-header { display: flex; flex-direction: column; gap: 10px; align-items: center; }
.name-input { width: 300px; margin-bottom: 10px; }
.contact-input { width: 250px; margin: 0 5px; }
.intro-input { width: 400px; margin-top: 10px; }
.section-input { width: 100%; margin-bottom: 15px; }
.section-edit-group { display: flex; flex-direction: column; gap: 10px; }
.section-edit-input { width: 100%; }
.edit-hint { font-size: 0.8rem; color: #999; margin: -4px 0 0; }

/* 经历条目编辑 */
.entry-edit-list { display: flex; flex-direction: column; gap: 14px; }
.entry-edit-item {
  display: flex; flex-direction: column; gap: 10px;
  padding: 14px; border: 1px dashed #d9d9e3; border-radius: 10px;
  background: #fafafc;
}
.entry-edit-head { display: flex; justify-content: space-between; align-items: center; }
.entry-index {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: 50%;
  background: #8a8eaa; color: #fff; font-size: 0.75rem; font-weight: 600;
}
.entry-add-btn { align-self: flex-start; }

/* 响应式调整 */
@media (max-width: 768px) {
  .name-input { width: 100%; }
  .contact-input { width: 100%; margin: 5px 0; }
  .intro-input { width: 100%; }
  .top-bar { flex-direction: column; align-items: flex-start; gap: 10px; }
  .action-buttons { align-self: flex-end; }
}

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
.skill-panel {
  padding: 20px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: linear-gradient(180deg, #fbfbfd 0%, #fafafa 100%);
}
.skill-cloud { display: flex; flex-wrap: wrap; gap: 10px; }
.skill-chip {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 7px 16px 7px 12px;
  border-radius: 999px;
  border: 1px solid hsl(var(--chip-hue), 55%, 88%);
  background: hsl(var(--chip-hue), 70%, 97%);
  color: hsl(var(--chip-hue), 42%, 34%);
  font-size: 0.92rem; font-weight: 600; letter-spacing: 0.2px;
  line-height: 1.4;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  cursor: default;
}
.skill-chip:hover {
  transform: translateY(-2px);
  border-color: hsl(var(--chip-hue), 60%, 72%);
  box-shadow: 0 6px 16px hsla(var(--chip-hue), 60%, 60%, 0.18);
}
.skill-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: hsl(var(--chip-hue), 68%, 62%);
  flex-shrink: 0;
}
.skill-name { white-space: nowrap; }
.skill-summary {
  margin: 16px 0 0; padding-top: 14px;
  border-top: 1px dashed #e8e8ef;
  font-size: 0.82rem; color: #9a9aad; letter-spacing: 0.3px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .card-header { flex-direction: column; gap: 5px; }
}

</style>
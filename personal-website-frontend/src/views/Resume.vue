<script setup>
import { useRouter } from 'vue-router'
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Message, User, Link, School, Briefcase, Files, Tools,
  Edit, Check, Close, Trophy
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

// 编辑拆解字段——教育
const eduSchool = ref('')
const eduMajor = ref('')
const eduTime = ref('')
const eduDesc = ref('')
// 编辑拆解字段——工作
const workCompany = ref('')
const workRole = ref('')
const workTime = ref('')
const workDesc = ref('')
// 编辑拆解字段——项目
const projName = ref('')
const projRole = ref('')
const projTime = ref('')
const projDesc = ref('')

// 从 resume 数据中拆解各段
const parseResumeFields = (data) => {
  const edu = (data.education || '').split(' | ')
  eduSchool.value = edu[0] || ''
  eduMajor.value = edu[1] || ''
  eduTime.value = edu[2] || ''
  eduDesc.value = edu[3] || ''

  const work = (data.workExperience || '').split(' | ')
  workCompany.value = work[0] || ''
  workRole.value = work[1] || ''
  workTime.value = work[2] || ''
  workDesc.value = work[3] || ''

  const proj = (data.projectExperience || '').split(' | ')
  projName.value = proj[0] || ''
  projRole.value = proj[1] || ''
  projTime.value = proj[2] || ''
  projDesc.value = proj[3] || ''
}

// 技能列表（从 skill 字段解析）
const skillList = ref([])

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
    const res = await request.get('/resume/get')
    if (res.data.code === 200) {
      resume.value = res.data.data
      parseResumeFields(resume.value)
      skillList.value = parseSkills(resume.value.skill)
    }
  } catch (error) {
    console.error('获取个人信息失败:', error)
  } finally {
    loading.value = false
    setTimeout(() => { pageLoaded.value = true }, 50)
  }
}

// 保存个人信息
const saveResume = async () => {
  // 组装教育/工作/项目字符串
  resume.value.education = [eduSchool.value, eduMajor.value, eduTime.value, eduDesc.value].filter(Boolean).join(' | ')
  resume.value.workExperience = [workCompany.value, workRole.value, workTime.value, workDesc.value].filter(Boolean).join(' | ')
  resume.value.projectExperience = [projName.value, projRole.value, projTime.value, projDesc.value].filter(Boolean).join(' | ')

  try {
    const res = await request.post('/resume/update', resume.value)
    if (res.data.code === 200) {
      ElMessage.success('个人信息更新成功！')
      isEditMode.value = false
    }
  } catch (error) {
    console.error('更新个人信息失败:', error)
    ElMessage.error('更新失败，请重试')
  }
}

// 取消编辑
const cancelEdit = () => {
  isEditMode.value = false
  fetchResume() // 重新获取数据，恢复到原始状态
}

// 进入编辑模式时解析字段
const enterEditMode = () => {
  parseResumeFields(resume.value)
  skillList.value = parseSkills(resume.value.skill)
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
            <el-button type="success" icon="Check" @click="saveResume">保存</el-button>
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
            <el-timeline>
              <el-timeline-item placement="top" :hollow="true">
                <el-card shadow="never" class="content-card">
                  <div class="card-header">
                    <div class="org-info">
                      <el-avatar :size="40" src="https://img1.baidu.com/it/u=460186796,737045498&fm=253&app=120&f=JPEG?w=814&h=800" />
                      <div class="text">
                        <h4>{{ resume.education.split(' | ')[0] || '未知学校' }}</h4>
                        <span>{{ resume.education.split(' | ')[1] || '' }}</span>
                      </div>
                    </div>
                    <span class="date">{{ resume.education.split(' | ')[2] || '' }}</span>
                  </div>
                  <p class="desc">{{ resume.education.split(' | ')[3] || '暂无描述' }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else class="section-edit-group">
            <el-input v-model="eduSchool" placeholder="学校" class="section-edit-input" />
            <el-input v-model="eduMajor" placeholder="专业" class="section-edit-input" />
            <el-input v-model="eduTime" placeholder="时间，如 2023.09 ~ 至今" class="section-edit-input" />
            <el-input v-model="eduDesc" placeholder="描述，如 大三在读" class="section-edit-input" />
          </div>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Briefcase /></el-icon> 实习 & 工作经历</h3>
          <div v-if="!isEditMode">
            <el-timeline>
              <el-timeline-item placement="top" :hollow="true">
                <el-card shadow="never" class="content-card">
                  <div class="card-header">
                    <h4>{{ resume.workExperience.split(' | ')[0] || '未知公司' }}</h4>
                    <span class="date">{{ resume.workExperience.split(' | ')[2] || '' }}</span>
                  </div>
                  <div class="sub-title">{{ resume.workExperience.split(' | ')[1] || '' }}</div>
                  <p class="desc">{{ resume.workExperience.split(' | ')[3] || '暂无描述' }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else class="section-edit-group">
            <el-input v-model="workCompany" placeholder="公司名称" class="section-edit-input" />
            <el-input v-model="workRole" placeholder="职位" class="section-edit-input" />
            <el-input v-model="workTime" placeholder="时间，如 2026.01 ~ 2026.02" class="section-edit-input" />
            <el-input v-model="workDesc" placeholder="工作描述" class="section-edit-input" />
          </div>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Files /></el-icon> 项目经历</h3>
          <div v-if="!isEditMode">
            <el-timeline>
              <el-timeline-item placement="top" :hollow="true">
                <el-card shadow="never" class="content-card">
                  <div class="card-header">
                    <h4>{{ resume.projectExperience.split(' | ')[0] || '未知项目' }}</h4>
                    <span class="date">{{ resume.projectExperience.split(' | ')[2] || '' }}</span>
                  </div>
                  <div class="sub-title">{{ resume.projectExperience.split(' | ')[1] || '' }}</div>
                  <p class="desc">{{ resume.projectExperience.split(' | ')[3] || '暂无描述' }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else class="section-edit-group">
            <el-input v-model="projName" placeholder="项目名称" class="section-edit-input" />
            <el-input v-model="projRole" placeholder="角色 / 职责" class="section-edit-input" />
            <el-input v-model="projTime" placeholder="时间，如 2025.12 ~ 2026.01" class="section-edit-input" />
            <el-input v-model="projDesc" placeholder="项目描述" class="section-edit-input" />
          </div>
        </section>

        <section class="resume-section">
          <h3 class="section-title"><el-icon><Tools /></el-icon> 技术栈</h3>
          <div v-if="!isEditMode" class="skill-list">
            <el-card v-for="skill in skillList" :key="skill" shadow="never" class="skill-card">
              <div class="skill-info">
                <div class="skill-icon-fallback">{{ skill.charAt(0).toUpperCase() }}</div>
                <div class="text">
                  <h4>{{ skill }}</h4>
                  <p>熟练使用 {{ skill }}</p>
                </div>
              </div>
            </el-card>
            <el-empty v-if="skillList.length === 0" description="暂无技术栈" :image-size="40" />
          </div>
          <div v-else class="section-edit-group">
            <el-input v-model="resume.skill" placeholder="多个技术用逗号分隔，如: JavaScript, Vue, SpringBoot, MySQL" class="section-edit-input" />
            <p class="edit-hint">编辑保存后，技术栈将自动拆分为独立卡片展示</p>
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
.skill-list { display: flex; flex-direction: column; gap: 15px; }
.skill-card { border-radius: 12px; border: 1px solid #f0f0f0; }
.skill-info { display: flex; align-items: center; gap: 20px; }
.skill-icon-fallback {
  width: 48px; height: 48px; border-radius: 10px;
  background: linear-gradient(135deg, #9e92b0, #8a8eaa);
  color: #fff; font-size: 1.3rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.skill-info h4 { margin: 0 0 5px 0; font-size: 1.1rem; color: #333; }
.skill-info p { margin: 0; color: #666; font-size: 0.9rem; }

/* 响应式调整 */
@media (max-width: 768px) {
  .card-header { flex-direction: column; gap: 5px; }
}

</style>
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 1. 引入 v-md-editor 预览组件
import VMdPreview from '@kangc/v-md-editor/lib/preview'
import '@kangc/v-md-editor/lib/style/preview.css'
// 2. 引入 github 主题
import githubTheme from '@kangc/v-md-editor/lib/theme/github.js'
import '@kangc/v-md-editor/lib/theme/style/github.css'

// 3. 引入代码高亮所需的样式 (可选)
import hljs from 'highlight.js'

// 配置主题
VMdPreview.use(githubTheme, {
  Hljs: hljs,
})

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 挂载路由
app.use(router)

app.use(ElementPlus)
// 4. 全局注册
app.use(VMdPreview)
app.mount('#app')
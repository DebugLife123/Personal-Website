<template>
  <div class="md-editor" :class="[`mode-${viewMode}`, { 'is-fullscreen': isFullscreen, 'md-dark': isDark }]">
    <!-- ============ Toolbar ============ -->
    <div class="md-toolbar">
      <!-- Text formatting -->
      <span class="tb-group">
        <button class="tb-btn" title="加粗" @click="wrapSelection('**','**')"><b>B</b></button>
        <button class="tb-btn" title="斜体" @click="wrapSelection('*','*')"><i>I</i></button>
        <button class="tb-btn" title="删除线" @click="wrapSelection('~~','~~')"><s>S</s></button>
        <button class="tb-btn" title="下划线" @click="wrapSelection('<u>','</u>')"><u>U</u></button>
        <button class="tb-btn" title="上标" @click="wrapSelection('<sup>','</sup>')"><sup>X²</sup></button>
        <button class="tb-btn" title="下标" @click="wrapSelection('<sub>','</sub>')"><sub>X₂</sub></button>
        <el-popover placement="bottom" :width="180" trigger="click" popper-class="md-popover">
          <template #reference>
            <button class="tb-btn" title="高亮文本"><span style="border-bottom:2px solid #f7c948;font-weight:600">A</span></button>
          </template>
          <div class="highlight-picker">
            <span
              v-for="c in highlightColors" :key="c"
              class="h-color"
              :style="{ background: c }"
              @click="insertHighlight(c)"
            />
            <div class="h-custom">
              <span>自定义：</span>
              <input type="color" v-model="customColor" @change="insertCustomHighlight" />
            </div>
          </div>
        </el-popover>
      </span>

      <span class="tb-sep" />

      <!-- Headings -->
      <span class="tb-group">
        <el-dropdown trigger="click" @command="h => insertAtCursor(h)">
          <button class="tb-btn" title="标题">H<span style="font-size:0.65rem">▼</span></button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="i in 6" :key="i" :command="'#'.repeat(i) + ' '">H{{ i }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button class="tb-btn" title="无序列表" @click="insertAtCursor('- ')"><el-icon><List /></el-icon></button>
        <button class="tb-btn" title="有序列表" @click="insertAtCursor('1. ')">1.</button>
        <button class="tb-btn" title="任务列表" @click="insertAtCursor('- [ ] ')"><el-icon><Finished /></el-icon></button>
        <button class="tb-btn" title="引用块" @click="insertAtCursor('> ')">❝</button>
        <button class="tb-btn" title="代码块" @click="insertCodeBlock">&lt;/&gt;</button>
        <button class="tb-btn" title="分割线" @click="insertAtCursor('\n---\n')"><el-icon><Remove /></el-icon></button>
      </span>

      <span class="tb-sep" />

      <!-- Insert -->
      <span class="tb-group">
        <button class="tb-btn" title="链接" @click="insertLink"><el-icon><Link /></el-icon></button>
        <el-upload
          :action="uploadUrl"
          :show-file-list="false"
          :on-success="imgUploadSuccess"
          :on-error="() => {}"
          :before-upload="imgBeforeUpload"
          accept="image/*"
          style="display:inline-flex"
        >
          <button class="tb-btn" title="图片"><el-icon><Picture /></el-icon></button>
        </el-upload>
        <button class="tb-btn" title="表格" @click="showTableDialog = true"><el-icon><Grid /></el-icon></button>
        <el-popover placement="bottom" :width="280" trigger="click" popper-class="md-popover">
          <template #reference>
            <button class="tb-btn" title="表情符号">😊</button>
          </template>
          <div class="emoji-panel">
            <span v-for="e in emojis" :key="e" class="emoji-item" @click="insertAtCursor(e)">{{ e }}</span>
          </div>
        </el-popover>
      </span>

      <span class="tb-sep" />

      <!-- Actions -->
      <span class="tb-group">
        <button class="tb-btn" title="撤销" @click="undo"><el-icon><Back /></el-icon></button>
        <button class="tb-btn" title="重做" @click="redo"><el-icon><Right /></el-icon></button>
        <button class="tb-btn" title="清空" @click="clearContent"><el-icon><Delete /></el-icon></button>
        <button class="tb-btn" :title="isFullscreen?'退出全屏':'全屏'" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
        </button>
        <div class="tb-divider" />
        <button
          class="tb-btn tb-view-btn"
          :class="{ active: viewMode === 'edit' }"
          title="仅编辑"
          @click="viewMode = 'edit'"
        ><el-icon><EditPen /></el-icon></button>
        <button
          class="tb-btn tb-view-btn"
          :class="{ active: viewMode === 'split' }"
          title="双栏"
          @click="viewMode = 'split'"
        ><el-icon><Operation /></el-icon></button>
        <button
          class="tb-btn tb-view-btn"
          :class="{ active: viewMode === 'preview' }"
          title="仅预览"
          @click="viewMode = 'preview'"
        ><el-icon><View /></el-icon></button>
      </span>
    </div>

    <!-- ============ Body ============ -->
    <div class="md-body" ref="bodyRef">
      <!-- Editor pane -->
      <div class="md-editor-pane" ref="editorPaneRef" :style="editorPaneStyle">
        <div class="md-gutter" ref="gutterRef">
          <div v-for="n in lineCount" :key="n" class="gutter-line">{{ n }}</div>
        </div>
        <div class="md-input-wrap">
          <pre class="md-highlight-layer" ref="highlightLayer" aria-hidden="true"><code ref="highlightCode"></code></pre>
          <textarea
            ref="textareaRef"
            :value="modelValue"
            @input="handleInput"
            @scroll="syncScroll"
            @keydown.tab.prevent="insertAtCursor('  ')"
            class="md-textarea"
            placeholder="输入 Markdown 内容..."
            spellcheck="false"
          ></textarea>
          <!-- Drag overlay when dragging -->
          <div v-if="dragging" class="drag-overlay" @mousemove="onDrag" @mouseup="endDrag" @mouseleave="endDrag"></div>
        </div>
      </div>

      <!-- Drag handle (only in split mode) -->
      <div
        v-if="viewMode === 'split'"
        class="md-drag-handle"
        @mousedown="startDrag"
      >
        <div class="drag-dots"><span></span><span></span><span></span></div>
      </div>

      <!-- Preview pane -->
      <div class="md-preview-pane" v-show="viewMode !== 'edit'" :style="previewPaneStyle">
        <div class="md-preview-inner">
          <v-md-preview :text="modelValue" />
        </div>
      </div>
    </div>

    <!-- ============ Status bar ============ -->
    <div class="md-status">
      <span>{{ contentWords }} 词 · {{ modelValue.length }} 字符 · {{ lineCount }} 行</span>
      <span class="status-right">
        <span v-if="autoSaveStatus" :class="'save-' + autoSaveStatus">
          {{ autoSaveStatus === 'saving' ? '自动保存中…' : autoSaveStatus === 'saved' ? '已自动保存' : '' }}
        </span>
        <span v-if="viewMode === 'edit'" style="color:#bbb">按 Ctrl+S 保存</span>
      </span>
    </div>

    <!-- ============ Table dialog ============ -->
    <el-dialog v-model="showTableDialog" title="插入表格" width="380px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="列数">
          <el-input-number v-model="tableCols" :min="2" :max="10" />
        </el-form-item>
        <el-form-item label="行数">
          <el-input-number v-model="tableRows" :min="2" :max="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTableDialog = false">取消</el-button>
        <el-button type="primary" @click="insertTable">插入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import {
  List, Finished, Remove,
  Link, Picture, Grid, Back, Right, Delete,
  FullScreen, EditPen, Operation, View
} from '@element-plus/icons-vue'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])

// --- state ---
const textareaRef = ref(null)
const highlightLayer = ref(null)
const highlightCode = ref(null)
const gutterRef = ref(null)
const bodyRef = ref(null)
const editorPaneRef = ref(null)

const viewMode = ref('split')
const isFullscreen = ref(false)
const isDark = ref(false)
const dragging = ref(false)
const dragStartX = ref(0)
const dragStartRatio = ref(0.5)
const splitRatio = ref(0.5)

// Auto save
const autoSaveStatus = ref('')
let autoSaveTimer = null
const DRAFT_KEY = 'article-draft'

// Table dialog
const showTableDialog = ref(false)
const tableCols = ref(3)
const tableRows = ref(3)

// Highlight colors
const highlightColors = ['#f7c948', '#f28b82', '#aecbfa', '#ccff90', '#ffd699', '#d7aefb', '#ff8bcb']
const customColor = ref('#ffff00')

const insertHighlight = (color) => {
  wrapSelection(`<mark style="background:${color}">`, '</mark>')
}
const insertCustomHighlight = () => {
  wrapSelection(`<mark style="background:${customColor.value}">`, '</mark>')
}

// Emojis
const emojis = '😀😃😄😁😆😅😂🤣🙂😊😇🥰😍🤩😘😗😚😋😛😜🤪😝🤑🤗🤭🤫🤔🤐🤨😐😑😶😏😒🙄😬🤥😌😔😪🤤😴😷🤒🤕🤢🤮🥴😵🤯🥳😎🧐😕😟🙁😮😯😲😳🥺😢😭😤😠😡🤬💀👋✌🤞🤟🤘🤙👌👍👎✊👊🤛🤜👏🙌👐🤲🙏💪🦵🦶👂👃🧠🦷👀👅👄💋❤🧡💛💚💙💜🖤🤍🤎💔❣💕💞💓💗💖💘💝🌟⭐✨🔥💥🌈☀🌙⭐🌸🌺🌻🌹🌷🌼🌿🍀🌊🔥🌟⭐✨'.split('')

// Upload
const uploadUrl = 'http://localhost:8080/api/upload/image'

// --- computed ---
const lineCount = computed(() => {
  if (!props.modelValue) return 1
  return props.modelValue.split('\n').length
})

const contentWords = computed(() => {
  const text = props.modelValue || ''
  return text.split(/[\s\n]+/).filter(w => w.length > 0).length
})

const editorPaneStyle = computed(() => {
  if (viewMode.value === 'edit') return { width: '100%' }
  if (viewMode.value === 'preview') return { display: 'none' }
  return { width: `calc(${splitRatio.value * 100}% - ${splitRatio.value > 0 ? 6 : 0}px)` }
})

const previewPaneStyle = computed(() => {
  if (viewMode.value === 'edit') return { display: 'none' }
  if (viewMode.value === 'preview') return { width: '100%' }
  return { width: `calc(${(1 - splitRatio.value) * 100}% - 0px)` }
})

// --- textarea manipulation ---
const getTextarea = () => textareaRef.value
const getSel = () => {
  const ta = getTextarea()
  if (!ta) return { start: 0, end: 0, text: '' }
  return { start: ta.selectionStart, end: ta.selectionEnd, text: props.modelValue.substring(ta.selectionStart, ta.selectionEnd) }
}

const setCursor = (pos) => {
  nextTick(() => {
    const ta = getTextarea()
    if (ta) { ta.focus(); ta.setSelectionRange(pos, pos) }
  })
}

const wrapSelection = (before, after) => {
  const ta = getTextarea()
  if (!ta) return
  const sel = getSel()
  const newText = props.modelValue.substring(0, sel.start) + before + sel.text + after + props.modelValue.substring(sel.end)
  emit('update:modelValue', newText)
  const cursor = sel.start + before.length + sel.text.length + after.length
  setCursor(cursor)
}

const insertAtCursor = (text) => {
  const ta = getTextarea()
  if (!ta) return
  const sel = getSel()
  const newText = props.modelValue.substring(0, sel.start) + text + props.modelValue.substring(sel.end)
  emit('update:modelValue', newText)
  setCursor(sel.start + text.length)
}

const insertCodeBlock = () => {
  insertAtCursor('\n```\n\n```\n')
  nextTick(() => {
    const ta = getTextarea()
    if (ta) {
      const pos = ta.value.length - 5 // cursor before closing ```
      ta.focus(); ta.setSelectionRange(pos, pos)
    }
  })
}

const insertLink = () => {
  const sel = getSel()
  const text = sel.text || '链接文字'
  const md = `[${text}](url)`
  const ta = getTextarea()
  if (!ta) return
  const newText = props.modelValue.substring(0, sel.start) + md + props.modelValue.substring(sel.end)
  emit('update:modelValue', newText)
  // select the url part
  nextTick(() => {
    const ta2 = getTextarea()
    if (ta2) {
      const urlStart = sel.start + text.length + 3
      ta2.focus()
      ta2.setSelectionRange(urlStart, urlStart + 3)
    }
  })
}

const insertTable = () => {
  showTableDialog.value = false
  const cols = tableCols.value
  const rows = tableRows.value
  let table = '\n|' + ' 标题 |'.repeat(cols) + '\n|' + ' --- |'.repeat(cols) + '\n'
  for (let i = 0; i < rows; i++) {
    table += '|' + ' 内容 |'.repeat(cols) + '\n'
  }
  insertAtCursor(table)
}

const clearContent = () => {
  emit('update:modelValue', '')
  getTextarea()?.focus()
}

// Undo/redo using native textarea undo stack
const undo = () => document.execCommand('undo')
const redo = () => document.execCommand('redo')

// --- Fullscreen ---
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

// --- Drag split ---
const startDrag = (e) => {
  dragging.value = true
  dragStartX.value = e.clientX
  dragStartRatio.value = splitRatio.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const onDrag = (e) => {
  if (!dragging.value || !bodyRef.value) return
  const rect = bodyRef.value.getBoundingClientRect()
  const ratio = (e.clientX - rect.left) / rect.width
  splitRatio.value = Math.max(0.2, Math.min(0.8, ratio))
}

const endDrag = () => {
  if (!dragging.value) return
  dragging.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

// --- Input handler ---
const handleInput = (e) => {
  emit('update:modelValue', e.target.value)
}

// --- Sync scroll ---
const syncScroll = () => {
  const ta = getTextarea()
  if (!ta) return
  if (gutterRef.value) gutterRef.value.scrollTop = ta.scrollTop
  if (highlightLayer.value) highlightLayer.value.scrollTop = ta.scrollTop
  if (highlightLayer.value) highlightLayer.value.scrollLeft = ta.scrollLeft
}

// --- Highlight ---
const highlightCodeBlock = async () => {
  if (!highlightCode.value) return
  const code = highlightCode.value
  const text = props.modelValue || ''
  try {
    const hljs = (await import('highlight.js/lib/core')).default
    const mdLang = (await import('highlight.js/lib/languages/markdown')).default
    hljs.registerLanguage('markdown', mdLang)
    code.textContent = text + '\n'
    hljs.highlightElement(code)
    // Force min-height sync
    if (highlightLayer.value) {
      highlightLayer.value.style.minHeight = '100%'
    }
  } catch {
    code.textContent = text + '\n'
  }
}

// --- Auto save ---
const saveDraft = () => {
  if (props.modelValue) {
    localStorage.setItem(DRAFT_KEY, props.modelValue)
    autoSaveStatus.value = 'saved'
    setTimeout(() => { if (autoSaveStatus.value === 'saved') autoSaveStatus.value = '' }, 2000)
  }
}

const startAutoSave = () => {
  autoSaveTimer = setInterval(() => {
    if (props.modelValue) {
      autoSaveStatus.value = 'saving'
      saveDraft()
    }
  }, 30000)
}

const clearDraft = () => {
  localStorage.removeItem(DRAFT_KEY)
}

const restoreDraft = () => {
  const draft = localStorage.getItem(DRAFT_KEY)
  return draft || ''
}

// --- Dark mode detection ---
const checkDarkMode = () => {
  isDark.value = document.documentElement.classList.contains('dark')
}

// --- Image upload ---
const imgBeforeUpload = (file) => {
  if (!file.type.startsWith('image/')) return false
  if (file.size > 5 * 1024 * 1024) return false
  return true
}

const imgUploadSuccess = (res) => {
  if (res.code === 200) {
    const md = `![图片](${res.data})`
    insertAtCursor(md)
  }
}

// --- Watch for content changes to update highlight ---
watch(() => props.modelValue, () => {
  highlightCodeBlock()
})

// --- Lifecycle ---
onMounted(() => {
  checkDarkMode()
  startAutoSave()

  // Restore draft if content is empty (new article, not edit)
  if (!props.modelValue) {
    const draft = restoreDraft()
    if (draft) {
      emit('update:modelValue', draft)
    }
  }

  // Keyboard shortcut
  const handleKeydown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault()
      saveDraft()
    }
  }
  document.addEventListener('keydown', handleKeydown)

  // Watch dark mode changes
  const observer = new MutationObserver(() => checkDarkMode())
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })

  onUnmounted(() => {
    clearInterval(autoSaveTimer)
    document.removeEventListener('keydown', handleKeydown)
    observer.disconnect()
  })
})

// Expose clearDraft for parent component
defineExpose({ clearDraft })
</script>

<style scoped>
/* ============================================================
   Markdown Editor
   ============================================================ */
.md-editor {
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  display: flex;
  flex-direction: column;
  font-size: 14px;
  line-height: 1.5;
}

/* ---- Toolbar ---- */
.md-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 6px 10px;
  background: #f8f8fa;
  border-bottom: 1px solid #e8e8ec;
  flex-wrap: wrap;
  user-select: none;
}

.tb-group {
  display: inline-flex;
  align-items: center;
  gap: 1px;
}

.tb-sep {
  width: 1px;
  height: 22px;
  background: #e0e0e4;
  margin: 0 6px;
  flex-shrink: 0;
}

.tb-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 30px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #555;
  font-size: 0.9rem;
  transition: background 0.15s, color 0.15s;
  padding: 0;
}

.tb-btn:hover { background: #e8e8ee; color: #333; }
.tb-btn b { font-weight: 800; font-size: 1rem; }
.tb-btn i { font-style: italic; }
.tb-btn s { text-decoration: line-through; }
.tb-btn u { text-decoration: underline; }
.tb-btn sup { font-size: 0.75rem; }
.tb-btn sub { font-size: 0.75rem; }

.tb-view-btn.active {
  background: #e0dcf0;
  color: #6c5fa0;
}

.tb-group:deep(.el-upload) { display: inline-flex; }

.tb-divider {
  width: 1px;
  height: 18px;
  background: #ddd;
  margin: 0 4px;
}

/* ---- Body ---- */
.md-body {
  display: flex;
  flex: 1;
  min-height: 420px;
  position: relative;
  overflow: hidden;
}

/* Editor pane */
.md-editor-pane {
  display: flex;
  position: relative;
  overflow: hidden;
}

/* Line numbers gutter */
.md-gutter {
  width: 44px;
  flex-shrink: 0;
  background: #f8f8fa;
  border-right: 1px solid #ececf0;
  overflow: hidden;
  padding: 14px 0 14px;
  text-align: right;
  user-select: none;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.82rem;
  line-height: 1.65;
}

.gutter-line {
  padding: 0 8px 0 0;
  color: #bbb;
  font-size: 0.78rem;
}

/* Highlight layer + textarea overlay */
.md-input-wrap {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.md-highlight-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  margin: 0;
  padding: 14px 16px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.85rem;
  line-height: 1.65;
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow: auto;
  pointer-events: none;
  z-index: 1;
  color: transparent;
  background: transparent;
  border: none;
}

.md-highlight-layer code {
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  background: transparent !important;
  color: transparent !important;
}

/* Overlaid textarea */
.md-textarea {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  padding: 14px 16px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.85rem;
  line-height: 1.65;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: transparent;
  caret-color: #333;
  z-index: 2;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  -webkit-text-fill-color: transparent;
}

.md-textarea::placeholder {
  color: #ccc;
  -webkit-text-fill-color: #ccc;
}

.md-textarea:focus { outline: none; }

/* Drag handle */
.md-drag-handle {
  width: 8px;
  flex-shrink: 0;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f4;
  transition: background 0.2s;
  position: relative;
  z-index: 5;
}

.md-drag-handle:hover { background: #ddddf0; }

.drag-dots {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.drag-dots span {
  display: block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #bbb;
}

.drag-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  cursor: col-resize;
}

/* Preview pane */
.md-preview-pane {
  overflow-y: auto;
  padding: 14px 20px;
  background: #fff;
}

.md-preview-inner {
  max-width: 800px;
  margin: 0 auto;
}

/* ---- Status bar ---- */
.md-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 14px;
  background: #f8f8fa;
  border-top: 1px solid #e8e8ec;
  font-size: 0.78rem;
  color: #aaa;
}

.status-right { display: flex; align-items: center; gap: 12px; }
.save-saving { color: #999; animation: pulse 1s infinite; }
.save-saved { color: #67c23a; }

@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.5} }

/* ---- Fullscreen ---- */
.md-editor.is-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 2000;
  border-radius: 0;
  border: none;
}

.is-fullscreen .md-body {
  min-height: 0;
  flex: 1;
}

/* ---- Dark mode ---- */
.md-editor.md-dark {
  border-color: #24242c;
  background: #131316;
}

.md-dark .md-toolbar {
  background: #1a1a20;
  border-bottom-color: #24242c;
}

.md-dark .tb-btn { color: #aaa; }
.md-dark .tb-btn:hover { background: #2a2a34; color: #ece8e4; }
.md-dark .tb-sep { background: #2a2a34; }
.md-dark .tb-view-btn.active { background: #2a2438; color: #9e8ec0; }

.md-dark .md-gutter {
  background: #1a1a20;
  border-right-color: #24242c;
}
.md-dark .gutter-line { color: #555; }

.md-dark .md-textarea { caret-color: #ece8e4; }
.md-dark .md-textarea::placeholder { -webkit-text-fill-color: #555; }

.md-dark .md-preview-pane {
  background: #131316;
}

.md-dark .md-preview-inner :deep(.v-md-editor-preview) {
  background: transparent !important;
}

.md-dark .md-drag-handle { background: #1e1e26; }
.md-dark .md-drag-handle:hover { background: #2a2a38; }
.md-dark .drag-dots span { background: #555; }

.md-dark .md-status {
  background: #1a1a20;
  border-top-color: #24242c;
  color: #666;
}

.md-dark .tb-divider { background: #2a2a34; }

/* ---- Highlight picker popover ---- */
.highlight-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px;
}

.h-color {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.15s;
}

.h-color:hover { border-color: #666; }

.h-custom {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin-top: 6px;
  font-size: 0.8rem;
  color: #888;
}

.h-custom input[type="color"] {
  width: 30px;
  height: 30px;
  border: none;
  padding: 0;
  cursor: pointer;
  border-radius: 4px;
}

/* ---- Emoji panel ---- */
.emoji-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.emoji-item {
  font-size: 1.3rem;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  transition: background 0.15s;
  line-height: 1.3;
}

.emoji-item:hover { background: #eee; }

/* ---- Mode visibility ---- */
.mode-edit .md-preview-pane { display: none !important; }
.mode-preview .md-editor-pane { display: none !important; }
.mode-preview .md-drag-handle { display: none !important; }
</style>

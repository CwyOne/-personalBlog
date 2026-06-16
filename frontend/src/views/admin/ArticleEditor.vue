<template>
  <div class="article-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑文章' : '新建文章' }}</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        v-loading="pageLoading"
      >
        <!-- 标题 -->
        <el-form-item label="标题" prop="title">
          <div class="input-with-count">
            <el-input
              v-model="form.title"
              placeholder="请输入文章标题"
              :maxlength="TITLE_MAX"
              show-word-limit
              @input="onTitleInput"
            />
            <span :class="['char-counter', { 'at-limit': titleLen >= TITLE_MAX, 'over-limit': titleLen > TITLE_MAX }]">
              {{ titleLen }}/{{ TITLE_MAX }}
            </span>
          </div>
        </el-form-item>

        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文章摘要"
          />
        </el-form-item>

        <el-form-item label="封面图">
          <div class="cover-upload">
            <div class="cover-preview" v-if="coverPreviewUrl || form.coverImage">
              <img :src="coverPreviewUrl || form.coverImage" />
              <el-button type="danger" size="small" circle @click="clearCover">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            <div class="cover-actions">
              <input
                ref="coverInput"
                type="file"
                accept="image/*"
                style="display: none;"
                @change="handleCoverUpload"
              />
              <el-button @click="coverInput.click()">
                选择图片
              </el-button>
              <span class="cover-hint">或</span>
              <el-input v-model="form.coverImage" placeholder="输入图片URL" style="flex:1;" />
            </div>
          </div>
        </el-form-item>

        <!-- Markdown 编辑器 -->
        <el-form-item label="内容 (Markdown)" prop="contentMd">
          <!-- DOCX 上传按钮 -->
          <div class="docx-upload-bar">
            <input
              ref="docxInput"
              type="file"
              accept=".docx"
              style="display: none;"
              @change="handleDocxUpload"
            />
            <el-button
              :loading="docxUploading"
              @click="$refs.docxInput.click()"
              :disabled="docxUploading"
            >
              <el-icon style="margin-right: 4px;"><Upload /></el-icon>
              {{ docxUploading ? '解析中...' : '上传 DOCX 文档' }}
            </el-button>
            <span class="docx-hint">支持 .docx 格式，最大 10MB，自动转换为 Markdown</span>
          </div>
          <div class="editor-container" :class="{ 'limit-reached': contentLen >= CONTENT_MAX }">
            <div class="editor-pane">
              <div class="pane-header">
                <span class="pane-label">编辑</span>
                <span :class="['char-counter', { 'at-limit': contentLen >= CONTENT_MAX, 'over-limit': contentLen > CONTENT_MAX }]">
                  {{ contentLen }}/{{ CONTENT_MAX }}
                </span>
              </div>
              <textarea
                ref="textareaRef"
                v-model="form.contentMd"
                class="md-textarea"
                placeholder="使用 Markdown 编写文章内容..."
                @input="onContentInput"
                @keydown="handleTab"
              ></textarea>
              <div v-if="contentLen >= CONTENT_MAX" class="limit-toast">
                已达字数上限
              </div>
            </div>
            <div class="preview-pane">
              <div class="pane-header">
                <span class="pane-label">预览</span>
              </div>
              <div
                class="preview-content article-content"
                v-html="previewHtml"
              ></div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="发布状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="发布"
            inactive-text="草稿"
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="form.tagIds"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入新标签"
            style="width: 100%;"
            @change="handleTagChange"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-tooltip
            :visible="showSubmitTooltip"
            placement="top"
            :content="submitTooltipText"
            :disabled="!isOverLimit"
          >
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="isOverLimit"
              @click="handleSubmit"
            >
              {{ submitting ? '保存中...' : '保存' }}
            </el-button>
          </el-tooltip>
          <el-button @click="$router.push('/admin/articles')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import { createArticle, updateArticle, getArticleDetail, getTags, createTag, uploadImage, uploadDocx } from '@/utils/api'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

// ── Constants ──────────────────────────────────────────────
const TITLE_MAX = 80
const CONTENT_MAX = 15000

// ── Marked + highlight.js 配置 ────────────────────────────
const marked = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value
      }
      return hljs.highlightAuto(code).value
    }
  }),
  {
    gfm: true,
    breaks: true
  }
)

// ── State ──────────────────────────────────────────────────
const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const textareaRef = ref(null)
const pageLoading = ref(false)
const submitting = ref(false)
const coverInput = ref(null)
const pendingCoverFile = ref(null)
const coverPreviewUrl = ref('')
const tags = ref([])
const docxUploading = ref(false)
const docxInput = ref(null)

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  summary: '',
  coverImage: '',
  contentMd: '',
  status: 0,
  tagIds: []
})

// ── 字数统计 ──────────────────────────────────────────────
const titleLen = computed(() => form.title.length)
const contentLen = computed(() => form.contentMd.length)

const isOverLimit = computed(() =>
  titleLen.value > TITLE_MAX || contentLen.value > CONTENT_MAX
)

const showSubmitTooltip = ref(false)
const submitTooltipText = computed(() => {
  const parts = []
  if (titleLen.value > TITLE_MAX) parts.push(`标题超出 ${titleLen.value - TITLE_MAX} 字`)
  if (contentLen.value > CONTENT_MAX) parts.push(`正文超出 ${contentLen.value - CONTENT_MAX} 字`)
  return parts.join('；')
})

// ── 输入拦截 ──────────────────────────────────────────────
function onTitleInput(val) {
  if (val.length > TITLE_MAX) {
    form.title = val.slice(0, TITLE_MAX)
  }
}

// ── DOCX 上传 ────────────────────────────────────────────
async function handleDocxUpload(e) {
  const file = e.target.files[0]
  if (!file) return

  // 格式校验
  if (!file.name.toLowerCase().endsWith('.docx')) {
    ElMessage.warning('仅支持 .docx 格式的 Word 文档')
    docxInput.value.value = ''
    return
  }

  // 大小校验
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 10MB')
    docxInput.value.value = ''
    return
  }

  docxUploading.value = true
  try {
    const res = await uploadDocx(file)
    const markdown = res.data
    form.contentMd = markdown
    ElMessage.success('文档转换成功，内容已回填至编辑器')
  } catch {
    // 错误消息已由 request.js 拦截器处理
  } finally {
    docxUploading.value = false
    docxInput.value.value = ''
  }
}

function onContentInput() {
  if (form.contentMd.length > CONTENT_MAX) {
    form.contentMd = form.contentMd.slice(0, CONTENT_MAX)
  }
}

// Tab 键插入缩进
function handleTab(e) {
  if (e.key === 'Tab') {
    e.preventDefault()
    const ta = textareaRef.value
    if (!ta) return
    const start = ta.selectionStart
    const end = ta.selectionEnd
    form.contentMd = form.contentMd.substring(0, start) + '  ' + form.contentMd.substring(end)
    requestAnimationFrame(() => {
      ta.selectionStart = ta.selectionEnd = start + 2
    })
  }
}

// ── 校验规则 ──────────────────────────────────────────────
const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && value.length > TITLE_MAX) {
          callback(new Error(`标题不能超过 ${TITLE_MAX} 字`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  contentMd: [
    { required: true, message: '请输入文章内容', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && value.length > CONTENT_MAX) {
          callback(new Error(`正文不能超过 ${CONTENT_MAX} 字`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// ── Markdown 预览 ─────────────────────────────────────────
const previewHtml = computed(() => {
  if (!form.contentMd) return '<p style="color:#999;">预览区域</p>'
  try {
    return marked.parse(form.contentMd)
  } catch {
    return '<p style="color:#e74c3c;">Markdown 解析出错</p>'
  }
})

// ── 封面图 ────────────────────────────────────────────────
function handleCoverUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif' || file.type === 'image/webp'
  if (!isImage) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WebP 格式的图片')
    coverInput.value.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    coverInput.value.value = ''
    return
  }
  pendingCoverFile.value = file
  if (coverPreviewUrl.value) URL.revokeObjectURL(coverPreviewUrl.value)
  coverPreviewUrl.value = URL.createObjectURL(file)
  ElMessage.success('封面已选择，保存文章时上传')
}

function clearCover() {
  form.coverImage = ''
  pendingCoverFile.value = null
  if (coverPreviewUrl.value) {
    URL.revokeObjectURL(coverPreviewUrl.value)
    coverPreviewUrl.value = ''
  }
  coverInput.value.value = ''
}

// ── 标签 ──────────────────────────────────────────────────
function fetchTags() {
  getTags().then(res => {
    tags.value = res.data || []
  })
}

async function handleTagChange(val) {
  const newNames = val.filter(v => typeof v === 'string')
  if (newNames.length === 0) return

  for (const name of newNames) {
    try {
      const res = await createTag(name.trim())
      const newTag = res.data
      tags.value.push(newTag)
      const idx = form.tagIds.indexOf(name)
      if (idx !== -1) {
        form.tagIds.splice(idx, 1, newTag.id)
      }
    } catch {
      const idx = form.tagIds.indexOf(name)
      if (idx !== -1) form.tagIds.splice(idx, 1)
    }
  }
}

// ── 文章加载 ──────────────────────────────────────────────
function fetchArticle() {
  if (!isEdit.value) return
  const id = route.params.id
  pageLoading.value = true
  getArticleDetail(id)
    .then(res => {
      const article = res.data
      form.title = article.title || ''
      form.summary = article.summary || ''
      form.coverImage = article.coverImage || ''
      form.contentMd = article.contentMd || ''
      form.status = article.status
      form.tagIds = (article.tags || []).map(t => t.id)
    })
    .finally(() => {
      pageLoading.value = false
    })
}

// ── 提交 ──────────────────────────────────────────────────
async function handleSubmit() {
  // 字数前置校验
  if (isOverLimit.value) {
    showSubmitTooltip.value = true
    setTimeout(() => { showSubmitTooltip.value = false }, 3000)
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (pendingCoverFile.value) {
      const res = await uploadImage(pendingCoverFile.value)
      form.coverImage = res.data.url
      pendingCoverFile.value = null
      if (coverPreviewUrl.value) {
        URL.revokeObjectURL(coverPreviewUrl.value)
        coverPreviewUrl.value = ''
      }
    }
    const data = {
      title: form.title,
      summary: form.summary,
      coverImage: form.coverImage,
      contentMd: form.contentMd,
      status: form.status,
      tagIds: form.tagIds
    }
    const action = isEdit.value
      ? updateArticle(route.params.id, data)
      : createArticle(data)
    await action
    ElMessage.success(isEdit.value ? '文章已更新' : '文章已创建')
    router.push('/admin/articles')
  } finally {
    submitting.value = false
  }
}

// ── Lifecycle ─────────────────────────────────────────────
onMounted(() => {
  fetchTags()
  fetchArticle()
})

onUnmounted(() => {
  if (coverPreviewUrl.value) URL.revokeObjectURL(coverPreviewUrl.value)
})
</script>

<style scoped>
.article-editor {
  max-width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* ── 字数统计 ─────────────────────────── */
.char-counter {
  font-size: 12px;
  color: #999;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  transition: color 0.2s;
}

.char-counter.at-limit {
  color: #e6a23c;
}

.char-counter.over-limit {
  color: #f56c6c;
  font-weight: 600;
}

.input-with-count {
  width: 100%;
  position: relative;
}

.input-with-count .char-counter {
  position: absolute;
  right: 60px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

/* ── 封面图 ───────────────────────────── */
.cover-upload {
  width: 100%;
}

.cover-preview {
  position: relative;
  display: inline-block;
  margin-bottom: 10px;
}

.cover-preview img {
  width: 200px;
  height: 120px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--gray-200, #e0e0e0);
}

.cover-preview .el-button {
  position: absolute;
  top: -8px;
  right: -8px;
}

.cover-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cover-hint {
  color: #999;
  font-size: 13px;
}

/* ── DOCX 上传栏 ─────────────────────── */
.docx-upload-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: var(--gray-50, #f9fafb);
  border: 1px dashed var(--gray-200, #d1d5de);
  border-radius: 8px;
  transition: background-color 0.3s;
}

.docx-hint {
  font-size: 12px;
  color: #999;
}

/* ── 编辑器容器 ───────────────────────── */
.editor-container {
  display: flex;
  gap: 1px;
  background: var(--gray-200, #ddd);
  border: 1px solid var(--gray-200, #ddd);
  border-radius: 6px;
  overflow: hidden;
  min-height: 500px;
  transition: border-color 0.2s;
}

.editor-container.limit-reached {
  border-color: #f56c6c;
}

.editor-pane,
.preview-pane {
  flex: 1;
  background: var(--bg-card, #fff);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.pane-header {
  padding: 8px 16px;
  font-size: 13px;
  color: var(--gray-600, #666);
  background: var(--gray-50, #fafafa);
  border-bottom: 1px solid var(--gray-200, #eee);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.pane-label {
  font-weight: 500;
}

/* ── 文本输入框 ───────────────────────── */
.md-textarea {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.7;
  tab-size: 2;
  color: var(--gray-800, #1a1a2e);
  background: var(--bg-card, #fff);
}

.md-textarea::placeholder {
  color: #b0b5c0;
}

/* 字数上限提示 */
.limit-toast {
  padding: 6px 16px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  border-top: 1px solid #fde2e2;
  animation: toastPulse 2s ease-in-out infinite;
}

@keyframes toastPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* ── 预览区 ───────────────────────────── */
.preview-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-width: 100%;
}

/* 预览区内代码块样式 */
.preview-content :deep(pre) {
  background: #1e2028;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  margin: 1em 0;
  border: 1px solid #2a2d36;
}

.preview-content :deep(pre code) {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #c9d1d9;
  background: none;
  padding: 0;
  border-radius: 0;
}

.preview-content :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  background: #f0f1f4;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.88em;
  color: #c9596d;
}

/* 预览区表格 */
.preview-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  font-size: 14px;
}

.preview-content :deep(th),
.preview-content :deep(td) {
  border: 1px solid #e8eaef;
  padding: 10px 14px;
  text-align: left;
}

.preview-content :deep(th) {
  background: #f4f5f7;
  font-weight: 600;
  font-size: 13px;
}

.preview-content :deep(tr:nth-child(even)) {
  background: #fafbfc;
}

/* 预览区引用块 */
.preview-content :deep(blockquote) {
  border-left: 3px solid #7884c9;
  padding: 0.6em 1.2em;
  margin: 1em 0;
  color: #555b6a;
  background: #eef0f8;
  border-radius: 0 6px 6px 0;
}

/* 预览区链接 */
.preview-content :deep(a) {
  color: #5b6abf;
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* ── 响应式 ───────────────────────────── */
@media (max-width: 768px) {
  .editor-container {
    flex-direction: column;
    min-height: auto;
  }

  .editor-pane {
    min-height: 300px;
  }

  .md-textarea {
    height: 280px;
  }

  .preview-pane {
    min-height: 250px;
  }

  .preview-content {
    min-height: 250px;
  }

  .cover-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .cover-hint {
    display: none;
  }

  .input-with-count .char-counter {
    right: 12px;
    top: auto;
    bottom: -20px;
    transform: none;
  }
}

/* ── 深色模式 ─────────────────────────── */
.dark-mode .editor-container {
  background: var(--gray-200, #2a2d36);
  border-color: var(--gray-200, #2a2d36);
}

.dark-mode .editor-pane,
.dark-mode .preview-pane {
  background: var(--bg-card, #1e2028);
}

.dark-mode .pane-header {
  background: var(--gray-50, #181a20);
  color: var(--gray-500, #9498a6);
  border-bottom-color: var(--gray-200, #2a2d36);
}

.dark-mode .md-textarea {
  background: var(--bg-card, #1e2028);
  color: var(--gray-700, #e5e7ed);
}

.dark-mode .md-textarea::placeholder {
  color: var(--gray-400, #555b6a);
}

.dark-mode .docx-upload-bar {
  background: var(--bg-card, #1e2028);
  border-color: var(--gray-200, #2a2d36);
}

.dark-mode .docx-hint {
  color: var(--gray-400, #6b7080);
}

.dark-mode .preview-content {
  color: var(--gray-700, #e5e7ed);
}

.dark-mode .preview-content :deep(h1),
.dark-mode .preview-content :deep(h2),
.dark-mode .preview-content :deep(h3),
.dark-mode .preview-content :deep(h4) {
  color: var(--gray-800, #f0f1f4);
}

.dark-mode .preview-content :deep(p) {
  color: var(--gray-700, #e5e7ed);
}

.dark-mode .preview-content :deep(pre) {
  background: #14161c;
  border-color: var(--gray-200, #2a2d36);
}

.dark-mode .preview-content :deep(pre code) {
  color: #c9d1d9;
}

.dark-mode .preview-content :deep(code) {
  background: var(--gray-100, #252830);
  color: #c9596d;
}

.dark-mode .preview-content :deep(blockquote) {
  background: var(--indigo-bg, #2a2d3a);
  color: var(--gray-500, #9498a6);
  border-left-color: var(--indigo, #5b6abf);
}

.dark-mode .preview-content :deep(th),
.dark-mode .preview-content :deep(td) {
  border-color: var(--gray-200, #2a2d36);
}

.dark-mode .preview-content :deep(th) {
  background: var(--gray-100, #252830);
  color: var(--gray-700, #e5e7ed);
}

.dark-mode .preview-content :deep(tr:nth-child(even)) {
  background: rgba(255, 255, 255, 0.02);
}

.dark-mode .preview-content :deep(a) {
  color: var(--indigo-soft, #7884c9);
}

.dark-mode .limit-toast {
  background: #2d1f1f;
  color: #f56c6c;
  border-top-color: #3d2020;
}

.dark-mode .char-counter {
  color: var(--gray-400, #6b7080);
}

.dark-mode .cover-hint {
  color: var(--gray-400, #6b7080);
}

.dark-mode .editor-container.limit-reached {
  border-color: #f56c6c;
}
</style>

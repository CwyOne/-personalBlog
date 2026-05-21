<template>
  <div class="article-editor">
    <el-card>
      <template #header>
        <span>{{ isEdit ? '编辑文章' : '新建文章' }}</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        v-loading="pageLoading"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
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

        <el-form-item label="内容 (Markdown)" prop="contentMd">
          <div class="editor-container">
            <div class="editor-pane">
              <div class="pane-header">编辑</div>
              <textarea
                v-model="form.contentMd"
                class="md-textarea"
                placeholder="使用 Markdown 编写文章内容..."
              ></textarea>
            </div>
            <div class="preview-pane">
              <div class="pane-header">预览</div>
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
            placeholder="选择或创建标签"
            style="width: 100%;"
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
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ submitting ? '保存中...' : '保存' }}
          </el-button>
          <el-button @click="$router.push('/admin/articles')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { createArticle, updateArticle, getArticleDetail, getTags, uploadImage } from '@/utils/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const pageLoading = ref(false)
const submitting = ref(false)
const coverInput = ref(null)
const pendingCoverFile = ref(null)
const coverPreviewUrl = ref('')
const tags = ref([])

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  summary: '',
  coverImage: '',
  contentMd: '',
  status: 0,
  tagIds: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  contentMd: [{ required: true, message: '请输入文章内容', trigger: 'blur' }]
}

const previewHtml = computed(() => {
  if (!form.contentMd) return '<p style="color:#999;">预览区域</p>'
  return marked(form.contentMd)
})

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

function fetchTags() {
  getTags().then(res => {
    tags.value = res.data || []
  })
}

function fetchArticle() {
  if (!isEdit.value) return
  const id = route.params.id
  pageLoading.value = true
  getArticleDetail(id)
    .then(res => {
      const article = res.data
      form.title = article.title
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

async function handleSubmit() {
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
    router.push('/admin/articles')
  } finally {
    submitting.value = false
  }
}

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
  border: 1px solid #e0e0e0;
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

.editor-container {
  display: flex;
  gap: 1px;
  background: #ddd;
  border: 1px solid #ddd;
  border-radius: 6px;
  overflow: hidden;
  min-height: 500px;
}

.editor-pane,
.preview-pane {
  flex: 1;
  background: #fff;
}

.pane-header {
  padding: 8px 16px;
  font-size: 13px;
  color: #666;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

.md-textarea {
  width: 100%;
  height: 460px;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  tab-size: 2;
}

.preview-content {
  height: 460px;
  overflow-y: auto;
  padding: 16px;
  max-width: 100%;
}
</style>

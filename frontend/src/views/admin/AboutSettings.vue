<template>
  <div class="about-settings">
    <el-card v-loading="loading" class="settings-card">
      <template #header><span>关于页面设置</span></template>

      <!-- Avatar & Nickname -->
      <div class="section">
        <h3 class="section-title">基本信息</h3>
        <div class="avatar-row">
          <img :src="previewUrl || form.avatar || defaultAvatar" class="profile-avatar" @error="e => e.target.src = defaultAvatar" />
          <div class="avatar-actions">
            <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFileSelect" />
            <el-button size="small" @click="$refs.fileInput.click()">更换头像</el-button>
            <span class="avatar-hint">建议尺寸 200x200，支持 JPG/PNG/GIF/WebP</span>
          </div>
        </div>
        <el-form :model="form" label-width="80px" style="max-width:500px;margin-top:16px;">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="站长昵称（会同步到导航栏等位置）" />
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <!-- About me -->
      <div class="section">
        <h3 class="section-title">关于我</h3>
        <el-input
          v-model="form.bio"
          type="textarea"
          :rows="6"
          placeholder="介绍你自己..."
        />
      </div>

      <el-divider />

      <!-- Tech stack -->
      <div class="section">
        <h3 class="section-title">技术栈</h3>
        <div class="tech-editor">
          <div class="tech-tags">
            <el-tag
              v-for="(t, i) in techList"
              :key="i"
              closable
              size="large"
              class="tech-tag"
              @close="removeTech(i)"
            >
              {{ t }}
            </el-tag>
          </div>
          <div class="tech-add">
            <el-input
              v-model="newTech"
              placeholder="输入技术名称后按回车添加"
              size="small"
              style="width:220px"
              @keyup.enter="addTech"
            />
            <el-button size="small" @click="addTech" :disabled="!newTech.trim()">添加</el-button>
          </div>
        </div>
      </div>

      <el-divider />

      <!-- Contact -->
      <div class="section">
        <h3 class="section-title">联系方式</h3>
        <el-form :model="form" label-width="80px" style="max-width:500px;">
          <el-form-item label="公开邮箱">
            <el-input v-model="form.publicEmail" placeholder="对外展示的邮箱" />
          </el-form-item>
          <el-form-item label="GitHub">
            <el-input v-model="form.github" placeholder="GitHub 主页地址" />
          </el-form-item>
          <el-form-item label="Twitter">
            <el-input v-model="form.twitter" placeholder="Twitter 主页地址" />
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <!-- Site Appearance -->
      <div class="section">
        <h3 class="section-title">网站外观</h3>

        <div class="appearance-row">
          <div class="appearance-item">
            <label class="appearance-label">网站图标 (Favicon)</label>
            <div class="appearance-preview">
              <img :src="faviconPreview || form.favicon || defaultFavicon" class="favicon-preview" @error="e => e.target.src = defaultFavicon" />
            </div>
            <div class="appearance-actions">
              <input ref="faviconInput" type="file" accept="image/*" style="display:none" @change="handleFaviconSelect" />
              <el-button size="small" @click="$refs.faviconInput.click()">上传图标</el-button>
              <span class="avatar-hint">建议 32x32 或 64x64，支持 ICO/PNG/SVG</span>
            </div>
          </div>

          <div class="appearance-item">
            <label class="appearance-label">登录页背景图</label>
            <div class="appearance-preview login-bg-preview" :style="loginBgPreviewStyle">
              <span v-if="!loginBgPreview && !form.loginBg" class="preview-placeholder">未设置</span>
            </div>
            <div class="appearance-actions">
              <input ref="loginBgInput" type="file" accept="image/*" style="display:none" @change="handleLoginBgSelect" />
              <el-button size="small" @click="$refs.loginBgInput.click()">上传背景图</el-button>
              <span class="avatar-hint">建议 1920x1080，支持 JPG/PNG/WebP</span>
            </div>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="form-actions">
        <el-button type="primary" size="large" :loading="saving" @click="handleSave">保存设置</el-button>
        <el-button size="large" @click="$router.push('/')">返回首页</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminAbout, updateAdminAbout, adminUploadAvatar, adminUploadFavicon, adminUploadLoginBg } from '@/utils/api'
import { updateUser, currentUser } from '@/store/user'
import { refreshSiteInfo, updateSiteInfo } from '@/store/site'

const loading = ref(false)
const saving = ref(false)
const newTech = ref('')
const previewUrl = ref('')
const pendingFile = ref(null)
const fileInput = ref(null)
const faviconPreview = ref('')
const loginBgPreview = ref('')
const pendingFavicon = ref(null)
const pendingLoginBg = ref(null)
const faviconInput = ref(null)
const loginBgInput = ref(null)

const loginBgPreviewStyle = computed(() => {
  const url = loginBgPreview.value || form.loginBg
  if (url) {
    return { backgroundImage: `url(${url})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
})

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'
const defaultFavicon = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" rx="4" fill="%23409eff"/><text x="16" y="22" text-anchor="middle" font-size="18" font-weight="bold" fill="white">B</text></svg>'

const form = reactive({
  nickname: '',
  avatar: '',
  bio: '',
  techStack: '[]',
  publicEmail: '',
  github: '',
  twitter: '',
  favicon: '',
  loginBg: ''
})

const techList = ref([])

watch(() => form.techStack, (val) => {
  try { techList.value = JSON.parse(val || '[]') } catch { techList.value = [] }
}, { immediate: true })

function addTech() {
  const name = newTech.value.trim()
  if (!name) return
  if (techList.value.includes(name)) {
    ElMessage.warning('该技术已存在')
    return
  }
  techList.value.push(name)
  form.techStack = JSON.stringify(techList.value)
  newTech.value = ''
}

function removeTech(index) {
  techList.value.splice(index, 1)
  form.techStack = JSON.stringify(techList.value)
}

function handleFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!isImage) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WebP 格式')
    fileInput.value.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    fileInput.value.value = ''
    return
  }
  pendingFile.value = file
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
}

function handleFaviconSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/x-icon', 'image/vnd.microsoft.icon', 'image/svg+xml'].includes(file.type)
  if (!isImage) {
    ElMessage.warning('仅支持图片或图标格式')
    faviconInput.value.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图标大小不能超过 5MB')
    faviconInput.value.value = ''
    return
  }
  pendingFavicon.value = file
  if (faviconPreview.value) URL.revokeObjectURL(faviconPreview.value)
  faviconPreview.value = URL.createObjectURL(file)
}

function handleLoginBgSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!isImage) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WebP 格式')
    loginBgInput.value.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    loginBgInput.value.value = ''
    return
  }
  pendingLoginBg.value = file
  if (loginBgPreview.value) URL.revokeObjectURL(loginBgPreview.value)
  loginBgPreview.value = URL.createObjectURL(file)
}

async function handleSave() {
  saving.value = true
  try {
    if (pendingFile.value) {
      const res = await adminUploadAvatar(pendingFile.value)
      form.avatar = res.data.url
      pendingFile.value = null
      if (previewUrl.value) { URL.revokeObjectURL(previewUrl.value); previewUrl.value = '' }
    }
    if (pendingFavicon.value) {
      const res = await adminUploadFavicon(pendingFavicon.value)
      form.favicon = res.data.url
      pendingFavicon.value = null
      if (faviconPreview.value) { URL.revokeObjectURL(faviconPreview.value); faviconPreview.value = '' }
    }
    if (pendingLoginBg.value) {
      const res = await adminUploadLoginBg(pendingLoginBg.value)
      form.loginBg = res.data.url
      pendingLoginBg.value = null
      if (loginBgPreview.value) { URL.revokeObjectURL(loginBgPreview.value); loginBgPreview.value = '' }
    }
    await updateAdminAbout({
      nickname: form.nickname,
      avatar: form.avatar,
      bio: form.bio,
      techStack: form.techStack,
      publicEmail: form.publicEmail,
      github: form.github,
      twitter: form.twitter,
      favicon: form.favicon,
      loginBg: form.loginBg
    })
    ElMessage.success('保存成功')
    // Sync currentUser if admin is editing themselves
    updateUser({ nickname: form.nickname, avatar: form.avatar })
    // Immediately update site store so NavBar/footer react at once
    updateSiteInfo({ nickname: form.nickname, avatar: form.avatar, favicon: form.favicon, loginBg: form.loginBg })
    // Also refresh from server to get complete data
    await refreshSiteInfo()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loading.value = true
  getAdminAbout().then(res => {
    Object.assign(form, {
      nickname: res.data.nickname || '',
      avatar: res.data.avatar || '',
      bio: res.data.bio || '',
      techStack: res.data.techStack || '[]',
      publicEmail: res.data.publicEmail || '',
      github: res.data.github || '',
      twitter: res.data.twitter || '',
      favicon: res.data.favicon || '',
      loginBg: res.data.loginBg || ''
    })
  }).finally(() => { loading.value = false })
})

onUnmounted(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  if (faviconPreview.value) URL.revokeObjectURL(faviconPreview.value)
  if (loginBgPreview.value) URL.revokeObjectURL(loginBgPreview.value)
})
</script>

<style scoped>
.about-settings { max-width: 800px; }
.settings-card { border-radius: 12px; }
.section { margin-bottom: 8px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--text-primary); margin-bottom: 16px; padding-left: 10px; border-left: 3px solid #409eff; }

.avatar-row { display: flex; align-items: center; gap: 20px; }
.profile-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; border: 3px solid #eee; }
.avatar-actions { display: flex; flex-direction: column; gap: 6px; }
.avatar-hint { font-size: 12px; color: #999; }

.tech-editor { background: #f9f9f9; border-radius: 8px; padding: 16px; }
.tech-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; min-height: 32px; }
.tech-tag { cursor: default; }
.tech-add { display: flex; gap: 8px; }

.form-actions { display: flex; gap: 12px; padding-top: 8px; }

.appearance-row { display: flex; gap: 40px; flex-wrap: wrap; }
.appearance-item { display: flex; flex-direction: column; gap: 12px; }
.appearance-label { font-size: 14px; font-weight: 500; color: var(--text-secondary); }
.appearance-preview { width: 64px; height: 64px; border-radius: 8px; border: 2px dashed #ddd; display: flex; align-items: center; justify-content: center; overflow: hidden; background: #fafafa; }
.favicon-preview { width: 100%; height: 100%; object-fit: contain; }
.login-bg-preview { width: 200px; height: 120px; }
.preview-placeholder { font-size: 12px; color: #ccc; }
.appearance-actions { display: flex; flex-direction: column; gap: 6px; }
</style>

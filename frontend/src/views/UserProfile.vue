<template>
  <div class="profile-page">
    <NavBar />
    <main class="profile-container">
      <el-card v-loading="loading" class="profile-card">
        <template #header><span>个人中心</span></template>

        <div class="profile-header">
          <div class="avatar-section">
            <img :src="previewUrl || form.avatar || defaultAvatar" class="profile-avatar" @error="e => e.target.src = defaultAvatar" />
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              style="display: none;"
              @change="handleFileSelect"
            />
            <el-button size="small" class="upload-btn" @click="fileInput.click()">
              更换头像
            </el-button>
          </div>
          <div class="profile-info">
            <h2>{{ form.nickname || form.username }}</h2>
            <p>{{ form.email }} <el-tag v-if="form.emailVerified === 1" size="small" type="success">已验证</el-tag><el-tag v-else size="small" type="warning">未验证</el-tag></p>
            <p>注册时间：{{ form.createTime }}</p>
          </div>
        </div>

        <el-divider />

        <el-form ref="formRef" :model="form" label-width="100px" style="max-width: 500px;">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="设置昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.password" type="password" placeholder="留空则不修改" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            <el-button @click="$router.push('/')">返回首页</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, updateProfile, uploadAvatar } from '@/utils/api'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import { updateUser, isLoggedIn } from '@/store/user'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const fileInput = ref(null)
const pendingFile = ref(null)
const previewUrl = ref('')

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=default&backgroundColor=e0e0e0'

const form = reactive({
  nickname: '', phone: '', avatar: '', password: '',
  username: '', email: '', emailVerified: 0, createTime: ''
})

function fetchProfile() {
  loading.value = true
  getProfile()
    .then(res => { Object.assign(form, res.data); form.password = '' })
    .finally(() => { loading.value = false })
}

function handleFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif' || file.type === 'image/webp'
  if (!isImage) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WebP 格式的图片')
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

async function handleSave() {
  saving.value = true
  try {
    if (pendingFile.value) {
      const res = await uploadAvatar(pendingFile.value)
      form.avatar = res.data.url
      pendingFile.value = null
      fileInput.value.value = ''
    }
    const data = { nickname: form.nickname, phone: form.phone, avatar: form.avatar }
    if (form.password) data.password = form.password
    await updateProfile(data)
    ElMessage.success('保存成功')
    updateUser({ nickname: form.nickname, avatar: form.avatar })
    form.password = ''
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value)
      previewUrl.value = ''
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (!isLoggedIn.value) { router.push('/login?redirect=/user/profile'); return }
  fetchProfile()
})

onUnmounted(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<style scoped>
.profile-page { min-height: 100vh; background: var(--bg-secondary); transition: background-color 0.3s; }
.profile-container { max-width: 800px; margin: 0 auto; padding: 40px 20px; }
.profile-header { display: flex; align-items: flex-start; gap: 24px; }
.avatar-section { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.profile-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; border: 3px solid var(--border-color); }
.upload-btn { margin-top: 4px; }
.profile-info h2 { font-size: 22px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; transition: color 0.3s; }
.profile-info p { font-size: 14px; color: var(--text-muted); margin: 3px 0; }
</style>

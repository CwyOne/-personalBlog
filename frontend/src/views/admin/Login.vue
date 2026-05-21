<template>
  <div class="login-page" :style="loginBgStyle">
    <div class="login-card">
      <h1 class="login-logo">Blog</h1>

      <div class="tabs">
        <span :class="['tab', { active: activeTab === 'login' }]" @click="activeTab = 'login'">登录</span>
        <span :class="['tab', { active: activeTab === 'register' }]" @click="activeTab = 'register'">注册</span>
      </div>

      <!-- Login Form -->
      <el-form v-if="activeTab === 'login'" ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin" style="width: 100%;">{{ loading ? '登录中...' : '登录' }}</el-button>
        </el-form-item>
      </el-form>

      <!-- Register Form -->
      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请设置用户名（3-20位）" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请设置密码（至少6位）" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号（选填）" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" size="large" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="registerForm.code" placeholder="请输入邮箱验证码" size="large" @keyup.enter="handleRegister" />
            <el-button type="primary" :loading="sendingCode" :disabled="countdown > 0" @click="handleSendCode" size="large" class="code-btn">
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleRegister" style="width: 100%;">{{ loading ? '注册中...' : '注册' }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, register, sendCode } from '@/utils/api'
import { ElMessage } from 'element-plus'
import { setUser } from '@/store/user'
import { siteOwner, fetchSiteInfo } from '@/store/site'

const router = useRouter()
const route = useRoute()
const activeTab = ref('login')
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer = null

const loginBgStyle = computed(() => {
  if (siteOwner.value.loginBg) {
    return {
      background: `url(${siteOwner.value.loginBg}) center/cover no-repeat`
    }
  }
  return {}
})

const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerForm = reactive({
  username: '', password: '', confirmPassword: '',
  phone: '', email: '', code: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

function handleSendCode() {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  sendingCode.value = true
  sendCode(registerForm.email)
    .then(() => {
      ElMessage.success('验证码已发送，请查收邮箱')
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
          timer = null
        }
      }, 1000)
    })
    .finally(() => { sendingCode.value = false })
}

function handleLogin() {
  loginFormRef.value?.validate(valid => {
    if (!valid) return
    loading.value = true
    login({ username: loginForm.username, password: loginForm.password })
      .then(res => {
        localStorage.setItem('token', res.data.token)
        setUser(res.data.user)
        ElMessage({ message: `欢迎回来，${res.data.user.nickname || res.data.user.username}！`, type: 'success', duration: 2000, center: true })
        setTimeout(() => {
          const redirect = route.query.redirect
          if (redirect) router.push(redirect)
          else router.push('/')
        }, 600)
      })
      .finally(() => { loading.value = false })
  })
}

function handleRegister() {
  registerFormRef.value?.validate(valid => {
    if (!valid) return
    loading.value = true
    register({
      username: registerForm.username,
      password: registerForm.password,
      phone: registerForm.phone,
      email: registerForm.email,
      code: registerForm.code
    })
      .then(() => {
        ElMessage.success('注册成功，请登录')
        activeTab.value = 'login'
        loginForm.username = registerForm.username
        loginForm.password = ''
        registerForm.username = ''
        registerForm.password = ''
        registerForm.confirmPassword = ''
        registerForm.phone = ''
        registerForm.email = ''
        registerForm.code = ''
      })
      .finally(() => { loading.value = false })
  })
}

onMounted(() => {
  fetchSiteInfo()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}
.login-card {
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px;
  width: 420px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08);
}
.login-logo {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 24px;
}
.tabs {
  display: flex;
  border-bottom: 2px solid #eee;
  margin-bottom: 24px;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}
.tab.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 600;
}
.tab:hover { color: #409eff; }
.code-row {
  display: flex;
  gap: 10px;
}
.code-btn {
  min-width: 120px;
  flex-shrink: 0;
}
</style>

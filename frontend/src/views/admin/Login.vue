<template>
  <div class="login-page">
    <!-- Background: custom image (with GIF support) or animated gradient -->
    <div class="login-bg" :class="{ 'has-custom-bg': !!siteOwner.loginBg }">
      <div v-if="siteOwner.loginBg" class="custom-bg">
        <img :src="siteOwner.loginBg" class="custom-bg-img" alt="" />
      </div>
      <template v-else>
        <div class="bg-orb bg-orb-1"></div>
        <div class="bg-orb bg-orb-2"></div>
        <div class="bg-orb bg-orb-3"></div>
      </template>
      <div class="bg-grain"></div>
      <div class="bg-grid"></div>
      <!-- Floating particles -->
      <div class="bg-particles">
        <span class="particle p1"></span>
        <span class="particle p2"></span>
        <span class="particle p3"></span>
        <span class="particle p4"></span>
        <span class="particle p5"></span>
        <span class="particle p6"></span>
      </div>
      <!-- Vignette overlay -->
      <div class="bg-vignette"></div>
    </div>

    <!-- Back to home -->
    <router-link to="/" class="back-home">
      <span class="back-arrow">←</span>
      <span>返回首页</span>
    </router-link>

    <!-- Radial glow behind card -->
    <div class="card-glow" :class="{ 'glow-enter': entered }"></div>

    <!-- Login card -->
    <div class="login-card" :class="{ 'card-enter': entered }">
      <!-- Header -->
      <div class="card-header">
        <div class="card-logo">
          <span class="logo-mark">✦</span>
        </div>
        <h1 class="card-title">{{ siteOwner.nickname || '子墨' }}<span class="title-suffix">的博客</span></h1>
        <p class="card-subtitle">记录技术 · 思考与生活</p>
      </div>

      <!-- Tabs -->
      <div class="card-tabs">
        <button
          :class="['tab-btn', { active: activeTab === 'login' }]"
          @click="activeTab = 'login'"
        >
          登录
          <span class="tab-indicator" v-if="activeTab === 'login'"></span>
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'register' }]"
          @click="activeTab = 'register'"
        >
          注册
          <span class="tab-indicator" v-if="activeTab === 'register'"></span>
        </button>
      </div>

      <!-- Login Form -->
      <el-form
        v-if="activeTab === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        @submit.prevent="handleLogin"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="submit-btn"
          >
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Register Form -->
      <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        @submit.prevent="handleRegister"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请设置用户名（3-20位）" size="large" class="custom-input" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请设置密码（至少6位）" size="large" show-password class="custom-input" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password class="custom-input" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号（选填）" size="large" class="custom-input" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" size="large" class="custom-input" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="registerForm.code" placeholder="请输入邮箱验证码" size="large" @keyup.enter="handleRegister" class="custom-input" />
            <el-button
              :loading="sendingCode"
              :disabled="countdown > 0"
              @click="handleSendCode"
              size="large"
              class="code-btn"
            >
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            size="large"
            :loading="loading"
            @click="handleRegister"
            class="submit-btn"
          >
            <span v-if="!loading">注册</span>
            <span v-else>注册中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Decorative elements -->
    <div class="deco-corner deco-tl"></div>
    <div class="deco-corner deco-br"></div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
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
const entered = ref(false)
let timer = null

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
        sessionStorage.setItem('token', res.data.token)
        setUser(res.data.user)
        ElMessage({ message: `欢迎回来，${res.data.user.nickname || res.data.user.username}！`, type: 'success', duration: 2000, center: true })
        setTimeout(() => {
          const redirect = route.query.redirect
          if (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) {
            router.push(redirect)
          } else {
            router.push('/')
          }
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

onMounted(async () => {
  await fetchSiteInfo()
  requestAnimationFrame(() => {
    entered.value = true
  })
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ============================================================
   LOGIN PAGE — Refined Editorial Aesthetic
   ============================================================ */
.login-page {
  /* Login-specific tokens (slate palette not in global.css) */
  --indigo-deep: #4a58a8;
  --slate-50: #f8f9fc;
  --slate-100: #f0f1f6;
  --slate-200: #e2e4ed;
  --slate-700: #3a3f4b;
  --slate-800: #252830;
  --slate-900: #181a20;

  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 40px 20px;
}

/* ============================================================
   ANIMATED BACKGROUND — Layered Atmospheric
   ============================================================ */
.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: linear-gradient(160deg, #f0f1f8 0%, #e8eaf4 30%, #f5f3f0 60%, #ede9f4 100%);
}

.login-bg.has-custom-bg {
  background: #1a1a2e;
}

.custom-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

.custom-bg-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: orbFloat 20s ease-in-out infinite;
}

.bg-orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(91, 106, 191, 0.25), transparent 70%);
  top: -10%;
  right: -5%;
  animation-duration: 25s;
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(120, 132, 201, 0.2), transparent 70%);
  bottom: -10%;
  left: -5%;
  animation-duration: 30s;
  animation-delay: -5s;
}

.bg-orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(91, 106, 191, 0.15), transparent 70%);
  top: 40%;
  left: 30%;
  animation-duration: 22s;
  animation-delay: -10s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -40px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(15px, 30px) scale(1.02); }
}

.bg-grain {
  position: absolute;
  inset: 0;
  opacity: 0.3;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.04'/%3E%3C/svg%3E");
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(91, 106, 191, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(91, 106, 191, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}

/* Floating particles for atmospheric depth */
.bg-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(91, 106, 191, 0.3);
  animation: particleFloat linear infinite;
}

.p1 { top: 15%; left: 20%; animation-duration: 18s; animation-delay: 0s; width: 4px; height: 4px; opacity: 0.4; }
.p2 { top: 60%; left: 75%; animation-duration: 22s; animation-delay: -4s; width: 2px; height: 2px; opacity: 0.3; }
.p3 { top: 35%; left: 50%; animation-duration: 20s; animation-delay: -8s; width: 3px; height: 3px; opacity: 0.35; }
.p4 { top: 80%; left: 30%; animation-duration: 25s; animation-delay: -12s; width: 2px; height: 2px; opacity: 0.25; }
.p5 { top: 25%; left: 85%; animation-duration: 19s; animation-delay: -6s; width: 4px; height: 4px; opacity: 0.3; }
.p6 { top: 70%; left: 10%; animation-duration: 24s; animation-delay: -15s; width: 3px; height: 3px; opacity: 0.2; }

@keyframes particleFloat {
  0% { transform: translate(0, 0) scale(1); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translate(80px, -120px) scale(0.5); opacity: 0; }
}

/* Vignette for depth */
.bg-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 50%, rgba(0, 0, 0, 0.08) 100%);
  pointer-events: none;
}

/* Radial glow behind card */
.card-glow {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(91, 106, 191, 0.12) 0%, transparent 70%);
  filter: blur(60px);
  z-index: 1;
  pointer-events: none;
  opacity: 0;
  transition: opacity 1.2s ease;
}

.card-glow.glow-enter {
  opacity: 1;
}

/* ============================================================
   BACK HOME LINK
   ============================================================ */
.back-home {
  position: fixed;
  top: 28px;
  left: 32px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-family: var(--font-body);
  color: var(--slate-700);
  text-decoration: none;
  opacity: 0;
  animation: fadeIn 0.6s 0.2s forwards;
  transition: color 0.2s;
}

.back-home:hover {
  color: var(--indigo);
}

.back-arrow {
  font-size: 16px;
  transition: transform 0.2s;
}

.back-home:hover .back-arrow {
  transform: translateX(-3px);
}

/* ============================================================
   DECORATIVE CORNERS
   ============================================================ */
.deco-corner {
  position: fixed;
  z-index: 1;
  pointer-events: none;
  opacity: 0;
  animation: fadeIn 1s 0.8s forwards;
}

.deco-tl {
  top: 60px;
  left: 32px;
  width: 80px;
  height: 80px;
  border-top: 1.5px solid rgba(91, 106, 191, 0.15);
  border-left: 1.5px solid rgba(91, 106, 191, 0.15);
}

.deco-br {
  bottom: 40px;
  right: 32px;
  width: 80px;
  height: 80px;
  border-bottom: 1.5px solid rgba(91, 106, 191, 0.15);
  border-right: 1.5px solid rgba(91, 106, 191, 0.15);
}

/* ============================================================
   LOGIN CARD — Frosted Glass with Depth
   ============================================================ */
.login-card {
  position: relative;
  z-index: 5;
  width: 100%;
  max-width: 440px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px) saturate(1.5);
  -webkit-backdrop-filter: blur(24px) saturate(1.5);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: 44px 40px 36px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.02),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  opacity: 0;
  transform: translateY(24px) scale(0.98);
  transition: opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1),
              transform 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}

.login-card.card-enter {
  opacity: 1;
  transform: translateY(0) scale(1);
}

/* ============================================================
   CARD HEADER
   ============================================================ */
.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-logo {
  margin-bottom: 16px;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 22px;
  color: var(--indigo);
  background: var(--indigo-bg);
  border-radius: 14px;
  border: 1px solid rgba(91, 106, 191, 0.12);
  animation: logoBreathe 4s ease-in-out infinite;
}

@keyframes logoBreathe {
  0%, 100% { box-shadow: 0 0 0 0 rgba(91, 106, 191, 0); }
  50% { box-shadow: 0 0 20px 4px rgba(91, 106, 191, 0.15); }
}

.card-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 400;
  color: var(--slate-900);
  letter-spacing: -0.03em;
  margin: 0 0 6px;
  line-height: 1.2;
}

.title-suffix {
  color: var(--indigo-soft);
}

.card-subtitle {
  font-size: 13px;
  color: #8a8fa0;
  letter-spacing: 0.06em;
  margin: 0;
}

/* ============================================================
   TABS
   ============================================================ */
.card-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 28px;
  position: relative;
}

.card-tabs::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
}

.tab-btn {
  flex: 1;
  position: relative;
  background: none;
  border: none;
  padding: 12px 0;
  font-size: 15px;
  font-family: var(--font-body);
  color: #a0a5b2;
  cursor: pointer;
  transition: color 0.3s;
  font-weight: 500;
}

.tab-btn:hover {
  color: var(--slate-700);
}

.tab-btn.active {
  color: var(--indigo);
  font-weight: 600;
}

.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 20%;
  right: 20%;
  height: 2px;
  background: var(--indigo);
  border-radius: 2px 2px 0 0;
  animation: tabSlideIn 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes tabSlideIn {
  from { transform: scaleX(0); opacity: 0; }
  to { transform: scaleX(1); opacity: 1; }
}

/* ============================================================
   FORM STYLING — Refined Inputs
   ============================================================ */
.login-form {
  animation: formFadeIn 0.35s ease;
}

@keyframes formFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: var(--slate-700);
  padding-bottom: 4px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.6);
  padding: 4px 14px;
  transition: box-shadow 0.25s, background 0.25s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(91, 106, 191, 0.2);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(91, 106, 191, 0.15), 0 0 0 1px var(--indigo-soft);
  background: rgba(255, 255, 255, 0.85);
}

.login-form :deep(.el-input__inner) {
  font-size: 14px;
  font-family: var(--font-body);
  color: var(--slate-800);
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #b0b5c2;
}

/* Submit button with shimmer */
.submit-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--indigo) 0%, var(--indigo-deep) 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: var(--font-body);
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow: 0 2px 12px rgba(91, 106, 191, 0.25);
  margin-top: 4px;
  position: relative;
  overflow: hidden;
}

.submit-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0% { left: -100%; }
  50% { left: 100%; }
  100% { left: 100%; }
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(91, 106, 191, 0.35);
  background: linear-gradient(135deg, var(--indigo-soft) 0%, var(--indigo) 100%);
}

.submit-btn:active {
  transform: translateY(0);
  box-shadow: 0 1px 8px rgba(91, 106, 191, 0.2);
}

/* Code row */
.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-btn {
  min-width: 120px;
  flex-shrink: 0;
  border-radius: 10px !important;
  border-color: var(--indigo-soft) !important;
  color: var(--indigo) !important;
  font-weight: 500 !important;
  transition: all 0.2s !important;
}

.code-btn:hover:not(:disabled) {
  background: var(--indigo-bg) !important;
}

/* ============================================================
   ENTRANCE ANIMATION
   ============================================================ */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ============================================================
   DARK MODE
   ============================================================ */
:root[data-theme="dark"] .login-page {
  --slate-50: #181a20;
  --slate-100: #1e2028;
  --slate-200: #2a2d36;
  --slate-700: #e5e7ed;
  --slate-800: #f0f1f4;
  --slate-900: #f8f9fb;
}

:root[data-theme="dark"] .login-bg {
  background: linear-gradient(160deg, #14161c 0%, #1a1d26 30%, #181a22 60%, #16181f 100%);
}

:root[data-theme="dark"] .login-bg.has-custom-bg {
  background: #0a0a12;
}

:root[data-theme="dark"] .bg-orb-1 {
  background: radial-gradient(circle, rgba(91, 106, 191, 0.12), transparent 70%);
}

:root[data-theme="dark"] .bg-orb-2 {
  background: radial-gradient(circle, rgba(120, 132, 201, 0.08), transparent 70%);
}

:root[data-theme="dark"] .bg-orb-3 {
  background: radial-gradient(circle, rgba(91, 106, 191, 0.06), transparent 70%);
}

:root[data-theme="dark"] .bg-grid {
  background-image:
    linear-gradient(rgba(91, 106, 191, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(91, 106, 191, 0.04) 1px, transparent 1px);
}

:root[data-theme="dark"] .login-card {
  background: rgba(30, 32, 40, 0.75);
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.15),
    0 1px 2px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

:root[data-theme="dark"] .card-title {
  color: #f0f1f4;
}

:root[data-theme="dark"] .card-subtitle {
  color: #6b7080;
}

:root[data-theme="dark"] .tab-btn {
  color: #6b7080;
}

:root[data-theme="dark"] .tab-btn:hover {
  color: #c5c8d2;
}

:root[data-theme="dark"] .card-tabs::after {
  background: rgba(255, 255, 255, 0.06);
}

:root[data-theme="dark"] .login-form :deep(.el-form-item__label) {
  color: #c5c8d2;
}

:root[data-theme="dark"] .login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08);
}

:root[data-theme="dark"] .login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(91, 106, 191, 0.25);
}

:root[data-theme="dark"] .login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(91, 106, 191, 0.2), 0 0 0 1px var(--indigo-soft);
  background: rgba(255, 255, 255, 0.06);
}

:root[data-theme="dark"] .login-form :deep(.el-input__inner) {
  color: #e5e7ed;
}

:root[data-theme="dark"] .login-form :deep(.el-input__inner::placeholder) {
  color: #555b6a;
}

:root[data-theme="dark"] .back-home {
  color: #9498a6;
}

:root[data-theme="dark"] .back-home:hover {
  color: var(--indigo-soft);
}

:root[data-theme="dark"] .deco-tl,
:root[data-theme="dark"] .deco-br {
  border-color: rgba(91, 106, 191, 0.1);
}

:root[data-theme="dark"] .logo-mark {
  background: rgba(91, 106, 191, 0.1);
  border-color: rgba(91, 106, 191, 0.15);
}

:root[data-theme="dark"] .particle {
  background: rgba(91, 106, 191, 0.2);
}

:root[data-theme="dark"] .bg-vignette {
  background: radial-gradient(ellipse at center, transparent 50%, rgba(0, 0, 0, 0.2) 100%);
}

:root[data-theme="dark"] .card-glow {
  background: radial-gradient(circle, rgba(91, 106, 191, 0.08) 0%, transparent 70%);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px 28px;
    border-radius: 16px;
  }

  .card-title {
    font-size: 24px;
  }

  .deco-corner {
    display: none;
  }

  .back-home {
    left: 20px;
    top: 20px;
  }
}
</style>

<template>
  <header class="navbar" :class="{ scrolled: isScrolled }">
    <div class="navbar-inner">
      <router-link to="/" class="brand">
        <img :src="brandAvatar" class="brand-avatar" alt="avatar" @error="e => e.target.src = defaultAvatar" />
        <span class="brand-name">{{ brandName }}</span>
      </router-link>

      <nav class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/archive" class="nav-link">归档</router-link>
        <router-link to="/about" class="nav-link">关于</router-link>

        <span class="theme-toggle" @click="toggleTheme" :title="isDark ? '切换到浅色模式' : '切换到深色模式'">
          <el-icon :size="18"><Moon v-if="isDark" /><Sunny v-else /></el-icon>
        </span>

        <router-link v-if="isAdmin" to="/admin/articles" class="nav-link">后台</router-link>

        <template v-if="!isLoggedIn">
          <router-link to="/login" class="nav-link login-btn">登录</router-link>
        </template>

        <template v-else>
          <el-dropdown trigger="click">
            <span class="user-dropdown">
              <img :src="currentUser.avatar || defaultAvatar" class="user-avatar" @error="e => e.target.src = defaultAvatar" />
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ currentUser.nickname || currentUser.username }}</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/user/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { currentUser, isLoggedIn, isAdmin, clearUser } from '@/store/user'
import { useTheme } from '@/store/theme'
import { siteOwner, fetchSiteInfo } from '@/store/site'

const router = useRouter()
const { isDark, toggle: toggleTheme } = useTheme()

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'
const isScrolled = ref(false)

const brandName = computed(() => {
  const name = siteOwner.value.nickname || '子墨'
  return name + '的博客'
})

const brandAvatar = computed(() => siteOwner.value.avatar || defaultAvatar)

function onScroll() {
  isScrolled.value = window.scrollY > 60
}

onMounted(() => {
  fetchSiteInfo()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})

function handleLogout() {
  clearUser()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 0 32px;
  height: 64px;
  display: flex;
  align-items: center;
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  background: transparent;
}

.navbar.scrolled {
  background: rgba(245, 247, 250, 0.78);
  backdrop-filter: blur(16px) saturate(1.4);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}

.navbar-inner {
  max-width: 1120px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.brand-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--gray-800);
  letter-spacing: -0.02em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
}

.nav-link {
  font-size: 14px;
  color: var(--gray-500);
  text-decoration: none;
  transition: color 0.2s;
  font-weight: 450;
}

.nav-link:hover,
.router-link-exact-active {
  color: var(--gray-800);
}

.login-btn {
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 6px;
  background: var(--indigo);
  color: #fff !important;
  transition: background 0.2s;
}

.login-btn:hover {
  background: var(--indigo-soft);
}

.theme-toggle {
  cursor: pointer;
  color: var(--gray-400);
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 50%;
  transition: color 0.2s;
}

.theme-toggle:hover {
  color: var(--indigo);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

:root[data-theme="dark"] .navbar.scrolled {
  background: rgba(24, 26, 32, 0.82);
}

@media (max-width: 640px) {
  .navbar { padding: 0 16px; }
  .nav-links { gap: 16px; }
}
</style>

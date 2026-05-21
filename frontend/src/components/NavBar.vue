<template>
  <header class="navbar">
    <div class="navbar-inner">
      <router-link to="/" class="brand">
        <img :src="brandAvatar" class="brand-avatar" alt="avatar" @error="e => e.target.src = defaultAvatar" />
        <span class="brand-name">{{ brandName }}</span>
      </router-link>

      <nav class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/archive" class="nav-link">归档</router-link>
        <router-link to="/about" class="nav-link">关于</router-link>

        <!-- Theme toggle -->
        <span class="theme-toggle" @click="toggleTheme" :title="isDark ? '切换到浅色模式' : '切换到深色模式'">
          <el-icon :size="18"><Moon v-if="isDark" /><Sunny v-else /></el-icon>
        </span>

        <!-- Admin link (only for admin) -->
        <router-link v-if="isAdmin" to="/admin/articles" class="nav-link">后台管理</router-link>

        <!-- Guest: show login/register -->
        <template v-if="!isLoggedIn">
          <router-link to="/login" class="nav-link login-btn">登录 / 注册</router-link>
        </template>

        <!-- Logged in: show user info -->
        <template v-else>
          <el-dropdown trigger="click">
            <span class="user-dropdown">
              <img :src="currentUser.avatar || defaultAvatar" class="user-avatar" @error="e => e.target.src = defaultAvatar" />
              <span class="user-name">{{ currentUser.nickname || currentUser.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ currentUser.nickname || currentUser.username }}
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/user/profile')">
                  个人中心
                </el-dropdown-item>
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
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { currentUser, isLoggedIn, isAdmin, clearUser } from '@/store/user'
import { useTheme } from '@/store/theme'
import { siteOwner, fetchSiteInfo } from '@/store/site'

const router = useRouter()
const { isDark, toggle: toggleTheme } = useTheme()

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'

const brandName = computed(() => {
  const name = siteOwner.value.nickname || '子墨'
  return name + '的博客'
})

const brandAvatar = computed(() => {
  return siteOwner.value.avatar || defaultAvatar
})

onMounted(() => {
  fetchSiteInfo()
})

function handleLogout() {
  clearUser()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.navbar {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  transition: background-color 0.3s, border-color 0.3s;
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #eee;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  transition: color 0.3s;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-link {
  font-size: 15px;
  color: var(--text-secondary);
  transition: color 0.2s;
}

.nav-link:hover {
  color: var(--text-primary);
}

.login-btn {
  background: #409eff;
  color: #fff;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
}

.login-btn:hover {
  background: #337ecc;
  color: #fff;
}

.theme-toggle {
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s;
}

.theme-toggle:hover {
  color: var(--link-color);
  background: var(--bg-code);
}

/* User dropdown */
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-dropdown:hover {
  background: #f5f5f5;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  font-size: 14px;
  color: #333;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

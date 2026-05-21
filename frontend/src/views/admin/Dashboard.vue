<template>
  <div class="dashboard">
    <el-container>
      <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
        <div class="sidebar-header">
          <span v-show="!isCollapse" class="sidebar-title">Blog Admin</span>
          <span v-show="isCollapse" class="sidebar-title-short">B</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <el-menu-item index="/admin/articles">
            <el-icon><Document /></el-icon>
            <span>文章管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/articles/new">
            <el-icon><Edit /></el-icon>
            <span>新建文章</span>
          </el-menu-item>
          <el-menu-item index="/admin/comments">
            <el-icon><ChatDotRound /></el-icon>
            <span>评论管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/about">
            <el-icon><Setting /></el-icon>
            <span>关于设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="dash-header">
          <div class="header-left">
            <el-button @click="isCollapse = !isCollapse" text>
              <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
            </el-button>
          </div>
          <div class="header-right">
            <img :src="userAvatar" class="header-avatar" @error="e => e.target.src = defaultAvatar" />
            <span class="username">{{ userName }}</span>
            <el-button type="danger" text @click="handleLogout">退出登录</el-button>
          </div>
        </el-header>
        <el-main class="dash-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Document, Edit, Fold, Expand, ChatDotRound, User, Setting } from '@element-plus/icons-vue'
import { currentUser, clearUser } from '@/store/user'

const router = useRouter()
const route = useRoute()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=admin&backgroundColor=e0e0e0'
const userName = computed(() => currentUser.value.nickname || currentUser.value.username || 'Admin')
const userAvatar = computed(() => currentUser.value.avatar || defaultAvatar)

function handleLogout() {
  clearUser()
  router.push('/')
}
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-title {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}

.sidebar-title-short {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.el-menu {
  border-right: none;
}

.dash-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  font-size: 14px;
  color: #666;
}

.header-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.dash-main {
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}
</style>

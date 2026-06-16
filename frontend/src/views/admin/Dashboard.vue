<template>
  <div class="admin-shell" :class="{ 'dark-mode': isDark }">
    <!-- Mobile overlay -->
    <div class="sidebar-overlay" v-if="mobileOpen" @click="mobileOpen = false"></div>

    <!-- Sidebar -->
    <aside class="admin-sidebar" :class="{ collapsed: isCollapse, 'mobile-open': mobileOpen }">
      <div class="sidebar-brand">
        <div class="brand-icon">B</div>
        <span class="brand-text" v-show="!isCollapse">Blog</span>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="['nav-item', { active: activeMenu === item.path }]"
          @click="mobileOpen = false"
        >
          <span class="nav-icon" v-html="item.icon"></span>
          <span class="nav-label" v-show="!isCollapse">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="nav-item collapse-btn" @click="toggleCollapse">
          <span class="nav-icon">
            <svg v-if="!isCollapse" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/></svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><polyline points="13 17 18 12 13 7"/><polyline points="6 17 11 12 6 7"/></svg>
          </span>
          <span class="nav-label" v-show="!isCollapse">收起</span>
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <div class="admin-main">
      <!-- Top bar -->
      <header class="admin-topbar">
        <button class="mobile-menu-btn" @click="mobileOpen = !mobileOpen">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
        </button>
        <div class="topbar-spacer"></div>
        <div class="topbar-right">
          <router-link to="/" class="view-blog-btn" title="查看博客">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
            <span>查看博客</span>
          </router-link>
          <button class="theme-btn" @click="toggleTheme" :title="isDark ? '浅色模式' : '深色模式'">
            <svg v-if="!isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          </button>
          <div class="user-pill">
            <img :src="userAvatar" class="user-avatar" @error="e => e.target.src = defaultAvatar" />
            <span class="user-name">{{ userName }}</span>
          </div>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </header>

      <!-- Page content -->
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { currentUser, clearUser } from '@/store/user'
import { useTheme } from '@/store/theme'

const router = useRouter()
const route = useRoute()
const isCollapse = ref(false)
const { isDark, toggle: toggleTheme } = useTheme()
const mobileOpen = ref(false)

const activeMenu = computed(() => route.path)
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=admin&backgroundColor=e0e0e0'
const userName = computed(() => currentUser.value.nickname || currentUser.value.username || 'Admin')
const userAvatar = computed(() => currentUser.value.avatar || defaultAvatar)

const navItems = [
  {
    path: '/admin/articles',
    label: '文章',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>'
  },
  {
    path: '/admin/articles/new',
    label: '写文章',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>'
  },
  {
    path: '/admin/comments',
    label: '评论',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>'
  },
  {
    path: '/admin/users',
    label: '用户',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>'
  },
  {
    path: '/admin/about',
    label: '关于',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>'
  }
]

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

function handleLogout() {
  clearUser()
  router.push('/')
}
</script>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: var(--gray-50);
  font-family: var(--font-body, -apple-system, 'Segoe UI', sans-serif);
  color: var(--gray-700);
}

/* ============================================================
   SIDEBAR
   ============================================================ */
.admin-sidebar {
  width: 220px;
  background: var(--bg-card, #fff);
  border-right: 1px solid var(--gray-200);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  transition: width 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  overflow: hidden;
}

.admin-sidebar.collapsed { width: 64px; }

.sidebar-brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--gray-100);
  flex-shrink: 0;
}

.brand-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--indigo, #5b6abf);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--gray-800);
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--gray-500);
  text-decoration: none;
  transition: all 0.15s;
  cursor: pointer;
  background: transparent;
  border: none;
  font-family: inherit;
  width: 100%;
  text-align: left;
  white-space: nowrap;
}

.nav-item:hover { background: var(--gray-100); color: var(--gray-700); }

.nav-item.active {
  background: var(--indigo-bg, #eef0f8);
  color: var(--indigo, #5b6abf);
  font-weight: 500;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 20px;
}

.sidebar-footer {
  padding: 8px;
  border-top: 1px solid var(--gray-100);
}

.collapse-btn { color: var(--gray-400) !important; }
.collapse-btn:hover { color: var(--gray-600) !important; }

/* ============================================================
   MAIN AREA
   ============================================================ */
.admin-main {
  flex: 1;
  margin-left: 220px;
  transition: margin-left 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.admin-sidebar.collapsed ~ .admin-main { margin-left: 64px; }

/* Top bar */
.admin-topbar {
  height: 52px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: var(--bg-card, rgba(255, 255, 255, 0.72));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--gray-100);
  position: sticky;
  top: 0;
  z-index: 50;
  gap: 12px;
}

.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  color: var(--gray-500);
  cursor: pointer;
  padding: 4px;
}

.topbar-spacer { flex: 1; }

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--gray-400);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.theme-btn:hover { background: var(--gray-100); color: var(--gray-700); }

.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 100px;
  background: var(--gray-100);
}

.user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  font-size: 13px;
  color: var(--gray-600);
  font-weight: 500;
}

.logout-btn {
  background: none;
  border: none;
  font-size: 13px;
  font-family: inherit;
  color: var(--gray-400);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.logout-btn:hover { color: #f56c6c; background: rgba(245, 108, 108, 0.06); }

.view-blog-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-family: inherit;
  color: var(--gray-400);
  text-decoration: none;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s;
}

.view-blog-btn:hover { color: var(--indigo, #5b6abf); background: var(--indigo-bg, #eef0f8); }

/* Content — 紧凑布局，减少无效空白 */
.admin-content {
  flex: 1;
  padding: 20px 24px;
}

/* ============================================================
   DARK MODE
   ============================================================ */
.dark-mode {
  --gray-50: #14161c;
  --gray-100: #1e2028;
  --gray-200: #2a2d36;
  --gray-300: #3a3e4a;
  --gray-400: #6b7080;
  --gray-500: #9498a6;
  --gray-600: #c5c8d2;
  --gray-700: #e5e7ed;
  --gray-800: #f0f1f4;
  --indigo-bg: #2a2d3a;
  --bg-card: #1e2028;
  --bg-primary: #181a20;
  --bg-secondary: #14161c;
}

.dark-mode .admin-sidebar { background: #1a1c24; border-right-color: var(--gray-200); }
.dark-mode .sidebar-brand { border-bottom-color: var(--gray-200); }
.dark-mode .sidebar-footer { border-top-color: var(--gray-200); }
.dark-mode .admin-topbar { background: rgba(26, 28, 36, 0.82); border-bottom-color: var(--gray-200); }
.dark-mode .user-pill { background: var(--gray-100); }

/* ============================================================
   MOBILE
   ============================================================ */
.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
}

@media (max-width: 768px) {
  .admin-sidebar {
    transform: translateX(-100%);
    width: 260px;
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
  }

  .admin-sidebar.mobile-open { transform: translateX(0); }
  .sidebar-overlay { display: block; }

  .admin-main { margin-left: 0 !important; }
  .mobile-menu-btn { display: flex; }
  .admin-content { padding: 16px; }
  .user-name { display: none; }
}

/* Sidebar collapsed: center icons */
.admin-sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 10px;
}

.admin-sidebar.collapsed .sidebar-brand {
  justify-content: center;
  padding: 0;
}

.admin-sidebar.collapsed .brand-text { display: none; }
</style>

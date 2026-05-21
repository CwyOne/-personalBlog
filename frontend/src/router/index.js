import { createRouter, createWebHistory } from 'vue-router'
import { currentUser } from '@/store/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/ArticleDetail.vue')
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue')
  },
  {
    path: '/archive',
    name: 'Archive',
    component: () => import('@/views/Archive.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/admin/Login.vue')
  },
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/login',
    redirect: '/login'
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/articles'
      },
      {
        path: 'articles',
        name: 'ArticleList',
        component: () => import('@/views/admin/ArticleList.vue')
      },
      {
        path: 'articles/new',
        name: 'ArticleCreate',
        component: () => import('@/views/admin/ArticleEditor.vue')
      },
      {
        path: 'articles/:id/edit',
        name: 'ArticleEdit',
        component: () => import('@/views/admin/ArticleEditor.vue')
      },
      {
        path: 'comments',
        name: 'CommentManage',
        component: () => import('@/views/admin/CommentManage.vue')
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue')
      },
      {
        path: 'about',
        name: 'AboutSettings',
        component: () => import('@/views/admin/AboutSettings.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (to.name === 'Home') {
      return { top: 0 }
    }
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  // Admin routes require auth + admin role
  if (to.matched.some(r => r.meta.requiresAuth)) {
    const token = localStorage.getItem('token')
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    // Check admin role for admin routes
    if (to.matched.some(r => r.meta.requiresAdmin)) {
      if (currentUser.value.role !== 'admin') {
        next({ name: 'Home' })
        return
      }
    }
  }

  // Already logged in and visiting login page
  if (to.name === 'Login') {
    const token = localStorage.getItem('token')
    if (token) {
      const redirect = to.query.redirect
      next(redirect || '/')
      return
    }
  }

  next()
})

export default router

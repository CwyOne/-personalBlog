import { createRouter, createWebHistory } from 'vue-router'
import { currentUser } from '@/store/user'
import { updateSEO, getCurrentUrl } from '@/utils/seo'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: {
      seo: {
        title: '首页',
        description: '一个记录技术、思考与生活的个人博客，分享全栈开发经验与实践。'
      }
    }
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/ArticleDetail.vue'),
    meta: {
      seo: {
        dynamic: true,
        type: 'article'
      }
    }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue'),
    meta: {
      seo: {
        title: '关于',
        description: '了解博主——一名热爱技术的全栈开发者，这个博客记录了技术学习与工作中的思考、总结和实践。'
      }
    }
  },
  {
    path: '/archive',
    name: 'Archive',
    component: () => import('@/views/Archive.vue'),
    meta: {
      seo: {
        title: '文章归档',
        description: '按时间线浏览所有已发布的技术文章和生活随笔。'
      }
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/admin/Login.vue'),
    meta: {
      seo: {
        title: '登录',
        noIndex: true
      }
    }
  },
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfile.vue'),
    meta: {
      requiresAuth: true,
      seo: {
        title: '个人中心',
        noIndex: true
      }
    }
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

// ── Auth guards ──────────────────────────────────────────
router.beforeEach((to, from, next) => {
  if (to.matched.some(r => r.meta.requiresAuth)) {
    const token = sessionStorage.getItem('token')
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    if (to.matched.some(r => r.meta.requiresAdmin)) {
      if (currentUser.value.role !== 'admin') {
        next({ name: 'Home' })
        return
      }
    }
  }

  if (to.name === 'Login') {
    const token = sessionStorage.getItem('token')
    if (token) {
      const redirect = to.query.redirect
      if (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) {
        next(redirect)
      } else {
        next('/')
      }
      return
    }
  }

  next()
})

// ── SEO: update head after each navigation ───────────────
router.afterEach((to) => {
  const seo = to.meta?.seo

  // Admin routes: noindex, nofollow
  if (to.matched.some(r => r.path.startsWith('/admin'))) {
    updateSEO({ title: '后台管理', noIndex: true })
    return
  }

  // Dynamic pages (e.g. ArticleDetail): component handles its own SEO
  if (seo?.dynamic) {
    updateSEO({ title: seo.title || '加载中...', type: seo.type, url: getCurrentUrl(), noIndex: seo.noIndex })
    return
  }

  // Static pages
  if (seo) {
    updateSEO({ title: seo.title, description: seo.description, url: getCurrentUrl(), noIndex: seo.noIndex })
  } else {
    // No SEO meta — clear robots
    updateSEO({ noIndex: false })
  }
})

export default router

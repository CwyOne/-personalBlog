<template>
  <div class="about-page">
    <NavBar />
    <main class="about-container">
      <el-card v-loading="loading" class="about-card">
        <div class="about-header">
          <img :src="about.avatar || defaultAvatar" class="about-avatar" @error="e => e.target.src = defaultAvatar" />
          <h1 class="about-name">{{ about.nickname || '博主' }}</h1>
          <p class="about-subtitle">全栈开发 · 技术爱好者</p>
        </div>

        <el-divider />

        <section class="about-section">
          <h2>关于我</h2>
          <p v-for="(line, i) in bioLines" :key="i">{{ line }}</p>
          <p v-if="bioLines.length === 0">暂无介绍~</p>
        </section>

        <el-divider />

        <section class="about-section">
          <h2>技术栈</h2>
          <div class="tech-tags">
            <el-tag size="large" v-for="t in techStack" :key="t" class="tech-tag">{{ t }}</el-tag>
          </div>
          <p v-if="techStack.length === 0" style="color: var(--text-muted);">暂无技术栈信息</p>
        </section>

        <el-divider />

        <section class="about-section">
          <h2>联系方式</h2>
          <div class="contact-list">
            <div class="contact-item" v-if="about.publicEmail">
              <el-icon><Message /></el-icon>
              <span>{{ about.publicEmail }}</span>
            </div>
            <a v-if="about.github" class="contact-item" :href="about.github" target="_blank">
              <el-icon><Link /></el-icon>
              <span>GitHub</span>
            </a>
            <a v-if="about.twitter" class="contact-item" :href="about.twitter" target="_blank">
              <el-icon><Link /></el-icon>
              <span>Twitter</span>
            </a>
            <p v-if="!about.publicEmail && !about.github && !about.twitter" style="color: var(--text-muted);">暂无联系方式</p>
          </div>
        </section>

        <el-divider />

        <section class="about-section">
          <h2>博客统计</h2>
          <div class="stats-row">
            <div class="stat-box">
              <span class="stat-num">{{ stats.articles }}</span>
              <span class="stat-label">篇文章</span>
            </div>
            <div class="stat-box">
              <span class="stat-num">{{ stats.tags }}</span>
              <span class="stat-label">个标签</span>
            </div>
            <div class="stat-box">
              <span class="stat-num">{{ stats.days }}</span>
              <span class="stat-label">天运行</span>
            </div>
          </div>
        </section>
      </el-card>
    </main>

    <footer class="site-footer">
      <p>© 2026 {{ brandName }} · Powered by Vue 3 & Spring Boot</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getAbout, getArticles, getTags } from '@/utils/api'
import NavBar from '@/components/NavBar.vue'
import { Message, Link } from '@element-plus/icons-vue'
import { siteOwner } from '@/store/site'

const loading = ref(false)
const about = reactive({ nickname: '', avatar: '', email: '', createTime: '', bio: '', techStack: '[]', github: '', twitter: '', publicEmail: '' })
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'

const brandName = computed(() => {
  const name = siteOwner.value.nickname || '子墨'
  return name + '的博客'
})

const techStack = computed(() => {
  try { return JSON.parse(about.techStack || '[]') } catch { return [] }
})

const bioLines = computed(() => {
  return (about.bio || '').split('\n').filter(line => line.trim())
})

const stats = reactive({ articles: 0, tags: 0, days: 0 })

onMounted(() => {
  loading.value = true
  Promise.all([
    getAbout(),
    getArticles({ page: 1, size: 1 }),
    getTags()
  ]).then(([aboutRes, articlesRes, tagsRes]) => {
    Object.assign(about, aboutRes.data)
    stats.articles = articlesRes.data.total || 0
    stats.tags = tagsRes.data.length || 0
    if (about.createTime) {
      const diff = Date.now() - new Date(about.createTime).getTime()
      stats.days = Math.max(1, Math.floor(diff / 86400000))
    }
  }).finally(() => { loading.value = false })
})
</script>

<style scoped>
.about-page { min-height: 100vh; background: var(--bg-secondary); transition: background-color 0.3s; }
.about-container { max-width: 750px; margin: 0 auto; padding: 40px 20px; }
.about-card { border-radius: 12px; }

.about-header { text-align: center; padding: 20px 0 8px; }
.about-avatar { width: 100px; height: 100px; border-radius: 50%; object-fit: cover; border: 3px solid var(--border-color); }
.about-name { font-size: 26px; font-weight: 700; color: var(--text-primary); margin: 16px 0 4px; transition: color 0.3s; }
.about-subtitle { font-size: 15px; color: var(--text-muted); }

.about-section h2 { font-size: 18px; font-weight: 600; color: var(--text-primary); margin-bottom: 16px; padding-left: 10px; border-left: 3px solid var(--link-color); transition: color 0.3s; }
.about-section p { font-size: 15px; color: var(--text-secondary); line-height: 1.8; margin-bottom: 8px; }

.tech-tags { display: flex; flex-wrap: wrap; gap: 10px; }
.tech-tag { cursor: default; }

.contact-list { display: flex; flex-direction: column; gap: 12px; }
.contact-item { display: flex; align-items: center; gap: 8px; font-size: 15px; color: var(--text-secondary); }
a.contact-item { color: var(--link-color); }
a.contact-item:hover { text-decoration: underline; }

.stats-row { display: flex; justify-content: center; gap: 48px; }
.stat-box { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--link-color); }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.site-footer { text-align: center; padding: 32px 0; margin-top: 40px; color: var(--text-muted); font-size: 13px; }
</style>

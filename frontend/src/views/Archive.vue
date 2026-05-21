<template>
  <div class="archive-page">
    <NavBar />
    <main class="archive-container">
      <h1 class="archive-title">文章归档</h1>
      <p class="archive-subtitle">共 {{ totalCount }} 篇文章</p>

      <div v-loading="loading">
        <template v-if="archives.length > 0">
          <div v-for="year in archives" :key="year.year" class="year-block">
            <h2 class="year-heading">{{ year.year }} 年</h2>
            <div v-for="month in year.months" :key="month.month" class="month-block">
              <h3 class="month-heading">
                {{ month.month }} 月
                <span class="month-count">{{ month.articles.length }} 篇</span>
              </h3>
              <div class="article-list">
                <router-link
                  v-for="article in month.articles"
                  :key="article.id"
                  :to="`/article/${article.id}`"
                  class="archive-article"
                >
                  <span class="archive-date">{{ formatDay(article.createTime) }}</span>
                  <span class="archive-article-title">{{ article.title }}</span>
                </router-link>
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无文章" :image-size="100" />
      </div>
    </main>

    <footer class="site-footer">
      <p>© 2026 {{ brandName }} · Powered by Vue 3 & Spring Boot</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getArchive } from '@/utils/api'
import NavBar from '@/components/NavBar.vue'
import { siteOwner } from '@/store/site'

const loading = ref(false)
const archives = ref([])

const brandName = computed(() => {
  const name = siteOwner.value.nickname || '子墨'
  return name + '的博客'
})

const totalCount = computed(() => {
  let count = 0
  for (const y of archives.value) {
    for (const m of y.months) {
      count += m.articles.length
    }
  }
  return count
})

function formatDay(dateStr) {
  if (!dateStr) return ''
  return dateStr.length >= 10 ? dateStr.substring(8, 10) + '日' : dateStr
}

onMounted(() => {
  loading.value = true
  getArchive().then(res => {
    archives.value = res.data || []
  }).finally(() => { loading.value = false })
})
</script>

<style scoped>
.archive-page { min-height: 100vh; background: var(--bg-secondary); transition: background-color 0.3s; }
.archive-container { max-width: 750px; margin: 0 auto; padding: 40px 20px; }

.archive-title { font-size: 28px; font-weight: 700; color: var(--text-primary); text-align: center; margin-bottom: 6px; transition: color 0.3s; }
.archive-subtitle { text-align: center; font-size: 14px; color: var(--text-muted); margin-bottom: 36px; }

.year-block { margin-bottom: 32px; }
.year-heading { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 16px; padding-left: 12px; border-left: 4px solid var(--link-color); transition: color 0.3s; }

.month-block { margin-bottom: 20px; padding-left: 20px; }
.month-heading { font-size: 16px; font-weight: 600; color: var(--text-secondary); margin-bottom: 10px; }
.month-count { font-weight: 400; font-size: 13px; color: var(--text-muted); margin-left: 8px; }

.article-list { display: flex; flex-direction: column; gap: 8px; }
.archive-article { display: flex; align-items: baseline; gap: 16px; padding: 10px 16px; background: var(--bg-card); border-radius: 8px; box-shadow: var(--shadow); transition: box-shadow 0.2s, transform 0.2s, background-color 0.3s; }
.archive-article:hover { box-shadow: var(--shadow-hover); transform: translateX(4px); }

.archive-date { font-size: 13px; color: var(--text-muted); flex-shrink: 0; min-width: 36px; }
.archive-article-title { font-size: 15px; color: var(--text-primary); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; transition: color 0.3s; }
.archive-article-title:hover { color: var(--link-color); }

.site-footer { text-align: center; padding: 48px 0 32px; color: var(--text-muted); font-size: 13px; }
</style>

<template>
  <div class="home">
    <NavBar />

    <!-- Hero with typewriter -->
    <section class="hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="typed-text">{{ typedText }}</span>
          <span class="cursor" :class="{ blink: !typing }">|</span>
        </h1>
        <p class="hero-desc">记录技术思考与生活点滴 · 保持好奇，持续学习</p>
        <div class="hero-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
            @clear="clearSearch"
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">{{ totalArticles }}</span>
            <span class="stat-label">篇文章</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ totalTags }}</span>
            <span class="stat-label">个标签</span>
          </div>
        </div>
      </div>
    </section>

    <main class="container">
      <!-- Tag cloud -->
      <div class="tag-section" v-loading="tagLoading">
        <span class="tag-label">
          <el-icon><CollectionTag /></el-icon>
          分类：
        </span>
        <el-tag
          v-for="tag in tags"
          :key="tag.id"
          :color="selectedTag === tag.id ? tagColor(tag.id) : ''"
          :effect="selectedTag === tag.id ? 'dark' : 'plain'"
          :style="selectedTag === tag.id ? 'color:#fff;border:none;' : ''"
          class="tag-item"
          @click="toggleTag(tag.id)"
        >
          {{ tag.name }} ({{ tag.articleCount || 0 }})
        </el-tag>
        <span v-if="selectedTag" class="clear-filter" @click="selectedTag = null; currentPage = 1; fetchArticles()">
          清除筛选
        </span>
      </div>

      <!-- Search status -->
      <div v-if="isSearching" class="search-status">
        <span>搜索"{{ searchKeyword }}"的结果，共 {{ total }} 篇</span>
        <el-button type="primary" link @click="clearSearch">返回全部文章</el-button>
      </div>

      <!-- Article list -->
      <div v-loading="loading" class="article-list">
        <template v-if="articles.length > 0">
          <article
            v-for="(article, index) in articles"
            :key="article.id"
            class="article-card card-hover"
            :style="{ animationDelay: index * 0.1 + 's' }"
            @click="goDetail(article.id)"
          >
            <div class="card-cover" v-if="article.coverImage">
              <img :src="article.coverImage" :alt="article.title" loading="lazy" />
            </div>
            <div class="card-body">
              <div class="card-author" v-if="article.authorNickname">
                <img :src="article.authorAvatar || defaultAvatar" class="author-avatar" />
                <span class="author-name">{{ article.authorNickname }}</span>
                <span class="card-time">{{ formatTime(article.createTime) }}</span>
              </div>
              <h2 class="card-title">{{ article.title }}</h2>
              <p class="card-summary">{{ article.summary }}</p>
              <div class="card-footer">
                <div class="card-tags">
                  <el-tag
                    v-for="tag in article.tags"
                    :key="tag.id"
                    size="small"
                    :color="tagColor(tag.id)"
                    style="color: #fff; border: none;"
                  >
                    {{ tag.name }}
                  </el-tag>
                </div>
                <span class="card-views">
                  <el-icon><View /></el-icon> {{ article.viewCount }}
                  <span class="card-likes">
                    <el-icon><StarFilled /></el-icon> {{ article.likeCount || 0 }}
                  </span>
                </span>
              </div>
            </div>
          </article>
        </template>
        <el-empty
          v-if="articles.length === 0 && !loading"
          description="暂无文章，敬请期待"
          :image-size="100"
        />
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
          background
        />
      </div>
    </main>

    <footer class="site-footer">
      <p>© 2026 {{ brandName }} · Powered by Vue 3 & Spring Boot</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getArticles, getTags, searchArticles } from '@/utils/api'
import NavBar from '@/components/NavBar.vue'
import { View, CollectionTag, StarFilled, Search } from '@element-plus/icons-vue'
import { siteOwner, fetchSiteInfo } from '@/store/site'

const router = useRouter()
const loading = ref(false)
const tagLoading = ref(false)
const articles = ref([])
const tags = ref([])
const currentPage = ref(1)
const pageSize = ref(6)
const total = ref(0)
const totalArticles = ref(0)
const totalTags = ref(0)
const selectedTag = ref(null)
const searchKeyword = ref('')
const isSearching = ref(false)

// Typewriter effect
const fullTitle = computed(() => {
  const name = siteOwner.value.nickname || '子墨'
  return name + '的博客'
})
const typedText = ref('')
const typing = ref(true)
let typeTimer = null

function startTyping() {
  let i = 0
  typedText.value = ''
  typing.value = true
  typeTimer = setInterval(() => {
    if (i < fullTitle.value.length) {
      typedText.value += fullTitle.value.charAt(i)
      i++
    } else {
      typing.value = false
      clearInterval(typeTimer)
      // Restart after 3 seconds
      setTimeout(() => {
        startTyping()
      }, 3000)
    }
  }, 150)
}

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'

const brandName = computed(() => {
  const name = siteOwner.value.nickname || '子墨'
  return name + '的博客'
})

const tagColors = [
  '#409eff', '#67c23a', '#e6a23c', '#f56c6c',
  '#909399', '#b37feb', '#36cfc9', '#ff85c0',
  '#597ef7', '#ff9c6e'
]

function tagColor(tagId) {
  return tagColors[tagId % tagColors.length]
}

function toggleTag(tagId) {
  if (selectedTag.value === tagId) {
    selectedTag.value = null
  } else {
    selectedTag.value = tagId
  }
  currentPage.value = 1
  fetchArticles()
}

function fetchArticles() {
  loading.value = true
  const params = { page: currentPage.value, size: pageSize.value }
  if (selectedTag.value) params.tag = selectedTag.value
  const api = isSearching.value
    ? searchArticles({ ...params, keyword: searchKeyword.value })
    : getArticles(params)
  api.then(res => {
    articles.value = res.data.records || []
    total.value = res.data.total || 0
    if (!isSearching.value) totalArticles.value = res.data.total || 0
  }).finally(() => { loading.value = false })
}

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  isSearching.value = true
  currentPage.value = 1
  selectedTag.value = null
  fetchArticles()
}

function clearSearch() {
  searchKeyword.value = ''
  isSearching.value = false
  currentPage.value = 1
  fetchArticles()
}

function fetchTags() {
  tagLoading.value = true
  getTags().then(res => {
    tags.value = res.data || []
    totalTags.value = res.data.length || 0
  }).finally(() => { tagLoading.value = false })
}

function handlePageChange(page) {
  currentPage.value = page
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goDetail(id) {
  router.push(`/article/${id}`)
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = now - date
  const days = Math.floor(diff / 86400000)
  if (days < 1) return '今天'
  if (days < 2) return '昨天'
  if (days < 30) return `${days}天前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

onMounted(async () => {
  await fetchSiteInfo()
  startTyping()
  fetchTags()
  fetchArticles()
})

onUnmounted(() => {
  if (typeTimer) clearInterval(typeTimer)
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: var(--bg-secondary);
  transition: background-color 0.3s;
}

/* Hero */
.hero {
  position: relative;
  padding: 80px 0 72px;
  text-align: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 100%);
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(64, 158, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(103, 194, 58, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 50% 80%, rgba(179, 127, 235, 0.06) 0%, transparent 45%);
  animation: bgFloat 8s ease-in-out infinite alternate;
}

@keyframes bgFloat {
  0% { opacity: 0.8; transform: scale(1); }
  100% { opacity: 1; transform: scale(1.05); }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 600px;
  margin: 0 auto;
  padding: 0 20px;
}

.hero-title {
  font-size: 40px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 16px;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.typed-text {
  font-family: 'Georgia', 'Noto Serif SC', serif;
}

.cursor {
  font-weight: 300;
  color: #409eff;
}

.cursor.blink {
  animation: blinkAnim 0.8s infinite;
}

@keyframes blinkAnim {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.hero-desc {
  font-size: 16px;
  color: rgba(255,255,255,0.7);
  margin-bottom: 24px;
}

.hero-search {
  display: flex;
  gap: 10px;
  max-width: 440px;
  margin: 0 auto 28px;
}

.search-input {
  flex: 1;
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 48px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  font-size: 13px;
  color: rgba(255,255,255,0.5);
  margin-top: 4px;
}

/* Tags */
.tag-section {
  padding: 24px 0 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.tag-label {
  font-size: 14px;
  color: var(--text-muted);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.tag-item {
  cursor: pointer;
  transition: transform 0.2s;
}

.tag-item:hover {
  transform: scale(1.05);
}

.clear-filter {
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
}

/* Search status */
.search-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 4px;
  font-size: 14px;
  color: #666;
}

/* Article list */
.article-list {
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-card {
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  box-shadow: var(--shadow);
  animation: fadeInUp 0.5s ease both;
  transition: background-color 0.3s;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 640px) {
  .article-card { flex-direction: column; }
  .hero-title { font-size: 28px; }
}

.card-cover {
  width: 280px;
  height: 190px;
  flex-shrink: 0;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s;
}

.article-card:hover .card-cover img {
  transform: scale(1.08);
}

@media (max-width: 640px) {
  .card-cover { width: 100%; height: 200px; }
}

.card-body {
  flex: 1;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.card-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.card-time {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: auto;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.card-views {
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.card-likes {
  display: flex;
  align-items: center;
  gap: 3px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 32px 0 48px;
}

.site-footer {
  text-align: center;
  padding: 32px 0;
  background: var(--bg-card);
  border-top: 1px solid var(--border-color);
  transition: background-color 0.3s, border-color 0.3s;
}

.site-footer p {
  font-size: 13px;
  color: var(--text-muted);
}
</style>

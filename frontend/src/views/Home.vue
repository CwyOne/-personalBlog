<template>
  <div class="home">
    <NavBar />

    <!-- Hero: asymmetric editorial layout -->
    <section class="hero" ref="heroRef">
      <div class="hero-grain"></div>
      <div class="hero-grid">
        <div class="hero-left">
          <div class="hero-eyebrow reveal-up" :style="{ '--delay': '0.1s' }">
            <span class="eyebrow-line"></span>
            <span>PERSONAL BLOG</span>
          </div>
          <h1 class="hero-title reveal-up" :style="{ '--delay': '0.25s' }">
            <span class="title-line" ref="titleLine1">记录技术</span>
            <span class="title-line title-indent" ref="titleLine2">思考与生活</span>
          </h1>
          <p class="hero-desc reveal-up" :style="{ '--delay': '0.4s' }">
            保持好奇，持续学习。<br />
            在代码与文字之间，探索更广阔的可能。
          </p>
          <div class="hero-search reveal-up" :style="{ '--delay': '0.55s' }">
            <div class="search-wrap">
              <el-icon class="search-icon"><Search /></el-icon>
              <input
                v-model="searchKeyword"
                placeholder="搜索文章..."
                @keyup.enter="handleSearch"
                class="search-input"
              />
            </div>
          </div>
        </div>
        <div class="hero-right">
          <div class="hero-stats reveal-up" :style="{ '--delay': '0.5s' }">
            <div class="stat-block">
              <span class="stat-num">{{ totalArticles }}</span>
              <span class="stat-label">篇文章</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-num">{{ totalTags }}</span>
              <span class="stat-label">个标签</span>
            </div>
          </div>
          <div class="hero-deco reveal-up" :style="{ '--delay': '0.65s' }">
            <div class="deco-line deco-1"></div>
            <div class="deco-line deco-2"></div>
            <div class="deco-dot"></div>
          </div>
        </div>
      </div>
      <div class="hero-scroll-hint reveal-up" :style="{ '--delay': '0.8s' }">
        <span class="scroll-line"></span>
      </div>
    </section>

    <!-- Main content -->
    <main class="main-wrap">
      <!-- Tag filter bar -->
      <div class="filter-bar reveal-up" v-loading="tagLoading">
        <div class="filter-inner">
          <span class="filter-label">标签</span>
          <div class="filter-tags">
            <button
              v-for="tag in tags"
              :key="tag.id"
              :class="['filter-tag', { active: selectedTag === tag.id }]"
              @click="toggleTag(tag.id)"
            >
              {{ tag.name }}
              <span class="tag-count">{{ tag.articleCount || 0 }}</span>
            </button>
          </div>
          <button v-if="selectedTag" class="clear-btn" @click="selectedTag = null; currentPage = 1; fetchArticles()">
            清除
          </button>
        </div>
      </div>

      <!-- Search status -->
      <div v-if="isSearching" class="search-status reveal-up">
        <span>搜索 "{{ searchKeyword }}" · {{ total }} 篇结果</span>
        <button class="clear-btn" @click="clearSearch">返回全部</button>
      </div>

      <!-- Article grid -->
      <div v-loading="loading" class="article-grid">
        <template v-if="articles.length > 0">
          <article
            v-for="(article, idx) in articles"
            :key="article.id"
            :class="['article-card', idx === 0 ? 'card-featured' : 'card-list']"
            :style="{ '--delay': idx * 0.08 + 's' }"
            @click="goDetail(article.id)"
          >
            <!-- === Featured card (first): left text, right image === -->
            <template v-if="idx === 0">
              <div class="card-body">
                <div class="card-meta">
                  <span class="card-date">{{ formatTime(article.createTime) }}</span>
                  <span v-if="article.tags?.length" class="card-tag-pill">{{ article.tags[0]?.name }}</span>
                </div>
                <h2 class="card-title card-title--featured">{{ article.title }}</h2>
                <p class="card-summary">{{ article.summary }}</p>
                <div class="card-bottom">
                  <div class="card-tags" v-if="article.tags?.length > 1">
                    <span v-for="tag in article.tags.slice(1, 4)" :key="tag.id" class="mini-tag">{{ tag.name }}</span>
                  </div>
                  <div class="card-stats">
                    <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
                    <span><el-icon><StarFilled /></el-icon> {{ article.likeCount || 0 }}</span>
                  </div>
                </div>
              </div>
              <div class="card-cover" v-if="article.coverImage">
                <img :src="article.coverImage" :alt="article.title" loading="lazy" />
              </div>
            </template>

            <!-- === Regular list cards: title-first above image === -->
            <template v-else>
              <div class="card-body">
                <div class="card-meta">
                  <span class="card-date">{{ formatTime(article.createTime) }}</span>
                  <span v-if="article.tags?.length" class="card-tag-pill">{{ article.tags[0]?.name }}</span>
                </div>
                <h2 class="card-title">{{ article.title }}</h2>
                <div class="card-cover" v-if="article.coverImage">
                  <img :src="article.coverImage" :alt="article.title" loading="lazy" />
                </div>
                <p class="card-summary">{{ article.summary }}</p>
                <div class="card-bottom">
                  <div class="card-tags" v-if="article.tags?.length > 1">
                    <span v-for="tag in article.tags.slice(1, 3)" :key="tag.id" class="mini-tag">{{ tag.name }}</span>
                  </div>
                  <div class="card-stats">
                    <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
                    <span><el-icon><StarFilled /></el-icon> {{ article.likeCount || 0 }}</span>
                  </div>
                </div>
              </div>
            </template>
          </article>
        </template>
        <div v-if="articles.length === 0 && !loading" class="empty-state">
          <p>暂无文章</p>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination-wrap" v-if="total > pageSize">
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

    <!-- Footer -->
    <footer class="site-footer">
      <div class="footer-inner">
        <span class="footer-copy">© 2026 {{ brandName }}</span>
        <span class="footer-dot">·</span>
        <span class="footer-tech">Vue 3 & Spring Boot</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getArticles, getTags, searchArticles } from '@/utils/api'
import NavBar from '@/components/NavBar.vue'
import { View, StarFilled, Search } from '@element-plus/icons-vue'
import { siteOwner, fetchSiteInfo } from '@/store/site'
import { useRevealOnScroll } from '@/composables/useRevealOnScroll'

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

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'

const brandName = computed(() => {
  const name = siteOwner.value.nickname || '子墨'
  return name + '的博客'
})

const brandAvatar = computed(() => siteOwner.value.avatar || defaultAvatar)

function toggleTag(tagId) {
  selectedTag.value = selectedTag.value === tagId ? null : tagId
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
  const diff = Date.now() - new Date(dateStr).getTime()
  const days = Math.floor(diff / 86400000)
  if (days < 1) return '今天'
  if (days < 2) return '昨天'
  if (days < 30) return `${days}天前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

useRevealOnScroll()

onMounted(async () => {
  await fetchSiteInfo()
  fetchTags()
  fetchArticles()
})
</script>

<style scoped>
/* ============================================================
   DESIGN TOKENS
   ============================================================ */
.home {
  min-height: 100vh;
  background: var(--gray-50);
  color: var(--gray-700);
  font-family: var(--font-body);
}

/* ============================================================
   HERO — asymmetric editorial layout
   ============================================================ */
.hero {
  position: relative;
  min-height: 88vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 120px 32px 60px;
  overflow: hidden;
  background: var(--gray-50);
}

.hero-grid {
  max-width: 1120px;
  margin: 0 auto;
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 80px;
  align-items: center;
  position: relative;
  z-index: 1;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--indigo);
  margin-bottom: 32px;
}

.eyebrow-line {
  display: block;
  width: 32px;
  height: 1.5px;
  background: var(--indigo);
}

.hero-title {
  font-family: var(--font-display);
  font-size: clamp(42px, 5.5vw, 68px);
  font-weight: 400;
  line-height: 1.15;
  color: var(--gray-900);
  margin: 0 0 28px;
  letter-spacing: -0.03em;
}

.title-line {
  display: block;
}

.title-indent {
  padding-left: 48px;
  color: var(--gray-600);
}

.hero-desc {
  font-size: 16px;
  line-height: 1.75;
  color: var(--gray-500);
  margin: 0 0 40px;
  max-width: 420px;
}

.hero-search {
  max-width: 380px;
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 16px;
  color: var(--gray-400);
  font-size: 16px;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 14px 16px 14px 44px;
  border: 1.5px solid var(--gray-200);
  border-radius: 10px;
  background: var(--bg-card);
  font-size: 14px;
  font-family: var(--font-body);
  color: #252830;
  outline: none;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.search-input::placeholder {
  color: #a8adb8;
}

.search-input:focus {
  border-color: var(--indigo-soft);
  box-shadow: 0 0 0 3px rgba(91, 106, 191, 0.08);
}

/* Hero right: stats + decoration */
.hero-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 48px;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 32px;
}

.stat-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.stat-num {
  font-family: var(--font-display);
  font-size: 48px;
  font-weight: 400;
  color: var(--gray-900);
  line-height: 1;
  letter-spacing: -0.04em;
}

.stat-label {
  font-size: 12px;
  color: var(--gray-400);
  margin-top: 6px;
  letter-spacing: 0.04em;
}

.stat-divider {
  width: 1px;
  height: 48px;
  background: var(--gray-200);
}

.hero-deco {
  position: relative;
  width: 200px;
  height: 120px;
}

.deco-line {
  position: absolute;
  border-radius: 2px;
}

.deco-1 {
  width: 80px;
  height: 1.5px;
  background: var(--indigo-soft);
  opacity: 0.5;
  top: 20px;
  right: 0;
}

.deco-2 {
  width: 1.5px;
  height: 60px;
  background: var(--gray-300);
  top: 0;
  right: 80px;
}

.deco-dot {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--indigo);
  top: 17px;
  right: 77px;
}

.hero-scroll-hint {
  display: flex;
  justify-content: center;
  margin-top: auto;
  padding-top: 40px;
}

.scroll-line {
  display: block;
  width: 1px;
  height: 48px;
  background: linear-gradient(to bottom, var(--indigo-soft), transparent);
  animation: scrollPulse 2s ease-in-out infinite;
}

@keyframes scrollPulse {
  0%, 100% { opacity: 0.3; transform: scaleY(0.7); }
  50% { opacity: 0.8; transform: scaleY(1); }
}

/* ============================================================
   FILTER BAR
   ============================================================ */
.main-wrap {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 32px 80px;
}

.filter-bar {
  padding: 48px 0 16px;
}

.filter-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--gray-400);
  flex-shrink: 0;
}

.filter-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px 16px;
  border: 1.5px solid var(--gray-200);
  border-radius: 100px;
  background: transparent;
  font-size: 13px;
  font-family: var(--font-body);
  color: var(--gray-600);
  cursor: pointer;
  transition: all 0.2s;
  flex: 0 0 auto;
}

.filter-tag:hover {
  border-color: var(--gray-300);
  color: var(--gray-800);
  background: var(--gray-100);
}

.filter-tag.active {
  background: var(--indigo);
  border-color: var(--indigo);
  color: #fff;
}

.filter-tag.active .tag-count {
  color: rgba(255, 255, 255, 0.7);
}

.tag-count {
  font-size: 11px;
  color: var(--gray-400);
  font-weight: 500;
}

.clear-btn {
  background: none;
  border: none;
  font-size: 13px;
  font-family: var(--font-body);
  color: var(--indigo);
  cursor: pointer;
  padding: 6px 10px;
  transition: opacity 0.2s;
}

.clear-btn:hover {
  opacity: 0.7;
}

.search-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  font-size: 14px;
  color: var(--gray-500);
}

/* ============================================================
   ARTICLE GRID — card layout
   ============================================================ */
.article-grid {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Shared card base */
.article-card {
  background: var(--bg-card);
  border-radius: var(--radius);
  overflow: hidden;
  transition: background-color 0.3s;
  cursor: pointer;
  border: 1px solid var(--gray-200);
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
              box-shadow 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  animation: cardFadeIn 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: var(--delay, 0s);
}

@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}

/* ============================================================
   FEATURED CARD (first) — left text, right image
   ============================================================ */
.card-featured {
  display: grid;
  grid-template-columns: 1fr 380px;
  min-height: 280px;
}

.card-featured .card-body {
  padding: 32px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-featured .card-cover {
  height: 100%;
  min-height: 280px;
}

.card-featured .card-title--featured {
  font-size: 28px;
  line-height: 1.3;
  margin-bottom: 14px;
}

/* ============================================================
   REGULAR LIST CARDS — title-first, image below
   ============================================================ */
.card-list {
  display: flex;
  flex-direction: column;
}

.card-list .card-body {
  padding: 28px 32px 24px;
  display: flex;
  flex-direction: column;
}

.card-list .card-cover {
  margin: 0 32px;
  border-radius: 8px;
  overflow: hidden;
}

.card-list .card-summary {
  margin-top: 16px;
}

/* ============================================================
   SHARED CARD ELEMENTS
   ============================================================ */
.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.card-date {
  font-size: 12px;
  color: var(--gray-400);
  font-weight: 500;
  letter-spacing: 0.02em;
}

.card-tag-pill {
  font-size: 10px;
  color: var(--indigo);
  background: var(--indigo-bg);
  padding: 2px 10px;
  border-radius: 100px;
  font-weight: 500;
  white-space: nowrap;
}

.card-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  color: var(--gray-900);
  margin: 0 0 10px;
  line-height: 1.35;
  letter-spacing: -0.02em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-size: 14px;
  line-height: 1.7;
  color: var(--gray-400);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--gray-100);
}

.card-tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.mini-tag {
  font-size: 10px;
  color: var(--gray-400);
  padding: 2px 8px;
  border: 1px solid var(--gray-200);
  border-radius: 100px;
  white-space: nowrap;
  line-height: 1.6;
}

.card-stats {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--gray-400);
}

.card-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ============================================================
   COVER IMAGE — shared styles
   ============================================================ */
.card-cover {
  overflow: hidden;
  position: relative;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.article-card:hover .card-cover img {
  transform: scale(1.03);
}

.card-featured .card-cover img {
  height: 100%;
  min-height: 280px;
}

.card-list .card-cover {
  height: 220px;
}

.card-list .card-cover img {
  height: 220px;
}

/* ============================================================
   EMPTY STATE
   ============================================================ */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--gray-400);
  font-size: 15px;
}

/* ============================================================
   PAGINATION
   ============================================================ */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 48px 0 16px;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 900px) {
  .hero-grid {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .hero-right {
    align-items: flex-start;
    flex-direction: row;
    justify-content: space-between;
  }

  .hero-deco {
    display: none;
  }

  .card-featured {
    grid-template-columns: 1fr;
  }

  .card-featured .card-cover {
    order: -1;
    min-height: 200px;
    height: 200px;
  }

  .card-featured .card-cover img {
    min-height: 200px;
    height: 200px;
  }

  .card-featured .card-body {
    padding: 24px;
  }

  .card-featured .card-title--featured {
    font-size: 24px;
  }
}

@media (max-width: 640px) {
  .hero {
    min-height: 70vh;
    padding: 100px 20px 40px;
  }

  .hero-title {
    font-size: 36px;
  }

  .title-indent {
    padding-left: 24px;
  }

  .hero-desc {
    font-size: 15px;
  }

  .stat-num {
    font-size: 36px;
  }

  .main-wrap {
    padding: 0 16px 60px;
  }

  .filter-inner {
    gap: 10px;
  }

  .filter-tag {
    padding: 5px 12px;
    font-size: 12px;
  }

  .card-list .card-body {
    padding: 20px;
  }

  .card-list .card-cover {
    margin: 0 16px;
    height: 180px;
  }

  .card-list .card-cover img {
    height: 180px;
  }

  .card-title {
    font-size: 19px;
  }

  .card-featured .card-title--featured {
    font-size: 21px;
  }

  .card-summary {
    font-size: 13px;
  }

  .card-bottom {
    margin-top: 14px;
    padding-top: 10px;
  }
}

/* ============================================================
   DARK MODE
   ============================================================ */
:root[data-theme="dark"] .article-card {
  background: #1e2028;
}

:root[data-theme="dark"] .search-input {
  background: #1e2028;
  border-color: #2a2d36;
  color: #f0f1f4;
}

:root[data-theme="dark"] .search-input::placeholder {
  color: #6b7080;
}
</style>

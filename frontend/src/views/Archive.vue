<template>
  <div class="archive-page">
    <NavBar />

    <!-- Hero section -->
    <section class="archive-hero">
      <div class="hero-grain"></div>
      <div class="hero-content">
        <div class="hero-eyebrow reveal-up" style="--delay: 0.1s">
          <span class="eyebrow-line"></span>
          <span>ARCHIVE</span>
        </div>
        <h1 class="archive-title reveal-up" style="--delay: 0.2s">文章归档</h1>
        <div class="archive-meta reveal-up" style="--delay: 0.35s">
          <span class="meta-count">{{ totalCount }}</span>
          <span class="meta-label">篇文章</span>
          <span class="meta-dot">·</span>
          <span class="meta-label">{{ archives.length }} 个年份</span>
        </div>
      </div>
      <div class="hero-deco reveal-up" style="--delay: 0.5s">
        <div class="deco-line deco-h"></div>
        <div class="deco-line deco-v"></div>
        <div class="deco-dot"></div>
      </div>
    </section>

    <!-- Timeline content -->
    <main class="archive-container" v-loading="loading">
      <template v-if="archives.length > 0">
        <div class="timeline">
          <div v-for="(year, yi) in archives" :key="year.year" class="year-block reveal-up" :style="{ '--delay': (yi * 0.08 + 0.2) + 's' }">
            <!-- Timeline node -->
            <div class="timeline-node">
              <div class="node-dot"></div>
              <div class="node-line"></div>
            </div>

            <!-- Year content -->
            <div class="year-content">
              <h2 class="year-heading">{{ year.year }}</h2>
              <div v-for="month in year.months" :key="month.month" class="month-block">
                <h3 class="month-heading">
                  {{ month.month }} 月
                  <span class="month-count">{{ month.articles.length }} 篇</span>
                </h3>
                <div class="article-list">
                  <router-link
                    v-for="(article, ai) in month.articles"
                    :key="article.id"
                    :to="`/article/${article.id}`"
                    class="archive-article"
                    :style="{ '--ai': ai }"
                  >
                    <span class="archive-date">{{ formatDay(article.createTime) }}</span>
                    <span class="archive-article-title">{{ article.title }}</span>
                    <span class="archive-arrow">→</span>
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
      <div v-else-if="!loading" class="empty-state">
        <p>暂无文章</p>
      </div>
    </main>

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
import { getArchive } from '@/utils/api'
import NavBar from '@/components/NavBar.vue'
import { siteOwner } from '@/store/site'
import { useRevealOnScroll } from '@/composables/useRevealOnScroll'

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
  return dateStr.length >= 10 ? dateStr.substring(8, 10) : dateStr
}

useRevealOnScroll()

onMounted(() => {
  loading.value = true
  getArchive().then(res => {
    archives.value = res.data || []
  }).finally(() => { loading.value = false })
})
</script>

<style scoped>
/* ============================================================
   ARCHIVE PAGE
   ============================================================ */
.archive-page {
  min-height: 100vh;
  background: var(--gray-50);
  color: var(--gray-700);
  font-family: var(--font-body);
}

/* ============================================================
   HERO SECTION
   ============================================================ */
.archive-hero {
  position: relative;
  padding: 140px 32px 60px;
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  overflow: hidden;
}

.hero-content {
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
  margin-bottom: 20px;
}

.eyebrow-line {
  display: block;
  width: 32px;
  height: 1.5px;
  background: var(--indigo);
}

.archive-title {
  font-family: var(--font-display);
  font-size: clamp(36px, 5vw, 52px);
  font-weight: 400;
  color: var(--gray-900);
  margin: 0 0 16px;
  letter-spacing: -0.03em;
  line-height: 1.15;
}

.archive-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.meta-count {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 400;
  color: var(--indigo);
  letter-spacing: -0.04em;
  line-height: 1;
}

.meta-label {
  font-size: 14px;
  color: var(--gray-400);
}

.meta-dot {
  color: var(--gray-300);
  font-size: 14px;
}

.hero-deco {
  position: relative;
  width: 120px;
  height: 80px;
  flex-shrink: 0;
}

.deco-line {
  position: absolute;
  border-radius: 2px;
}

.deco-h {
  width: 60px;
  height: 1.5px;
  background: var(--indigo-soft);
  opacity: 0.4;
  top: 20px;
  right: 0;
}

.deco-v {
  width: 1.5px;
  height: 48px;
  background: var(--gray-300);
  top: 0;
  right: 60px;
}

.deco-dot {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--indigo);
  top: 17px;
  right: 57px;
}

/* ============================================================
   TIMELINE
   ============================================================ */
.archive-container {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 32px 80px;
}

.timeline {
  position: relative;
  padding-left: 40px;
}

/* Vertical timeline line */
.timeline::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 0;
  bottom: 0;
  width: 1.5px;
  background: linear-gradient(to bottom, var(--indigo-soft), var(--gray-200), transparent);
}

/* ============================================================
   YEAR BLOCK
   ============================================================ */
.year-block {
  position: relative;
  margin-bottom: 48px;
}

.year-block:last-child {
  margin-bottom: 0;
}

/* Timeline node */
.timeline-node {
  position: absolute;
  left: -40px;
  top: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.node-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--indigo);
  border: 2.5px solid var(--gray-50);
  box-shadow: 0 0 0 2px var(--indigo-soft);
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.node-line {
  width: 1.5px;
  flex: 1;
  min-height: 0;
}

.year-content {
  padding-bottom: 8px;
}

.year-heading {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
  color: var(--gray-900);
  margin-bottom: 24px;
  letter-spacing: -0.03em;
  line-height: 1;
}

/* ============================================================
   MONTH BLOCK
   ============================================================ */
.month-block {
  margin-bottom: 28px;
}

.month-block:last-child {
  margin-bottom: 0;
}

.month-heading {
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 8px;
}

.month-heading::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--gray-200);
}

.month-count {
  font-weight: 400;
  font-size: 11px;
  color: var(--gray-400);
  text-transform: none;
  letter-spacing: 0;
}

/* ============================================================
   ARTICLE LIST
   ============================================================ */
.article-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.archive-article {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: var(--bg-card);
  border-radius: 8px;
  border: 1px solid var(--gray-200);
  transition: background-color 0.3s;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  position: relative;
  overflow: hidden;
  animation: articleSlideIn 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(var(--ai, 0) * 0.05s);
}

@keyframes articleSlideIn {
  from {
    opacity: 0;
    transform: translateX(-12px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* Left border reveal on hover */
.archive-article::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--indigo);
  transform: scaleY(0);
  transform-origin: bottom;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  border-radius: 0 2px 2px 0;
}

.archive-article:hover::before {
  transform: scaleY(1);
  transform-origin: top;
}

.archive-article:hover {
  transform: translateX(4px);
  border-color: var(--indigo-soft);
  box-shadow: 0 2px 12px rgba(91, 106, 191, 0.06);
}

.archive-date {
  font-size: 13px;
  color: var(--gray-400);
  flex-shrink: 0;
  min-width: 28px;
  font-weight: 600;
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: -0.02em;
}

.archive-article-title {
  font-size: 14px;
  color: var(--gray-700);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  transition: color 0.2s;
}

.archive-article:hover .archive-article-title {
  color: var(--indigo);
}

.archive-arrow {
  font-size: 14px;
  color: var(--gray-300);
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  opacity: 0;
  transform: translateX(-8px);
}

.archive-article:hover .archive-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--indigo);
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
   DARK MODE
   ============================================================ */
:root[data-theme="dark"] .archive-article {
  background: #1e2028;
}

:root[data-theme="dark"] .node-dot {
  border-color: #181a20;
}

:root[data-theme="dark"] .archive-article::before {
  background: var(--indigo-soft);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 640px) {
  .archive-hero {
    padding: 100px 20px 40px;
    flex-direction: column;
    align-items: flex-start;
    gap: 24px;
  }

  .hero-deco {
    display: none;
  }

  .archive-title {
    font-size: 32px;
  }

  .meta-count {
    font-size: 26px;
  }

  .archive-container {
    padding: 0 20px 60px;
  }

  .timeline {
    padding-left: 32px;
  }

  .timeline-node {
    left: -32px;
  }

  .year-heading {
    font-size: 24px;
  }

  .archive-article {
    padding: 10px 12px;
    gap: 12px;
  }

  .archive-date {
    font-size: 15px;
    min-width: 24px;
  }

  .archive-article-title {
    font-size: 13px;
  }
}
</style>

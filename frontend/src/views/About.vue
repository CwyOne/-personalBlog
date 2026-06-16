<template>
  <div class="about-page">
    <NavBar />

    <main class="about-main" v-loading="loading">
      <!-- Hero section -->
      <section class="about-hero">
        <div class="hero-grain"></div>
        <div class="about-hero-inner">
          <div class="about-avatar-wrap reveal-up" style="--delay: 0.1s">
            <div class="avatar-ring">
              <img :src="about.avatar || defaultAvatar" class="about-avatar" @error="e => e.target.src = defaultAvatar" />
            </div>
          </div>
          <div class="about-intro reveal-up" style="--delay: 0.25s">
            <h1 class="about-name">{{ about.nickname || '博主' }}</h1>
            <p class="about-role">全栈开发 · 技术爱好者</p>
          </div>
          <div class="about-divider reveal-up" style="--delay: 0.35s">
            <span class="divider-line"></span>
            <span class="divider-dot"></span>
            <span class="divider-line"></span>
          </div>
        </div>
      </section>

      <!-- Bio -->
      <section class="about-section reveal-up" style="--delay: 0.4s" v-if="bioLines.length > 0">
        <h2 class="section-title">
          <span class="title-accent"></span>
          关于我
        </h2>
        <div class="bio-text">
          <p v-for="(line, i) in bioLines" :key="i">{{ line }}</p>
        </div>
      </section>

      <!-- Tech stack -->
      <section class="about-section reveal-up" style="--delay: 0.5s" v-if="techStack.length > 0">
        <h2 class="section-title">
          <span class="title-accent"></span>
          技术栈
        </h2>
        <div class="tech-grid">
          <span v-for="(t, i) in techStack" :key="t" class="tech-chip" :style="{ '--ci': i }">{{ t }}</span>
        </div>
      </section>

      <!-- Contact -->
      <section class="about-section reveal-up" style="--delay: 0.6s" v-if="about.publicEmail || about.github || about.twitter">
        <h2 class="section-title">
          <span class="title-accent"></span>
          联系方式
        </h2>
        <div class="contact-grid">
          <a v-if="about.publicEmail" class="contact-card" :href="'mailto:' + about.publicEmail">
            <span class="contact-icon">✉</span>
            <div class="contact-info">
              <span class="contact-label">Email</span>
              <span class="contact-value">{{ about.publicEmail }}</span>
            </div>
            <span class="contact-arrow">→</span>
          </a>
          <a v-if="about.github" class="contact-card" :href="about.github" target="_blank">
            <span class="contact-icon">⌘</span>
            <div class="contact-info">
              <span class="contact-label">GitHub</span>
              <span class="contact-value">{{ about.github.replace('https://github.com/', '') }}</span>
            </div>
            <span class="contact-arrow">→</span>
          </a>
          <a v-if="about.twitter" class="contact-card" :href="about.twitter" target="_blank">
            <span class="contact-icon">𝕏</span>
            <div class="contact-info">
              <span class="contact-label">Twitter</span>
              <span class="contact-value">关注我</span>
            </div>
            <span class="contact-arrow">→</span>
          </a>
        </div>
      </section>

      <!-- Stats -->
      <section class="about-section stats-section reveal-up" style="--delay: 0.7s">
        <h2 class="section-title">
          <span class="title-accent"></span>
          博客统计
        </h2>
        <div class="stats-container">
          <div class="stats-row">
            <div class="stat-block">
              <span class="stat-num">{{ stats.articles }}</span>
              <span class="stat-label">篇文章</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-num">{{ stats.tags }}</span>
              <span class="stat-label">个标签</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-num">{{ stats.days }}</span>
              <span class="stat-label">天运行</span>
            </div>
          </div>
        </div>
      </section>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { getAbout, getArticles, getTags } from '@/utils/api'
import NavBar from '@/components/NavBar.vue'
import { siteOwner, fetchSiteInfo } from '@/store/site'
import { useRevealOnScroll } from '@/composables/useRevealOnScroll'

const loading = ref(false)
const about = reactive({ nickname: '', avatar: '', bio: '', techStack: '[]', github: '', twitter: '', publicEmail: '', createTime: '' })
const stats = reactive({ articles: 0, tags: 0, days: 0 })
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'

const brandName = computed(() => (siteOwner.value.nickname || '子墨') + '的博客')
const techStack = computed(() => { try { return JSON.parse(about.techStack || '[]') } catch { return [] } })
const bioLines = computed(() => (about.bio || '').split('\n').filter(l => l.trim()))

useRevealOnScroll()

onMounted(async () => {
  await fetchSiteInfo()
  loading.value = true
  Promise.all([getAbout(), getArticles({ page: 1, size: 1 }), getTags()]).then(([a, ar, t]) => {
    Object.assign(about, a.data)
    stats.articles = ar.data.total || 0
    stats.tags = t.data.length || 0
    if (about.createTime) stats.days = Math.max(1, Math.floor((Date.now() - new Date(about.createTime).getTime()) / 86400000))
  }).finally(() => { loading.value = false })
})
</script>

<style scoped>
/* ============================================================
   ABOUT PAGE
   ============================================================ */
.about-page {
  min-height: 100vh;
  background: var(--gray-50);
  color: var(--gray-700);
  font-family: var(--font-body);
}

/* ============================================================
   HERO SECTION
   ============================================================ */
.about-hero {
  position: relative;
  padding: 140px 32px 48px;
  text-align: center;
  overflow: hidden;
}

.about-hero-inner {
  max-width: 1120px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* Avatar with decorative ring */
.about-avatar-wrap {
  margin-bottom: 24px;
}

.avatar-ring {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 108px;
  height: 108px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--indigo-bg), rgba(91, 106, 191, 0.08));
  padding: 4px;
  position: relative;
}

.avatar-ring::before {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 1.5px dashed var(--indigo-soft);
  opacity: 0.3;
  animation: ringRotate 30s linear infinite;
}

@keyframes ringRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.about-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #fff;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.about-name {
  font-family: var(--font-display);
  font-size: clamp(32px, 4vw, 42px);
  font-weight: 400;
  color: var(--gray-900);
  letter-spacing: -0.03em;
  margin: 0 0 8px;
  line-height: 1.2;
}

.about-role {
  font-size: 15px;
  color: var(--gray-400);
  letter-spacing: 0.06em;
  margin: 0;
}

/* Decorative divider */
.about-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 32px;
}

.divider-line {
  display: block;
  width: 40px;
  height: 1px;
  background: var(--gray-200);
}

.divider-dot {
  display: block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--indigo);
  opacity: 0.5;
}

/* ============================================================
   MAIN CONTENT
   ============================================================ */
.about-main {
  max-width: 680px;
  margin: 0 auto;
  padding: 0 32px 80px;
}

/* ============================================================
   SECTIONS
   ============================================================ */
.about-section {
  margin-bottom: 56px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--indigo);
  margin-bottom: 24px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--gray-200);
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-accent {
  display: block;
  width: 16px;
  height: 1.5px;
  background: var(--indigo);
  flex-shrink: 0;
}

/* ============================================================
   BIO
   ============================================================ */
.bio-text p {
  font-size: 15px;
  line-height: 1.9;
  color: var(--gray-600);
  margin-bottom: 14px;
}

.bio-text p:last-child {
  margin-bottom: 0;
}

/* ============================================================
   TECH STACK
   ============================================================ */
.tech-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tech-chip {
  font-size: 13px;
  color: var(--gray-600);
  background: var(--gray-100);
  padding: 7px 16px;
  border-radius: 100px;
  border: 1px solid var(--gray-200);
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
  animation: chipFadeIn 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(var(--ci, 0) * 0.04s);
}

@keyframes chipFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.tech-chip:hover {
  border-color: var(--indigo-soft);
  color: var(--indigo);
  background: var(--indigo-bg);
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(91, 106, 191, 0.1);
}

/* ============================================================
   CONTACT CARDS
   ============================================================ */
.contact-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.contact-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: var(--bg-card);
  border: 1px solid var(--gray-200);
  border-radius: 12px;
  transition: background-color 0.3s;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  text-decoration: none;
  position: relative;
  overflow: hidden;
}

.contact-card::before {
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

.contact-card:hover::before {
  transform: scaleY(1);
  transform-origin: top;
}

.contact-card:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 16px rgba(91, 106, 191, 0.06);
  border-color: var(--indigo-soft);
}

.contact-icon {
  font-size: 18px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--indigo-bg);
  border-radius: 10px;
  flex-shrink: 0;
  transition: all 0.3s;
}

.contact-card:hover .contact-icon {
  background: var(--indigo);
  color: #fff;
}

.contact-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.contact-label {
  font-size: 11px;
  color: var(--gray-400);
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.contact-value {
  font-size: 14px;
  color: var(--gray-700);
  font-weight: 500;
}

.contact-arrow {
  font-size: 16px;
  color: var(--gray-300);
  flex-shrink: 0;
  transition: all 0.3s;
  opacity: 0;
  transform: translateX(-8px);
}

.contact-card:hover .contact-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--indigo);
}

/* ============================================================
   STATS
   ============================================================ */
.stats-section {
  margin-bottom: 0;
}

.stats-container {
  background: linear-gradient(135deg, var(--indigo-bg), rgba(91, 106, 191, 0.04));
  border: 1px solid rgba(91, 106, 191, 0.1);
  border-radius: 16px;
  padding: 40px 32px;
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
}

.stat-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-family: var(--font-display);
  font-size: 48px;
  font-weight: 400;
  color: var(--indigo);
  line-height: 1;
  letter-spacing: -0.04em;
}

.stat-label {
  font-size: 12px;
  color: var(--gray-400);
  margin-top: 8px;
  letter-spacing: 0.04em;
}

.stat-divider {
  width: 1px;
  height: 48px;
  background: rgba(91, 106, 191, 0.15);
}

/* ============================================================
   DARK MODE
   ============================================================ */
:root[data-theme="dark"] .about-avatar {
  border-color: #1e2028;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.2);
}

:root[data-theme="dark"] .avatar-ring {
  background: linear-gradient(135deg, rgba(91, 106, 191, 0.1), rgba(91, 106, 191, 0.04));
}

:root[data-theme="dark"] .avatar-ring::before {
  border-color: rgba(91, 106, 191, 0.15);
}

:root[data-theme="dark"] .contact-card {
  background: #1e2028;
}

:root[data-theme="dark"] .contact-card:hover .contact-icon {
  background: var(--indigo);
  color: #fff;
}

:root[data-theme="dark"] .tech-chip {
  background: #252830;
}

:root[data-theme="dark"] .stats-container {
  background: linear-gradient(135deg, rgba(91, 106, 191, 0.08), rgba(91, 106, 191, 0.02));
  border-color: rgba(91, 106, 191, 0.12);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 640px) {
  .about-hero {
    padding: 100px 20px 36px;
  }

  .about-name {
    font-size: 28px;
  }

  .about-main {
    padding: 0 20px 60px;
  }

  .stats-row {
    gap: 24px;
  }

  .stat-num {
    font-size: 36px;
  }

  .stats-container {
    padding: 28px 20px;
  }

  .contact-card {
    padding: 14px 16px;
  }
}
</style>

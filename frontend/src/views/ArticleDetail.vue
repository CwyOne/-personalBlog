<template>
  <div class="detail-page">
    <NavBar />

    <div class="detail-layout">
      <!-- Left sidebar: author info (desktop only) -->
      <aside class="sidebar-left" v-if="article">
        <div class="author-card">
          <img :src="article.authorAvatar || defaultAvatar" class="author-avatar" @error="e => e.target.src = defaultAvatar" />
          <div class="author-name">{{ article.authorNickname || '博主' }}</div>
          <div class="author-role">技术爱好者</div>
        </div>
        <div class="article-quick-info">
          <div class="quick-stat">
            <span class="qs-num">{{ article.viewCount }}</span>
            <span class="qs-label">阅读</span>
          </div>
          <div class="quick-stat">
            <span class="qs-num">{{ article.likeCount || 0 }}</span>
            <span class="qs-label">点赞</span>
          </div>
        </div>
      </aside>

      <!-- Center: article content -->
      <main class="detail-main">
        <div v-if="loading" class="loading-skeleton">
          <div class="sk-title"></div>
          <div class="sk-meta"></div>
          <div class="sk-line" v-for="i in 8" :key="i" :style="{ width: (85 + Math.random() * 15) + '%' }"></div>
        </div>

        <template v-else-if="article">
          <!-- Cover -->
          <div class="detail-cover" v-if="article.coverImage">
            <img :src="article.coverImage" :alt="article.title" />
          </div>

          <!-- Header -->
          <header class="article-header">
            <router-link to="/" class="back-link">
              <span class="back-arrow">←</span>
              <span>返回</span>
            </router-link>

            <h1 class="article-title">{{ article.title }}</h1>

            <div class="article-meta">
              <span class="meta-date">{{ formatDate(article.createTime) }}</span>
              <span class="meta-dot">·</span>
              <span class="meta-views">{{ article.viewCount }} 次阅读</span>
            </div>

            <!-- Tags: staggered -->
            <div class="article-tags" v-if="article.tags?.length">
              <span
                v-for="(tag, i) in article.tags"
                :key="tag.id"
                class="tag-chip"
                :style="{ '--offset': (i % 3) * 4 + 'px' }"
              >{{ tag.name }}</span>
            </div>
          </header>

          <!-- Content -->
          <article class="article-content" ref="articleContentRef" v-html="article.contentHtml"></article>

          <!-- Like bar -->
          <div class="like-bar">
            <button
              :class="['like-btn', { liked: article.liked }]"
              @click="handleLikeArticle"
              :disabled="likeLoading"
            >
              <span class="like-icon">{{ article.liked ? '♥' : '♡' }}</span>
              <span>{{ article.liked ? '已赞' : '点赞' }}</span>
              <span class="like-count">{{ article.likeCount || 0 }}</span>
            </button>
          </div>

          <!-- Recommended articles -->
          <section class="recommend-section" v-if="relatedArticles.length > 0">
            <h3 class="recommend-title">推荐阅读</h3>
            <div class="recommend-grid">
              <router-link
                v-for="(ra, i) in relatedArticles"
                :key="ra.id"
                :to="'/article/' + ra.id"
                :class="['recommend-card', 'rc-variant-' + (i % 3)]"
              >
                <div class="rc-cover" v-if="ra.coverImage && i % 3 !== 2">
                  <img :src="ra.coverImage" :alt="ra.title" loading="lazy" />
                </div>
                <div class="rc-body">
                  <span class="rc-date">{{ formatShortDate(ra.createTime) }}</span>
                  <h4 class="rc-title">{{ ra.title }}</h4>
                  <p class="rc-summary" v-if="i % 3 === 0">{{ ra.summary }}</p>
                </div>
              </router-link>
            </div>
          </section>

          <!-- Comments -->
          <section class="comment-section">
            <div class="comment-header-row">
              <h3 class="comment-title">留言</h3>
              <span class="comment-count">{{ totalCommentCount }}</span>
            </div>

            <div class="comment-list" v-if="comments.length > 0">
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <img :src="c.avatar || defaultAvatar" class="c-avatar" />
                <div class="c-body">
                  <div class="c-meta">
                    <span class="c-name">{{ c.nickname }}</span>
                    <span class="c-time">{{ formatDate(c.createTime) }}</span>
                  </div>
                  <p class="c-text">{{ c.content }}</p>
                  <div class="c-actions">
                    <button class="c-action-btn" @click="handleLikeComment(c)">
                      <span :class="{ active: c.liked }">{{ c.liked ? '♥' : '♡' }}</span>
                      {{ c.likeCount || 0 }}
                    </button>
                    <button v-if="isLoggedIn" class="c-action-btn" @click="startReply(c)">回复</button>
                  </div>
                  <!-- Replies -->
                  <div v-if="c.children?.length" class="reply-list">
                    <div v-for="r in c.children" :key="r.id" class="comment-item reply-item">
                      <img :src="r.avatar || defaultAvatar" class="c-avatar c-avatar-sm" />
                      <div class="c-body">
                        <div class="c-meta">
                          <span class="c-name">{{ r.nickname }}</span>
                          <span v-if="r.replyToNickname" class="c-reply-tag">→ {{ r.replyToNickname }}</span>
                          <span class="c-time">{{ formatDate(r.createTime) }}</span>
                        </div>
                        <p class="c-text">{{ r.content }}</p>
                        <div class="c-actions">
                          <button class="c-action-btn" @click="handleLikeComment(r)">
                            <span :class="{ active: r.liked }">{{ r.liked ? '♥' : '♡' }}</span>
                            {{ r.likeCount || 0 }}
                          </button>
                          <button v-if="isLoggedIn" class="c-action-btn" @click="startReply(r)">回复</button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="comment-empty">暂无留言</div>

            <!-- Comment form -->
            <div class="comment-form">
              <div v-if="!isLoggedIn" class="login-prompt">
                <router-link :to="'/login?redirect=/article/' + route.params.id" class="login-link">登录</router-link>
                后参与讨论
              </div>
              <template v-else>
                <div v-if="replyTo" class="reply-banner">
                  回复 <b>@{{ replyTo.nickname }}</b>
                  <button @click="cancelReply" class="cancel-reply">×</button>
                </div>
                <div class="form-user">
                  <img :src="currentUser.avatar || defaultAvatar" class="form-avatar" />
                  <span class="form-name">{{ currentUser.nickname || currentUser.username }}</span>
                </div>
                <textarea
                  v-model="commentText"
                  class="form-textarea"
                  placeholder="写下你的想法..."
                  maxlength="500"
                  rows="3"
                ></textarea>
                <div class="form-footer">
                  <span class="char-count">{{ commentText.length }}/500</span>
                  <button class="submit-btn" :disabled="commentLoading || !commentText.trim()" @click="handleComment">
                    {{ commentLoading ? '发送中...' : '发表' }}
                  </button>
                </div>
              </template>
            </div>
          </section>
        </template>

        <div v-else class="not-found">
          <p>文章未找到</p>
          <router-link to="/" class="back-link">返回首页</router-link>
        </div>
      </main>

    </div>

    <!-- Right sidebar: TOC — 使用 fixed 定位，独立于网格布局，确保始终可见 -->
    <aside class="sidebar-right" v-if="!loading && article && tocItems.length > 0">
      <div class="toc-wrap">
        <div class="toc-label">目录</div>
        <nav class="toc-nav">
          <a
            v-for="item in tocItems"
            :key="item.id"
            :class="['toc-link', 'tl-' + item.level, { active: activeTocId === item.id }]"
            :href="'#' + item.id"
            @click.prevent="scrollToHeading(item.id)"
          >{{ item.text }}</a>
        </nav>
      </div>
    </aside>

    <footer class="site-footer">
      <p>© 2026 {{ brandName }}</p>
    </footer>

    <!-- 图片点击放大查看器 -->
    <ImageViewer
      v-if="!loading && article"
      container-selector=".article-content"
      :enabled="true"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleDetail, getArticles, getComments, addComment, likeArticle, likeComment } from '@/utils/api'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import ImageViewer from '@/components/ImageViewer.vue'
import { useRevealOnScroll } from '@/composables/useRevealOnScroll'
import { currentUser, isLoggedIn } from '@/store/user'
import { siteOwner, fetchSiteInfo } from '@/store/site'
import { updateSEO, getCurrentUrl } from '@/utils/seo'
import hljs from 'highlight.js'

const route = useRoute()
const router = useRouter()

const article = ref(null)
const loading = ref(true)
const comments = ref([])
const commentText = ref('')
const commentLoading = ref(false)
const likeLoading = ref(false)
const replyTo = ref(null)
const relatedArticles = ref([])
const tocItems = ref([])
const activeTocId = ref('')
const articleContentRef = ref(null)

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'
const brandName = computed(() => (siteOwner.value.nickname || '子墨') + '的博客')

// Reveal-on-scroll animation (shared composable)
useRevealOnScroll()

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatShortDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

// TOC
function buildToc() {
  const container = articleContentRef.value || document.querySelector('.article-content')
  if (!container) return
  const headings = container.querySelectorAll('h1, h2, h3')
  const list = []
  headings.forEach(h => {
    if (!h.id) h.id = 'h-' + Math.random().toString(36).slice(2, 8)
    list.push({ id: h.id, text: h.textContent, level: parseInt(h.tagName.charAt(1)) })
  })
  tocItems.value = list
}

function scrollToHeading(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeTocId.value = id
  }
}

function onScroll() {
  const headings = tocItems.value.map(i => document.getElementById(i.id)).filter(Boolean)
  for (let i = headings.length - 1; i >= 0; i--) {
    if (headings[i].getBoundingClientRect().top < 120) {
      activeTocId.value = tocItems.value[i].id
      return
    }
  }
  activeTocId.value = ''
}

// Comments
function fetchComments() {
  getComments(route.params.id).then(res => { comments.value = res.data || [] })
}

const totalCommentCount = computed(() => {
  let count = comments.value.length
  for (const c of comments.value) { if (c.children) count += c.children.length }
  return count
})

function startReply(comment) {
  replyTo.value = { commentId: comment.id, userId: comment.userId, nickname: comment.nickname }
  commentText.value = ''
  nextTick(() => document.querySelector('.form-textarea')?.focus())
}

function cancelReply() { replyTo.value = null }

function handleComment() {
  const text = commentText.value.trim()
  if (!text) { ElMessage.warning('请输入留言内容'); return }
  commentLoading.value = true
  addComment(route.params.id, text, replyTo.value?.commentId, replyTo.value?.userId)
    .then(() => {
      ElMessage.success(replyTo.value ? '回复成功' : '留言成功')
      commentText.value = ''
      replyTo.value = null
      fetchComments()
    })
    .finally(() => { commentLoading.value = false })
}

function handleLikeArticle() {
  if (!isLoggedIn.value) { router.push(`/login?redirect=/article/${route.params.id}`); return }
  likeLoading.value = true
  likeArticle(route.params.id)
    .then(res => { article.value.liked = res.data.liked; article.value.likeCount = res.data.count })
    .finally(() => { likeLoading.value = false })
}

function handleLikeComment(c) {
  if (!isLoggedIn.value) { router.push(`/login?redirect=/article/${route.params.id}`); return }
  likeComment(c.id).then(res => { c.liked = res.data.liked; c.likeCount = res.data.count })
}

function enhanceContent(attempt = 0) {
  const container = articleContentRef.value || document.querySelector('.article-content')
  if (!container) {
    // Retry up to 10 times with increasing delays if DOM isn't ready yet
    if (attempt < 10) {
      setTimeout(() => enhanceContent(attempt + 1), 50 * (attempt + 1))
    }
    return
  }

  // Code blocks with line numbers + copy
  container.querySelectorAll('pre').forEach(pre => {
    if (pre.querySelector('.code-toolbar')) return
    const code = pre.querySelector('code')
    if (!code) return

    hljs.highlightElement(code)

    // Wrap
    const wrap = document.createElement('div')
    wrap.className = 'code-toolbar'
    pre.parentNode.insertBefore(wrap, pre)
    wrap.appendChild(pre)

    // Line numbers
    const lines = code.innerHTML.split('\n')
    if (lines.length > 1) {
      const lineNums = document.createElement('div')
      lineNums.className = 'line-numbers'
      lineNums.innerHTML = lines.map((_, i) => `<span>${i + 1}</span>`).join('')
      pre.insertBefore(lineNums, code)
    }

    // Copy button
    const btn = document.createElement('button')
    btn.className = 'copy-btn'
    btn.textContent = '复制'
    btn.addEventListener('click', () => {
      navigator.clipboard.writeText(code.textContent).then(() => {
        btn.textContent = '已复制'
        setTimeout(() => { btn.textContent = '复制' }, 1500)
      })
    })
    wrap.appendChild(btn)
  })

  // Image styling
  container.querySelectorAll('img').forEach(img => {
    img.classList.add('article-img')
  })

  buildToc()
}

// Fetch related articles
async function fetchRelated() {
  try {
    const res = await getArticles({ page: 1, size: 4 })
    relatedArticles.value = (res.data.records || []).filter(a => a.id !== article.value?.id).slice(0, 3)
  } catch {}
}

async function loadArticle(id) {
  loading.value = true
  article.value = null
  tocItems.value = []
  relatedArticles.value = []
  comments.value = []
  window.scrollTo({ top: 0 })
  try {
    const res = await getArticleDetail(id)
    article.value = res.data
    fetchRelated()
    // Update SEO meta with actual article data
    updateSEO({
      title: res.data.title,
      description: res.data.summary || undefined,
      image: res.data.coverImage || undefined,
      url: getCurrentUrl(),
      type: 'article',
      author: res.data.authorNickname || undefined,
      publishedTime: res.data.createTime || undefined
    })
  } finally {
    loading.value = false
  }
  fetchComments()
}

// Watch for article content to be rendered in DOM, then enhance
watch(
  () => article.value?.contentHtml,
  async (newHtml) => {
    if (!newHtml) return
    // Wait two ticks: one for reactivity, one for DOM render
    await nextTick()
    await nextTick()
    enhanceContent()
  }
)

onMounted(async () => {
  await fetchSiteInfo()
  await loadArticle(route.params.id)
  window.addEventListener('scroll', onScroll, { passive: true })
})

// Re-load when navigating between articles (e.g. clicking recommended articles)
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadArticle(newId)
  }
})

onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--bg-secondary);
  font-family: var(--font-body);
  transition: background-color 0.3s;
}

/* ============================================================
   THREE-COLUMN LAYOUT
   ============================================================ */
.detail-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 88px 24px 0;
  display: grid;
  grid-template-columns: 180px 1fr 200px;
  gap: 40px;
  align-items: start;
}

/* Left sidebar */
.sidebar-left {
  position: sticky;
  top: 88px;
  padding-top: 12px;
}

.author-card {
  text-align: center;
  margin-bottom: 24px;
}

.author-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 10px;
  border: 2px solid var(--gray-200);
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
  margin-bottom: 2px;
}

.author-role {
  font-size: 12px;
  color: var(--gray-400);
}

.article-quick-info {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.quick-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qs-num {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--gray-800);
  line-height: 1;
}

.qs-label {
  font-size: 11px;
  color: var(--gray-400);
  margin-top: 4px;
}

/* Right sidebar: TOC — fixed 定位，独立于网格布局 */
.sidebar-right {
  position: fixed;
  top: 88px;
  right: max(24px, calc((100vw - 1200px) / 2 - 220px));
  width: 200px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  z-index: 50;
}

.toc-wrap {
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius);
  transition: background-color 0.3s, border-color 0.3s;
}

.toc-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: 14px;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-link {
  display: block;
  padding: 5px 0 5px 10px;
  font-size: 13px;
  color: var(--gray-400);
  text-decoration: none;
  border-left: 2px solid transparent;
  transition: all 0.2s;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-link:hover { color: var(--gray-700); }
.toc-link.active { color: var(--indigo); border-left-color: var(--indigo); font-weight: 500; }
.tl-2 { padding-left: 18px; }
.tl-3 { padding-left: 26px; font-size: 12px; }

.toc-empty {
  font-size: 13px;
  color: var(--gray-400);
  padding: 4px 0;
}

/* ============================================================
   CENTER: ARTICLE CONTENT
   ============================================================ */
.detail-main {
  min-width: 0;
  padding-bottom: 80px;
}

/* Loading skeleton */
.loading-skeleton {
  padding: 40px 0;
}

.sk-title {
  height: 32px;
  background: var(--gray-200);
  border-radius: 4px;
  margin-bottom: 16px;
  width: 80%;
  animation: shimmer 1.5s infinite;
}

.sk-meta {
  height: 16px;
  background: var(--gray-100);
  border-radius: 4px;
  margin-bottom: 32px;
  width: 40%;
  animation: shimmer 1.5s infinite 0.1s;
}

.sk-line {
  height: 14px;
  background: var(--gray-100);
  border-radius: 4px;
  margin-bottom: 12px;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

/* Cover */
.detail-cover {
  margin: 0 -8px 32px;
  border-radius: 10px;
  overflow: hidden;
}

.detail-cover img {
  width: 100%;
  max-height: 380px;
  object-fit: cover;
  display: block;
}

/* Header */
.article-header {
  margin-bottom: 40px;
  padding-bottom: 32px;
  border-bottom: 1px solid var(--gray-200);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--gray-400);
  font-size: 13px;
  text-decoration: none;
  margin-bottom: 24px;
  transition: color 0.2s;
}

.back-link:hover { color: var(--indigo); }
.back-arrow { transition: transform 0.2s; }
.back-link:hover .back-arrow { transform: translateX(-3px); }

.article-title {
  font-family: var(--font-display);
  font-size: clamp(26px, 3.5vw, 36px);
  font-weight: 400;
  color: var(--gray-900);
  line-height: 1.3;
  margin-bottom: 16px;
  letter-spacing: -0.03em;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--gray-400);
  margin-bottom: 18px;
}

.meta-dot { color: var(--gray-300); }

.article-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 2px;
}

.tag-chip {
  font-size: 12px;
  color: var(--indigo);
  background: var(--indigo-bg);
  padding: 4px 12px;
  border-radius: 100px;
  font-weight: 500;
  transform: translateY(var(--offset, 0));
}

/* ============================================================
   ARTICLE CONTENT STYLES — 对标 CSDN 排版体验
   ============================================================ */
.article-content {
  font-family: var(--font-body);
  font-size: 16px;
  line-height: 1.9;
  color: var(--gray-700);
  max-width: 100%;
  word-break: break-word;
}

/* ── 标题：加粗 + 层级字号 + 合理间距 ── */
.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3),
.article-content :deep(h4),
.article-content :deep(h5),
.article-content :deep(h6) {
  font-family: var(--font-display);
  font-weight: 700;
  color: var(--gray-900);
  margin: 2.2em 0 0.8em;
  line-height: 1.35;
  letter-spacing: -0.01em;
  scroll-margin-top: 80px;
}

.article-content :deep(h1) {
  font-size: 1.85em;
  margin-top: 2.5em;
  margin-bottom: 1em;
  padding-bottom: 0.4em;
  border-bottom: 2px solid var(--gray-200);
}

.article-content :deep(h2) {
  font-size: 1.55em;
  margin-top: 2.2em;
  padding-bottom: 0.45em;
  border-bottom: 1px solid var(--gray-200);
}

.article-content :deep(h3) {
  font-size: 1.3em;
  margin-top: 2em;
}

.article-content :deep(h4) {
  font-size: 1.15em;
  margin-top: 1.8em;
}

.article-content :deep(h5) {
  font-size: 1.05em;
  margin-top: 1.6em;
}

.article-content :deep(h6) {
  font-size: 1em;
  margin-top: 1.4em;
  color: var(--gray-600);
}

/* ── 正文段落 ── */
.article-content :deep(p) {
  margin: 1.2em 0;
  line-height: 1.9;
}

.article-content :deep(img) {
  cursor: zoom-in;
  transition: transform 0.2s, box-shadow 0.2s;
}

.article-content :deep(img:hover) {
  transform: scale(1.01);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.article-content :deep(.article-img) {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
  margin: 1.5em 0;
}

/* ── 引用块 ── */
.article-content :deep(blockquote) {
  border-left: 4px solid var(--indigo-soft);
  padding: 0.8em 1.4em;
  margin: 1.6em 0;
  color: var(--gray-600);
  background: var(--indigo-bg);
  border-radius: 0 8px 8px 0;
  font-size: 15px;
  line-height: 1.8;
}

.article-content :deep(blockquote p) {
  margin: 0.6em 0;
}

/* ── 列表 ── */
.article-content :deep(ul),
.article-content :deep(ol) {
  padding-left: 1.8em;
  margin: 1.2em 0;
}

.article-content :deep(li) {
  margin: 0.5em 0;
  line-height: 1.8;
}

.article-content :deep(li::marker) {
  color: var(--indigo-soft);
}

/* ── 链接 ── */
.article-content :deep(a) {
  color: var(--indigo);
  text-decoration: underline;
  text-underline-offset: 3px;
  text-decoration-color: rgba(91, 106, 191, 0.3);
  transition: text-decoration-color 0.2s, color 0.2s;
}

.article-content :deep(a:hover) {
  text-decoration-color: var(--indigo);
  color: var(--indigo-soft);
}

/* ── 表格 ── */
.article-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1.6em 0;
  font-size: 14px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--gray-200);
}

.article-content :deep(th),
.article-content :deep(td) {
  border: 1px solid var(--gray-200);
  padding: 12px 16px;
  text-align: left;
}

.article-content :deep(th) {
  background: var(--gray-100);
  font-weight: 600;
  font-size: 13px;
  color: var(--gray-700);
}

.article-content :deep(tr:hover td) {
  background: var(--gray-50);
}

/* ---- Code blocks: dark frosted ---- */
.article-content :deep(.code-toolbar) {
  position: relative;
  margin: 1.5em 0;
  border-radius: 10px;
  overflow: hidden;
  background: #1a1d27;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.article-content :deep(pre) {
  margin: 0;
  padding: 20px 20px 20px 0;
  background: transparent;
  overflow-x: auto;
  display: flex;
  gap: 0;
}

.article-content :deep(.line-numbers) {
  display: flex;
  flex-direction: column;
  padding: 20px 16px 20px 20px;
  text-align: right;
  user-select: none;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.article-content :deep(.line-numbers span) {
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.18);
}

.article-content :deep(pre code) {
  font-family: var(--font-mono);
  font-size: 13.5px;
  line-height: 1.7;
  color: #c9d1d9;
  background: none;
  padding: 0;
  flex: 1;
  min-width: 0;
}

.article-content :deep(.copy-btn) {
  position: absolute;
  top: 10px;
  right: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.4);
  padding: 4px 12px;
  border-radius: 5px;
  font-size: 12px;
  font-family: var(--font-body);
  cursor: pointer;
  transition: all 0.2s;
  z-index: 2;
}

.article-content :deep(.copy-btn:hover) {
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.7);
}

.article-content :deep(code) {
  font-family: var(--font-mono);
  background: var(--gray-100);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.88em;
  color: #c9596d;
}

.article-content :deep(pre code) {
  background: none;
  padding: 0;
  color: #c9d1d9;
}

/* ============================================================
   LIKE BAR
   ============================================================ */
.like-bar {
  text-align: center;
  margin: 48px 0;
  padding: 28px 0;
  border-top: 1px solid var(--gray-200);
  border-bottom: 1px solid var(--gray-200);
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 28px;
  border: 1.5px solid var(--gray-200);
  border-radius: 100px;
  background: transparent;
  font-size: 14px;
  font-family: var(--font-body);
  color: var(--gray-600);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.like-btn:hover { border-color: var(--indigo-soft); color: var(--indigo); transform: translateY(-1px); }
.like-btn.liked { background: var(--indigo); border-color: var(--indigo); color: #fff; }
.like-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.like-icon { font-size: 18px; }
.like-count { font-weight: 600; }

/* ============================================================
   RECOMMENDED ARTICLES — staggered cards
   ============================================================ */
.recommend-section {
  margin: 0 0 48px;
}

.recommend-title {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: 20px;
}

.recommend-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 12px;
}

.recommend-card {
  display: flex;
  background: var(--bg-card);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius);
  transition: background-color 0.3s;
  overflow: hidden;
  text-decoration: none;
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.recommend-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04); }

.rc-variant-0 { grid-column: span 1; }
.rc-variant-1 { grid-column: span 1; }
.rc-variant-2 { grid-column: span 2; }

.rc-variant-2 .rc-body { display: flex; align-items: center; gap: 16px; }

.rc-cover {
  width: 140px;
  height: 100px;
  flex-shrink: 0;
  overflow: hidden;
}

.rc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}

.recommend-card:hover .rc-cover img { transform: scale(1.06); }

.rc-body {
  padding: 14px 16px;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.rc-date {
  font-size: 11px;
  color: var(--gray-400);
  margin-bottom: 6px;
}

.rc-title {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 400;
  color: var(--gray-800);
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc-summary {
  font-size: 12px;
  color: var(--gray-500);
  margin: 6px 0 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============================================================
   COMMENTS
   ============================================================ */
.comment-section {
  border-top: 1px solid var(--gray-200);
  padding-top: 36px;
}

.comment-header-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.comment-title {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--gray-400);
}

.comment-count {
  font-size: 12px;
  color: var(--gray-400);
  background: var(--gray-100);
  padding: 2px 10px;
  border-radius: 100px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.comment-item { display: flex; gap: 12px; }

.c-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.c-avatar-sm { width: 28px; height: 28px; }

.c-body { flex: 1; min-width: 0; }

.c-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
  flex-wrap: wrap;
}

.c-name { font-size: 13px; font-weight: 600; color: var(--gray-800); }
.c-time { font-size: 11px; color: var(--gray-400); }
.c-reply-tag { font-size: 11px; color: var(--gray-400); }
.c-reply-tag b { color: var(--indigo); font-weight: 500; }
.c-text { font-size: 14px; color: var(--gray-600); line-height: 1.7; margin: 0; }
.comment-empty { text-align: center; padding: 40px 0; color: var(--gray-400); font-size: 14px; }

.c-actions {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.c-action-btn {
  background: none;
  border: none;
  font-size: 12px;
  font-family: var(--font-body);
  color: var(--gray-400);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
  padding: 0;
}

.c-action-btn:hover { color: var(--indigo); }
.c-action-btn .active { color: #e6a23c; }

.reply-list {
  margin-top: 16px;
  padding-left: 0;
  border-left: 2px solid var(--gray-200);
}

.reply-item { padding-left: 14px; margin-top: 12px; }
.reply-item:first-child { margin-top: 0; }

/* Comment form */
.comment-form {
  padding: 20px;
  background: var(--gray-100);
  border-radius: var(--radius);
}

.login-prompt {
  text-align: center;
  font-size: 14px;
  color: var(--gray-500);
}

.login-link { color: var(--indigo); font-weight: 500; text-decoration: none; }
.login-link:hover { text-decoration: underline; }

.reply-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: var(--indigo-bg);
  border-radius: 6px;
  font-size: 13px;
  color: var(--indigo);
}

.reply-banner b { font-weight: 600; }

.cancel-reply {
  background: none;
  border: none;
  font-size: 18px;
  color: var(--gray-400);
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
}

.form-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.form-avatar { width: 26px; height: 26px; border-radius: 50%; object-fit: cover; }
.form-name { font-size: 13px; color: var(--gray-600); font-weight: 500; }

.form-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1.5px solid var(--gray-200);
  border-radius: 8px;
  background: var(--bg-card);
  font-size: 14px;
  font-family: var(--font-body);
  color: var(--gray-700);
  resize: vertical;
  transition: background-color 0.3s;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  line-height: 1.6;
}

.form-textarea::placeholder { color: var(--gray-400); }
.form-textarea:focus { border-color: var(--indigo-soft); box-shadow: 0 0 0 3px rgba(91, 106, 191, 0.06); }

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.char-count { font-size: 12px; color: var(--gray-400); }

.submit-btn {
  padding: 8px 22px;
  border: none;
  border-radius: 6px;
  background: var(--indigo);
  color: #fff;
  font-size: 13px;
  font-family: var(--font-body);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover:not(:disabled) { background: var(--indigo-soft); }
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Not found */
.not-found {
  text-align: center;
  padding: 160px 0;
}

.not-found p { font-size: 16px; color: var(--gray-400); margin-bottom: 16px; }

/* ============================================================
   DARK MODE
   ============================================================ */
:root[data-theme="dark"] .toc-wrap { background: #1e2028; }
:root[data-theme="dark"] .comment-form { background: #1e2028; }
:root[data-theme="dark"] .form-textarea { background: #252830; }
:root[data-theme="dark"] .recommend-card { background: #1e2028; }
:root[data-theme="dark"] .detail-cover img { box-shadow: 0 2px 16px rgba(0, 0, 0, 0.2); }

/* 暗黑模式：标题颜色适配 */
:root[data-theme="dark"] .article-content :deep(h1),
:root[data-theme="dark"] .article-content :deep(h2),
:root[data-theme="dark"] .article-content :deep(h3),
:root[data-theme="dark"] .article-content :deep(h4),
:root[data-theme="dark"] .article-content :deep(h5) {
  color: #f0f1f4;
}

:root[data-theme="dark"] .article-content :deep(h6) {
  color: #c5c8d2;
}

:root[data-theme="dark"] .article-content :deep(h1),
:root[data-theme="dark"] .article-content :deep(h2) {
  border-bottom-color: #2a2d36;
}

/* 暗黑模式：引用块 */
:root[data-theme="dark"] .article-content :deep(blockquote) {
  background: #1e2028;
  border-left-color: #5b6abf;
  color: #9498a6;
}

/* 暗黑模式：表格 */
:root[data-theme="dark"] .article-content :deep(table) {
  border-color: #2a2d36;
}

:root[data-theme="dark"] .article-content :deep(th),
:root[data-theme="dark"] .article-content :deep(td) {
  border-color: #2a2d36;
}

:root[data-theme="dark"] .article-content :deep(th) {
  background: #252830;
  color: #e5e7ed;
}

:root[data-theme="dark"] .article-content :deep(tr:hover td) {
  background: #1a1c24;
}

/* 暗黑模式：列表标记 */
:root[data-theme="dark"] .article-content :deep(li::marker) {
  color: #7884c9;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1080px) {
  .detail-layout {
    grid-template-columns: 1fr;
    padding-top: 80px;
  }

  .sidebar-left,
  .sidebar-right { display: none; }
}

@media (max-width: 640px) {
  .detail-layout { padding: 72px 16px 0; gap: 0; }

  .detail-cover { margin: 0 0 24px; border-radius: 8px; }

  .article-title { font-size: 24px; }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .rc-variant-2 { grid-column: span 1; }
  .rc-variant-2 .rc-body { flex-direction: column; align-items: flex-start; }

  .comment-form { padding: 16px; }
}
</style>

<template>
  <div class="detail-page">
    <NavBar />
    <TableOfContents v-if="!loading && article" />
    <main>
      <div v-if="loading" v-loading="loading" style="min-height: 400px;"></div>
      <template v-else-if="article">
        <div class="detail-cover" v-if="article.coverImage">
          <img :src="article.coverImage" :alt="article.title" />
        </div>

        <div class="detail-container">
          <router-link to="/" class="back-link">← 返回首页</router-link>

          <h1 class="article-title">{{ article.title }}</h1>

          <div class="article-meta">
            <div class="meta-author" v-if="article.authorNickname">
              <img :src="article.authorAvatar || defaultAvatar" class="meta-avatar" />
              <div class="meta-author-info">
                <span class="meta-author-name">{{ article.authorNickname }}</span>
                <span class="meta-time">{{ formatDate(article.createTime) }} · {{ article.viewCount }} 次阅读</span>
              </div>
            </div>
          </div>

          <div class="article-tags" v-if="article.tags && article.tags.length > 0">
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

          <div class="article-content" v-html="article.contentHtml"></div>

          <!-- Article Like -->
          <div class="article-like-bar">
            <el-button
              :type="article.liked ? 'danger' : 'default'"
              @click="handleLikeArticle"
              :loading="likeLoading"
              round
            >
              <el-icon style="margin-right:4px;"><StarFilled v-if="article.liked" /><Star v-else /></el-icon>
              {{ article.liked ? '已赞' : '点赞' }} {{ article.likeCount || 0 }}
            </el-button>
          </div>

          <!-- Comment Section -->
          <div class="comment-section">
            <h3 class="comment-title">留言 ({{ comments.length }})</h3>

            <!-- Comment list -->
            <div class="comment-list" v-if="comments.length > 0">
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <img :src="c.avatar || defaultAvatar" class="comment-avatar" />
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-author">{{ c.nickname }}</span>
                    <span class="comment-time">{{ formatDate(c.createTime) }}</span>
                  </div>
                  <p class="comment-text">{{ c.content }}</p>
                  <div class="comment-actions">
                    <span class="like-btn" @click="handleLikeComment(c)">
                      <el-icon><component :is="c.liked ? 'StarFilled' : 'Star'" /></el-icon>
                      {{ c.liked ? '已赞' : '赞' }} {{ c.likeCount || 0 }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="comment-empty">
              <p>暂无留言，来说两句吧</p>
            </div>

            <!-- Comment form -->
            <div class="comment-form">
              <div v-if="!isLoggedIn" class="login-hint">
                <el-button type="primary" @click="goLogin" size="small">登录</el-button>
                <span> 后参与留言</span>
              </div>
              <template v-else>
                <div class="comment-form-header">
                  <img :src="currentUser.avatar || defaultAvatar" class="comment-form-avatar" />
                  <span class="comment-form-name">{{ currentUser.nickname || currentUser.username }}</span>
                </div>
                <el-input
                  v-model="commentText"
                  type="textarea"
                  :rows="3"
                  placeholder="写下你的想法..."
                  maxlength="500"
                  show-word-limit
                />
                <el-button
                  type="primary"
                  :loading="commentLoading"
                  @click="handleComment"
                  style="margin-top: 12px;"
                >
                  发表留言
                </el-button>
              </template>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="文章未找到" :image-size="120" style="padding: 120px 0;" />
    </main>

    <footer class="site-footer">
      <p>© 2026 子墨的博客 · Powered by Vue 3 & Spring Boot</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleDetail, getComments, addComment, likeArticle, likeComment } from '@/utils/api'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import TableOfContents from '@/components/TableOfContents.vue'
import { currentUser, isLoggedIn } from '@/store/user'
import hljs from 'highlight.js'

const route = useRoute()
const router = useRouter()
const article = ref(null)
const loading = ref(true)
const comments = ref([])
const commentText = ref('')
const commentLoading = ref(false)
const likeLoading = ref(false)

const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf'

const tagColors = [
  '#409eff', '#67c23a', '#e6a23c', '#f56c6c',
  '#909399', '#b37feb', '#36cfc9', '#ff85c0'
]

function tagColor(tagId) {
  return tagColors[tagId % tagColors.length]
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function fetchComments() {
  getComments(route.params.id).then(res => {
    comments.value = res.data || []
  })
}

function goLogin() {
  router.push(`/login?redirect=/article/${route.params.id}`)
}

function handleComment() {
  const text = commentText.value.trim()
  if (!text) {
    ElMessage.warning('请输入留言内容')
    return
  }
  commentLoading.value = true
  addComment(route.params.id, text)
    .then(() => {
      ElMessage.success('留言成功')
      commentText.value = ''
      fetchComments()
    })
    .finally(() => {
      commentLoading.value = false
    })
}

function handleLikeArticle() {
  if (!isLoggedIn.value) { goLogin(); return }
  likeLoading.value = true
  likeArticle(route.params.id)
    .then(res => {
      article.value.liked = res.data.liked
      article.value.likeCount = res.data.count
    })
    .finally(() => { likeLoading.value = false })
}

function handleLikeComment(c) {
  if (!isLoggedIn.value) { goLogin(); return }
  likeComment(c.id)
    .then(res => {
      c.liked = res.data.liked
      c.likeCount = res.data.count
    })
}

function enhanceContent() {
  nextTick(() => {
    const container = document.querySelector('.article-content')
    if (!container) return

    // Code highlighting
    container.querySelectorAll('pre code').forEach(block => {
      hljs.highlightElement(block)
    })

    // Copy buttons
    container.querySelectorAll('pre').forEach(pre => {
      if (pre.querySelector('.copy-btn')) return
      const btn = document.createElement('button')
      btn.className = 'copy-btn'
      btn.textContent = '复制'
      btn.addEventListener('click', () => {
        const code = pre.querySelector('code')
        const text = code ? code.textContent : pre.textContent
        navigator.clipboard.writeText(text).then(() => {
          btn.textContent = '已复制!'
          setTimeout(() => { btn.textContent = '复制' }, 1500)
        })
      })
      pre.appendChild(btn)
    })
  })
}

onMounted(() => {
  const id = route.params.id
  getArticleDetail(id).then(res => {
    article.value = res.data
    enhanceContent()
  }).finally(() => {
    loading.value = false
  })
  fetchComments()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--bg-primary);
}

.detail-cover {
  width: 100%;
  max-height: 360px;
  overflow: hidden;
}

.detail-cover img {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  display: block;
}

.detail-container {
  max-width: 750px;
  margin: 0 auto;
  padding: 32px 20px 60px;
}

.back-link {
  display: inline-block;
  color: var(--text-muted);
  font-size: 15px;
  margin-bottom: 28px;
  transition: color 0.2s;
}

.back-link:hover {
  color: var(--link-color);
}

.article-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
  margin-bottom: 24px;
}

.article-meta {
  margin-bottom: 20px;
}

.meta-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.meta-author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-author-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.meta-time {
  font-size: 13px;
  color: var(--text-muted);
}

.article-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

/* Comment section */
.comment-section {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid var(--border-color);
}

.comment-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--text-muted);
}

.comment-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.comment-empty {
  text-align: center;
  padding: 32px 0;
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 24px;
}

.comment-form {
  padding: 20px;
  background: var(--bg-code);
  border-radius: 8px;
}

.login-hint {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.comment-form-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.comment-form-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-form-name {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.comment-actions {
  margin-top: 8px;
}

.like-btn {
  cursor: pointer;
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 3px;
  transition: color 0.2s;
}

.like-btn:hover {
  color: #f56c6c;
}

.article-like-bar {
  text-align: center;
  margin: 32px 0;
  padding: 20px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
}

.site-footer {
  text-align: center;
  padding: 32px 0;
  background: var(--bg-code);
  border-top: 1px solid var(--border-color);
}

.site-footer p {
  font-size: 13px;
  color: var(--text-muted);
}
</style>

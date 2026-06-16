<template>
  <div class="page-comments">
    <div class="page-header">
      <h1 class="page-title">评论管理</h1>
      <span class="page-count">共 {{ total }} 条</span>
    </div>

    <div class="table-card">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th class="th-article">关联文章</th>
              <th class="th-user">用户</th>
              <th class="th-content">评论内容</th>
              <th class="th-num">赞</th>
              <th class="th-time">时间</th>
              <th class="th-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in comments" :key="c.id">
              <td>
                <router-link v-if="c.articleId" :to="`/article/${c.articleId}`" target="_blank" class="article-link">
                  {{ c.articleTitle || `文章 #${c.articleId}` }}
                </router-link>
                <span v-else class="deleted-text">已删除</span>
              </td>
              <td>
                <div class="user-cell">
                  <img :src="c.avatar || defaultAvatar" class="user-avatar" />
                  <span>{{ c.nickname }}</span>
                </div>
              </td>
              <td class="td-content">
                <span v-if="c.replyToNickname" class="reply-tag">→ {{ c.replyToNickname }}</span>
                {{ c.content }}
              </td>
              <td class="td-num">{{ c.likeCount || 0 }}</td>
              <td class="td-time">{{ c.createTime }}</td>
              <td>
                <button class="action-btn delete" @click="handleDelete(c.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="comments.length === 0 && !loading" class="table-empty">
        <span>💬</span><span>暂无评论</span>
      </div>
    </div>

    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination :current-page="currentPage" @update:current-page="currentPage = $event; fetchData()" :page-size="pageSize" :total="total" layout="prev, pager, next" background />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminComments, deleteComment } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const comments = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=default&backgroundColor=e0e0e0'

function fetchData() {
  loading.value = true
  getAdminComments({ page: currentPage.value, size: pageSize.value })
    .then(res => { comments.value = res.data.records || []; total.value = res.data.total || 0 })
    .finally(() => { loading.value = false })
}

function handleDelete(id) {
  ElMessageBox.confirm('确定删除这条评论？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => { deleteComment(id).then(() => { ElMessage.success('已删除'); fetchData() }) })
    .catch(() => {})
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-comments { max-width: 1200px; }

.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; }
.page-title { font-family: var(--font-display); font-size: 24px; font-weight: 400; color: var(--gray-900); letter-spacing: -0.02em; }
.page-count { font-size: 13px; color: var(--gray-400); }

.table-card { background: var(--bg-card, #fff); border: 1px solid var(--gray-200); border-radius: var(--radius); overflow: hidden; transition: background-color 0.3s; }
.table-scroll { overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th { text-align: left; padding: 12px 16px; font-size: 12px; font-weight: 600; color: var(--gray-400); text-transform: uppercase; letter-spacing: 0.06em; background: var(--gray-50); border-bottom: 1px solid var(--gray-200); white-space: nowrap; }
.data-table td { padding: 12px 16px; border-bottom: 1px solid var(--gray-100); color: var(--gray-600); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--gray-50); }

.th-article { min-width: 160px; }
.th-user { width: 120px; }
.th-content { min-width: 200px; }
.th-num { width: 50px; text-align: center; }
.th-time { width: 140px; }
.th-actions { width: 70px; }

.article-link { color: var(--indigo); text-decoration: none; font-weight: 500; }
.article-link:hover { text-decoration: underline; }
.deleted-text { color: var(--gray-300); font-style: italic; }

.user-cell { display: flex; align-items: center; gap: 8px; }
.user-avatar { width: 26px; height: 26px; border-radius: 50%; object-fit: cover; }

.td-content { font-size: 13px; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.reply-tag { font-size: 11px; color: var(--gray-400); }
.reply-tag::after { content: ' '; }

.td-num { text-align: center; font-variant-numeric: tabular-nums; }
.td-time { font-size: 13px; color: var(--gray-400); white-space: nowrap; }

.action-btn { background: none; border: none; font-size: 13px; font-family: inherit; cursor: pointer; padding: 4px 10px; border-radius: 5px; transition: all 0.15s; }
.action-btn.delete { color: var(--gray-400); }
.action-btn.delete:hover { color: #f56c6c; background: rgba(245, 108, 108, 0.06); }

.table-empty { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 48px; color: var(--gray-400); font-size: 14px; }

.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }

.dark-mode .table-card { background: var(--bg-card, #1e2028); }
.dark-mode .data-table th { background: var(--gray-50, #1a1c24); }
.dark-mode .data-table tr:hover td { background: rgba(255, 255, 255, 0.02); }

@media (max-width: 640px) {
  .th-num, .td-num { display: none; }
  .th-time, .td-time { display: none; }
}
</style>

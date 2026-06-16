<template>
  <div class="page-articles">
    <div class="page-header">
      <h1 class="page-title">文章管理</h1>
      <router-link to="/admin/articles/new" class="new-btn">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        写文章
      </router-link>
    </div>

    <!-- Article table -->
    <div class="table-card">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th class="th-title">标题</th>
              <th class="th-status">状态</th>
              <th class="th-num">阅读</th>
              <th class="th-num">点赞</th>
              <th class="th-time">创建时间</th>
              <th class="th-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="article in articles" :key="article.id" v-loading="loading">
              <td class="td-title">
                <span class="title-text" @click="handleEdit(article.id)">{{ article.title }}</span>
              </td>
              <td>
                <span :class="['status-badge', article.status === 1 ? 'published' : 'draft']">
                  {{ article.status === 1 ? '已发布' : '草稿' }}
                </span>
              </td>
              <td class="td-num">{{ article.viewCount }}</td>
              <td class="td-num">{{ article.likeCount || 0 }}</td>
              <td class="td-time">{{ article.createTime }}</td>
              <td class="td-actions">
                <button class="action-btn edit" @click="handleEdit(article.id)">编辑</button>
                <button class="action-btn delete" @click="handleDelete(article.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="articles.length === 0 && !loading" class="table-empty">
        <span class="empty-icon">📄</span>
        <span>暂无文章</span>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchArticles"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminArticles, deleteArticle } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

function fetchArticles() {
  loading.value = true
  getAdminArticles({ page: currentPage.value, size: pageSize.value })
    .then(res => {
      articles.value = res.data.records || []
      total.value = res.data.total || 0
    })
    .finally(() => { loading.value = false })
}

function handleEdit(id) {
  router.push(`/admin/articles/${id}/edit`)
}

function handleDelete(id) {
  ElMessageBox.confirm('确定删除这篇文章吗？', '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteArticle(id).then(() => {
      ElMessage.success('已删除')
      fetchArticles()
    })
  }).catch(() => {})
}

onMounted(() => { fetchArticles() })
</script>

<style scoped>
.page-articles { max-width: 1200px; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 400;
  color: var(--gray-900);
  letter-spacing: -0.02em;
}

.new-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: var(--indigo);
  color: #fff;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;
}

.new-btn:hover { background: var(--indigo-soft); transform: translateY(-1px); }

/* Table card */
.table-card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius);
  overflow: hidden;
  transition: background-color 0.3s;
}

.table-scroll { overflow-x: auto; }

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-400);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  background: var(--gray-50);
  border-bottom: 1px solid var(--gray-200);
  white-space: nowrap;
}

.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--gray-100);
  color: var(--gray-600);
}

.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--gray-50); }

.th-title { min-width: 200px; }
.th-status { width: 80px; }
.th-num { width: 60px; text-align: center; }
.th-time { width: 160px; }
.th-actions { width: 120px; }

.td-title .title-text {
  color: var(--gray-800);
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.td-title .title-text:hover { color: var(--indigo); }

.td-num { text-align: center; font-variant-numeric: tabular-nums; }
.td-time { font-size: 13px; color: var(--gray-400); white-space: nowrap; }

.status-badge {
  font-size: 12px;
  padding: 3px 14px;
  border-radius: 100px;
  font-weight: 500;
  white-space: nowrap;
}

.status-badge.published { color: #3d9a50; background: #e8f5e9; }
.status-badge.draft { color: var(--gray-500); background: var(--gray-100); }

.td-actions { white-space: nowrap; }

.action-btn {
  background: none;
  border: none;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 5px;
  transition: all 0.15s;
}

.action-btn.edit { color: var(--indigo); }
.action-btn.edit:hover { background: var(--indigo-bg); }
.action-btn.delete { color: var(--gray-400); }
.action-btn.delete:hover { color: #f56c6c; background: rgba(245, 108, 108, 0.06); }

.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 48px;
  color: var(--gray-400);
  font-size: 14px;
}

.empty-icon { font-size: 28px; }

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

/* Dark overrides */
.dark-mode .table-card { background: var(--bg-card, #1e2028); }
.dark-mode .data-table th { background: var(--gray-50, #1a1c24); }
.dark-mode .data-table tr:hover td { background: rgba(255, 255, 255, 0.02); }
.dark-mode .status-badge.published { background: rgba(61, 154, 80, 0.12); }
.dark-mode .status-badge.draft { background: var(--gray-100); }

@media (max-width: 640px) {
  .page-header { flex-direction: column; align-items: flex-start; gap: 12px; }
  .th-num, .td-num { display: none; }
  .th-time, .td-time { display: none; }
}
</style>

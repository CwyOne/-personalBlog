<template>
  <div class="comment-manage">
    <el-card>
      <template #header><span>评论管理</span></template>
      <el-table :data="comments" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="关联文章" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <router-link
              v-if="row.articleId"
              :to="`/article/${row.articleId}`"
              target="_blank"
              class="article-link"
            >
              {{ row.articleTitle || `文章 #${row.articleId}` }}
            </router-link>
            <span v-else class="deleted-article">文章已删除</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <img :src="row.avatar || defaultAvatar" class="user-avatar" />
              <span>{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="180" show-overflow-tooltip />
        <el-table-column prop="likeCount" label="点赞" width="80" align="center" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定删除这条评论？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无评论" />
        </template>
      </el-table>
    </el-card>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminComments, deleteComment } from '@/utils/api'

const loading = ref(false)
const comments = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=default&backgroundColor=e0e0e0'

function fetchData() {
  loading.value = true
  getAdminComments({ page: currentPage.value, size: pageSize.value })
    .then(res => {
      comments.value = res.data.records || []
      total.value = res.data.total || 0
    })
    .finally(() => { loading.value = false })
}

function handleDelete(id) {
  deleteComment(id).then(() => fetchData())
}

onMounted(() => fetchData())
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.article-link {
  color: #409eff;
  text-decoration: none;
}

.article-link:hover {
  text-decoration: underline;
}

.deleted-article {
  color: #ccc;
  font-style: italic;
}
</style>

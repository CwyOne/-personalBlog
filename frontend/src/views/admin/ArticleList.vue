<template>
  <div class="article-list">
    <el-card>
      <el-table :data="articles" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row.id)">编辑</el-button>
            <el-popconfirm
              title="确定删除这篇文章吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无文章" />
        </template>
      </el-table>
    </el-card>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchArticles"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminArticles, deleteArticle } from '@/utils/api'

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
    .finally(() => {
      loading.value = false
    })
}

function handleEdit(id) {
  router.push(`/admin/articles/${id}/edit`)
}

function handleDelete(id) {
  deleteArticle(id).then(() => {
    fetchArticles()
  })
}

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.article-list {
  padding: 0;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>

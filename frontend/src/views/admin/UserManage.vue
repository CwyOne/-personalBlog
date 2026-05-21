<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" size="small" @click="dialogVisible = true">添加用户</el-button>
        </div>
      </template>
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <img :src="row.avatar || defaultAvatar" class="user-avatar" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="!(row.username === 'admin' && row.role === 'admin')"
              title="确定删除该用户？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" />
        </template>
      </el-table>
    </el-card>

    <!-- Add user dialog -->
    <el-dialog v-model="dialogVisible" title="添加用户" width="420px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20位" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio value="user">普通用户</el-radio>
            <el-radio value="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminUsers, createUser, deleteUser } from '@/utils/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const users = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=default&backgroundColor=e0e0e0'

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  role: 'user'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

function fetchData() {
  loading.value = true
  getAdminUsers()
    .then(res => { users.value = res.data || [] })
    .finally(() => { loading.value = false })
}

function handleDelete(id) {
  deleteUser(id).then(() => {
    ElMessage.success('删除成功')
    fetchData()
  })
}

function handleAdd() {
  formRef.value?.validate(valid => {
    if (!valid) return
    submitting.value = true
    createUser({
      username: form.username,
      password: form.password,
      nickname: form.nickname || form.username,
      role: form.role
    })
      .then(() => {
        ElMessage.success('添加成功')
        dialogVisible.value = false
        form.username = ''
        form.password = ''
        form.nickname = ''
        form.role = 'user'
        fetchData()
      })
      .finally(() => { submitting.value = false })
  })
}

onMounted(() => fetchData())
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}
</style>

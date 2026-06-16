<template>
  <div class="page-users">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <button class="new-btn" @click="dialogVisible = true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        添加用户
      </button>
    </div>

    <div class="table-card">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th class="th-avatar">头像</th>
              <th>用户名</th>
              <th>昵称</th>
              <th class="th-role">角色</th>
              <th class="th-time">注册时间</th>
              <th class="th-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td><img :src="u.avatar || defaultAvatar" class="user-avatar" /></td>
              <td class="td-name">{{ u.username }}</td>
              <td>{{ u.nickname }}</td>
              <td>
                <span :class="['role-badge', u.role === 'admin' ? 'admin' : 'user']">
                  {{ u.role === 'admin' ? '管理员' : '用户' }}
                </span>
              </td>
              <td class="td-time">{{ u.createTime }}</td>
              <td>
                <button
                  v-if="!(u.username === 'admin' && u.role === 'admin')"
                  class="action-btn delete"
                  @click="handleDelete(u.id)"
                >删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="users.length === 0 && !loading" class="table-empty">
        <span>👤</span><span>暂无用户</span>
      </div>
    </div>

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
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const users = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=default&backgroundColor=e0e0e0'

const form = reactive({ username: '', password: '', nickname: '', role: 'user' })

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
  getAdminUsers().then(res => { users.value = res.data || [] }).finally(() => { loading.value = false })
}

function handleDelete(id) {
  ElMessageBox.confirm('确定删除该用户？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => { deleteUser(id).then(() => { ElMessage.success('已删除'); fetchData() }) })
    .catch(() => {})
}

function handleAdd() {
  formRef.value?.validate(valid => {
    if (!valid) return
    submitting.value = true
    createUser({ username: form.username, password: form.password, nickname: form.nickname || form.username, role: form.role })
      .then(() => {
        ElMessage.success('添加成功')
        dialogVisible.value = false
        Object.assign(form, { username: '', password: '', nickname: '', role: 'user' })
        fetchData()
      })
      .finally(() => { submitting.value = false })
  })
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-users { max-width: 1000px; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-title { font-family: var(--font-display); font-size: 24px; font-weight: 400; color: var(--gray-900); letter-spacing: -0.02em; }

.new-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 8px 18px;
  background: var(--indigo); color: #fff; border-radius: 8px; font-size: 13px;
  font-family: inherit; font-weight: 500; border: none; cursor: pointer; transition: all 0.2s;
}
.new-btn:hover { background: var(--indigo-soft); transform: translateY(-1px); }

.table-card { background: var(--bg-card, #fff); border: 1px solid var(--gray-200); border-radius: var(--radius); overflow: hidden; transition: background-color 0.3s; }
.table-scroll { overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th { text-align: left; padding: 12px 16px; font-size: 12px; font-weight: 600; color: var(--gray-400); text-transform: uppercase; letter-spacing: 0.06em; background: var(--gray-50); border-bottom: 1px solid var(--gray-200); white-space: nowrap; }
.data-table td { padding: 12px 16px; border-bottom: 1px solid var(--gray-100); color: var(--gray-600); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--gray-50); }

.th-avatar { width: 50px; }
.th-role { width: 80px; }
.th-time { width: 160px; }
.th-actions { width: 70px; }

.user-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.td-name { font-weight: 500; color: var(--gray-800); }
.td-time { font-size: 13px; color: var(--gray-400); white-space: nowrap; }

.role-badge { font-size: 12px; padding: 3px 14px; border-radius: 100px; font-weight: 500; white-space: nowrap; }
.role-badge.admin { color: #c9596d; background: rgba(201, 89, 109, 0.08); }
.role-badge.user { color: var(--gray-500); background: var(--gray-100); }

.action-btn { background: none; border: none; font-size: 13px; font-family: inherit; cursor: pointer; padding: 4px 10px; border-radius: 5px; transition: all 0.15s; }
.action-btn.delete { color: var(--gray-400); }
.action-btn.delete:hover { color: #f56c6c; background: rgba(245, 108, 108, 0.06); }

.table-empty { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 48px; color: var(--gray-400); font-size: 14px; }

.dark-mode .table-card { background: var(--bg-card, #1e2028); }
.dark-mode .data-table th { background: var(--gray-50, #1a1c24); }
.dark-mode .data-table tr:hover td { background: rgba(255, 255, 255, 0.02); }
</style>

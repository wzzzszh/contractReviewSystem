<template>
  <div class="users-grid">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>创建用户</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model.trim="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="creating" class="full-button" @click="handleCreate">创建</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-button :icon="Refresh" @click="loadUsers">刷新</el-button>
        </div>
      </template>
      <el-table :data="users" v-loading="loading" empty-text="暂无用户">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="nickname" label="昵称" min-width="150">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createUser, listUsers } from '../api/users'
import { showError } from '../api/request'
import { formatDate } from '../utils/format'

const formRef = ref()
const loading = ref(false)
const creating = ref(false)
const users = ref([])

const form = reactive({
  username: '',
  nickname: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(loadUsers)

async function loadUsers() {
  loading.value = true
  try {
    users.value = await listUsers()
  } catch (error) {
    showError(error, '用户加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  await formRef.value.validate()
  creating.value = true
  try {
    await createUser(form)
    ElMessage.success('创建成功')
    form.username = ''
    form.nickname = ''
    form.password = ''
    await loadUsers()
  } catch (error) {
    showError(error, '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

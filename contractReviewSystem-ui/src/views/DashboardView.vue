<template>
  <div class="page-stack">
    <div class="metric-grid">
      <el-card shadow="never">
        <span class="metric-label">登录用户</span>
        <strong>{{ auth.displayName }}</strong>
      </el-card>
      <el-card shadow="never">
        <span class="metric-label">用户 ID</span>
        <strong>{{ auth.user?.userId || '-' }}</strong>
      </el-card>
      <el-card shadow="never">
        <span class="metric-label">服务状态</span>
        <strong>{{ health || '-' }}</strong>
      </el-card>
      <el-card shadow="never">
        <span class="metric-label">我的文件</span>
        <strong>{{ files.length }}</strong>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>最近文件</span>
          <el-button type="primary" :icon="EditPen" @click="$router.push('/review')">开始审查</el-button>
        </div>
      </template>
      <el-table :data="recentFiles" v-loading="loading" empty-text="暂无文件记录">
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="fileCategory" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.fileCategory === 'modified' ? 'success' : 'info'">
              {{ row.fileCategory === 'modified' ? '修改稿' : '原始稿' }}
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
import { computed, onMounted, ref } from 'vue'
import { EditPen } from '@element-plus/icons-vue'
import { getHealth } from '../api/monitor'
import { listFilesByUser } from '../api/files'
import { showError } from '../api/request'
import { useAuthStore } from '../stores/auth'
import { formatDate } from '../utils/format'

const auth = useAuthStore()
const loading = ref(false)
const health = ref('')
const files = ref([])

const recentFiles = computed(() => files.value.slice(0, 6))

onMounted(async () => {
  loading.value = true
  try {
    const [healthText, userFiles] = await Promise.all([
      getHealth(),
      auth.user?.userId ? listFilesByUser(auth.user.userId) : []
    ])
    health.value = healthText
    files.value = userFiles || []
  } catch (error) {
    showError(error, '工作台数据加载失败')
  } finally {
    loading.value = false
  }
})
</script>

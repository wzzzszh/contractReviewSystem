<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>我的文件</span>
        <div class="header-actions">
          <el-input v-model.trim="keyword" clearable placeholder="搜索文件名" :prefix-icon="Search" />
          <el-button :icon="Refresh" @click="loadFiles">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="filteredFiles" v-loading="loading" empty-text="暂无文件记录">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
      <el-table-column prop="fileCategory" label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="row.fileCategory === 'modified' ? 'success' : 'info'">
            {{ row.fileCategory === 'modified' ? '修改稿' : '原始稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceFileId" label="来源 ID" width="100">
        <template #default="{ row }">{{ row.sourceFileId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="filePath" label="路径" min-width="280" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" :icon="Download" @click="download(row.filePath)">下载</el-button>
          <el-popconfirm title="只删除磁盘文件，记录会保留" @confirm="removeFile(row)">
            <template #reference>
              <el-button text type="danger" :icon="Delete">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Delete, Download, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { deletePhysicalFile, downloadFile, listFilesByUser } from '../api/files'
import { showError } from '../api/request'
import { useAuthStore } from '../stores/auth'
import { fileNameFromPath, formatDate } from '../utils/format'

const auth = useAuthStore()
const loading = ref(false)
const keyword = ref('')
const files = ref([])

const filteredFiles = computed(() => {
  const text = keyword.value.toLowerCase()
  if (!text) return files.value
  return files.value.filter((item) => item.fileName?.toLowerCase().includes(text))
})

onMounted(loadFiles)

async function loadFiles() {
  if (!auth.user?.userId) return
  loading.value = true
  try {
    files.value = await listFilesByUser(auth.user.userId)
  } catch (error) {
    showError(error, '文件加载失败')
  } finally {
    loading.value = false
  }
}

async function removeFile(row) {
  try {
    await deletePhysicalFile(row.filePath)
    ElMessage.success('文件已删除')
    await loadFiles()
  } catch (error) {
    showError(error, '删除失败')
  }
}

async function download(path) {
  try {
    const blob = await downloadFile(path)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileNameFromPath(path)
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    showError(error, '下载失败')
  }
}
</script>

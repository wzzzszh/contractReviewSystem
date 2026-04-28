<template>
  <div class="page-stack">
    <div class="metric-grid">
      <el-card shadow="never">
        <span class="metric-label">应用</span>
        <strong>{{ info.appName || '-' }}</strong>
      </el-card>
      <el-card shadow="never">
        <span class="metric-label">版本</span>
        <strong>{{ info.appVersion || '-' }}</strong>
      </el-card>
      <el-card shadow="never">
        <span class="metric-label">Java</span>
        <strong>{{ info.javaVersion || '-' }}</strong>
      </el-card>
      <el-card shadow="never">
        <span class="metric-label">运行时间</span>
        <strong>{{ uptimeText }}</strong>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>运行信息</span>
          <el-button :icon="Refresh" :loading="loading" @click="loadInfo">刷新</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="JVM">{{ info.jvmName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="JVM 版本">{{ info.jvmVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ osText }}</el-descriptions-item>
        <el-descriptions-item label="工作目录">{{ info.userDir || '-' }}</el-descriptions-item>
        <el-descriptions-item label="堆内存">{{ heapText }}</el-descriptions-item>
        <el-descriptions-item label="非堆内存">{{ nonHeapText }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getSystemInfo } from '../api/monitor'
import { showError } from '../api/request'
import { formatBytes } from '../utils/format'

const loading = ref(false)
const info = ref({})

const uptimeText = computed(() => {
  const seconds = Math.floor((info.value.uptime || 0) / 1000)
  if (!seconds) return '-'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${hours}小时 ${minutes}分钟`
})

const osText = computed(() => [info.value.osName, info.value.osVersion, info.value.osArch].filter(Boolean).join(' '))
const heapText = computed(() => {
  const memory = info.value.memory || {}
  return `${formatBytes(memory.heapUsed)} / ${formatBytes(memory.heapMax)}`
})
const nonHeapText = computed(() => {
  const memory = info.value.memory || {}
  return `${formatBytes(memory.nonHeapUsed)} / ${formatBytes(memory.nonHeapMax)}`
})

onMounted(loadInfo)

async function loadInfo() {
  loading.value = true
  try {
    info.value = await getSystemInfo()
  } catch (error) {
    showError(error, '系统信息加载失败')
  } finally {
    loading.value = false
  }
}
</script>

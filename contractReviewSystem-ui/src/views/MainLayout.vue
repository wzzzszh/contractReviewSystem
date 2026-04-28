<template>
  <el-container class="app-shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">
        <el-icon><DocumentChecked /></el-icon>
        <span>合同审查系统</span>
      </div>

      <el-menu :default-active="$route.path" router class="side-menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/review">
          <el-icon><EditPen /></el-icon>
          <span>合同审查</span>
        </el-menu-item>
        <el-menu-item index="/files">
          <el-icon><FolderOpened /></el-icon>
          <span>文件记录</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><Monitor /></el-icon>
          <span>系统状态</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <strong>{{ title }}</strong>
          <span>{{ subtitle }}</span>
        </div>
        <el-dropdown @command="handleCommand">
          <button class="user-button">
            <el-icon><UserFilled /></el-icon>
            {{ auth.displayName }}
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main-view">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const titles = {
  '/dashboard': ['工作台', '今日审查入口与最近文件'],
  '/review': ['合同审查', '上传合同并生成风险审查结果'],
  '/files': ['文件记录', '查看上传文件与修改后文件'],
  '/users': ['用户管理', '创建登录用户与查看账号'],
  '/monitor': ['系统状态', '后端服务与运行信息']
}

const current = computed(() => titles[route.path] || titles['/dashboard'])
const title = computed(() => current.value[0])
const subtitle = computed(() => current.value[1])

function handleCommand(command) {
  if (command === 'logout') {
    auth.logout()
    router.replace('/login')
  }
}
</script>

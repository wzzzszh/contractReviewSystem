<template>
  <div class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <el-icon><DocumentChecked /></el-icon>
        <h1>合同审查系统</h1>
        <p>上传合同，按甲方或乙方视角生成风险审查和修改结果。</p>
      </div>

      <el-card class="login-card" shadow="never">
        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="handleLogin">
              <el-form-item label="用户名" prop="username">
                <el-input v-model.trim="loginForm.username" placeholder="请输入用户名" size="large" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" placeholder="请输入密码" show-password size="large" />
              </el-form-item>
              <el-button type="primary" size="large" :loading="loginLoading" class="full-button" @click="handleLogin">
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="新建用户" name="register">
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
              <el-form-item label="用户名" prop="username">
                <el-input v-model.trim="registerForm.username" placeholder="用于登录" size="large" />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model.trim="registerForm.nickname" placeholder="页面展示名称" size="large" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" placeholder="请输入密码" show-password size="large" />
              </el-form-item>
              <el-button size="large" :loading="registerLoading" class="full-button" @click="handleRegister">
                创建用户
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'
import { showError } from '../api/request'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeTab = ref('login')
const loginFormRef = ref()
const registerFormRef = ref()
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loginLoading.value = true
  try {
    await auth.login(loginForm)
    router.replace(route.query.redirect || '/dashboard')
  } catch (error) {
    showError(error, '登录失败')
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  registerLoading.value = true
  try {
    await register(registerForm)
    ElMessage.success('用户创建成功')
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
    activeTab.value = 'login'
  } catch (error) {
    showError(error, '用户创建失败')
  } finally {
    registerLoading.value = false
  }
}
</script>

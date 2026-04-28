<template>
  <!-- 登录弹窗 -->
  <el-dialog v-model="showLogin" title="登录" width="400px" :close-on-click-modal="true">
    <el-form :model="loginForm" @submit.prevent="handleLogin">
      <el-form-item label="用户名">
        <el-input v-model="loginForm.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loginLoading" style="width: 100%" native-type="submit">
          登录
        </el-button>
      </el-form-item>
      <div class="dialog-footer-text">
        没有账号？<el-link type="primary" @click="switchToRegister">注册</el-link>
      </div>
    </el-form>
  </el-dialog>

  <!-- 注册弹窗 -->
  <el-dialog v-model="showRegister" title="注册" width="400px" :close-on-click-modal="true">
    <el-form :model="registerForm" @submit.prevent="handleRegister">
      <el-form-item label="用户名">
        <el-input v-model="registerForm.username" placeholder="3-50字符" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="registerForm.password" type="password" placeholder="8位以上含字母数字特殊字符" show-password />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="registerForm.email" placeholder="可选，用于找回密码" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="registerForm.role" style="width: 100%">
          <el-option label="学生" value="STUDENT" />
          <el-option label="教师" value="TEACHER" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="registerLoading" style="width: 100%" native-type="submit">
          注册
        </el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { userApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'

const showLogin = ref(false)
const showRegister = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', email: '', role: 'STUDENT' })

const emit = defineEmits(['login-success'])

function switchToRegister() {
  showLogin.value = false
  showRegister.value = true
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loginLoading.value = true
  try {
    const res = await userApi.login(loginForm)
    if (res.code === 0 && res.data) {
      emit('login-success', res.data)
      showLogin.value = false
      ElMessage.success('登录成功')
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e) || '登录失败')
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password || !registerForm.nickname) {
    ElMessage.warning('请填写所有必填项')
    return
  }
  registerLoading.value = true
  try {
    const res = await userApi.register(registerForm)
    if (res.code === 0) {
      ElMessage.success('注册成功，请登录')
      showRegister.value = false
      showLogin.value = true
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e) || '注册失败')
  } finally {
    registerLoading.value = false
  }
}

defineExpose({ showLogin, showRegister })
</script>

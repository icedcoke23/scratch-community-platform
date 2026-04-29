<template>
  <!-- 登录弹窗 -->
  <el-dialog v-model="showLogin" title="" width="440px" :close-on-click-modal="true" class="auth-dialog">
    <div class="dialog-header">
      <div class="mascot">🐱</div>
      <h2 class="dialog-title">欢迎回来！</h2>
      <p class="dialog-subtitle">登录你的 Scratch 社区账号</p>
    </div>
    <el-form :model="loginForm" @submit.prevent="handleLogin" label-position="top" class="auth-form">
      <el-form-item label="👤 用户名">
        <el-input
          v-model="loginForm.username"
          placeholder="输入你的用户名"
          size="large"
          clearable
        />
      </el-form-item>
      <el-form-item label="🔒 密码">
        <el-input
          v-model="loginForm.password"
          type="password"
          placeholder="输入你的密码"
          show-password
          size="large"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :loading="loginLoading"
          style="width: 100%"
          native-type="submit"
          size="large"
          class="submit-btn"
        >
          🚀 登录
        </el-button>
      </el-form-item>
      <div class="dialog-footer-text">
        还没有账号？
        <el-link type="primary" @click="switchToRegister" class="switch-link">注册一个</el-link>
      </div>
    </el-form>
  </el-dialog>

  <!-- 注册弹窗 -->
  <el-dialog v-model="showRegister" title="" width="480px" :close-on-click-modal="true" class="auth-dialog">
    <div class="dialog-header">
      <div class="mascot">🎉</div>
      <h2 class="dialog-title">加入我们！</h2>
      <p class="dialog-subtitle">创建你的 Scratch 社区账号</p>
    </div>
    <el-form :model="registerForm" @submit.prevent="handleRegister" label-position="top" class="auth-form">
      <el-form-item label="👤 用户名">
        <el-input
          v-model="registerForm.username"
          placeholder="3-50个字符"
          size="large"
          clearable
        />
      </el-form-item>
      <el-form-item label="🔒 密码">
        <el-input
          v-model="registerForm.password"
          type="password"
          placeholder="8位以上，包含字母和数字"
          show-password
          size="large"
        />
      </el-form-item>
      <el-form-item label="✨ 昵称">
        <el-input
          v-model="registerForm.nickname"
          placeholder="你想让大家叫你什么？"
          size="large"
          clearable
        />
      </el-form-item>
      <el-form-item label="📧 邮箱（可选）">
        <el-input
          v-model="registerForm.email"
          placeholder="用于找回密码"
          size="large"
          clearable
        />
      </el-form-item>
      <el-form-item label="🎓 我是">
        <div class="role-cards">
          <div
            class="role-card"
            :class="{ selected: registerForm.role === 'STUDENT' }"
            @click="registerForm.role = 'STUDENT'"
          >
            <span class="role-icon">🎓</span>
            <span class="role-name">学生</span>
            <span class="role-desc">学习编程，创作作品</span>
          </div>
          <div
            class="role-card"
            :class="{ selected: registerForm.role === 'TEACHER' }"
            @click="registerForm.role = 'TEACHER'"
          >
            <span class="role-icon">👨‍🏫</span>
            <span class="role-name">老师</span>
            <span class="role-desc">管理班级，布置作业</span>
          </div>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :loading="registerLoading"
          style="width: 100%"
          native-type="submit"
          size="large"
          class="submit-btn"
        >
          🎊 注册
        </el-button>
      </el-form-item>
      <div class="dialog-footer-text">
        已有账号？
        <el-link type="primary" @click="switchToLogin" class="switch-link">去登录</el-link>
      </div>
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

function switchToLogin() {
  showRegister.value = false
  showLogin.value = true
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
      ElMessage.success('🎉 登录成功！')
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
      ElMessage.success('🎉 注册成功！请登录')
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

<style scoped>
.auth-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.auth-dialog :deep(.el-dialog__header) {
  display: none;
}

.auth-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.dialog-header {
  text-align: center;
  padding: 32px 24px 16px;
  background: linear-gradient(135deg, #EFF6FF, #F3E8FF);
}

.mascot {
  font-size: 56px;
  margin-bottom: 12px;
  animation: mascot-bounce 1s ease-in-out infinite;
}

@keyframes mascot-bounce {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-8px) scale(1.05); }
}

.dialog-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 4px;
}

.dialog-subtitle {
  font-size: 15px;
  color: var(--text2);
  margin: 0;
}

.auth-form {
  padding: 24px 28px 28px;
}

.auth-form :deep(.el-form-item__label) {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  padding-bottom: 4px;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  min-height: 44px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.auth-form :deep(.el-input__inner) {
  font-size: 16px;
}

.submit-btn {
  height: 48px;
  font-size: 17px;
  font-weight: 700;
  border-radius: 14px;
  margin-top: 8px;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  border: none;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transition: all 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
}

.dialog-footer-text {
  text-align: center;
  font-size: 15px;
  color: var(--text2);
  margin-top: 8px;
}

.switch-link {
  font-weight: 600;
  font-size: 15px;
}

.role-cards {
  display: flex;
  gap: 12px;
  width: 100%;
}

.role-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 12px;
  border: 2px solid var(--border);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--card);
}

.role-card:hover {
  border-color: var(--primary-light);
  background: var(--primary-bg);
  transform: translateY(-2px);
}

.role-card.selected {
  border-color: var(--primary);
  background: var(--primary-bg);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.2);
}

.role-icon {
  font-size: 36px;
}

.role-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.role-desc {
  font-size: 12px;
  color: var(--text2);
  text-align: center;
}

@media (max-width: 480px) {
  .auth-form { padding: 16px 20px 20px; }
  .mascot { font-size: 44px; }
  .dialog-title { font-size: 20px; }
  .role-icon { font-size: 28px; }
}
</style>

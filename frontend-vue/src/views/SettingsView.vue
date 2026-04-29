<template>
  <div>
    <h1 class="page-title">⚙️ 个人设置</h1>

    <LoadingSkeleton v-if="loading" variant="detail" />

    <template v-else>
      <!-- 基本信息 -->
      <div class="page-card">
        <div class="section-title">👤 基本信息</div>
        <el-form label-width="80px" label-position="left">
          <el-form-item label="用户名">
            <el-input :model-value="userStore.user?.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="请输入昵称" :maxlength="30" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="请输入邮箱" type="email" />
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input
              v-model="form.bio"
              type="textarea"
              :rows="3"
              placeholder="介绍一下自己吧..."
              :maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveProfile" :loading="saving">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 外观设置 -->
      <div class="page-card">
        <div class="section-title">🎨 外观设置</div>
        <el-form label-width="80px" label-position="left">
          <el-form-item label="主题">
            <el-radio-group v-model="currentTheme" @change="onThemeChange">
              <el-radio-button value="light">☀️ 浅色</el-radio-button>
              <el-radio-button value="dark">🌙 深色</el-radio-button>
              <el-radio-button value="auto">🔄 跟随系统</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="语言">
            <el-select v-model="currentLang" @change="onLangChange" style="width: 200px">
              <el-option label="简体中文" value="zh-CN" />
              <el-option label="English" value="en" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- 通知设置 -->
      <div class="page-card">
        <div class="section-title">🔔 通知设置</div>
        <el-form label-width="120px" label-position="left">
          <el-form-item label="点赞通知">
            <el-switch v-model="notifications.like" />
          </el-form-item>
          <el-form-item label="评论通知">
            <el-switch v-model="notifications.comment" />
          </el-form-item>
          <el-form-item label="作业通知">
            <el-switch v-model="notifications.homework" />
          </el-form-item>
          <el-form-item label="竞赛通知">
            <el-switch v-model="notifications.competition" />
          </el-form-item>
          <el-form-item label="系统通知">
            <el-switch v-model="notifications.system" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 账号安全 -->
      <div class="page-card">
        <div class="section-title">🔒 账号安全</div>
        <div class="security-items">
          <div class="security-item">
            <div class="security-info">
              <div class="security-label">修改密码</div>
              <div class="security-desc">定期修改密码有助于保护账号安全</div>
            </div>
            <el-button @click="showChangePassword = true">修改</el-button>
          </div>
          <div class="security-item">
            <div class="security-info">
              <div class="security-label">退出登录</div>
              <div class="security-desc">退出当前设备的登录状态</div>
            </div>
            <el-button type="danger" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </div>

      <!-- 数据导出 -->
      <div class="page-card">
        <div class="section-title">📦 数据管理</div>
        <div class="security-items">
          <div class="security-item">
            <div class="security-info">
              <div class="security-label">导出我的数据</div>
              <div class="security-desc">下载你的项目、积分等个人数据</div>
            </div>
            <el-button @click="exportData">导出</el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showChangePassword" title="修改密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少6位含字母数字" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChangePassword = false">取消</el-button>
        <el-button type="primary" @click="changePassword" :loading="changingPassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Settings' })

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTheme, type ThemeMode } from '@/composables/useTheme'
import { useI18n, type Locale } from '@/composables/useI18n'
import { userApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const router = useRouter()
const userStore = useUserStore()
const { themeMode, setTheme } = useTheme()
const { locale, setLocale } = useI18n()

const loading = ref(true)
const saving = ref(false)
const showChangePassword = ref(false)
const changingPassword = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  bio: ''
})

const notifications = reactive({
  like: true,
  comment: true,
  homework: true,
  competition: true,
  system: true
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const currentTheme = ref<ThemeMode>('light')
const currentLang = ref<Locale>('zh-CN')

async function saveProfile() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    const res = await userApi.updateProfile({
      nickname: form.nickname.trim(),
      email: form.email.trim() || undefined,
      bio: form.bio.trim() || undefined
    })
    if (res.code === 0) {
      ElMessage.success('✅ 保存成功')
      // 更新本地 store 中的用户信息
      if (userStore.user) {
        userStore.user.nickname = form.nickname.trim()
        userStore.user.bio = form.bio.trim()
      }
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e) || '保存失败')
  } finally {
    saving.value = false
  }
}

function onThemeChange(mode: ThemeMode) {
  setTheme(mode)
}

function onLangChange(lang: Locale) {
  setLocale(lang)
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写所有字段')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  changingPassword.value = true
  try {
    const res = await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.code === 0) {
      ElMessage.success('✅ 密码修改成功')
      showChangePassword.value = false
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e) || '修改失败')
  } finally {
    changingPassword.value = false
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/feed')
}

function exportData() {
  ElMessage.info('数据导出功能开发中')
}

onMounted(() => {
  if (userStore.user) {
    form.nickname = userStore.user.nickname || ''
    form.email = '' // email 不在 User 类型中
    form.bio = userStore.user.bio || ''
  }
  currentTheme.value = themeMode.value
  currentLang.value = locale.value
  loading.value = false
})
</script>

<style scoped>
.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.security-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
}

.security-item:last-child {
  border-bottom: none;
}

.security-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.security-desc {
  font-size: 14px;
  color: var(--text2);
  margin-top: 4px;
}

@media (max-width: 768px) {
  :deep(.el-form-item__label) {
    font-size: 14px;
  }
  .security-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}

@media (max-width: 480px) {
  :deep(.el-radio-button__inner) {
    padding: 8px 14px;
    font-size: 13px;
  }
}
</style>

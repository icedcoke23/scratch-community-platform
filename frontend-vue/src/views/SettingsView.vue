<template>
  <div class="settings-page">
    <h1 class="page-title">
      <span class="title-emoji">⚙️</span>
      个人设置
    </h1>

    <LoadingSkeleton v-if="loading" variant="detail" />

    <template v-else>
      <!-- 头像卡片 -->
      <div class="page-card avatar-card">
        <div class="avatar-section">
          <div class="avatar-display">
            <div class="avatar-circle" :style="avatarStyle">
              <span class="avatar-letter">{{ avatarLetter }}</span>
            </div>
            <div class="avatar-level">Lv.{{ userStore.user?.level || 1 }}</div>
          </div>
          <div class="avatar-info">
            <h3>{{ userStore.user?.nickname || userStore.user?.username }}</h3>
            <p class="avatar-role">
              <el-tag :type="roleType" size="small" effect="dark">{{ roleLabel }}</el-tag>
              <span class="points-display">⭐ {{ userStore.user?.points || 0 }} 积分</span>
            </p>
            <p class="avatar-joined">加入于 {{ formatDate(userStore.user?.createdAt || '') }}</p>
          </div>
        </div>
      </div>

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
            <el-button type="primary" @click="saveProfile" :loading="saving" round>
              💾 保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 修改密码 -->
      <div class="page-card">
        <div class="section-title">🔒 修改密码</div>
        <el-form label-width="80px" label-position="left">
          <el-form-item label="旧密码">
            <el-input v-model="pwForm.oldPassword" type="password" show-password placeholder="输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwForm.newPassword" type="password" show-password placeholder="输入新密码（至少6位）" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="pwForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" @click="changePassword" :loading="changingPw" round>
              🔑 修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 外观设置 -->
      <div class="page-card">
        <div class="section-title">🎨 外观设置</div>
        <div class="setting-row">
          <div class="setting-info">
            <div class="setting-label">深色模式</div>
            <div class="setting-desc">切换深色/浅色主题</div>
          </div>
          <el-switch v-model="isDark" @change="toggleTheme" size="large" />
        </div>
        <div class="setting-row">
          <div class="setting-info">
            <div class="setting-label">语言</div>
            <div class="setting-desc">切换界面语言</div>
          </div>
          <el-radio-group v-model="currentLocale" @change="toggleLocale" size="small">
            <el-radio-button label="zh-CN">中文</el-radio-button>
            <el-radio-button label="en">EN</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 第三方账号 -->
      <div class="page-card">
        <div class="section-title">🔗 第三方账号</div>
        <div class="oauth-list">
          <div v-for="provider in oauthProviders" :key="provider.key" class="oauth-item">
            <div class="oauth-info">
              <span class="oauth-icon">{{ provider.icon }}</span>
              <span class="oauth-name">{{ provider.name }}</span>
            </div>
            <el-button
              v-if="!provider.bound"
              type="primary"
              size="small"
              plain
              round
              @click="bindOAuth(provider.key)"
            >
              绑定
            </el-button>
            <el-button
              v-else
              type="danger"
              size="small"
              text
              @click="unbindOAuth(provider.key)"
            >
              解绑
            </el-button>
          </div>
        </div>
      </div>

      <!-- 危险操作 -->
      <div class="page-card danger-card">
        <div class="section-title danger-title">⚠️ 危险操作</div>
        <div class="setting-row">
          <div class="setting-info">
            <div class="setting-label">退出登录</div>
            <div class="setting-desc">退出当前账号，需要重新登录</div>
          </div>
          <el-button type="danger" @click="handleLogout" round>退出登录</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SettingsView' })
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { useI18n } from '@/composables/useI18n'
import { useTheme } from '@/composables/useTheme'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const router = useRouter()
const userStore = useUserStore()
const { locale, t: i18nT } = useI18n()
const { isDark, toggleTheme: rawToggleTheme } = useTheme()

const loading = ref(true)
const saving = ref(false)
const changingPw = ref(false)

const form = ref({
  nickname: '',
  email: '',
  bio: ''
})

const pwForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const currentLocale = ref(locale.value)

const avatarLetter = computed(() => (userStore.user?.nickname || userStore.user?.username || '?')[0].toUpperCase())
const roleLabel = computed(() => ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[userStore.user?.role || 'STUDENT'] || '学生'))
const roleType = computed(() => ({ ADMIN: 'danger', TEACHER: 'warning' }[userStore.user?.role || ''] || 'info') as 'danger' | 'warning' | 'info')

const avatarStyle = computed(() => ({
  background: `linear-gradient(135deg, ${userStore.user?.role === 'ADMIN' ? '#EF4444' : userStore.user?.role === 'TEACHER' ? '#F59E0B' : '#3B82F6'}, #8B5CF6)`
}))

const oauthProviders = ref([
  { key: 'github', name: 'GitHub', icon: '🐙', bound: false },
  { key: 'wechat', name: '微信', icon: '💬', bound: false },
  { key: 'google', name: 'Google', icon: '🔍', bound: false }
])

function toggleTheme() {
  rawToggleTheme()
}

function toggleLocale() {
  locale.value = currentLocale.value
}

async function loadProfile() {
  try {
    const res = await userApi.getMyInfo()
    if (res.code === 0 && res.data) {
      form.value.nickname = res.data.nickname || ''
      form.value.email = (res.data as any).email || ''
      form.value.bio = res.data.bio || ''
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function saveProfile() {
  saving.value = true
  try {
    const res = await userApi.updateProfile({
      nickname: form.value.nickname,
      email: form.value.email,
      bio: form.value.bio
    })
    if (res.code === 0) {
      ElMessage.success('保存成功！')
      userStore.fetchUser()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally { saving.value = false }
}

async function changePassword() {
  if (pwForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (pwForm.value.newPassword !== pwForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  changingPw.value = true
  try {
    const res = await userApi.changePassword({
      oldPassword: pwForm.value.oldPassword,
      newPassword: pwForm.value.newPassword
    })
    if (res.code === 0) {
      ElMessage.success('密码修改成功！')
      pwForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e))
  } finally { changingPw.value = false }
}

async function bindOAuth(provider: string) {
  ElMessage.info(`${provider} 绑定功能即将开放`)
}

async function unbindOAuth(provider: string) {
  ElMessage.info(`${provider} 解绑功能即将开放`)
}

function handleLogout() {
  userStore.logout()
  router.push('/feed')
  ElMessage.success('已退出登录')
}

onMounted(loadProfile)
</script>

<style scoped>
.settings-page {
  max-width: 700px;
  margin: 0 auto;
}

/* Avatar Card */
.avatar-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
}

.avatar-section {
  display: flex;
  gap: 20px;
  align-items: center;
}

.avatar-display {
  position: relative;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgba(255,255,255,0.3);
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
  transition: transform 0.3s ease;
}

.avatar-circle:hover {
  transform: scale(1.08) rotate(5deg);
}

.avatar-letter {
  font-size: 36px;
  font-weight: 800;
  color: #fff;
}

.avatar-level {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: linear-gradient(135deg, #F59E0B, #EF4444);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  border: 2px solid rgba(255,255,255,0.5);
}

.avatar-info h3 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 800;
}

.avatar-role {
  display: flex;
  gap: 10px;
  align-items: center;
  margin: 0 0 4px;
}

.points-display {
  font-weight: 600;
  font-size: 14px;
}

.avatar-joined {
  font-size: 13px;
  opacity: 0.8;
  margin: 0;
}

/* Sections */
.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}

/* Setting Rows */
.setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--border, #e5e7eb);
}

.setting-row:last-child {
  border-bottom: none;
}

.setting-label {
  font-weight: 600;
  font-size: 15px;
  color: var(--text, #1a1a2e);
}

.setting-desc {
  font-size: 13px;
  color: var(--text2, #64748b);
  margin-top: 2px;
}

/* OAuth */
.oauth-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.oauth-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg, #f5f5f5);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.oauth-item:hover {
  background: var(--border, #e5e7eb);
}

.oauth-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.oauth-icon {
  font-size: 24px;
}

.oauth-name {
  font-weight: 600;
  font-size: 15px;
}

/* Danger */
.danger-card {
  border: 2px solid #FEE2E2;
  background: #FEF2F2;
}

.danger-title {
  color: #DC2626;
}

/* Responsive */
@media (max-width: 768px) {
  .avatar-section {
    flex-direction: column;
    text-align: center;
  }
  .avatar-role {
    justify-content: center;
  }
}
</style>

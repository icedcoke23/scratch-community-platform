<template>
  <el-dialog
    v-model="visible"
    :title="isRegister ? '注册' : '登录'"
    width="400px"
    :close-on-click-modal="false"
    @close="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
      <el-form-item prop="username">
        <el-input
          v-model="form.username"
          placeholder="用户名"
          prefix-icon="User"
          size="large"
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码"
          prefix-icon="Lock"
          size="large"
          show-password
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
      <template v-if="isRegister">
        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="昵称"
            prefix-icon="UserFilled"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱（可选，用于找回密码）"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>
      </template>
    </el-form>

    <div class="dialog-footer">
      <el-button type="primary" size="large" :loading="loading" @click="handleSubmit" style="width: 100%">
        {{ isRegister ? '注册' : '登录' }}
      </el-button>
      <div class="switch-mode">
        <span v-if="isRegister">已有账号？</span>
        <span v-else>没有账号？</span>
        <el-button type="primary" link @click="isRegister = !isRegister">
          {{ isRegister ? '去登录' : '去注册' }}
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getErrorMessage } from '@/utils/error'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const visible = ref(props.modelValue)
const isRegister = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const userStore = useUserStore()

watch(() => props.modelValue, v => visible.value = v)
watch(visible, v => emit('update:modelValue', v))

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度 6-50', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

function resetForm() {
  formRef.value?.resetFields()
  isRegister.value = false
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch { return }

  loading.value = true
  try {
    if (isRegister.value) {
      const res = await userApi.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username
      })
      if (res.code === 0) {
        ElMessage.success('注册成功，请登录')
        isRegister.value = false
      }
    } else {
      const res = await userApi.login({
        username: form.username,
        password: form.password
      })
      if (res.code === 0 && res.data) {
        userStore.setLogin(res.data.token, res.data.userInfo)
        ElMessage.success('登录成功')
        visible.value = false
        emit('success')
      }
    }
  } catch (e: unknown) {
    ElMessage.error(getErrorMessage(e) || (isRegister.value ? '注册失败' : '登录失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.dialog-footer { margin-top: 10px; }
.switch-mode {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  color: var(--text2, #6b7280);
}
</style>

<template>
  <div class="config-page">
    <h1 class="page-title">
      <span class="title-emoji">⚙️</span>
      系统配置
    </h1>

    <!-- 配置列表 -->
    <div class="page-card">
      <div class="section-title">🔧 平台配置项</div>
      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else class="config-list">
        <div v-for="item in configs" :key="item.key" class="config-item">
          <div class="config-info">
            <div class="config-key">{{ item.key }}</div>
            <div class="config-desc" v-if="item.description">{{ item.description }}</div>
          </div>
          <div class="config-value">
            <el-input v-if="editingKey !== item.key" :model-value="item.value" readonly size="small" @click="startEdit(item)">
              <template #append>
                <el-button @click="startEdit(item)">编辑</el-button>
              </template>
            </el-input>
            <div v-else class="edit-row">
              <el-input v-model="editValue" size="small" autofocus @keyup.enter="saveConfig(item.key)" @keyup.escape="cancelEdit" />
              <el-button type="primary" size="small" @click="saveConfig(item.key)" :loading="saving">保存</el-button>
              <el-button size="small" @click="cancelEdit">取消</el-button>
            </div>
          </div>
        </div>
        <div v-if="configs.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <div>暂无配置项</div>
        </div>
      </div>
    </div>

    <!-- 添加配置 -->
    <div class="page-card">
      <div class="section-title">➕ 添加配置</div>
      <el-form :model="newConfig" inline>
        <el-form-item label="Key">
          <el-input v-model="newConfig.key" placeholder="config.key" />
        </el-form-item>
        <el-form-item label="Value">
          <el-input v-model="newConfig.value" placeholder="值" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="newConfig.description" placeholder="配置说明" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addConfig" :loading="saving">添加</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 系统信息 -->
    <div class="page-card">
      <div class="section-title">📊 系统信息</div>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">Java 版本</span>
          <span class="info-value">17 (Spring Boot 3.2.5)</span>
        </div>
        <div class="info-item">
          <span class="info-label">数据库</span>
          <span class="info-value">MySQL 8.0</span>
        </div>
        <div class="info-item">
          <span class="info-label">缓存</span>
          <span class="info-value">Redis 7</span>
        </div>
        <div class="info-item">
          <span class="info-label">前端框架</span>
          <span class="info-value">Vue 3.5 + Vite 8</span>
        </div>
        <div class="info-item">
          <span class="info-label">平台版本</span>
          <span class="info-value">v3.6.0</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SystemConfigView' })
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

interface ConfigItem {
  key: string
  value: string
  description?: string
}

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const configs = ref<ConfigItem[]>([])
const editingKey = ref('')
const editValue = ref('')
const newConfig = ref({ key: '', value: '', description: '' })

function getHeaders() {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${userStore.token}`
  }
}

async function loadConfigs() {
  loading.value = true
  try {
    const res = await fetch('/api/v1/admin/config', { headers: getHeaders() })
    const data = await res.json()
    if (data.code === 0) {
      configs.value = data.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function startEdit(item: ConfigItem) {
  editingKey.value = item.key
  editValue.value = item.value
}

function cancelEdit() {
  editingKey.value = ''
  editValue.value = ''
}

async function saveConfig(key: string) {
  saving.value = true
  try {
    const res = await fetch(`/api/v1/admin/config/${encodeURIComponent(key)}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify({ value: editValue.value })
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('配置已更新')
      cancelEdit()
      loadConfigs()
    } else {
      ElMessage.error(data.msg || '更新失败')
    }
  } catch { ElMessage.error('更新失败') }
  finally { saving.value = false }
}

async function addConfig() {
  if (!newConfig.value.key.trim()) {
    ElMessage.warning('请输入配置Key')
    return
  }
  saving.value = true
  try {
    const res = await fetch(`/api/v1/admin/config/${encodeURIComponent(newConfig.value.key)}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify({ value: newConfig.value.value, description: newConfig.value.description })
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('配置已添加')
      newConfig.value = { key: '', value: '', description: '' }
      loadConfigs()
    } else {
      ElMessage.error(data.msg || '添加失败')
    }
  } catch { ElMessage.error('添加失败') }
  finally { saving.value = false }
}

onMounted(loadConfigs)
</script>

<style scoped>
.config-page {
  max-width: 1024px;
  margin: 0 auto;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border, #e5e7eb);
  transition: all 0.2s ease;
}

.config-item:hover {
  background: var(--bg, #f9fafb);
}

.config-info {
  flex: 1;
  min-width: 0;
}

.config-key {
  font-weight: 700;
  font-size: 14px;
  color: var(--text, #1a1a2e);
  font-family: 'Courier New', monospace;
}

.config-desc {
  font-size: 12px;
  color: var(--text2, #64748b);
  margin-top: 2px;
}

.config-value {
  flex: 1;
  min-width: 0;
}

.edit-row {
  display: flex;
  gap: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.info-item {
  padding: 12px 16px;
  background: var(--bg, #f5f5f5);
  border-radius: 10px;
}

.info-label {
  display: block;
  font-size: 12px;
  color: var(--text2, #64748b);
  margin-bottom: 4px;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text, #1a1a2e);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .config-item {
    flex-direction: column;
    align-items: flex-start;
  }
  .config-value {
    width: 100%;
  }
}
</style>

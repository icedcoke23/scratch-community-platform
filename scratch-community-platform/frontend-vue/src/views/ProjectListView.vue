<template>
  <div class="project-list-view">
    <header class="list-header">
      <div class="header-content">
        <h1 class="page-title">我的项目</h1>
        <div class="header-actions">
          <el-input
            v-model="searchQuery"
            placeholder="搜索项目..."
            class="search-input"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            创建项目
          </el-button>
        </div>
      </div>
    </header>

    <div class="filter-bar">
      <div class="filter-left">
        <el-radio-group v-model="statusFilter" @change="handleStatusChange">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="published">已发布</el-radio-button>
          <el-radio-button label="draft">草稿</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-right">
        <el-select v-model="sortBy" @change="handleSortChange" style="width: 140px">
          <el-option label="最新创建" value="createdAt" />
          <el-option label="最近更新" value="updatedAt" />
          <el-option label="最多点赞" value="likeCount" />
        </el-select>
        <el-select v-model="sortOrder" @change="handleSortChange" style="width: 100px">
          <el-option label="降序" value="desc" />
          <el-option label="升序" value="asc" />
        </el-select>
        <el-select v-model="pageSize" @change="handlePageSizeChange" style="width: 100px">
          <el-option :value="8" label="8/页" />
          <el-option :value="12" label="12/页" />
          <el-option :value="16" label="16/页" />
          <el-option :value="24" label="24/页" />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="list-content">
      <div v-if="error" class="error-state">
        <el-icon class="error-icon"><WarningFilled /></el-icon>
        <p class="error-text">{{ error }}</p>
        <el-button type="primary" @click="fetchProjects">重试</el-button>
      </div>

      <div v-else-if="!loading && projects.length === 0" class="empty-state">
        <el-icon class="empty-icon"><FolderOpened /></el-icon>
        <h3 class="empty-title">{{ searchQuery || statusFilter ? '没有找到项目' : '还没有项目' }}</h3>
        <p class="empty-description">
          {{ searchQuery || statusFilter ? '尝试调整搜索条件' : '开始创建你的第一个 Scratch 项目吧！' }}
        </p>
        <el-button v-if="!searchQuery && !statusFilter" type="primary" @click="handleCreate">
          创建第一个项目
        </el-button>
      </div>

      <div v-else class="project-grid">
        <div
          v-for="project in projects"
          :key="project.id"
          class="project-card"
        >
          <ScratchPreview
            :project-id="project.id"
            :title="project.title"
            :author="project.author?.username"
            :like-count="project.likeCount"
            :view-count="project.viewCount"
            :project-status="project.status"
            size="small"
            :show-info="true"
            :show-play-hint="true"
            @click="handleCardClick(project)"
          />
          <div class="card-actions">
            <el-button
              type="primary"
              size="small"
              @click.stop="handleEdit(project)"
            >
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click.stop="handleDelete(project)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="projects.length > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-count="totalPages"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="400px"
    >
      <p>确定要删除项目「{{ currentProject?.title }}」吗？</p>
      <p class="warning-text">此操作不可恢复</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="confirmDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  Plus,
  Edit,
  Delete,
  WarningFilled,
  FolderOpened
} from '@element-plus/icons-vue'
import ScratchPreview from '@/components/ScratchPreview.vue'
import { projectApi, type ProjectDetail, type ProjectQueryParams } from '@/api'

const router = useRouter()

const loading = ref(false)
const deleting = ref(false)
const error = ref('')
const projects = ref<ProjectDetail[]>([])
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

const searchQuery = ref('')
const statusFilter = ref<'draft' | 'published' | ''>('')
const sortBy = ref<'createdAt' | 'updatedAt' | 'likeCount'>('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')

const deleteDialogVisible = ref(false)
const currentProject = ref<ProjectDetail | null>(null)

let searchTimer: number | null = null

const fetchProjects = async () => {
  loading.value = true
  error.value = ''

  const params: ProjectQueryParams = {
    page: currentPage.value,
    pageSize: pageSize.value,
    status: statusFilter.value,
    search: searchQuery.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value
  }

  try {
    const response = await projectApi.getList(params)
    if (response.code === 0) {
      projects.value = response.data.list || []
      total.value = response.data.total || 0
      totalPages.value = response.data.totalPages || 0
    } else {
      error.value = response.msg || '获取项目列表失败'
    }
  } catch (err: any) {
    error.value = err.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    currentPage.value = 1
    fetchProjects()
  }, 300)
}

const handleStatusChange = () => {
  currentPage.value = 1
  fetchProjects()
}

const handleSortChange = () => {
  currentPage.value = 1
  fetchProjects()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchProjects()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  fetchProjects()
}

const handleCreate = () => {
  router.push('/editor/new')
}

const handleCardClick = (project: ProjectDetail) => {
  router.push(`/editor/${project.id}`)
}

const handleEdit = (project: ProjectDetail) => {
  router.push(`/editor/${project.id}`)
}

const handleDelete = (project: ProjectDetail) => {
  currentProject.value = project
  deleteDialogVisible.value = true
}

const confirmDelete = async () => {
  if (!currentProject.value) return

  deleting.value = true
  try {
    const response = await projectApi.delete(currentProject.value.id)
    if (response.code === 0) {
      ElMessage.success('删除成功')
      deleteDialogVisible.value = false
      fetchProjects()
    } else {
      ElMessage.error(response.msg || '删除失败')
    }
  } catch (err: any) {
    ElMessage.error(err.message || '删除失败')
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped>
.project-list-view {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
}

.list-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px 24px;
  color: white;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 280px;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
}

.filter-bar {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-left,
.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.list-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  min-height: 400px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.project-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-actions {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #eee;
}

.card-actions .el-button {
  flex: 1;
}

.card-actions .el-button + .el-button {
  margin-left: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 24px 0;
}

.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
}

.error-icon,
.empty-icon {
  font-size: 64px;
  color: #ccc;
  margin-bottom: 16px;
}

.error-text {
  color: #666;
  font-size: 16px;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.empty-description {
  color: #999;
  font-size: 14px;
  margin-bottom: 24px;
}

.warning-text {
  color: #f56c6c;
  font-size: 13px;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-left,
  .filter-right {
    flex-wrap: wrap;
    justify-content: center;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>

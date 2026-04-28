import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Project, ProjectDetail } from '@/types'
import { projectApi } from '@/api'

export const useProjectStore = defineStore('project', () => {
  /** 当前项目详情 */
  const currentProject = ref<ProjectDetail | null>(null)
  /** 项目列表缓存（按页码） */
  const projectCache = ref<Map<number, Project[]>>(new Map())
  /** 加载状态 */
  const loading = ref(false)

  const hasCurrentProject = computed(() => !!currentProject.value)

  /** 加载项目详情 */
  async function loadProject(id: number) {
    loading.value = true
    try {
      const res = await projectApi.getDetail(id)
      if (res.code === 0) {
        currentProject.value = res.data || null
      }
    } catch { /* 忽略 */ }
    finally { loading.value = false }
  }

  /** 清除当前项目 */
  function clearCurrent() {
    currentProject.value = null
  }

  /** 缓存项目列表 */
  function setCache(page: number, projects: Project[]) {
    projectCache.value.set(page, projects)
  }

  /** 获取缓存的项目列表 */
  function getCache(page: number): Project[] | undefined {
    return projectCache.value.get(page)
  }

  /** 清除所有缓存 */
  function clearCache() {
    projectCache.value.clear()
  }

  return {
    currentProject,
    loading,
    hasCurrentProject,
    loadProject,
    clearCurrent,
    setCache,
    getCache,
    clearCache
  }
})

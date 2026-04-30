import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useI18n } from '@/composables/useI18n'

export interface NavLink {
  path: string
  labelKey: string
  label: string
  roles: string[]
  icon: string
}

const ALL_NAV_LINKS: NavLink[] = [
  { path: '/feed', labelKey: 'nav.feed', label: '社区', roles: ['all'], icon: '🏠' },
  { path: '/search', labelKey: 'nav.search', label: '搜索', roles: ['all'], icon: '🔍' },
  { path: '/problems', labelKey: 'nav.problems', label: '题库', roles: ['all'], icon: '📝' },
  { path: '/competition', labelKey: 'nav.competition', label: '竞赛', roles: ['all'], icon: '🏆' },
  { path: '/rank', labelKey: 'nav.rank', label: '排行榜', roles: ['all'], icon: '🏅' },
  { path: '/class', labelKey: 'nav.class', label: '班级', roles: ['all'], icon: '🏫' },
  { path: '/points', labelKey: 'nav.points', label: '积分', roles: ['all'], icon: '⭐' },
  { path: '/homework', labelKey: 'nav.homework', label: '作业', roles: ['all'], icon: '📚' },
  { path: '/analytics', labelKey: 'nav.analytics', label: '学情', roles: ['TEACHER', 'ADMIN'], icon: '📊' },
  { path: '/admin', labelKey: 'nav.admin', label: '管理', roles: ['ADMIN'], icon: '⚙️' }
]

export function useNavigation() {
  const userStore = useUserStore()
  const { t } = useI18n()

  const navLinks = computed(() => {
    return ALL_NAV_LINKS.filter(l => {
      if (l.roles.includes('all')) return true
      if (!userStore.isLoggedIn) return false
      return l.roles.includes(userStore.user?.role || '')
    }).map(l => ({
      ...l,
      label: t(l.labelKey)
    }))
  })

  const mobileNavLinks = computed(() => navLinks.value.slice(0, 5).map(l => ({
    ...l,
    icon: ALL_NAV_LINKS.find(a => a.path === l.path)?.icon || '📄'
  })))

  return { navLinks, mobileNavLinks }
}

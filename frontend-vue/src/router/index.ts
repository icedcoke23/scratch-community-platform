import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useRouteLoading } from '@/composables/useRouteLoading'

const { start: startLoading, finish: finishLoading } = useRouteLoading()

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/feed'
    },
    // ===== 社区模块 =====
    {
      path: '/feed',
      name: 'Feed',
      component: () => import('@/views/social/FeedView.vue'),
      meta: { title: '社区', keepAlive: true }
    },
    {
      path: '/project/:id',
      name: 'ProjectDetail',
      component: () => import('@/views/social/ProjectDetailView.vue'),
      meta: { title: '项目详情' }
    },
    {
      path: '/user/:id',
      name: 'UserProfile',
      component: () => import('@/views/social/UserProfileView.vue'),
      meta: { title: '用户主页' }
    },
    // ===== 编辑器模块 =====
    {
      path: '/editor',
      name: 'NewEditor',
      component: () => import('@/views/editor/ScratchEditorView.vue'),
      meta: { title: '新建项目', requiresAuth: true }
    },
    {
      path: '/editor/:id',
      name: 'Editor',
      component: () => import('@/views/editor/ScratchEditorView.vue'),
      meta: { title: '编辑项目', requiresAuth: true }
    },
    // ===== 协作编辑模块 =====
    {
      path: '/collab/:projectId',
      name: 'CollabEditor',
      component: () => import('@/views/collab/CollabEditorView.vue'),
      meta: { title: '协作编辑', requiresAuth: true }
    },
    {
      path: '/rank',
      name: 'Rank',
      component: () => import('@/views/social/RankView.vue'),
      meta: { title: '排行榜', keepAlive: true }
    },
    {
      path: '/search',
      name: 'Search',
      component: () => import('@/views/social/SearchView.vue'),
      meta: { title: '搜索' }
    },
    // ===== 第三方登录回调 =====
    {
      path: '/oauth/:provider/callback',
      name: 'OAuthCallback',
      component: () => import('@/views/OAuthCallbackView.vue'),
      meta: { title: '第三方登录' }
    },
    // ===== 判题模块 =====
    {
      path: '/problems',
      name: 'Problems',
      component: () => import('@/views/judge/ProblemsView.vue'),
      meta: { title: '题库', keepAlive: true }
    },
    {
      path: '/competition',
      name: 'Competition',
      component: () => import('@/views/judge/CompetitionView.vue'),
      meta: { title: '竞赛', keepAlive: true }
    },
    {
      path: '/competition/:id',
      name: 'CompetitionDetail',
      component: () => import('@/views/judge/CompetitionDetailView.vue'),
      meta: { title: '竞赛详情' }
    },
    // ===== 教室模块 =====
    {
      path: '/homework',
      name: 'Homework',
      component: () => import('@/views/classroom/HomeworkView.vue'),
      meta: { title: '作业', requiresAuth: true, keepAlive: true }
    },
    {
      path: '/homework/:id',
      name: 'HomeworkDetail',
      component: () => import('@/views/classroom/HomeworkDetailView.vue'),
      meta: { title: '作业详情', requiresAuth: true }
    },
    {
      path: '/analytics',
      name: 'Analytics',
      component: () => import('@/views/classroom/AnalyticsView.vue'),
      meta: { title: '学情分析', requiresAuth: true, requiresTeacher: true }
    },
    // ===== 积分模块 =====
    {
      path: '/points',
      name: 'Points',
      component: () => import('@/views/points/PointsView.vue'),
      meta: { title: '积分', requiresAuth: true, keepAlive: true }
    },
    // ===== 管理模块 =====
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/views/admin/AdminView.vue'),
      meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
    },
    // ===== 通知模块 =====
    {
      path: '/notifications',
      name: 'Notifications',
      component: () => import('@/views/NotificationsView.vue'),
      meta: { title: '通知中心', requiresAuth: true, keepAlive: true }
    },
    // ===== 设置模块 =====
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('@/views/SettingsView.vue'),
      meta: { title: '个人设置', requiresAuth: true }
    },
    // ===== 成就模块 =====
    {
      path: '/achievements',
      name: 'Achievements',
      component: () => import('@/views/AchievementsView.vue'),
      meta: { title: '成就系统', requiresAuth: true }
    },
    // ===== 404 兜底 =====
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面未找到' }
    }
  ]
})

// 路由守卫
let tokenValidated = false

router.beforeEach(async (to) => {
  startLoading()

  const userStore = useUserStore()

  // 首次加载时验证 token 有效性
  if (!tokenValidated && userStore.isLoggedIn) {
    tokenValidated = true
    await userStore.validateToken()
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Feed' }
  }
  if (to.meta.requiresTeacher && userStore.user?.role !== 'TEACHER' && userStore.user?.role !== 'ADMIN') {
    return { name: 'Feed' }
  }
  if (to.meta.requiresAdmin && userStore.user?.role !== 'ADMIN') {
    return { name: 'Feed' }
  }
})

// 路由后置守卫：设置页面标题 + 隐藏加载进度条
router.afterEach((to) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - Scratch 社区`
  }

  finishLoading()
})

export default router

import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/preview-demo',
      name: 'PreviewDemo',
      component: () => import('@/views/PreviewDemoView.vue')
    },
    {
      path: '/projects',
      name: 'ProjectList',
      component: () => import('@/views/ProjectListView.vue')
    },
    {
      path: '/editor/:id?',
      name: 'Editor',
      component: () => import('@/views/editor/ScratchEditorView.vue')
    }
  ]
})

export default router

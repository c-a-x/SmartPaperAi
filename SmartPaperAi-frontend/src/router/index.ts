import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'teaching',
        name: 'Teaching',
        component: () => import('@/views/teaching/TeachingListView.vue'),
        meta: { title: '教学设计' }
      },
      {
        path: 'teaching/create',
        name: 'TeachingCreate',
        component: () => import('@/views/teaching/TeachingEditView.vue'),
        meta: { title: '创建教学设计' }
      },
      {
        path: 'teaching/:id',
        name: 'TeachingDetail',
        component: () => import('@/views/teaching/TeachingDetailView.vue'),
        meta: { title: '教学设计详情' }
      },
      {
        path: 'teaching/:id/edit',
        name: 'TeachingEdit',
        component: () => import('@/views/teaching/TeachingEditView.vue'),
        meta: { title: '编辑教学设计' }
      },
      {
        path: 'documents',
        name: 'Documents',
        component: () => import('@/views/document/DocumentListView.vue'),
        meta: { title: '文档管理' }
      },
      {
        path: 'documents/:id',
        name: 'DocumentDetail',
        component: () => import('@/views/document/DocumentDetailView.vue'),
        meta: { title: '文档详情' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgeListView.vue'),
        meta: { title: '知识库管理' }
      },
      {
        path: 'knowledge/:id',
        name: 'KnowledgeDetail',
        component: () => import('@/views/knowledge/KnowledgeDetailView.vue'),
        meta: { title: '知识库详情' }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { title: 'AI 对话' }
      },
      {
        path: 'rag-chat',
        name: 'RagChat',
        component: () => import('@/views/rag/RagChatView.vue'),
        meta: { title: '知识库问答' }
      },
      {
        path: 'analysis/summary',
        name: 'PaperSummary',
        component: () => import('@/views/analysis/PaperSummaryView.vue'),
        meta: { title: '论文总结' }
      },
      {
        path: 'analysis/comparison',
        name: 'PaperComparison',
        component: () => import('@/views/analysis/PaperComparisonView.vue'),
        meta: { title: '论文对比' }
      },
      {
        path: 'analysis/innovation',
        name: 'InnovationCluster',
        component: () => import('@/views/analysis/InnovationClusterView.vue'),
        meta: { title: '创新点聚合' }
      },
      {
        path: 'analysis/review',
        name: 'LiteratureReview',
        component: () => import('@/views/analysis/LiteratureReviewView.vue'),
        meta: { title: '文献综述' }
      },
      {
        path: 'knowledge-graph',
        name: 'KnowledgeGraph',
        component: () => import('@/views/knowledge-graph/KnowledgeGraphView.vue'),
        meta: { title: '知识图谱' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { requiresAuth: false, title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth !== false

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - SmartPaperAI` : 'SmartPaperAI'

  if (requiresAuth && !authStore.isLoggedIn) {
    // 需要登录但未登录，跳转到登录页
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.path === '/login' && authStore.isLoggedIn) {
    // 已登录访问登录页，跳转到首页
    next('/')
  } else {
    next()
  }
})

export default router

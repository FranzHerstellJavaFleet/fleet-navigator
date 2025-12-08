import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/layout/MainLayout.vue'
import { useAuthStore } from '../stores/authStore'

const routes = [
  // Login page (public)
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: 'Anmelden - Fleet Navigator', public: true }
  },
  // Register page (redirect to login with register tab)
  {
    path: '/register',
    redirect: '/login'
  },
  // Main app with sidebar (chat interface)
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('../views/Home.vue'),
        meta: { title: 'Fleet Navigator' }
      },
      {
        path: 'experts',
        name: 'experts',
        component: () => import('../components/ExpertManager.vue'),
        meta: { title: 'Experten-System' }
      }
    ]
  },
  // Standalone agent pages (no sidebar)
  {
    path: '/agents/fleet-mates',
    name: 'fleet-mates',
    component: () => import('../views/agents/FleetMatesView.vue'),
    meta: { title: 'Fleet Mates', requiresAuth: true }
  },
  {
    path: '/agents/fleet-mates/:mateId',
    name: 'mate-detail',
    component: () => import('../views/agents/MateDetailView.vue'),
    meta: { title: 'Mate Details', requiresAuth: true },
    props: true
  },
  // Catch-all 404 route
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// Navigation guard for authentication
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Initialize auth on first navigation
  if (!authStore.isInitialized) {
    await authStore.checkAuth()
  }

  // Public routes don't need auth
  if (to.meta.public) {
    // If already authenticated and going to login, redirect to home
    if (authStore.isAuthenticated && to.name === 'login') {
      next({ name: 'home' })
      return
    }
    next()
    return
  }

  // Protected routes need authentication
  if (to.meta.requiresAuth || to.matched.some(record => record.meta.requiresAuth)) {
    if (!authStore.isAuthenticated) {
      next({ name: 'login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})

// Update document title on route change
router.afterEach((to) => {
  document.title = to.meta.title || 'Fleet Navigator'
})

export default router

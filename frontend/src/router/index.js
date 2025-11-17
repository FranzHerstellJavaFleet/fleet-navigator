import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/layout/MainLayout.vue'

const routes = [
  // Main app with sidebar (chat interface)
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('../views/Home.vue'),
        meta: { title: 'Fleet Navigator' }
      }
    ]
  },
  // Standalone agent pages (no sidebar)
  {
    path: '/agents/fleet-mates',
    name: 'fleet-mates',
    component: () => import('../views/agents/FleetMatesView.vue'),
    meta: { title: 'Fleet Mates' }
  },
  {
    path: '/agents/fleet-mates/:mateId',
    name: 'mate-detail',
    component: () => import('../views/agents/MateDetailView.vue'),
    meta: { title: 'Mate Details' },
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

// Optional: Update document title on route change
router.afterEach((to) => {
  document.title = to.meta.title || 'Fleet Navigator'
})

export default router

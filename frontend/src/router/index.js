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
    path: '/agents/fleet-officers',
    name: 'fleet-officers',
    component: () => import('../views/agents/FleetOfficersView.vue'),
    meta: { title: 'Fleet Officers' }
  },
  {
    path: '/agents/fleet-officers/:officerId',
    name: 'officer-detail',
    component: () => import('../views/agents/OfficerDetailView.vue'),
    meta: { title: 'Officer Details' },
    props: true
  },
  {
    path: '/agents/email',
    name: 'email-agent',
    component: () => import('../views/agents/EmailAgentView.vue'),
    meta: { title: 'Email Agent' }
  },
  {
    path: '/agents/documents',
    name: 'document-agent',
    component: () => import('../views/agents/DocumentAgentView.vue'),
    meta: { title: 'Document Agent' }
  },
  {
    path: '/agents/os',
    name: 'os-agent',
    component: () => import('../views/agents/OSAgentView.vue'),
    meta: { title: 'OS Agent' }
  },
  {
    path: '/agents/system',
    name: 'system-monitor',
    component: () => import('../views/agents/SystemMonitorView.vue'),
    meta: { title: 'System Monitor' }
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

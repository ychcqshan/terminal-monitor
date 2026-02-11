import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/agents',
    name: 'AgentList',
    component: () => import('../views/AgentList.vue')
  },
  {
    path: '/agents/:id',
    name: 'AgentDetail',
    component: () => import('../views/AgentDetail.vue')
  },
  {
    path: '/baselines',
    name: 'BaselineManager',
    component: () => import('../views/BaselineManager.vue')
  },
  {
    path: '/alerts',
    name: 'AlertCenter',
    component: () => import('../views/AlertCenter.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

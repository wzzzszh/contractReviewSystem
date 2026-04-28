import { createRouter, createWebHistory } from 'vue-router'
import { storage } from '../api/request'
import MainLayout from '../views/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import ReviewView from '../views/ReviewView.vue'
import FilesView from '../views/FilesView.vue'
import UsersView from '../views/UsersView.vue'
import MonitorView from '../views/MonitorView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        { path: 'dashboard', name: 'dashboard', component: DashboardView },
        { path: 'review', name: 'review', component: ReviewView },
        { path: 'files', name: 'files', component: FilesView },
        { path: 'users', name: 'users', component: UsersView },
        { path: 'monitor', name: 'monitor', component: MonitorView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !storage.getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && storage.getToken()) {
    return '/dashboard'
  }
})

export default router

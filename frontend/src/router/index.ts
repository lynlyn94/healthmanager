import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layout/AppLayout.vue'),
      redirect: '/square',
      children: [
        {
          path: '/tasks',
          name: 'Tasks',
          component: () => import('@/views/therapist/task/TaskView.vue'),
        },
        {
          path: '/patients',
          name: 'Patients',
          component: () => import('@/views/therapist/patient/PatientListView.vue'),
        },
        {
          path: '/patients/:id',
          name: 'PatientDetail',
          component: () => import('@/views/therapist/patient/PatientDetailView.vue'),
        },
        {
          path: '/workload',
          name: 'Workload',
          component: () => import('@/views/therapist/workload/WorkloadView.vue'),
        },
        {
          path: '/square',
          name: 'Square',
          component: () => import('@/views/therapist/square/SquareView.vue'),
        },
        {
          path: '/orders',
          name: 'Orders',
          component: () => import('@/views/doctor/OrderView.vue'),
        },
        {
          path: '/doctor/patients',
          name: 'DoctorPatients',
          component: () => import('@/views/doctor/DoctorPatientView.vue'),
        },
        {
          path: '/settings',
          name: 'Settings',
          component: () => import('@/views/settings/SettingsView.vue'),
        },
        {
          path: '/doctor/plans-review',
          name: 'PlanReview',
          component: () => import('@/views/doctor/PlanReviewView.vue'),
          meta: { role: 'DOCTOR' },
        },
        {
          path: '/admin/users',
          name: 'AdminUsers',
          component: () => import('@/views/admin/UserManageView.vue'),
          meta: { role: 'ADMIN' },
        },
        {
          path: '/admin/groups',
          name: 'AdminGroups',
          component: () => import('@/views/admin/GroupManageView.vue'),
          meta: { role: 'ADMIN' },
        },
        {
          path: '/admin/dicts',
          name: 'AdminDicts',
          component: () => import('@/views/admin/DictManageView.vue'),
          meta: { role: 'ADMIN' },
        },
        {
          path: '/admin/logs',
          name: 'AdminLogs',
          component: () => import('@/views/admin/LogView.vue'),
          meta: { role: 'ADMIN' },
        },
        {
          path: '/admin/stats',
          name: 'AdminStats',
          component: () => import('@/views/admin/StatsView.vue'),
          meta: { role: 'ADMIN' },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    next()
    return
  }
  if (!authStore.isLoggedIn) {
    next('/login')
    return
  }
  if (to.meta.role && to.meta.role !== authStore.role) {
    next(authStore.isDoctor ? '/orders' : '/tasks')
    return
  }
  // Redirect / to role-appropriate home, but allow intentional navigation to /square
  if (to.path === '/') {
    if (authStore.isDoctor) {
      next('/orders')
      return
    }
    if (authStore.isTherapist) {
      next('/tasks')
      return
    }
  }
  next()
})

export default router

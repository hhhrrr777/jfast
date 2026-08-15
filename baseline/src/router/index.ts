import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { hasRefreshToken } from '@/api/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** 公开页(免登录)。 */
    public?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/home/index.vue')
  },
  {
    path: '/:module/:entity/form/:id?',
    name: 'entity-form',
    component: () => import('@/views/entity/form.vue'),
    props: true
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫:非公开页需登录态,未登录跳登录页并记录回跳地址
router.beforeEach((to) => {
  if (to.meta.public) {
    // 已登录访问登录页则直接进首页
    if (to.name === 'login' && hasRefreshToken()) {
      return { path: '/' }
    }
    return true
  }
  if (!hasRefreshToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router

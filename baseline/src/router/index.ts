import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { hasRefreshToken } from '@/api/auth'
import { loadPermissions, permissionState, type RouterNode } from '@/store/permission'

declare module 'vue-router' {
  interface RouteMeta {
    /** 公开页(免登录)。 */
    public?: boolean
  }
}

/**
 * 路由骨架:登录页/首页/404/实体表单页为静态;系统管理等业务路由
 * 由 /auth/routers 动态注入(component 经 import.meta.glob 按路径解析)。
 */

const Layout = () => import('@/layout/index.vue')

const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/home/index.vue')
      },
      // 完整后台的静态隐式表单页路由约定(ADR-0003):实体表单独立路由页,不进菜单表
      {
        path: ':module/:entity/form/:id?',
        name: 'entity-form',
        component: () => import('@/views/entity/form.vue'),
        props: true
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes
})

/** views 目录组件映射:component 字符串(如 system/user/index)→ 异步组件。 */
const viewModules = import.meta.glob('@/views/**/*.vue')

function resolveComponent(component: string) {
  const key = `/src/views/${component}.vue`
  return viewModules[key]
}

/** RouterNode 树 → vue-router 路由记录,注入 Layout 下。 */
function buildDynamicRoutes(nodes: RouterNode[], parentPath = ''): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const node of nodes) {
    if (!node.path) continue
    const fullPath = parentPath ? `${parentPath}/${node.path}` : `/${node.path}`
    if (node.children && node.children.length > 0) {
      routes.push({
        path: fullPath,
        component: Layout,
        children: buildDynamicRoutes(node.children, fullPath)
      })
    } else if (node.component && resolveComponent(node.component)) {
      routes.push({
        path: fullPath,
        name: node.name,
        component: resolveComponent(node.component)
      })
    }
  }
  return routes
}

let dynamicAdded = false

/** 注入动态路由(幂等);登录后与刷新页面(路由守卫)时调用。 */
export function addDynamicRoutes(routers: RouterNode[]): void {
  if (dynamicAdded) return
  for (const route of buildDynamicRoutes(routers)) {
    router.addRoute(route)
  }
  dynamicAdded = true
}

export function resetDynamicRoutes(): void {
  dynamicAdded = false
}

// 登录守卫:公开页直接放行;其余需登录态,且首次进入(F5)先装载权限与
// 动态路由再重放导航——否则动态路由未注入,目标路径会落到 404 catch-all。
router.beforeEach(async (to) => {
  if (to.meta.public) {
    if (to.name === 'login' && hasRefreshToken()) {
      return { path: '/' }
    }
    return true
  }
  if (!hasRefreshToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!permissionState.loaded) {
    const { fetchCurrentUser } = await import('@/api/auth')
    try {
      const user = await fetchCurrentUser()
      await loadPermissions(user)
      addDynamicRoutes(permissionState.routers)
    } catch {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 装载完成后重放导航,让新注入的动态路由参与匹配
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  }
  return true
})

export default router

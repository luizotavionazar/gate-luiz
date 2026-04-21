import { createRouter, createWebHistory } from 'vue-router'
import SetupView from '../views/SetupView.vue'
import LoginView from '../views/LoginView.vue'
import MinhaContaView from '../views/MinhaContaView.vue'
import AdminRolesView from '../views/AdminRolesView.vue'
import AdminPermissoesView from '../views/AdminPermissoesView.vue'
import AdminUsuariosView from '../views/AdminUsuariosView.vue'
import { getToken, isTokenExpired, logout } from '../services/autenticacaoService'
import { obterStatusSetup } from '../services/setupService'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/setup', name: 'setup', component: SetupView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/conta', name: 'conta', component: MinhaContaView, meta: { requiresAuth: true } },
  { path: '/admin/roles', name: 'admin-roles', component: AdminRolesView, meta: { requiresAuth: true } },
  { path: '/admin/permissoes', name: 'admin-permissoes', component: AdminPermissoesView, meta: { requiresAuth: true } },
  { path: '/admin/usuarios', name: 'admin-usuarios', component: AdminUsuariosView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const token = getToken()

  if (to.path !== '/setup') {
    try {
      const status = await obterStatusSetup()
      if (!status.setupConcluido) return next('/setup')
    } catch {
      return next('/setup')
    }
  }

  if (token && isTokenExpired()) logout()

  if (to.meta.requiresAuth && (!token || isTokenExpired())) {
    return next('/login')
  }

  if (to.path === '/login' && token && !isTokenExpired()) {
    return next('/conta')
  }

  next()
})

export default router

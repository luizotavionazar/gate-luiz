import { createRouter, createWebHistory } from 'vue-router'
import SetupView from '../views/SetupView.vue'
import SemAcessoView from '../views/SemAcessoView.vue'
import MinhaContaView from '../views/MinhaContaView.vue'
import AdminRolesView from '../views/AdminRolesView.vue'
import AdminPermissoesView from '../views/AdminPermissoesView.vue'
import AdminUsuariosView from '../views/AdminUsuariosView.vue'
import { getToken, isTokenExpired, logout } from '../services/autenticacaoService'

const routes = [
  { path: '/', redirect: '/admin/roles' },
  { path: '/setup', name: 'setup', component: SetupView },
  { path: '/sem-acesso', name: 'sem-acesso', component: SemAcessoView },
  { path: '/conta', name: 'conta', component: MinhaContaView, meta: { requiresAuth: true } },
  { path: '/admin/roles', name: 'admin-roles', component: AdminRolesView, meta: { requiresAuth: true } },
  { path: '/admin/permissoes', name: 'admin-permissoes', component: AdminPermissoesView, meta: { requiresAuth: true } },
  { path: '/admin/usuarios', name: 'admin-usuarios', component: AdminUsuariosView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.path === '/setup') return next()

  if (getToken() && isTokenExpired()) logout()

  if (!getToken() || isTokenExpired()) return next('/setup')

  next()
})

export default router

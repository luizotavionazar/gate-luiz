import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import CadastroView from '../views/CadastroView.vue'
import ContaView from '../views/ContaView.vue'
import RecuperarSenhaView from '../views/RecuperarSenhaView.vue'
import RedefinirSenhaView from '../views/RedefinirSenhaView.vue'
import SetupView from '../views/SetupView.vue'
import VincularContaGoogleView from '../views/VincularContaGoogleView.vue'
import { getToken, isTokenExpired, logout } from '../services/autenticacaoService'
import { obterStatusSetup } from '../services/setupService'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/cadastro', name: 'cadastro', component: CadastroView },
  { path: '/login/google/vincular', name: 'login-google-vincular', component: VincularContaGoogleView },
  { path: '/conta', name: 'conta', component: ContaView, meta: { requiresAuth: true } },
  { path: '/recuperar-senha', name: 'recuperar-senha', component: RecuperarSenhaView },
  { path: '/redefinir-senha', name: 'redefinir-senha', component: RedefinirSenhaView },
  { path: '/setup', name: 'setup', component: SetupView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = getToken()
  const publicPaths = ['/setup']

  if (!publicPaths.includes(to.path)) {
    try {
      const status = await obterStatusSetup()
      if (!status.setupConcluido) {
        return next('/setup')
      }
    } catch {
      return next('/setup')
    }
  }

  if (token && isTokenExpired()) {
    logout()
  }

  if (to.meta.requiresAuth) {
    if (!token || isTokenExpired()) {
      return next('/login')
    }
  }

  if ((to.path === '/login' || to.path === '/cadastro' || to.path === '/login/google/vincular') && token && !isTokenExpired()) {
    return next('/conta')
  }

  next()
})

export default router

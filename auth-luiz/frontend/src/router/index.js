import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import CadastroView from '../views/CadastroView.vue'
import ContaView from '../views/ContaView.vue'
import RecuperarSenhaView from '../views/RecuperarSenhaView.vue'
import RedefinirSenhaView from '../views/RedefinirSenhaView.vue'
import SetupView from '../views/SetupView.vue'
import VerificacaoEmailView from '../views/VerificacaoEmailView.vue'
import VerificacaoTelefoneView from '../views/VerificacaoTelefoneView.vue'
import CancelarRecuperacaoView from '../views/CancelarRecuperacaoView.vue'
import { getToken, isTokenExpired, logout } from '../services/autenticacaoService'
import { obterStatusSetup } from '../services/setupService'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/cadastro', name: 'cadastro', component: CadastroView },
  { path: '/conta', name: 'conta', component: ContaView, meta: { requiresAuth: true } },
  { path: '/recuperar-senha', name: 'recuperar-senha', component: RecuperarSenhaView },
  { path: '/recuperar-senha/cancelar', name: 'cancelar-recuperacao', component: CancelarRecuperacaoView },
  { path: '/redefinir-senha', name: 'redefinir-senha', component: RedefinirSenhaView },
  { path: '/verificar-email', name: 'verificar-email', component: VerificacaoEmailView },
  { path: '/verificar-telefone', name: 'verificar-telefone', component: VerificacaoTelefoneView, meta: { requiresAuth: true } },
  { path: '/setup', name: 'setup', component: SetupView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
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

  if ((to.path === '/login' || to.path === '/cadastro') && token && !isTokenExpired()) {
    return next('/conta')
  }

  next()
})

export default router

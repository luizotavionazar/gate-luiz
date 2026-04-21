import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { salvarSessaoDoFragment } from './services/autenticacaoService'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'

let redirecionarAposMount = null

const hash = window.location.hash
if (hash.startsWith('#token=')) {
  salvarSessaoDoFragment(hash.slice('#token='.length))
  history.replaceState(null, '', window.location.pathname + window.location.search)
  redirecionarAposMount = '/admin/roles'
}

createApp(App)
  .use(router)
  .mount('#app')

if (redirecionarAposMount) {
  router.replace(redirecionarAposMount)
}

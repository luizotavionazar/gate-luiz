<script setup>
import { AUTH_LUIZ_URL } from '../services/api'
</script>

<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light">
    <div class="card shadow-sm text-center" style="width: 420px">
      <div class="card-body p-4">
        <i class="bi bi-slash-circle fs-1 text-danger mb-3 d-block"></i>
        <h4 class="card-title mb-1">Acesso negado</h4>
        <p class="text-muted small mb-4">
          Sua conta não tem permissão para acessar o painel de administração.
          Entre em contato com o administrador do sistema.
        </p>
        <a :href="AUTH_LUIZ_URL" class="btn btn-outline-secondary w-100">
          Voltar para o AuthLuiz
        </a>
      </div>
    </div>
  </div>
</template>

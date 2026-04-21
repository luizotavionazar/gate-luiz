<script setup>
import { AUTH_LUIZ_URL } from '../services/api'
</script>

<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light">
    <div class="card shadow-sm text-center" style="width: 420px">
      <div class="card-body p-4">
        <i class="bi bi-shield-lock fs-1 text-secondary mb-3 d-block"></i>
        <h4 class="card-title mb-1">PermLuiz</h4>
        <p class="text-muted small mb-4">Acesso restrito ao administrador</p>

        <p class="text-muted small">
          Faça login no <strong>AuthLuiz</strong> e acesse o painel de permissões diretamente pela sua conta.
        </p>

        <a :href="AUTH_LUIZ_URL" class="btn btn-dark w-100 mt-3">
          Ir para o AuthLuiz
        </a>
      </div>
    </div>
  </div>
</template>

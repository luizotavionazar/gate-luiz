# AuthLuiz Frontend

Frontend em Vue 3 + Vite para a AuthLuiz.

## Variáveis de ambiente

Crie um arquivo `.env` na raiz do frontend com base no `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=seu-client-id-do-google.apps.googleusercontent.com
```

## Rodando em desenvolvimento

```bash
npm install
npm run dev
```

## Fluxos já preparados

- cadastro local
- login local
- login com Google
- vínculo opcional de conta local com Google
- recuperação de senha por e-mail
- visualização da conta autenticada
- alteração de nome
- alteração de e-mail
- troca de senha local
- definição de senha local para contas criadas via Google

import axios from 'axios'

const PERMISSOES_API_URL = import.meta.env.VITE_PERM_API_URL || 'http://localhost:8081'
const setupApi = axios.create({ baseURL: PERMISSOES_API_URL })

export async function obterStatusSetup() {
  const response = await setupApi.get('/setup')
  return response.data
}

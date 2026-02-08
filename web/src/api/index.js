import axios from 'axios'

const API_BASE = '/api'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000
})

export const agentApi = {
  getAllAgents() {
    return api.get('/agents')
  },

  getAgent(id) {
    return api.get(`/agents/${id}`)
  },

  getAgentStatus() {
    return api.get('/agents/status')
  },

  getAgentProcesses(id) {
    return api.get(`/agents/${id}/processes`)
  },

  getAgentPorts(id) {
    return api.get(`/agents/${id}/ports`)
  },

  updateAgent(id, data) {
    return api.put(`/agents/${id}`, data)
  },

  deleteAgent(id) {
    return api.delete(`/agents/${id}`)
  }
}

export const healthApi = {
  check() {
    return api.get('/health')
  }
}

export default api

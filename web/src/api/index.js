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

  getAgentHostInfo(id) {
    return api.get(`/agents/${id}/host-info`)
  },

  getAgentSecuritySoftware(id) {
    return api.get(`/agents/${id}/installed-software`)
  },

  getAgentUsbDevices(id) {
    return api.get(`/agents/${id}/usb-devices`)
  },

  getAgentLoginLogs(id) {
    return api.get(`/agents/${id}/login-logs`)
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

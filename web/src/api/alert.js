import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export const alertApi = {
  getAll() {
    return api.get('/alerts')
  },

  getByStatus(status) {
    return api.get(`/alerts/status/${status}`)
  },

  getStats() {
    return api.get('/alerts/stats')
  },

  getRecent(hours = 24) {
    return api.get('/alerts/recent', { params: { hours } })
  },

  getById(id) {
    return api.get(`/alerts/${id}`)
  },

  getByAgent(agentId) {
    return api.get(`/alerts/agent/${agentId}`)
  },

  getByAgentAndStatus(agentId, status) {
    return api.get(`/alerts/agent/${agentId}/status/${status}`)
  },

  acknowledge(id, acknowledgedBy = 'admin') {
    return api.post(`/alerts/${id}/acknowledge`, { acknowledgedBy })
  },

  resolve(id, resolvedBy = 'admin', resolutionNote = '') {
    return api.post(`/alerts/${id}/resolve`, { resolvedBy, resolutionNote })
  },

  ignore(id, ignoredBy = 'admin', reason = '') {
    return api.post(`/alerts/${id}/ignore`, { ignoredBy, reason })
  },

  getAllRules() {
    return api.get('/alerts/rules')
  },

  getEnabledRules() {
    return api.get('/alerts/rules/enabled')
  },

  getRulesByType(ruleType) {
    return api.get(`/alerts/rules/type/${ruleType}`)
  },

  createRule(ruleData) {
    return api.post('/alerts/rules', ruleData)
  },

  toggleRule(id, enabled) {
    return api.put(`/alerts/rules/${id}/toggle`, { enabled })
  },

  deleteRule(id) {
    return api.delete(`/alerts/rules/${id}`)
  }
}

export default api

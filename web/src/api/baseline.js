import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export const baselineApi = {
  getConfigs(agentId) {
    return api.get(`/baselines/${agentId}`)
  },

  getConfig(agentId, type) {
    return api.get(`/baselines/${agentId}/${type}`)
  },

  startQuickLearn(agentId, type) {
    return api.post(`/baselines/${agentId}/${type}/quick-learn`)
  },

  startStandardLearn(agentId, type) {
    return api.post(`/baselines/${agentId}/${type}/standard-learn`)
  },

  startCustomLearn(agentId, type, days) {
    return api.post(`/baselines/${agentId}/${type}/custom-learn`, { days })
  },

  importFromCurrent(agentId, type) {
    return api.post(`/baselines/${agentId}/${type}/import`)
  },

  copyFromAgent(agentId, type, sourceAgentId) {
    return api.post(`/baselines/${agentId}/${type}/copy`, { sourceAgentId })
  },

  manualCreate(agentId, type, items) {
    return api.post(`/baselines/${agentId}/${type}/manual`, items)
  },

  getSnapshots(agentId, type) {
    return api.get(`/baselines/${agentId}/${type}/snapshots`)
  },

  getItems(agentId, type) {
    return api.get(`/baselines/${agentId}/${type}/items`)
  },

  compare(agentId, type) {
    return api.get(`/baselines/${agentId}/${type}/compare`)
  },

  completeLearning(agentId, type) {
    return api.post(`/baselines/${agentId}/${type}/complete-learn`)
  },

  delete(agentId, type) {
    return api.delete(`/baselines/${agentId}/${type}`)
  }
}

export default api

<template>
  <div class="baseline-manager">
    <div class="page-card">
      <div class="page-title">
        <span>基线管理</span>
        <el-select v-model="selectedAgentId" placeholder="选择Agent" style="width: 200px; margin-left: 20px;">
          <el-option
            v-for="agent in agents"
            :key="agent.id"
            :label="agent.name || agent.id"
            :value="agent.id"
          />
        </el-select>
        <el-button type="primary" style="margin-left: 20px;" @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card" style="margin-top: 20px;" v-if="selectedAgentId">
      <el-tab-pane v-for="type in baselineTypes" :key="type.value" :label="type.label" :name="type.value">
        <BaselineTypePanel
          :agent-id="selectedAgentId"
          :baseline-type="type.value"
          @refresh="loadConfigs"
        />
      </el-tab-pane>
    </el-tabs>

    <el-empty v-else description="请选择Agent" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { agentApi } from '@/api'
import BaselineTypePanel from '@/components/BaselineTypePanel.vue'

const agents = ref([])
const selectedAgentId = ref('')
const loading = ref(false)
const activeTab = ref('PROCESS')

const baselineTypes = [
  { label: '进程', value: 'PROCESS' },
  { label: '端口', value: 'PORT' },
  { label: 'USB设备', value: 'USB' },
  { label: '登录记录', value: 'LOGIN' },
  { label: '软件安装', value: 'SOFTWARE' }
]

const loadAgents = async () => {
  try {
    const res = await agentApi.getAllAgents()
    agents.value = res.data
    if (agents.value.length > 0) {
      selectedAgentId.value = agents.value[0].id
    }
  } catch (e) {
    ElMessage.error('加载Agent列表失败')
  }
}

const loadConfigs = async () => {
  if (!selectedAgentId.value) return
  loading.value = true
  try {
    const res = await baselineApi.getConfigs(selectedAgentId.value)
    console.log('Baseline configs loaded:', res.data)
  } catch (e) {
    console.error('加载基线配置失败:', e)
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadConfigs()
}

onMounted(() => {
  loadAgents()
})
</script>

<style scoped>
.baseline-manager {
  padding: 20px;
}
</style>

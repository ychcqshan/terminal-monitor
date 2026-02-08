<template>
  <div class="agent-detail">
    <div class="page-card" v-if="agent">
      <div class="page-title">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span style="margin-left: 10px;">{{ agent.name }} - 详细信息</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Agent ID">{{ agent.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="agent.status === 'online' ? 'success' : 'danger'">
            {{ agent.status === 'online' ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="平台">{{ agent.platform }}</el-descriptions-item>
        <el-descriptions-item label="主机名">{{ agent.hostname }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ agent.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatTime(agent.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">进程信息 ({{ processes.length }})</div>
          <el-table :data="processes" max-height="400" v-loading="loading">
            <el-table-column prop="pid" label="PID" width="80" />
            <el-table-column prop="name" label="进程名" />
            <el-table-column prop="cpuPercent" label="CPU%" width="80">
              <template #default="{ row }">
                {{ row.cpuPercent?.toFixed(1) }}%
              </template>
            </el-table-column>
            <el-table-column prop="memoryPercent" label="内存%" width="90">
              <template #default="{ row }">
                {{ row.memoryPercent?.toFixed(1) }}%
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">端口信息 ({{ ports.length }})</div>
          <el-table :data="ports" max-height="400" v-loading="loading">
            <el-table-column prop="port" label="端口" width="80" />
            <el-table-column prop="protocol" label="协议" width="70" />
            <el-table-column prop="status" label="状态" width="80" />
            <el-table-column prop="processName" label="进程名" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const agent = ref(null)
const processes = ref([])
const ports = ref([])

const agentId = route.params.id

const fetchData = async () => {
  loading.value = true
  try {
    const [agentRes, processesRes, portsRes] = await Promise.all([
      agentApi.getAgent(agentId),
      agentApi.getAgentProcesses(agentId),
      agentApi.getAgentPorts(agentId)
    ])
    agent.value = agentRes.data
    processes.value = processesRes.data
    ports.value = portsRes.data
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const goBack = () => {
  router.push('/agents')
}

let timer
onMounted(() => {
  fetchData()
  timer = setInterval(fetchData, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

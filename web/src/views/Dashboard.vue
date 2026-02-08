<template>
  <div class="dashboard">
    <div class="page-card">
      <div class="page-title">系统总览</div>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="stat-card success">
            <div class="stat-number">{{ status.online }}</div>
            <div class="stat-label">在线Agent</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card">
            <div class="stat-number">{{ status.total }}</div>
            <div class="stat-label">总Agent数</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card warning">
            <div class="stat-number">{{ status.offline }}</div>
            <div class="stat-label">离线Agent</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <div class="page-title">最近在线Agent</div>
      <el-table :data="agents" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="platform" label="平台" />
        <el-table-column prop="hostname" label="主机名" />
        <el-table-column prop="ipAddress" label="IP地址" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'online' ? 'success' : 'danger'">
              {{ row.status === 'online' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后更新">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { agentApi } from '../api'

const router = useRouter()
const loading = ref(false)
const agents = ref([])
const status = ref({ online: 0, offline: 0, total: 0 })

const fetchData = async () => {
  loading.value = true
  try {
    const [agentsRes, statusRes] = await Promise.all([
      agentApi.getAllAgents(),
      agentApi.getAgentStatus()
    ])
    agents.value = agentsRes.data
    status.value = statusRes.data
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

const viewDetail = (id) => {
  router.push(`/agents/${id}`)
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

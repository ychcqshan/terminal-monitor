<template>
  <div class="login-logs">
    <div class="page-card">
      <div class="page-title">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span style="margin-left: 10px;">{{ agentName }} - 登录日志</span>
      </div>
      <div v-loading="loading" style="min-height: 200px;">
        <el-table :data="loginLogsList" max-height="500" v-if="loginLogsList.length > 0">
          <el-table-column prop="loginTime" label="登录时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.loginTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="logoutTime" label="登出时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.logoutTime) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="loginType" label="登录类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getLoginTypeLabel(row.loginType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="loginIp" label="登录IP" width="140" />
          <el-table-column prop="loginStatus" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.loginStatus === 'success' ? 'success' : 'danger'" size="small">
                {{ row.loginStatus === 'success' ? '成功' : row.loginStatus || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="120" show-overflow-tooltip />
          <el-table-column prop="sessionId" label="会话ID" width="150" show-overflow-tooltip />
        </el-table>
        <el-empty v-else description="暂无登录日志" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const agentName = ref('')
const loginLogsList = ref([])
const agentId = route.params.id

const fetchData = async () => {
  loading.value = true
  try {
    const [agentRes, loginLogsRes] = await Promise.all([
      agentApi.getAgent(agentId),
      agentApi.getAgentLoginLogs(agentId)
    ])
    agentName.value = agentRes.data.name
    loginLogsList.value = loginLogsRes.data || []
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return null
  return new Date(time).toLocaleString('zh-CN')
}

const getLoginTypeLabel = (type) => {
  const typeMap = {
    'local': '本地登录',
    'remote': '远程登录',
    'ssh': 'SSH',
    'rdp': 'RDP',
    'console': '控制台',
    'other': '其他'
  }
  return typeMap[type] || type || '未知'
}

const goBack = () => {
  router.push(`/agents/${agentId}`)
}

let timer
onMounted(() => {
  fetchData()
  timer = setInterval(fetchData, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

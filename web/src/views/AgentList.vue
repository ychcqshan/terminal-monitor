<template>
  <div class="agent-list">
    <div class="page-card">
      <div class="page-title">Agent列表</div>
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
        <el-table-column prop="createdAt" label="注册时间">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后更新">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">
              详情
            </el-button>
            <el-button type="info" link @click="refreshAgent(row.id)">
              刷新
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
import { ElMessage } from 'element-plus'
import { agentApi } from '../api'

const router = useRouter()
const loading = ref(false)
const agents = ref([])

const fetchAgents = async () => {
  loading.value = true
  try {
    const response = await agentApi.getAllAgents()
    agents.value = response.data
  } catch (error) {
    ElMessage.error('获取Agent列表失败')
    console.error(error)
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

const refreshAgent = async (id) => {
  try {
    await agentApi.getAgent(id)
    ElMessage.success('刷新成功')
    fetchAgents()
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}

let timer
onMounted(() => {
  fetchAgents()
  timer = setInterval(fetchAgents, 15000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

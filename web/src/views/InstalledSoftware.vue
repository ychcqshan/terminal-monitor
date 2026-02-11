<template>
  <div class="installed-software">
    <div class="page-card">
      <div class="page-title">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span style="margin-left: 10px;">{{ agentName }} - 已安装软件</span>
      </div>
      <div style="margin-bottom: 10px; color: #909399;">
        共 {{ installedSoftwareList.length }} 个软件
      </div>
      <div v-loading="loading" style="min-height: 200px;">
        <el-table :data="installedSoftwareList" max-height="500" v-if="installedSoftwareList.length > 0">
          <el-table-column prop="softwareName" label="软件名称" width="250" show-overflow-tooltip />
          <el-table-column prop="softwareType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getInstalledSoftwareTypeLabel(row.softwareType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="100" />
          <el-table-column prop="publisher" label="发行商" width="150" show-overflow-tooltip />
          <el-table-column prop="installDate" label="安装日期" width="120" />
          <el-table-column prop="size" label="大小(KB)" width="100">
            <template #default="{ row }">
              {{ row.size ? row.size + ' KB' : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getSoftwareSourceLabel(row.source) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="installLocation" label="安装路径" show-overflow-tooltip />
        </el-table>
        <el-empty v-else description="暂无已安装软件信息" />
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
const installedSoftwareList = ref([])
const agentId = route.params.id

const fetchData = async () => {
  loading.value = true
  try {
    const [agentRes, installedSoftwareRes] = await Promise.all([
      agentApi.getAgent(agentId),
      agentApi.getAgentSecuritySoftware(agentId)
    ])
    agentName.value = agentRes.data.name
    installedSoftwareList.value = installedSoftwareRes.data || []
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const getInstalledSoftwareTypeLabel = (type) => {
  const typeMap = {
    'development': '开发工具',
    'database': '数据库',
    'web_server': 'Web服务器',
    'security': '安全工具',
    'media': '媒体工具',
    'office': '办公软件',
    'browser': '浏览器',
    'communication': '通讯工具',
    'virtualization': '虚拟化',
    'backup': '备份工具',
    'system': '系统工具',
    'network': '网络工具',
    'application': '应用程序',
    'python_package': 'Python包',
    'node_package': 'Node包',
    'package_manager': '包管理器'
  }
  return typeMap[type] || type || '未知'
}

const getSoftwareSourceLabel = (source) => {
  const sourceMap = {
    'windows_registry': 'Windows',
    'windows_registry_64': 'Windows(64位)',
    'chocolatey': 'Chocolatey',
    'dpkg': 'Debian/Ubuntu',
    'rpm': 'RedHat/CentOS',
    'pip': 'Python PIP',
    'npm': 'Node NPM'
  }
  return sourceMap[source] || source || '未知'
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

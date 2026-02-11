<template>
  <div class="agent-detail">
    <div class="page-card" v-if="agent">
      <div class="page-title">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span style="margin-left: 10px;">{{ agent.name }} - 详细信息</span>
        <el-button type="primary" link style="margin-left: auto;" @click="refreshData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
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

    <el-tabs v-model="activeTab" type="border-card" style="margin-top: 20px;">
      <el-tab-pane label="进程信息" name="processes">
        <div class="tab-header">
          <span>共 {{ processes.length }} 个进程</span>
        </div>
        <el-table :data="paginatedProcesses" max-height="400" v-loading="loading">
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
        <el-pagination
          v-if="processes.length > pageSize"
          v-model:current-page="processesPage"
          :page-size="pageSize"
          :total="processes.length"
          layout="prev, pager, next"
          class="pagination"
        />
        <div v-if="processes.length === 0 && !loading" class="empty-tip">
          暂无进程信息
        </div>
      </el-tab-pane>

      <el-tab-pane label="端口信息" name="ports">
        <div class="tab-header">
          <span>共 {{ ports.length }} 个端口</span>
        </div>
        <el-table :data="paginatedPorts" max-height="400" v-loading="loading">
          <el-table-column prop="port" label="端口" width="80" />
          <el-table-column prop="protocol" label="协议" width="70" />
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column prop="processName" label="进程名" />
        </el-table>
        <el-pagination
          v-if="ports.length > pageSize"
          v-model:current-page="portsPage"
          :page-size="pageSize"
          :total="ports.length"
          layout="prev, pager, next"
          class="pagination"
        />
        <div v-if="ports.length === 0 && !loading" class="empty-tip">
          暂无端口信息
        </div>
      </el-tab-pane>

      <el-tab-pane label="主机信息" name="hostInfo">
        <div v-loading="loading">
          <el-descriptions :column="2" border v-if="hostInfo">
            <el-descriptions-item label="CPU 品牌">{{ hostInfo.cpuBrand || '-' }}</el-descriptions-item>
            <el-descriptions-item label="CPU 核心数">{{ hostInfo.cpuCores || '-' }}</el-descriptions-item>
            <el-descriptions-item label="操作系统">{{ hostInfo.osName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="系统版本">{{ hostInfo.osVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="总内存">{{ formatBytes(hostInfo.memoryTotal) }}</el-descriptions-item>
            <el-descriptions-item label="内存使用率">{{ hostInfo.memoryPercent ? hostInfo.memoryPercent.toFixed(1) + '%' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="总存储">{{ formatBytes(hostInfo.storageTotal) }}</el-descriptions-item>
          <el-descriptions-item label="存储设备">{{ formatStorageDevices(hostInfo.storageDevices) }}</el-descriptions-item>
          <el-descriptions-item label="主板型号">{{ hostInfo.motherboardModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="BIOS 版本">{{ hostInfo.biosVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="采集时间">{{ formatTime(hostInfo.collectedAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无主机信息" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="已安装软件" name="installedSoftware">
        <div class="tab-header">
          <span>共 {{ installedSoftware.length }} 个软件</span>
        </div>
        <el-table :data="paginatedSoftware" max-height="400" v-loading="loading">
          <el-table-column prop="softwareName" label="软件名称" width="250" show-overflow-tooltip />
          <el-table-column prop="softwareType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getInstalledSoftwareTypeLabel(row.softwareType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="100" />
          <el-table-column prop="publisher" label="发行商" width="150" show-overflow-tooltip />
          <el-table-column prop="installDate" label="安装日期" width="120" />
          <el-table-column prop="source" label="来源" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getSoftwareSourceLabel(row.source) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="installedSoftware.length > pageSize"
          v-model:current-page="softwarePage"
          :page-size="pageSize"
          :total="installedSoftware.length"
          layout="prev, pager, next"
          class="pagination"
        />
        <div v-if="installedSoftware.length === 0 && !loading" class="empty-tip">
          暂无已安装软件信息
        </div>
      </el-tab-pane>

      <el-tab-pane label="USB 设备" name="usbDevices">
        <div class="tab-header">
          <span>共 {{ usbDevices.length }} 个设备</span>
        </div>
        <el-table :data="paginatedUsbDevices" max-height="400" v-loading="loading">
          <el-table-column prop="name" label="设备名称" width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="设备类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getDeviceTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="manufacturer" label="制造商" width="150" />
          <el-table-column prop="serial_number" label="序列号" width="150" show-overflow-tooltip />
          <el-table-column prop="version" label="版本" width="100" />
          <el-table-column prop="plugged_time" label="插入时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.plugged_time) }}
            </template>
          </el-table-column>
          <el-table-column prop="vendor_id" label="VID" width="80" />
          <el-table-column prop="product_id" label="PID" width="80" />
        </el-table>
        <el-pagination
          v-if="usbDevices.length > pageSize"
          v-model:current-page="usbPage"
          :page-size="pageSize"
          :total="usbDevices.length"
          layout="prev, pager, next"
          class="pagination"
        />
        <div v-if="usbDevices.length === 0 && !loading" class="empty-tip">
          暂无 USB 设备信息
        </div>
      </el-tab-pane>

      <el-tab-pane label="登录日志" name="loginLogs">
        <div class="tab-header">
          <span>共 {{ loginLogs.length }} 条记录</span>
        </div>
        <el-table :data="paginatedLoginLogs" max-height="400" v-loading="loading">
          <el-table-column prop="loginTime" label="登录时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.loginTime) }}
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
        </el-table>
        <el-pagination
          v-if="loginLogs.length > pageSize"
          v-model:current-page="loginPage"
          :page-size="pageSize"
          :total="loginLogs.length"
          layout="prev, pager, next"
          class="pagination"
        />
        <div v-if="loginLogs.length === 0 && !loading" class="empty-tip">
          暂无登录日志
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi } from '../api'
import { Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const activeTab = ref('processes')
const loading = ref(false)
const agent = ref(null)
const processes = ref([])
const ports = ref([])
const hostInfo = ref(null)
const installedSoftware = ref([])
const usbDevices = ref([])
const loginLogs = ref([])

const pageSize = ref(20)
const processesPage = ref(1)
const portsPage = ref(1)
const softwarePage = ref(1)
const usbPage = ref(1)
const loginPage = ref(1)

const agentId = route.params.id

const paginatedProcesses = computed(() => {
  const start = (processesPage.value - 1) * pageSize.value
  return processes.value.slice(start, start + pageSize.value)
})

const paginatedPorts = computed(() => {
  const start = (portsPage.value - 1) * pageSize.value
  return ports.value.slice(start, start + pageSize.value)
})

const paginatedSoftware = computed(() => {
  const start = (softwarePage.value - 1) * pageSize.value
  return installedSoftware.value.slice(start, start + pageSize.value)
})

const paginatedUsbDevices = computed(() => {
  const start = (usbPage.value - 1) * pageSize.value
  return usbDevices.value.slice(start, start + pageSize.value)
})

const paginatedLoginLogs = computed(() => {
  const start = (loginPage.value - 1) * pageSize.value
  return loginLogs.value.slice(start, start + pageSize.value)
})

const fetchData = async () => {
  loading.value = true
  try {
    const [agentRes, processesRes, portsRes, hostInfoRes, installedSoftwareRes, usbDevicesRes, loginLogsRes] = await Promise.all([
      agentApi.getAgent(agentId),
      agentApi.getAgentProcesses(agentId),
      agentApi.getAgentPorts(agentId),
      agentApi.getAgentHostInfo(agentId),
      agentApi.getAgentSecuritySoftware(agentId),
      agentApi.getAgentUsbDevices(agentId),
      agentApi.getAgentLoginLogs(agentId)
    ])
    agent.value = agentRes.data
    processes.value = processesRes.data || []
    ports.value = portsRes.data || []
    const hostInfoData = hostInfoRes.data || []
    hostInfo.value = hostInfoData.length > 0 ? hostInfoData[0] : null
    installedSoftware.value = installedSoftwareRes.data || []
    usbDevices.value = usbDevicesRes.data || []
    loginLogs.value = loginLogsRes.data || []

    processesPage.value = 1
    portsPage.value = 1
    softwarePage.value = 1
    usbPage.value = 1
    loginPage.value = 1
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchData()
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatBytes = (bytes) => {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  if (gb >= 1) {
    return gb.toFixed(2) + ' GB'
  }
  const mb = bytes / (1024 * 1024)
  return mb.toFixed(2) + ' MB'
}

const formatStorageDevices = (devices) => {
  if (!devices) return '-'
  try {
    const parsed = typeof devices === 'string' ? JSON.parse(devices) : devices
    if (Array.isArray(parsed)) {
      return parsed.filter(d => d).join(', ') || '-'
    }
    return String(devices)
  } catch {
    return String(devices)
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

const getDeviceTypeLabel = (type) => {
  const typeMap = {
    'flash_drive': 'U盘',
    'external_hdd': '移动硬盘',
    'mouse': '鼠标',
    'keyboard': '键盘',
    'printer': '打印机',
    'camera': '摄像头',
    'phone': '手机',
    'tablet': '平板',
    'other': '其他'
  }
  return typeMap[type] || type || '未知'
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
  router.push('/agents')
}

let timer
onMounted(() => {
  fetchData()
  timer = setInterval(fetchData, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.tab-header {
  margin-bottom: 10px;
  color: #909399;
  font-size: 14px;
}

.pagination {
  justify-content: center;
  margin-top: 15px;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #909399;
}
</style>

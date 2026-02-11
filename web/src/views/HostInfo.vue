<template>
  <div class="host-info">
    <div class="page-card">
      <div class="page-title">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span style="margin-left: 10px;">{{ agentName }} - 主机信息</span>
      </div>
      <div v-if="loading" v-loading="loading" style="min-height: 200px;"></div>
      <div v-else-if="hostInfo">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="CPU 品牌">{{ hostInfo.cpuBrand || '-' }}</el-descriptions-item>
          <el-descriptions-item label="CPU 架构">{{ hostInfo.cpuArch || '-' }}</el-descriptions-item>
          <el-descriptions-item label="CPU 核心数">{{ hostInfo.cpuCores || '-' }}</el-descriptions-item>
          <el-descriptions-item label="CPU 线程数">{{ hostInfo.cpuThreads || '-' }}</el-descriptions-item>
          <el-descriptions-item label="CPU 频率">{{ hostInfo.cpuFrequency ? hostInfo.cpuFrequency + ' GHz' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作系统">{{ hostInfo.osName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="系统版本">{{ hostInfo.osVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="系统架构">{{ hostInfo.osArch || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内核版本">{{ hostInfo.kernelVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主板型号">{{ hostInfo.motherboardModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主板序列号">{{ hostInfo.motherboardSerial || '-' }}</el-descriptions-item>
          <el-descriptions-item label="BIOS 版本">{{ hostInfo.biosVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总内存">{{ formatBytes(hostInfo.memoryTotal) }}</el-descriptions-item>
          <el-descriptions-item label="可用内存">{{ formatBytes(hostInfo.memoryAvailable) }}</el-descriptions-item>
          <el-descriptions-item label="内存使用率">{{ hostInfo.memoryPercent ? hostInfo.memoryPercent.toFixed(1) + '%' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="总存储">{{ formatBytes(hostInfo.storageTotal) }}</el-descriptions-item>
          <el-descriptions-item label="MAC 地址" :span="2">
            <div v-if="hostInfo.macAddresses">
              <div v-for="(mac, index) in parseJsonArray(hostInfo.macAddresses)" :key="index" style="margin: 2px 0;">
                {{ mac }}
              </div>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="IP 地址" :span="2">
            <div v-if="hostInfo.ipAddresses">
              <div v-for="(ip, index) in parseJsonArray(hostInfo.ipAddresses)" :key="index" style="margin: 2px 0;">
                {{ ip }}
              </div>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="存储设备" :span="2">
            <div v-if="hostInfo.storageDevices">
              <div v-for="(device, index) in parseJsonArray(hostInfo.storageDevices)" :key="index" style="margin: 2px 0;">
                {{ device }}
              </div>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="采集时间">{{ formatTime(hostInfo.collectedAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="暂无主机信息" />
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
const hostInfo = ref(null)
const agentId = route.params.id

const fetchData = async () => {
  loading.value = true
  try {
    const [agentRes, hostInfoRes] = await Promise.all([
      agentApi.getAgent(agentId),
      agentApi.getAgentHostInfo(agentId)
    ])
    agentName.value = agentRes.data.name
    if (hostInfoRes.data && hostInfoRes.data.length > 0) {
      hostInfo.value = hostInfoRes.data[0]
    } else {
      hostInfo.value = null
    }
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

const formatBytes = (bytes) => {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  if (gb >= 1) {
    return gb.toFixed(2) + ' GB'
  }
  const mb = bytes / (1024 * 1024)
  return mb.toFixed(2) + ' MB'
}

const parseJsonArray = (json) => {
  if (!json) return []
  try {
    return JSON.parse(json)
  } catch (e) {
    return [json]
  }
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

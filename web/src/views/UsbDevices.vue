<template>
  <div class="usb-devices">
    <div class="page-card">
      <div class="page-title">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span style="margin-left: 10px;">{{ agentName }} - USB 设备</span>
      </div>
      <div v-loading="loading" style="min-height: 200px;">
        <el-table :data="usbDevicesList" max-height="500" v-if="usbDevicesList.length > 0">
          <el-table-column prop="deviceName" label="设备名称" width="200" show-overflow-tooltip />
          <el-table-column prop="deviceType" label="设备类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getDeviceTypeLabel(row.deviceType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="manufacturer" label="制造商" width="150" />
          <el-table-column prop="vendorId" label="Vendor ID" width="100" />
          <el-table-column prop="productId" label="Product ID" width="100" />
          <el-table-column prop="serialNumber" label="序列号" width="150" show-overflow-tooltip />
          <el-table-column prop="pluggedTime" label="插入时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.pluggedTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="collectedAt" label="采集时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.collectedAt) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无 USB 设备信息" />
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
const usbDevicesList = ref([])
const agentId = route.params.id

const fetchData = async () => {
  loading.value = true
  try {
    const [agentRes, usbDevicesRes] = await Promise.all([
      agentApi.getAgent(agentId),
      agentApi.getAgentUsbDevices(agentId)
    ])
    agentName.value = agentRes.data.name
    usbDevicesList.value = usbDevicesRes.data || []
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

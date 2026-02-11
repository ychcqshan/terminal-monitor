<template>
  <div class="alert-center">
    <div class="page-card">
      <div class="page-title">
        <span>告警中心</span>
        <el-button type="primary" link @click="refreshData" :loading="loading" style="margin-left: auto;">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>

      <div class="stats-cards">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value warning">{{ stats.totalNew }}</div>
          <div class="stat-label">待处理</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value info">{{ stats.totalAcknowledged }}</div>
          <div class="stat-label">已确认</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value success">{{ stats.totalResolved }}</div>
          <div class="stat-label">已解决</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value danger">{{ stats.criticalUnresolved }}</div>
          <div class="stat-label">严重告警</div>
        </el-card>
      </div>

      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 150px;">
          <el-option label="待处理" value="NEW" />
          <el-option label="已确认" value="ACKNOWLEDGED" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已忽略" value="IGNORED" />
        </el-select>
        <el-select v-model="filterLevel" placeholder="级别筛选" clearable style="width: 150px; margin-left: 10px;">
          <el-option label="严重" value="CRITICAL" />
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
      </div>

      <el-table :data="filteredAlerts" max-height="500" v-loading="loading" @row-click="handleRowClick">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="alertLevel" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.alertLevel)" size="small">{{ getLevelText(row.alertLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.alertStatus)" size="small">{{ getStatusText(row.alertStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertType" label="类型" width="100" />
        <el-table-column prop="alertTitle" label="标题" show-overflow-tooltip />
        <el-table-column prop="agentId" label="Agent" width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="handleAcknowledge(row)"
                       v-if="row.alertStatus === 'NEW'">确认</el-button>
            <el-button type="success" link size="small" @click.stop="handleResolve(row)"
                       v-if="row.alertStatus !== 'RESOLVED'">解决</el-button>
            <el-button type="warning" link size="small" @click.stop="handleIgnore(row)"
                       v-if="row.alertStatus === 'NEW'">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="showDetail" title="告警详情" size="50%">
      <div class="alert-detail" v-if="selectedAlert">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ selectedAlert.id }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="getLevelType(selectedAlert.alertLevel)">{{ selectedAlert.alertLevel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedAlert.alertStatus)">{{ selectedAlert.alertStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ selectedAlert.alertType }}</el-descriptions-item>
          <el-descriptions-item label="Agent">{{ selectedAlert.agentId }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatTime(selectedAlert.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ selectedAlert.alertTitle }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">{{ selectedAlert.alertContent }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-actions" v-if="selectedAlert.alertStatus !== 'RESOLVED'">
          <el-button type="primary" @click="handleAcknowledge(selectedAlert)"
                     v-if="selectedAlert.alertStatus === 'NEW'">确认告警</el-button>
          <el-button type="success" @click="handleResolve(selectedAlert)">解决告警</el-button>
          <el-button type="warning" @click="handleIgnore(selectedAlert)">忽略告警</el-button>
        </div>

        <div class="resolution-info" v-if="selectedAlert.resolvedAt">
          <h4>解决信息</h4>
          <p>解决人: {{ selectedAlert.resolvedBy }}</p>
          <p>解决时间: {{ formatTime(selectedAlert.resolvedAt) }}</p>
          <p>备注: {{ selectedAlert.resolutionNote }}</p>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="showResolveDialog" title="解决告警" width="500px">
      <el-form label-width="80px">
        <el-form-item label="解决人">
          <el-input v-model="resolveForm.resolvedBy" placeholder="请输入解决人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resolveForm.resolutionNote" type="textarea" rows="3" placeholder="请输入解决备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResolveDialog = false">取消</el-button>
        <el-button type="primary" @click="submitResolve">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showIgnoreDialog" title="忽略告警" width="500px">
      <el-form label-width="80px">
        <el-form-item label="忽略人">
          <el-input v-model="ignoreForm.ignoredBy" placeholder="请输入忽略人" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="ignoreForm.reason" type="textarea" rows="3" placeholder="请输入忽略原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showIgnoreDialog = false">取消</el-button>
        <el-button type="primary" @click="submitIgnore">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { alertApi } from '@/api'

const loading = ref(false)
const alerts = ref([])
const stats = ref({
  totalNew: 0,
  totalAcknowledged: 0,
  totalResolved: 0,
  totalIgnored: 0,
  criticalUnresolved: 0
})
const filterStatus = ref('')
const filterLevel = ref('')
const showDetail = ref(false)
const selectedAlert = ref(null)
const showResolveDialog = ref(false)
const showIgnoreDialog = ref(false)
const resolveForm = ref({ resolvedBy: '', resolutionNote: '' })
const ignoreForm = ref({ ignoredBy: '', reason: '' })

const filteredAlerts = computed(() => {
  let result = alerts.value
  if (filterStatus.value) {
    result = result.filter(a => a.alertStatus === filterStatus.value)
  }
  if (filterLevel.value) {
    result = result.filter(a => a.alertLevel === filterLevel.value)
  }
  return result
})

const loadData = async () => {
  loading.value = true
  try {
    const [alertsRes, statsRes] = await Promise.all([
      alertApi.getAll(),
      alertApi.getStats()
    ])
    alerts.value = alertsRes.data
    stats.value = statsRes.data
  } catch (e) {
    ElMessage.error('加载告警数据失败')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadData()
}

const handleRowClick = (row) => {
  selectedAlert.value = row
  showDetail.value = true
}

const handleAcknowledge = async (alert) => {
  try {
    await ElMessageBox.confirm('确定要确认此告警吗？', '确认')
    await alertApi.acknowledge(alert.id)
    ElMessage.success('确认成功')
    loadData()
    if (showDetail.value && selectedAlert.value?.id === alert.id) {
      showDetail.value = false
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('确认失败')
    }
  }
}

const handleResolve = (alert) => {
  selectedAlert.value = alert
  resolveForm.value = { resolvedBy: '', resolutionNote: '' }
  showResolveDialog.value = true
}

const submitResolve = async () => {
  try {
    await alertApi.resolve(selectedAlert.value.id, resolveForm.value.resolvedBy, resolveForm.value.resolutionNote)
    ElMessage.success('解决成功')
    showResolveDialog.value = false
    showDetail.value = false
    loadData()
  } catch (e) {
    ElMessage.error('解决失败')
  }
}

const handleIgnore = (alert) => {
  selectedAlert.value = alert
  ignoreForm.value = { ignoredBy: '', reason: '' }
  showIgnoreDialog.value = true
}

const submitIgnore = async () => {
  try {
    await alertApi.ignore(selectedAlert.value.id, ignoreForm.value.ignoredBy, ignoreForm.value.reason)
    ElMessage.success('忽略成功')
    showIgnoreDialog.value = false
    showDetail.value = false
    loadData()
  } catch (e) {
    ElMessage.error('忽略失败')
  }
}

const getLevelType = (level) => {
  const map = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: 'info', LOW: 'info' }
  return map[level] || 'info'
}

const getLevelText = (level) => {
  const map = { CRITICAL: '严重', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[level] || level
}

const getStatusType = (status) => {
  const map = { NEW: 'warning', ACKNOWLEDGED: 'info', RESOLVED: 'success', IGNORED: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { NEW: '待处理', ACKNOWLEDGED: '已确认', RESOLVED: '已解决', IGNORED: '已忽略' }
  return map[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.alert-center {
  padding: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-value.warning { color: #e6a23c; }
.stat-value.info { color: #909399; }
.stat-value.success { color: #67c23a; }
.stat-value.danger { color: #f56c6c; }

.stat-label {
  color: #666;
  margin-top: 5px;
}

.filter-bar {
  margin-bottom: 20px;
}

.detail-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.resolution-info {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.resolution-info h4 {
  margin-bottom: 10px;
}

.resolution-info p {
  margin: 5px 0;
  color: #666;
}
</style>

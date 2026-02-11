<template>
  <div class="baseline-type-panel" v-loading="loading">
    <div class="panel-header">
      <div class="config-info" v-if="config">
        <el-tag :type="statusType">{{ statusText }}</el-tag>
        <span class="config-mode" v-if="config.learningMode">
          {{ modeText }} - {{ config.learningDays || 0 }}天
        </span>
        <span class="config-time" v-if="config.learnStart">
          {{ formatTime(config.learnStart) }} ~ {{ formatTime(config.learnEnd) }}
        </span>
      </div>
      <div class="actions">
        <el-dropdown split-button type="primary" @command="handleLearnCommand">
          学习模式
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="quick">快速学习 (1小时)</el-dropdown-item>
              <el-dropdown-item command="standard">标准学习 (7天)</el-dropdown-item>
              <el-dropdown-item command="custom">自定义学习</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="success" @click="handleImport">导入当前</el-button>
        <el-button @click="handleCompare" :disabled="!config">对比基线</el-button>
        <el-button type="danger" @click="handleDelete" :disabled="!config" plain>删除基线</el-button>
      </div>
    </div>

    <div class="snapshots-section" v-if="snapshots.length > 0">
      <h4>基线快照</h4>
      <el-table :data="snapshots" max-height="300" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="itemCount" label="项目数" width="80" />
        <el-table-column prop="snapshotHash" label="Hash" width="120">
          <template #default="{ row }">
            {{ row.snapshotHash?.substring(0, 16) }}...
          </template>
        </el-table-column>
        <el-table-column prop="validFrom" label="生效时间">
          <template #default="{ row }">
            {{ formatTime(row.validFrom) }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="items-section" v-if="items.length > 0">
      <h4>基线项目 ({{ items.length }})</h4>
      <el-table :data="items" max-height="400" size="small">
        <el-table-column prop="itemKey" label="项目键" />
        <el-table-column prop="itemValue" label="项目值" show-overflow-tooltip />
        <el-table-column prop="itemType" label="类型" width="80" />
      </el-table>
    </div>

    <el-empty v-if="!config && !loading" description="暂未创建基线" />

    <el-dialog v-model="showCompareDialog" title="基线对比结果" width="80%">
      <div class="compare-stats">
        <el-tag type="warning">新增 {{ compareResult.newItemsCount }}</el-tag>
        <el-tag type="danger">缺失 {{ compareResult.missingItemsCount }}</el-tag>
        <el-tag type="info">修改 {{ compareResult.modifiedItemsCount }}</el-tag>
      </div>

      <el-tabs v-model="compareTab">
        <el-tab-pane label="新增项目" v-if="compareResult.newItems?.length > 0">
          <el-table :data="compareResult.newItems" max-height="300">
            <el-table-column prop="itemKey" label="项目键" />
            <el-table-column prop="currentValue" label="当前值" show-overflow-tooltip />
            <el-table-column prop="alertLevel" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.alertLevel)" size="small">{{ row.alertLevel }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="缺失项目" v-if="compareResult.missingItems?.length > 0">
          <el-table :data="compareResult.missingItems" max-height="300">
            <el-table-column prop="itemKey" label="项目键" />
            <el-table-column prop="baselineValue" label="基线值" show-overflow-tooltip />
            <el-table-column prop="alertLevel" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.alertLevel)" size="small">{{ row.alertLevel }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="修改项目" v-if="compareResult.modifiedItems?.length > 0">
          <el-table :data="compareResult.modifiedItems" max-height="300">
            <el-table-column prop="itemKey" label="项目键" />
            <el-table-column prop="currentValue" label="当前值" show-overflow-tooltip />
            <el-table-column prop="baselineValue" label="基线值" show-overflow-tooltip />
            <el-table-column prop="alertLevel" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.alertLevel)" size="small">{{ row.alertLevel }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="showCustomDaysDialog" title="自定义学习天数" width="400px">
      <el-form label-width="100px">
        <el-form-item label="学习天数">
          <el-input-number v-model="customDays" :min="1" :max="30" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCustomDaysDialog = false">取消</el-button>
        <el-button type="primary" @click="startCustomLearn">开始学习</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { baselineApi } from '@/api'

const props = defineProps({
  agentId: String,
  baselineType: String
})

const emit = defineEmits(['refresh'])

const loading = ref(false)
const config = ref(null)
const snapshots = ref([])
const items = ref([])
const showCompareDialog = ref(false)
const compareResult = ref({})
const compareTab = ref('new')
const showCustomDaysDialog = ref(false)
const customDays = ref(7)

const statusType = computed(() => {
  if (!config.value) return 'info'
  const status = config.value.status
  if (status === 'LEARNING') return 'warning'
  if (status === 'ACTIVE') return 'success'
  return 'info'
})

const statusText = computed(() => {
  if (!config.value) return '未创建'
  const status = config.value.status
  if (status === 'LEARNING') return '学习中'
  if (status === 'ACTIVE') return '已激活'
  return status
})

const modeText = computed(() => {
  if (!config.value) return ''
  const mode = config.value.learningMode
  if (mode === 'QUICK') return '快速'
  if (mode === 'STANDARD') return '标准'
  if (mode === 'CUSTOM') return '自定义'
  if (mode === 'MANUAL') return '手动'
  return mode
})

const loadData = async () => {
  if (!props.agentId || !props.baselineType) return
  loading.value = true
  try {
    const [configRes, snapshotsRes] = await Promise.all([
      baselineApi.getConfig(props.agentId, props.baselineType),
      baselineApi.getSnapshots(props.agentId, props.baselineType)
    ])
    config.value = configRes.data
    snapshots.value = snapshotsRes.data

    if (snapshots.value.length > 0) {
      const itemsRes = await baselineApi.getItems(props.agentId, props.baselineType)
      items.value = itemsRes.data
    } else {
      items.value = []
    }
  } catch (e) {
    config.value = null
    snapshots.value = []
    items.value = []
  } finally {
    loading.value = false
  }
}

const handleLearnCommand = async (command) => {
  if (command === 'custom') {
    showCustomDaysDialog.value = true
    return
  }

  try {
    loading.value = true
    if (command === 'quick') {
      await baselineApi.startQuickLearn(props.agentId, props.baselineType)
      ElMessage.success('快速学习已启动')
    } else if (command === 'standard') {
      await baselineApi.startStandardLearn(props.agentId, props.baselineType)
      ElMessage.success('标准学习已启动')
    }
    await loadData()
    emit('refresh')
  } catch (e) {
    ElMessage.error('启动学习失败')
  } finally {
    loading.value = false
  }
}

const startCustomLearn = async () => {
  try {
    loading.value = true
    showCustomDaysDialog.value = false
    await baselineApi.startCustomLearn(props.agentId, props.baselineType, customDays.value)
    ElMessage.success(`自定义学习已启动 (${customDays.value}天)`)
    await loadData()
    emit('refresh')
  } catch (e) {
    ElMessage.error('启动学习失败')
  } finally {
    loading.value = false
  }
}

const handleImport = async () => {
  try {
    await ElMessageBox.confirm('确定要导入当前数据作为基线吗？', '确认')
    loading.value = true
    await baselineApi.importFromCurrent(props.agentId, props.baselineType)
    ElMessage.success('导入成功')
    await loadData()
    emit('refresh')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('导入失败')
    }
  } finally {
    loading.value = false
  }
}

const handleCompare = async () => {
  try {
    loading.value = true
    const res = await baselineApi.compare(props.agentId, props.baselineType)
    compareResult.value = res.data
    compareTab.value = 'new'
    showCompareDialog.value = true
  } catch (e) {
    ElMessage.error('对比失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除此基线吗？', '警告', {
      type: 'warning'
    })
    loading.value = true
    await baselineApi.delete(props.agentId, props.baselineType)
    ElMessage.success('删除成功')
    await loadData()
    emit('refresh')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const getLevelType = (level) => {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'HIGH') return 'warning'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

watch(() => [props.agentId, props.baselineType], () => {
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.baseline-type-panel {
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.config-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.config-mode, .config-time {
  color: #666;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 10px;
}

.snapshots-section, .items-section {
  margin-top: 20px;
}

.snapshots-section h4, .items-section h4 {
  margin-bottom: 10px;
  color: #333;
}

.compare-stats {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}
</style>

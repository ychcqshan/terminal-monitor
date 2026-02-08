<template>
  <div class="agent-list">
    <div class="page-card">
      <div class="page-title">Agent列表</div>
      <el-table :data="agents" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="platform" label="平台" width="150" />
        <el-table-column prop="hostname" label="主机名" width="120" />
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'online' ? 'success' : 'danger'">
              {{ row.status === 'online' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后更新" width="160">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">
              详情
            </el-button>
            <el-button type="warning" link @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑Agent" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agentApi } from '../api'

const router = useRouter()
const loading = ref(false)
const agents = ref([])
const editDialogVisible = ref(false)
const editForm = ref({ id: '', name: '', status: '' })

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

const openEditDialog = (row) => {
  editForm.value = { id: row.id, name: row.name, status: row.status }
  editDialogVisible.value = true
}

const handleUpdate = async () => {
  try {
    await agentApi.updateAgent(editForm.value.id, {
      name: editForm.value.name,
      status: editForm.value.status
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchAgents()
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除Agent "${row.name}" 吗?`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await agentApi.deleteAgent(row.id)
    ElMessage.success('删除成功')
    fetchAgents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
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

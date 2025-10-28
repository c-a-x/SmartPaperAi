<template>
  <div class="teaching-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">教学设计管理</span>
          <el-button type="primary" :icon="Plus" @click="handleCreate">
            创建教学设计
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索教学设计..." clearable style="width: 300px"
          @keyup.enter="loadData">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
        <el-button :icon="Search" @click="loadData">搜索</el-button>
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="tableData" style="width: 100%" @row-click="handleRowClick">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="课题名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="grade" label="学段" width="120" />
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :icon="View" @click.stop="handleView(row.id)">
              查看
            </el-button>
            <!-- <el-button type="primary" link size="small" :icon="Edit" @click.stop="handleEdit(row.id)">
              编辑
            </el-button> -->
            <el-button type="danger" link size="small" :icon="Delete" @click.stop="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && tableData.length === 0" description="暂无教学设计，点击右上角创建" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, View, Edit, Delete } from '@element-plus/icons-vue'
import { getTeachingPlanList, deleteTeachingPlan } from '@/api/teaching'
import { formatDateTime } from '@/utils/format'
import type { TeachingPlanListVO } from '@/types'

const router = useRouter()

// 状态
const loading = ref(false)
const searchKeyword = ref('')
const tableData = ref<TeachingPlanListVO[]>([])

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const response = await getTeachingPlanList()
    if (response.success) {
      let data = response.data || []

      // 搜索过滤
      if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        data = data.filter(
          (item) =>
            item.title.toLowerCase().includes(keyword) ||
            item.grade.toLowerCase().includes(keyword) ||
            item.subject.toLowerCase().includes(keyword)
        )
      }

      tableData.value = data
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 创建
function handleCreate() {
  router.push('/teaching/create')
}

// 查看
function handleView(id: number) {
  router.push(`/teaching/${id}`)
}

// 编辑
function handleEdit(id: number) {
  router.push(`/teaching/${id}/edit`)
}

// 删除
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除这个教学设计吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteTeachingPlan(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 行点击
function handleRowClick(row: TeachingPlanListVO) {
  handleView(row.id)
}

// 刷新
function handleRefresh() {
  searchKeyword.value = ''
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.teaching-list-container {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }

  .search-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  :deep(.el-table) {
    cursor: pointer;

    .el-table__row {
      &:hover {
        background-color: #f5f7fa;
      }
    }
  }
}
</style>

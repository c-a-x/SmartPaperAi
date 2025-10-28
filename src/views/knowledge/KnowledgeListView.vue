<template>
  <div class="knowledge-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">知识库管理</span>
          <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
            创建知识库
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索知识库..." clearable style="width: 300px" @keyup.enter="loadData">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
        <el-button :icon="Search" @click="loadData">搜索</el-button>
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>

      <!-- 知识库列表 -->
      <el-row :gutter="20" v-loading="loading">
        <el-col v-for="item in list" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="kb-card" shadow="hover" @click="handleView(item.id)">
            <div class="kb-icon">
              <el-icon :size="48">
                <Collection />
              </el-icon>
            </div>
            <div class="kb-info">
              <div class="kb-name">{{ item.name }}</div>
              <div class="kb-desc">{{ item.description || '暂无描述' }}</div>
              <div class="kb-meta">
                <span class="doc-count">
                  <el-icon>
                    <Document />
                  </el-icon>
                  {{ item.documentCount }} 个文档
                </span>
                <span class="create-time">
                  {{ formatRelativeTime(item.createTime) }}
                </span>
              </div>
            </div>
            <div class="kb-actions" @click.stop>
              <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(item)">
                编辑
              </el-button>
              <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(item.id)">
                删除
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-empty v-if="!loading && list.length === 0" description="暂无知识库，点击右上角创建" />

      <!-- 分页 -->
      <div v-if="list.length > 0" class="pagination-container">
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
          :page-sizes="[5, 10, 15, 20]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="showCreateDialog" :title="editingItem ? '编辑知识库' : '创建知识库'" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入知识库名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="4" placeholder="请输入描述（可选）" maxlength="200"
            show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Refresh, Collection, Document, Edit, Delete } from '@element-plus/icons-vue'
import {
  getKnowledgeBaseList,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase
} from '@/api/knowledge'
import { formatRelativeTime } from '@/utils/format'
import type { KnowledgeBaseDO, CreateKBDTO } from '@/types'

const router = useRouter()

// 状态
const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const showCreateDialog = ref(false)
const editingItem = ref<KnowledgeBaseDO | null>(null)
const list = ref<KnowledgeBaseDO[]>([])
const total = ref(0)

// 分页参数
const pagination = reactive({
  current: 1,
  size: 5
})

// 表单
const formRef = ref<FormInstance>()
const formData = reactive<CreateKBDTO>({
  name: '',
  description: ''
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const response = await getKnowledgeBaseList({
      current: pagination.current,
      size: pagination.size,
      name: searchKeyword.value || undefined
    })
    console.log('API响应:', response)
    if (response.success && response.data) {
      list.value = response.data.records || []
      // 如果后端返回的 total 为 0，使用 records 长度作为 total
      total.value = response.data.total || response.data.records?.length || 0
      console.log('当前页:', pagination.current, '每页:', pagination.size, '总数:', total.value, '数据:', list.value.length)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

// 分页改变
function handleSizeChange(size: number) {
  console.log('每页大小改变:', size)
  pagination.size = size
  pagination.current = 1
  loadData()
}

function handleCurrentChange(current: number) {
  console.log('页码改变:', current)
  pagination.current = current
  loadData()
}

// 查看
function handleView(id: number) {
  router.push(`/knowledge/${id}`)
}

// 编辑
function handleEdit(item: KnowledgeBaseDO) {
  editingItem.value = item
  formData.name = item.name
  formData.description = item.description || ''
  showCreateDialog.value = true
}

// 删除
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除这个知识库吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteKnowledgeBase(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交
async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (editingItem.value) {
          await updateKnowledgeBase(editingItem.value.id, formData)
          ElMessage.success('更新成功')
        } else {
          await createKnowledgeBase(formData)
          ElMessage.success('创建成功')
        }
        showCreateDialog.value = false
        resetForm()
        loadData()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 重置表单
function resetForm() {
  formData.name = ''
  formData.description = ''
  editingItem.value = null
  formRef.value?.resetFields()
}

// 刷新
function handleRefresh() {
  searchKeyword.value = ''
  pagination.current = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.knowledge-list-container {
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
    margin-bottom: 20px;
  }

  .kb-card {
    margin-bottom: 20px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .kb-icon {
      text-align: center;
      color: #1890ff;
      margin-bottom: 16px;
    }

    .kb-info {
      .kb-name {
        font-size: 16px;
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .kb-desc {
        font-size: 14px;
        color: #666;
        margin-bottom: 12px;
        height: 40px;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        line-clamp: 2;
        -webkit-box-orient: vertical;
      }

      .kb-meta {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        color: #999;

        .doc-count {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }

    .kb-actions {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #f0f0f0;
      display: flex;
      justify-content: center;
      gap: 16px;
    }
  }

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
    padding: 20px 0;
  }
}
</style>

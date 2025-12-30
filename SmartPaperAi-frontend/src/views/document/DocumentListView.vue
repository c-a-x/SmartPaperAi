<template>
  <div class="document-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">文档管理</span>
          <el-upload :action="uploadAction" :headers="uploadHeaders" :before-upload="beforeUpload"
            :on-success="handleUploadSuccess" :on-error="handleUploadError" :show-file-list="false"
            accept=".pdf,.doc,.docx,.txt">
            <el-button type="primary" :icon="Upload">上传文档</el-button>
          </el-upload>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="searchParams.title" placeholder="搜索文档..." clearable style="width: 300px"
          @keyup.enter="handleSearch">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
        <el-button :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="tableData" style="width: 100%" @row-click="handleRowClick">
        <el-table-column prop="id" label="ID" width="180" show-overflow-tooltip />
        <el-table-column prop="title" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="plain">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :icon="View" @click.stop="handleView(row.id)">
              查看
            </el-button>
            <el-button type="primary" link size="small" :icon="Download" @click.stop="handleDownload(row)">
              下载
            </el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click.stop="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && tableData.length === 0" description="暂无文档，点击右上角上传" />

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-container">
        <el-pagination v-model:current-page="searchParams.current" v-model:page-size="searchParams.size"
          :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 文档预览对话框 -->
    <el-dialog v-model="showPreviewDialog" :title="currentDocument?.title || '文档预览'" width="80%" top="5vh">
      <div class="preview-header" v-if="currentDocument">
        <div class="preview-info">
          <span class="file-size">文件大小: {{ formatFileSize(currentDocument.fileSize) }}</span>
          <span class="create-time">创建时间: {{ currentDocument.createTime }}</span>
        </div>
      </div>
      <el-divider />
      <div v-loading="previewLoading" class="preview-content">
        <!-- PDF预览 -->
        <iframe v-if="previewUrl && currentFileType === 'pdf'" :src="previewUrl" class="preview-iframe"
          frameborder="0"></iframe>

        <!-- Word文档HTML预览 -->
        <div v-else-if="wordHtmlContent && (currentFileType === 'doc' || currentFileType === 'docx')"
          class="word-preview" v-html="wordHtmlContent"></div>

        <!-- 不支持的格式 -->
        <el-empty v-else-if="!previewLoading && (previewUrl || wordHtmlContent)" description="暂不支持该文件格式的在线预览，请下载后查看" />
        <el-empty v-else-if="!previewLoading && !previewUrl && !wordHtmlContent" description="暂无预览内容" />
      </div>
      <template #footer>
        <el-button @click="handleClosePreview">关闭</el-button>
        <el-button type="primary" @click="handleShowCitation">
          <el-icon>
            <Document />
          </el-icon>
          查看引用
        </el-button>
      </template>
    </el-dialog>

    <!-- 引用信息对话框 -->
    <el-dialog v-model="showCitationDialog" title="文档引用格式" width="700px">
      <div v-loading="citationLoading">
        <div v-if="citationData" class="citation-container">
          <div class="citation-item">
            <div class="citation-header">
              <span class="citation-label">GB/T 7714 格式</span>
              <el-button size="small" @click="handleCopyFormat(citationData.gbt7714 || '')">复制</el-button>
            </div>
            <div class="citation-content">{{ citationData.gbt7714 }}</div>
          </div>

          <div class="citation-item">
            <div class="citation-header">
              <span class="citation-label">APA 格式</span>
              <el-button size="small" @click="handleCopyFormat(citationData.apa || '')">复制</el-button>
            </div>
            <div class="citation-content">{{ citationData.apa }}</div>
          </div>

          <div class="citation-item">
            <div class="citation-header">
              <span class="citation-label">MLA 格式</span>
              <el-button size="small" @click="handleCopyFormat(citationData.mla || '')">复制</el-button>
            </div>
            <div class="citation-content">{{ citationData.mla }}</div>
          </div>

          <div class="citation-item">
            <div class="citation-header">
              <span class="citation-label">BibTeX 格式</span>
              <el-button size="small" @click="handleCopyFormat(citationData.bibtex || '')">复制</el-button>
            </div>
            <div class="citation-content code-block">{{ citationData.bibtex }}</div>
          </div>
        </div>
        <el-empty v-else-if="!citationLoading" description="暂无引用信息" />
      </div>
      <template #footer>
        <el-button @click="showCitationDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Search, Refresh, View, Download, Delete, Document } from '@element-plus/icons-vue'
import { getDocumentList, searchDocuments, deleteDocument, getDocumentCitation, type CitationFormats } from '@/api/document'
import { formatFileSize } from '@/utils/format'
import { config } from '@/config'
import type { DocumentListVO, DocumentSearchRequest } from '@/types'
import mammoth from 'mammoth'

const router = useRouter()

// 状态
const loading = ref(false)
const tableData = ref<DocumentListVO[]>([])
const total = ref(0)
const showPreviewDialog = ref(false)
const showCitationDialog = ref(false)
const previewLoading = ref(false)
const citationLoading = ref(false)
const previewUrl = ref('')
const wordHtmlContent = ref('')
const citationData = ref<CitationFormats | null>(null)
const currentDocument = ref<DocumentListVO | null>(null)
const currentFileType = ref<string>('')

// 搜索参数
const searchParams = reactive<Partial<DocumentSearchRequest>>({
  title: '',
  fileType: '',
  status: '',
  current: 1,
  size: 20
})

// 上传配置
const uploadAction = computed(() => `${config.apiBaseUrl}/api/documents/upload`)
const uploadHeaders = computed(() => {
  const token = localStorage.getItem(config.tokenKey)
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const response = await getDocumentList(searchParams as DocumentSearchRequest)

    if (response.success && response.data) {
      tableData.value = response.data.records || []
      total.value = response.data.total || 0
      console.log('文档列表:', tableData.value, '总数:', total.value)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 上传前检查
function beforeUpload(file: File) {
  const maxSize = config.uploadMaxSize * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过 ${config.uploadMaxSize}MB`)
    return false
  }
  ElMessage.info('正在上传文件...')
  return true
}

// 上传成功
function handleUploadSuccess(response: any) {
  if (response.success) {
    ElMessage.success('上传成功')
    loadData()
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 上传失败
function handleUploadError() {
  ElMessage.error('上传失败')
}

// 搜索
function handleSearch() {
  loadData()
}

// 刷新
function handleRefresh() {
  Object.assign(searchParams, {
    keyword: '',
    fileType: '',
    status: '',
    current: 1,
    size: 20
  })
  loadData()
}

// 分页大小改变
function handleSizeChange(size: number) {
  searchParams.size = size
  searchParams.current = 1
  loadData()
}

// 页码改变
function handleCurrentChange(current: number) {
  searchParams.current = current
  loadData()
}

// 查看
async function handleView(id: string) {
  const doc = tableData.value.find(d => d.id === id)
  if (!doc) return

  currentDocument.value = doc

  // 从文档标题获取文件类型
  const fileName = doc.title || ''
  const fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
  currentFileType.value = fileExtension

  showPreviewDialog.value = true
  previewLoading.value = true
  previewUrl.value = ''
  wordHtmlContent.value = ''

  try {
    const url = `${config.apiBaseUrl}/api/documents/${id}/preview`
    const token = localStorage.getItem(config.tokenKey)

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': '*/*',
        'Authorization': token || ''
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const blob = await response.blob()

    if (blob && blob.size > 0) {
      if (fileExtension === 'pdf') {
        previewUrl.value = URL.createObjectURL(blob)
      } else if (fileExtension === 'doc' || fileExtension === 'docx') {
        const arrayBuffer = await blob.arrayBuffer()
        const result = await mammoth.convertToHtml({ arrayBuffer })
        wordHtmlContent.value = result.value
      } else {
        ElMessage.warning('暂不支持该文件格式的在线预览')
      }
    } else {
      ElMessage.error('文件为空')
    }
  } catch (error: any) {
    console.error('预览请求失败:', error)
    ElMessage.error(error.message || '加载预览失败')
  } finally {
    previewLoading.value = false
  }
}

// 清理Blob URL
function cleanupPreviewUrl() {
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
  wordHtmlContent.value = ''
}

// 关闭预览对话框
function handleClosePreview() {
  showPreviewDialog.value = false
  cleanupPreviewUrl()
}

// 下载
async function handleDownload(row: DocumentListVO) {
  try {
    ElMessage.info('正在准备下载...')

    const url = `${config.apiBaseUrl}/api/documents/${row.id}/preview?download=true`
    const token = localStorage.getItem(config.tokenKey)

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': '*/*',
        'Authorization': token || ''
      }
    })

    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }

    const contentDisposition = response.headers.get('Content-Disposition')
    let filename = extractFilename(contentDisposition) || row.title || `document_${row.id}`

    const blob = await response.blob()
    downloadBlob(blob, filename)

    ElMessage.success('下载成功')
  } catch (error: any) {
    console.error('下载失败:', error)
    ElMessage.error(error.message || '下载失败')
  }
}

// 从 Content-Disposition 提取文件名
function extractFilename(contentDisposition: string | null): string | null {
  if (!contentDisposition) return null

  const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
  if (filenameMatch && filenameMatch[1]) {
    let filename = filenameMatch[1].replace(/['"]/g, '')
    try {
      filename = decodeURIComponent(filename)
    } catch (e) {
      console.warn('文件名解码失败:', e)
    }
    return filename
  }
  return null
}

// 下载 Blob
function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// 删除
async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm('确定要删除这个文档吗？删除后无法恢复！', '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    loading.value = true
    await deleteDocument(id)
    ElMessage.success('删除成功')

    // 如果删除的是当前页最后一条，且不是第一页，返回上一页
    if (tableData.value.length === 1 && searchParams.current! > 1) {
      searchParams.current = searchParams.current! - 1
    }

    await loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 行点击
function handleRowClick(row: DocumentListVO) {
  handleView(row.id)
}

// 显示引用信息
async function handleShowCitation() {
  if (!currentDocument.value) return

  showCitationDialog.value = true
  citationLoading.value = true
  citationData.value = null

  try {
    const response = await getDocumentCitation(currentDocument.value.id)

    if (response.success && response.data) {
      citationData.value = response.data
    } else {
      citationData.value = null
      ElMessage.error(response.msg || '获取引用信息失败')
    }
  } catch (error: any) {
    console.error('获取引用失败:', error)
    ElMessage.error(error.message || '获取引用信息失败')
  } finally {
    citationLoading.value = false
  }
}

// 复制引用格式
function handleCopyFormat(text: string) {
  if (!text) return

  navigator.clipboard.writeText(text)
    .then(() => {
      ElMessage.success('引用已复制到剪贴板')
    })
    .catch(() => {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        ElMessage.success('引用已复制到剪贴板')
      } catch (err) {
        ElMessage.error('复制失败，请手动复制')
      }
      document.body.removeChild(textarea)
    })
}

// 获取状态类型
function getStatusType(status: string) {
  const map: Record<string, any> = {
    processing: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

// 获取状态文本
function getStatusText(status: string) {
  const map: Record<string, string> = {
    processing: '处理中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.document-list-container {
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

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
    padding: 20px 0;
  }

  :deep(.el-table) {
    cursor: pointer;

    .el-table__row {
      &:hover {
        background-color: #f5f7fa;
      }
    }
  }

  .preview-header {
    margin-bottom: 16px;

    .preview-info {
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 14px;
      color: #666;

      .file-size,
      .create-time {
        display: flex;
        align-items: center;
      }
    }
  }

  .preview-content {
    min-height: 400px;
    max-height: 600px;
    overflow: hidden;
    padding: 0;
    background-color: #f9f9f9;
    border-radius: 4px;
    position: relative;

    .preview-iframe {
      width: 100%;
      height: 600px;
      border: none;
      background: white;
    }

    .word-preview {
      width: 100%;
      max-height: 600px;
      overflow-y: auto;
      padding: 40px;
      background: white;
      font-family: 'Arial', 'Microsoft YaHei', sans-serif;
      line-height: 1.6;

      :deep(p) {
        margin: 8px 0;
      }

      :deep(h1),
      :deep(h2),
      :deep(h3),
      :deep(h4),
      :deep(h5),
      :deep(h6) {
        margin: 16px 0 8px;
        font-weight: 600;
      }

      :deep(table) {
        border-collapse: collapse;
        width: 100%;
        margin: 16px 0;

        td,
        th {
          border: 1px solid #ddd;
          padding: 8px;
        }

        th {
          background-color: #f5f5f5;
          font-weight: 600;
        }
      }

      :deep(ul),
      :deep(ol) {
        margin: 8px 0;
        padding-left: 24px;
      }

      :deep(img) {
        max-width: 100%;
        height: auto;
      }
    }
  }

  .citation-container {
    .citation-item {
      margin-bottom: 24px;
      border: 1px solid #e8e8e8;
      border-radius: 8px;
      padding: 16px;
      background: #fafafa;

      &:last-child {
        margin-bottom: 0;
      }

      .citation-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .citation-label {
          font-weight: 600;
          color: #333;
        }
      }

      .citation-content {
        padding: 12px;
        background: white;
        border-radius: 4px;
        border: 1px solid #e8e8e8;
        font-size: 14px;
        line-height: 1.6;
        color: #333;
        word-break: break-word;
        white-space: pre-wrap;

        &.code-block {
          font-family: 'Courier New', monospace;
          background: #f5f5f5;
        }
      }
    }
  }
}
</style>

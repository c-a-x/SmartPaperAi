<template>
  <div class="knowledge-detail-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
          <div class="header-actions">
            <el-button :icon="Plus" @click="showAddDialog = true">添加文档</el-button>
            <el-button :icon="ChatDotRound" @click="handleChat">知识库问答</el-button>
          </div>
        </div>
      </template>

      <div v-if="detail" class="detail-content">
        <div class="detail-header">
          <h1 class="title">{{ detail.name }}</h1>
          <p class="description">{{ detail.description || '暂无描述' }}</p>
          <div class="stats">
            <el-statistic title="文档数量" :value="detail.documentCount" />
            <el-statistic title="创建时间" :value="formatDate(detail.createTime)" />
          </div>
        </div>

        <el-divider />

        <div class="documents-section">
          <h3 class="section-title">知识库文档</h3>
          <el-table :data="documents" style="width: 100%" v-if="documents.length > 0">
            <el-table-column prop="title" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.type || '未知' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'completed' ? 'success' : row.status === 'processing' ? 'warning' : 'info'">
                  {{ row.status === 'completed' ? '已完成' : row.status === 'processing' ? '处理中' : row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleViewDoc(row.id)">
                  查看
                </el-button>
                <el-button type="danger" link size="small" @click="handleRemoveDoc(row.id)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="documents.length === 0" description="暂无文档，点击右上角添加文档" />

          <!-- 分页器 -->
          <div v-if="documents.length > 0" class="pagination-container">
            <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
              :page-sizes="[5, 10, 15, 20]" :total="total" layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange" @current-change="handleCurrentChange" />
          </div>
        </div>
      </div>
    </el-card>

    <!-- 添加文档对话框 -->
    <el-dialog v-model="showAddDialog" title="添加文档到知识库" :width="addMode === 'select' ? '900px' : '700px'"
      :close-on-click-modal="addMode === 'select'" :close-on-press-escape="addMode === 'select'"
      :show-close="addMode === 'select' || uploadTasks.size === 0">

      <!-- 模式切换 -->
      <el-radio-group v-model="addMode" style="margin-bottom: 16px;" @change="handleModeChange">
        <el-radio-button label="select">选择已有文档</el-radio-button>
        <el-radio-button label="upload">上传新文档</el-radio-button>
      </el-radio-group>

      <!-- 选择文档模式 -->
      <div v-if="addMode === 'select'">
        <el-alert title="提示" description="从文档库中选择文档添加到当前知识库" type="info" :closable="false"
          style="margin-bottom: 16px" />

        <!-- 搜索栏 -->
        <div style="margin-bottom: 16px;">
          <el-input v-model="documentSearchKeyword" placeholder="搜索文档..." clearable style="width: 300px"
            @keyup.enter="loadAvailableDocuments">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
          <el-button :icon="Search" @click="loadAvailableDocuments" style="margin-left: 12px;">搜索</el-button>
        </div>

        <!-- 文档列表 -->
        <el-table ref="documentTableRef" :data="availableDocuments" style="width: 100%" max-height="400"
          @selection-change="handleSelectionChange" v-loading="documentListLoading">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="title" label="文件名" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small">
                {{ row.status === 'completed' ? '已完成' : '处理中' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div style="margin-top: 16px; display: flex; justify-content: center;">
          <el-pagination v-model:current-page="documentPagination.current" v-model:page-size="documentPagination.size"
            :page-sizes="[10, 20, 50]" :total="documentTotal" layout="total, sizes, prev, pager, next"
            @size-change="loadAvailableDocuments" @current-change="loadAvailableDocuments" />
        </div>
      </div>

      <!-- 上传文档模式 -->
      <div v-else>
        <el-alert title="提示" description="上传文档后会自动处理并添加到当前知识库" type="info" :closable="false"
          style="margin-bottom: 16px" />

        <el-upload ref="uploadRef" :action="uploadAction" :headers="uploadHeaders" :on-success="handleUploadSuccess"
          :on-error="handleUploadError" :before-upload="beforeUpload" :show-file-list="true" :auto-upload="false" drag
          multiple>
          <el-icon class="el-icon--upload">
            <Plus />
          </el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持PDF、Word等文档格式，单个文件不超过50MB
            </div>
          </template>
        </el-upload>

        <!-- 任务状态列表 -->
        <div v-if="uploadTasks.size > 0" style="margin-top: 20px;">
          <el-divider>处理进度</el-divider>
          <div v-for="[taskId, task] in uploadTasks" :key="taskId" style="margin-bottom: 16px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <span style="font-weight: 500;">文档ID: {{ task.documentId }}</span>
              <div>
                <el-tag v-if="task.status === 'processing'" type="info" size="small">处理中</el-tag>
                <el-tag v-else-if="task.status === 'completed'" type="success" size="small">完成</el-tag>
                <el-tag v-else-if="task.status === 'failed'" type="danger" size="small">失败</el-tag>
                <el-button v-if="task.status === 'failed'" type="text" size="small" @click="handleRetryTask(taskId)"
                  style="margin-left: 8px;">
                  重试
                </el-button>
              </div>
            </div>
            <el-progress :percentage="task.progress" :status="task.status === 'completed' ? 'success' :
              task.status === 'failed' ? 'exception' : undefined" />
            <div v-if="task.errorMessage" style="color: #f56c6c; font-size: 12px; margin-top: 4px;">
              {{ task.errorMessage }}
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <!-- 选择模式按钮 -->
        <template v-if="addMode === 'select'">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="handleAddSelectedDocuments" :disabled="selectedDocuments.length === 0">
            添加选中文档 ({{ selectedDocuments.length }})
          </el-button>
        </template>

        <!-- 上传模式按钮 -->
        <template v-else>
          <el-button v-if="uploadTasks.size === 0" @click="showAddDialog = false">取消</el-button>
          <el-button v-if="uploadTasks.size === 0" type="primary" @click="handleSubmitUpload">
            开始上传
          </el-button>
          <el-button v-else type="success" @click="handleFinishUpload">
            完成
          </el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 文档预览对话框 -->
    <el-dialog v-model="showPreviewDialog" :title="currentDocument?.title || '文档预览'" width="80%" top="5vh">
      <div class="preview-header" v-if="currentDocument">
        <div class="preview-info">
          <el-tag>{{ currentDocument.type }}</el-tag>
          <span class="file-size">大小: {{ formatFileSize(currentDocument.fileSize) }}</span>
          <span class="create-time">创建时间: {{ formatDateTime(currentDocument.createTime) }}</span>
        </div>
      </div>
      <el-divider />
      <div v-loading="previewLoading" class="preview-content">
        <!-- PDF预览 -->
        <iframe v-if="previewUrl && currentFileType === 'pdf'" :src="previewUrl" class="preview-iframe" frameborder="0"
          @load="console.log('iframe加载完成')" @error="console.error('iframe加载失败')"></iframe>

        <!-- Word文档HTML预览 -->
        <div v-else-if="wordHtmlContent && (currentFileType === 'doc' || currentFileType === 'docx')"
          class="word-preview" v-html="wordHtmlContent"></div>

        <!-- 不支持的格式 -->
        <el-empty v-else-if="!previewLoading && (previewUrl || wordHtmlContent)" description="暂不支持该文件格式的在线预览，请下载后查看" />
        <el-empty v-else-if="!previewLoading && !previewUrl && !wordHtmlContent" description="暂无预览内容" />
      </div>
      <template #footer>
        <el-button @click="handleClosePreview">关闭</el-button>
        <el-button type="success" @click="handleDownloadDoc(currentDocument?.id || '')">
          下载文档
        </el-button>
        <el-button type="primary" @click="handleShowCitation(currentDocument?.id || '')">
          引用
        </el-button>
      </template>
    </el-dialog>

    <!-- 引用信息对话框 -->
    <el-dialog v-model="showCitationDialog" title="文档引用格式" width="700px">
      <div v-loading="citationLoading">
        <div v-if="citationData" class="citation-container">
          <!-- APA格式 -->
          <div class="citation-item" v-if="citationData.apa">
            <div class="citation-header">
              <span class="citation-label">APA格式</span>
              <el-button size="small" text type="primary" @click="handleCopyFormat(citationData.apa)">
                复制
              </el-button>
            </div>
            <div class="citation-content">{{ citationData.apa }}</div>
          </div>

          <!-- BibTeX格式 -->
          <div class="citation-item" v-if="citationData.bibtex">
            <div class="citation-header">
              <span class="citation-label">BibTeX格式</span>
              <el-button size="small" text type="primary" @click="handleCopyFormat(citationData.bibtex)">
                复制
              </el-button>
            </div>
            <div class="citation-content code-block">{{ citationData.bibtex }}</div>
          </div>

          <!-- MLA格式 -->
          <div class="citation-item" v-if="citationData.mla">
            <div class="citation-header">
              <span class="citation-label">MLA格式</span>
              <el-button size="small" text type="primary" @click="handleCopyFormat(citationData.mla)">
                复制
              </el-button>
            </div>
            <div class="citation-content">{{ citationData.mla }}</div>
          </div>

          <!-- GB/T 7714格式 -->
          <div class="citation-item" v-if="citationData.gbt7714">
            <div class="citation-header">
              <span class="citation-label">GB/T 7714格式（中文）</span>
              <el-button size="small" text type="primary" @click="handleCopyFormat(citationData.gbt7714)">
                复制
              </el-button>
            </div>
            <div class="citation-content">{{ citationData.gbt7714 }}</div>
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
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, ChatDotRound, Search } from '@element-plus/icons-vue'
import { getKnowledgeBaseDetail, removeDocumentFromKB, batchAddDocumentsToKB } from '@/api/knowledge'
import { getDocumentList, previewDocument, getDocumentCitation, uploadDocument, getDocumentTaskStatus, retryDocumentTask, type CitationFormats, type DocumentTaskVO } from '@/api/document'
import { formatDate, formatDateTime, formatFileSize } from '@/utils/format'
import { config } from '@/config'
import type { KnowledgeBaseDO, DocumentListVO } from '@/types'
import mammoth from 'mammoth'  // Word文档转换库

// 引用数据类型使用API中的定义
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const showAddDialog = ref(false)
const showPreviewDialog = ref(false)
const showCitationDialog = ref(false)  // 引用对话框
const previewLoading = ref(false)
const citationLoading = ref(false)  // 引用加载状态
const documentListLoading = ref(false)  // 文档列表加载状态
const previewUrl = ref('')
const wordHtmlContent = ref('')  // Word文档转换后的HTML内容
const citationData = ref<CitationFormats | null>(null)  // 引用数据
const currentDocument = ref<DocumentListVO | null>(null)
const currentFileType = ref<string>('')  // 当前文件类型
const detail = ref<KnowledgeBaseDO | null>(null)
const documents = ref<DocumentListVO[]>([])
const total = ref(0)

// 可选文档列表相关
const availableDocuments = ref<DocumentListVO[]>([])
const documentTotal = ref(0)
const documentSearchKeyword = ref('')
const selectedDocuments = ref<DocumentListVO[]>([])
const documentTableRef = ref()

// 文档分页参数
const documentPagination = reactive({
  current: 1,
  size: 10
})

// 添加模式：'select' 选择文档 | 'upload' 上传文档
const addMode = ref<'select' | 'upload'>('select')

// 上传功能相关变量
const uploadRef = ref()
const uploadedDocIds = ref<number[]>([])
const uploadTasks = ref<Map<number, DocumentTaskVO>>(new Map())
const pollingTimers = ref<Map<number, number>>(new Map())
const uploadAction = `${config.apiBaseUrl}/api/documents/upload`
const uploadHeaders = {
  'Authorization': localStorage.getItem(config.tokenKey) || ''
}

// 分页参数
const pagination = reactive({
  current: 1,
  size: 5
})

// 加载详情
async function loadDetail() {
  const id = route.params.id
  console.log(route.params.id)
  // console.log('知识库ID:', Number(id))
  if (!id) {
    ElMessage.error('参数错误')
    router.back()
    return
  }

  loading.value = true
  try {
    const [detailRes, docsRes] = await Promise.all([
      getKnowledgeBaseDetail(id),
      getDocumentList({
        kbId: id,
        current: pagination.current,
        size: pagination.size
      })
    ])

    if (detailRes.success) {
      detail.value = detailRes.data
    }
    if (docsRes.success && docsRes.data) {
      documents.value = docsRes.data.records || []
      total.value = docsRes.data.total || 0
      console.log('知识库文档列表:', documents.value, '总数:', total.value)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载详情失败')
    console.error('加载失败:', error)
    router.back()
  } finally {
    loading.value = false
  }
}

// 分页改变
function handleSizeChange(size: number) {
  pagination.size = size
  pagination.current = 1
  loadDetail()
}

function handleCurrentChange(current: number) {
  pagination.current = current
  loadDetail()
}

// 查看文档（参考示例代码，使用fetch避免CORS问题）
async function handleViewDoc(documentId: string) {
  const doc = documents.value.find(d => d.id === documentId)
  if (doc) {
    currentDocument.value = doc

    // 从文档标题获取文件类型
    const fileName = doc.title || ''
    const fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
    currentFileType.value = fileExtension
    console.log('文件类型:', fileExtension)

    showPreviewDialog.value = true
    previewLoading.value = true
    previewUrl.value = ''
    wordHtmlContent.value = ''  // 清空之前的Word内容

    try {
      console.log('=== 开始预览文档 ===')
      console.log('文档ID:', documentId)
      console.log('文件类型:', fileExtension)

      // 构建预览URL
      const url = `${config.apiBaseUrl}/api/documents/${documentId}/preview`
      console.log('请求URL:', url)

      // 获取token
      const token = localStorage.getItem(config.tokenKey)
      console.log('Authorization Token:', token)

      // 使用fetch发起请求（参考示例代码）
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Accept': '*/*',
          'Authorization': token || ''  // 直接使用token，不加Bearer前缀
        }
      })

      console.log('响应状态:', response.status, response.statusText)
      console.log('Content-Type:', response.headers.get('Content-Type'))
      console.log('Content-Disposition:', response.headers.get('Content-Disposition'))
      console.log('Content-Length:', response.headers.get('Content-Length'))

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      // 获取blob数据
      const blob = await response.blob()
      console.log('✅ Blob获取成功:')
      console.log('  - Blob类型:', blob.type)
      console.log('  - Blob大小:', blob.size)

      if (blob && blob.size > 0) {
        // 根据文件类型处理
        if (fileExtension === 'pdf') {
          // PDF直接创建Blob URL
          const blobUrl = URL.createObjectURL(blob)
          previewUrl.value = blobUrl
          console.log('✅ PDF Blob URL创建成功:', previewUrl.value)
        } else if (fileExtension === 'doc' || fileExtension === 'docx') {
          // Word文档使用mammoth转换为HTML
          console.log('开始转换Word文档...')
          const arrayBuffer = await blob.arrayBuffer()
          const result = await mammoth.convertToHtml({ arrayBuffer })
          wordHtmlContent.value = result.value
          console.log('✅ Word文档转换成功')
          if (result.messages.length > 0) {
            console.warn('转换警告:', result.messages)
          }
        } else {
          // 其他格式尝试创建Blob URL
          const blobUrl = URL.createObjectURL(blob)
          previewUrl.value = blobUrl
          console.log('⚠️ 未知格式，尝试直接预览')
        }
      } else {
        console.error('❌ Blob无效，大小为0')
        ElMessage.error('文件为空')
      }
    } catch (error: any) {
      console.error('❌ 预览请求失败:', error)
      ElMessage.error(error.message || '加载预览失败')
    } finally {
      previewLoading.value = false
    }
  }
}

// 清理Blob URL（在对话框关闭时调用）
function cleanupPreviewUrl() {
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
  // 清空Word HTML内容
  wordHtmlContent.value = ''
}

// 关闭预览对话框
function handleClosePreview() {
  showPreviewDialog.value = false
  cleanupPreviewUrl()
}

// 下载文档
async function handleDownloadDoc(documentId: string) {
  if (!documentId) return

  try {
    // 显示下载提示
    ElMessage.info('正在准备下载...')

    // 构建下载URL
    const url = `${config.apiBaseUrl}/api/documents/${documentId}/preview?download=true`

    // 获取token
    const token = localStorage.getItem(config.tokenKey)

    // 使用fetch获取文件流
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

    // 从响应头获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    let filename = extractFilename(contentDisposition) || currentDocument.value?.title || `document_${documentId}`

    // 确保文件名有扩展名
    // if (!filename.includes('.') && currentDocument.value?.type) {
    //   filename += `.${currentDocument.value.type.toLowerCase()}`
    // }

    // 获取blob
    const blob = await response.blob()

    // 下载文件
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

  // 匹配 filename="xxx" 或 filename*=UTF-8''xxx
  const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
  if (filenameMatch && filenameMatch[1]) {
    let filename = filenameMatch[1].replace(/['"]/g, '')
    // 解码 URL 编码
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

// 移除文档
async function handleRemoveDoc(documentId: string) {
  try {
    await ElMessageBox.confirm('确定要从知识库中移除这个文档吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const kbId = route.params.id
    await removeDocumentFromKB(kbId, [documentId])
    ElMessage.success('移除成功')
    loadDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '移除失败')
    }
  }
}

// 知识库问答
function handleChat() {
  router.push({
    path: '/chat',
    query: { kbId: route.params.id }
  })
}

// 加载可选文档列表
async function loadAvailableDocuments() {
  documentListLoading.value = true
  try {
    const response = await getDocumentList({
      title: documentSearchKeyword.value,
      current: documentPagination.current,
      size: documentPagination.size,
      kbId: -1,  // 不限制知识库ID，获取所有文档
      status: 'completed'  // 只显示处理完成的文档
    })

    if (response.success && response.data) {
      availableDocuments.value = response.data.records || []
      documentTotal.value = response.data.total || 0
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载文档列表失败')
  } finally {
    documentListLoading.value = false
  }
}

// 处理文档选择变化
function handleSelectionChange(selection: DocumentListVO[]) {
  selectedDocuments.value = selection
}

// 添加选中的文档到知识库
async function handleAddSelectedDocuments() {
  if (selectedDocuments.value.length === 0) {
    ElMessage.warning('请至少选择一个文档')
    return
  }

  try {
    const kbId = route.params.id
    const documentIds = selectedDocuments.value.map(doc => doc.id)

    documentListLoading.value = true
    await batchAddDocumentsToKB(kbId, documentIds)

    ElMessage.success(`成功添加 ${documentIds.length} 个文档到知识库`)

    // 关闭对话框并刷新列表
    showAddDialog.value = false
    selectedDocuments.value = []
    await loadDetail()
  } catch (error: any) {
    ElMessage.error(error.message || '添加文档失败')
  } finally {
    documentListLoading.value = false
  }
}

// 监听对话框打开，加载可选文档
function handleDialogOpen() {
  if (showAddDialog.value && addMode.value === 'select') {
    documentSearchKeyword.value = ''
    documentPagination.current = 1
    loadAvailableDocuments()
  }
}

// 切换模式
function handleModeChange() {
  if (addMode.value === 'select') {
    handleDialogOpen()
  }
}

// 监听 showAddDialog 变化
watch(showAddDialog, (newVal) => {
  if (newVal) {
    handleDialogOpen()
  }
})

// 文件上传前校验
function beforeUpload(file: File) {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过 50MB!')
    return false
  }
  return true
}

// 上传成功
function handleUploadSuccess(response: any) {
  if (response.code === '0' && response.data) {
    const { documentId, taskId, title } = response.data
    console.log(response.data)
    uploadedDocIds.value.push(documentId)
    console.log('已上传文档ID列表:', uploadedDocIds.value)
    ElMessage.success(`${title} 上传成功，开始处理...`)

    // 开始轮询任务状态
    startTaskPolling(taskId)
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 开始轮询任务状态
async function startTaskPolling(taskId: number) {
  // 如果已经在轮询中，先清除
  if (pollingTimers.value.has(taskId)) {
    clearInterval(pollingTimers.value.get(taskId))
  }

  // 立即获取一次状态
  await pollTaskStatus(taskId)

  // 每2秒轮询一次
  const timer = setInterval(async () => {
    await pollTaskStatus(taskId)
  }, 2000)

  pollingTimers.value.set(taskId, timer)
}

// 轮询任务状态
async function pollTaskStatus(taskId: number) {
  try {
    const res = await getDocumentTaskStatus(taskId)
    if (res.code === '0' && res.data) {
      const taskStatus = res.data
      uploadTasks.value.set(taskId, taskStatus)

      // 如果任务完成或失败，停止轮询
      if (taskStatus.status === 'completed') {
        clearTaskPolling(taskId)
        checkAllTasksComplete()
      } else if (taskStatus.status === 'failed') {
        clearTaskPolling(taskId)
        ElMessage.error(`文档处理失败: ${taskStatus.errorMessage || '未知错误'}`)
      }
    }
  } catch (error) {
    console.error('获取任务状态失败:', error)
    clearTaskPolling(taskId)
  }
}

// 清除任务轮询
function clearTaskPolling(taskId: number) {
  if (pollingTimers.value.has(taskId)) {
    clearInterval(pollingTimers.value.get(taskId))
    pollingTimers.value.delete(taskId)
  }
}

// 检查所有任务是否完成
function checkAllTasksComplete() {
  const tasks = Array.from(uploadTasks.value.values())
  if (tasks.length === 0) return

  const allComplete = tasks.every(task =>
    task.status === 'completed' || task.status === 'failed'
  )

  if (allComplete) {
    // 获取成功完成的任务数量
    const completedCount = tasks.filter(task => task.status === 'completed').length
    const failedCount = tasks.filter(task => task.status === 'failed').length

    if (completedCount > 0) {
      // 所有任务完成，添加文档到知识库
      addToKnowledgeBase()
    }

    // 显示汇总消息
    if (failedCount > 0) {
      ElMessage.warning(`处理完成：${completedCount} 个成功，${failedCount} 个失败`)
    } else {
      ElMessage.success(`全部 ${completedCount} 个文档处理完成`)
    }
  }
}

// 添加文档到知识库
async function addToKnowledgeBase() {
  if (uploadedDocIds.value.length === 0) return

  try {
    const knowledgeId = route.params.id
    console.log(route.params.id)
    await batchAddDocumentsToKB(knowledgeId, uploadedDocIds.value)
    ElMessage.success('文档已成功添加到知识库')

    // 重新加载文档列表（但不关闭对话框，让用户点击完成按钮）
    await loadDetail()
  } catch (error) {
    console.error('添加文档到知识库失败:', error)
    ElMessage.error('添加文档到知识库失败，请重试')
  }
}

// 重试失败的任务
async function handleRetryTask(taskId: number) {
  try {
    const res = await retryDocumentTask(taskId)
    if (res.code === '0' && res.data.success) {
      ElMessage.success('开始重新处理...')
      // 重新开始轮询
      startTaskPolling(taskId)
    } else {
      ElMessage.error(res.msg || '重试失败')
    }
  } catch (error) {
    console.error('重试任务失败:', error)
    ElMessage.error('重试失败，请稍后再试')
  }
}

// 上传失败
function handleUploadError() {
  ElMessage.error('上传失败，请重试')
}

// 提交上传
async function handleSubmitUpload() {
  if (!uploadRef.value) return

  // 清空之前的任务状态
  uploadedDocIds.value = []
  uploadTasks.value.clear()

  // 清理所有轮询定时器
  pollingTimers.value.forEach((timer) => clearInterval(timer))
  pollingTimers.value.clear()

  // 提交上传
  uploadRef.value.submit()

  ElMessage.info('文件上传中，请等待处理完成...')
}

// 完成上传（关闭对话框）
function handleFinishUpload() {
  // 重置所有状态
  uploadedDocIds.value = []
  uploadTasks.value.clear()
  pollingTimers.value.forEach((timer) => clearInterval(timer))
  pollingTimers.value.clear()

  // 关闭对话框
  showAddDialog.value = false
}

// 显示引用信息
async function handleShowCitation(documentId: string) {
  if (!documentId) return

  showCitationDialog.value = true
  citationLoading.value = true
  citationData.value = null

  try {
    const response = await getDocumentCitation(documentId)

    if (response.code === '0' && response.data) {
      // 直接使用返回的引用数据对象
      citationData.value = response.data
    } else {
      citationData.value = null
      ElMessage.error('获取引用信息失败')
    }
  } catch (error: any) {
    console.error('获取引用失败:', error)
    ElMessage.error(error.message || '获取引用信息失败')
  } finally {
    citationLoading.value = false
  }
}

// 复制特定格式的引用
function handleCopyFormat(text: string) {
  if (!text) return

  navigator.clipboard.writeText(text)
    .then(() => {
      ElMessage.success('引用已复制到剪贴板')
    })
    .catch(() => {
      // 降级方案：使用传统方法复制
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

onMounted(() => {
  loadDetail()
})

// 组件卸载时清理所有轮询定时器
onUnmounted(() => {
  pollingTimers.value.forEach((timer) => clearInterval(timer))
  pollingTimers.value.clear()
})
</script>

<style scoped lang="scss">
.knowledge-detail-container {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .detail-content {
    .detail-header {
      margin-bottom: 24px;

      .title {
        font-size: 28px;
        font-weight: 600;
        color: #333;
        margin: 0 0 12px;
      }

      .description {
        font-size: 16px;
        color: #666;
        margin: 0 0 20px;
      }

      .stats {
        display: flex;
        gap: 48px;
      }
    }

    .documents-section {
      .section-title {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin: 0 0 16px;
        padding-left: 12px;
        border-left: 4px solid #1890ff;
      }

      .pagination-container {
        display: flex;
        justify-content: center;
        margin-top: 20px;
        padding: 20px 0;
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

      // Word文档样式优化
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

  // 引用信息样式
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
          font-size: 14px;
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
          font-family: 'Courier New', Consolas, monospace;
          background: #f5f5f5;
        }
      }
    }
  }

  // 上传区域样式
  :deep(.el-upload) {
    width: 100%;

    .el-upload-dragger {
      width: 100%;
      padding: 40px;
    }

    .el-icon--upload {
      font-size: 67px;
      color: #c0c4cc;
      margin-bottom: 16px;
    }

    .el-upload__text {
      color: #606266;
      font-size: 14px;

      em {
        color: #409eff;
        font-style: normal;
      }
    }

    .el-upload__tip {
      font-size: 12px;
      color: #909399;
      margin-top: 7px;
    }
  }
}
</style>

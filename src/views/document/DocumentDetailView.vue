<template>
  <div class="document-detail-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
          <div class="header-actions">
            <el-button :icon="Download" @click="handleDownloadDoc">下载</el-button>
            <el-button type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
          </div>
        </div>
      </template>

      <div v-if="detail" class="detail-content">
        <div class="detail-header">
          <h1 class="title">{{ detail.originalFilename }}</h1>
          <div class="meta">
            <el-tag>{{ detail.fileType.toUpperCase() }}</el-tag>
            <el-tag type="info">{{ formatFileSize(detail.fileSize) }}</el-tag>
            <el-tag :type="getStatusType(detail.status)">
              {{ getStatusText(detail.status) }}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件ID">
            {{ detail.id }}
          </el-descriptions-item>
          <el-descriptions-item label="文件名">
            {{ detail.filename }}
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            {{ formatDateTime(detail.uploadTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ detail.processTime ? formatDateTime(detail.processTime) : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.metadata" class="metadata-section">
          <h3 class="section-title">文档元数据</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="标题">
              {{ detail.metadata.title || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="作者">
              {{ detail.metadata.authors?.join(', ') || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="摘要" v-if="detail.metadata.abstract">
              <div class="abstract">{{ detail.metadata.abstract }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="关键词" v-if="detail.metadata.keywords">
              <el-tag v-for="keyword in detail.metadata.keywords" :key="keyword" style="margin-right: 8px">
                {{ keyword }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="发表日期">
              {{ detail.metadata.publishDate || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="期刊">
              {{ detail.metadata.journal || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="DOI">
              {{ detail.metadata.doi || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="action-section">
          <h3 class="section-title">文档操作</h3>
          <div class="actions">
            <el-button :icon="View" @click="handlePreview">预览文档</el-button>
            <el-button :icon="Document" @click="handleViewContent">查看内容</el-button>
            <el-button :icon="Download" @click="handleDownloadDoc">下载文档</el-button>
            <el-button :icon="ChatLineRound" @click="handleChat">与文档对话</el-button>
            <el-button :icon="Plus" @click="handleAddToKB">添加到知识库</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 文档预览对话框 -->
    <el-dialog v-model="showPreviewDialog" :title="detail?.originalFilename || '文档预览'" width="80%" top="5vh">
      <div class="preview-header" v-if="detail">
        <div class="preview-info">
          <span class="file-size">文件大小: {{ formatFileSize(detail.fileSize) }}</span>
          <span class="create-time">创建时间: {{ formatDateTime(detail.uploadTime) }}</span>
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
        <el-button type="success" @click="handleDownloadDoc">
          <el-icon>
            <Download />
          </el-icon>
          下载
        </el-button>
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Download, Delete, View, Document, ChatLineRound, Plus } from '@element-plus/icons-vue'
import { getDocumentDetail, deleteDocument, previewDocument, getDocumentCitation, type CitationFormats } from '@/api/document'
import { formatDateTime, formatFileSize } from '@/utils/format'
import { config } from '@/config'
import type { DocumentDetailVO } from '@/types'
import mammoth from 'mammoth'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<DocumentDetailVO | null>(null)
const showPreviewDialog = ref(false)
const showCitationDialog = ref(false)
const previewLoading = ref(false)
const citationLoading = ref(false)
const previewUrl = ref('')
const wordHtmlContent = ref('')
const citationData = ref<CitationFormats | null>(null)
const currentFileType = ref<string>('')

// 加载详情
async function loadDetail() {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('参数错误')
    router.back()
    return
  }

  loading.value = true
  try {
    const response = await getDocumentDetail(id)
    if (response.success) {
      detail.value = response.data
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载详情失败')
    router.back()
  } finally {
    loading.value = false
  }
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

// 预览文档
async function handlePreview() {
  if (!detail.value) return

  const fileName = detail.value.originalFilename || ''
  const fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
  currentFileType.value = fileExtension

  showPreviewDialog.value = true
  previewLoading.value = true
  previewUrl.value = ''
  wordHtmlContent.value = ''

  try {
    const url = `${config.apiBaseUrl}/api/documents/${detail.value.id}/preview`
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

// 查看内容
function handleViewContent() {
  handlePreview()
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

// 下载文档
async function handleDownloadDoc() {
  if (!detail.value) return

  try {
    ElMessage.info('正在准备下载...')

    const url = `${config.apiBaseUrl}/api/documents/${detail.value.id}/preview?download=true`
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
    let filename = extractFilename(contentDisposition) || detail.value.originalFilename || `document_${detail.value.id}`

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

// 显示引用信息
async function handleShowCitation() {
  if (!detail.value) return

  showCitationDialog.value = true
  citationLoading.value = true
  citationData.value = null

  try {
    const response = await getDocumentCitation(String(detail.value.id))

    if (response.code === '0' && response.data) {
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

// 与文档对话
function handleChat() {
  router.push({
    path: '/chat',
    query: { documentId: route.params.id }
  })
}

// 添加到知识库
function handleAddToKB() {
  ElMessage.info('添加到知识库功能开发中...')
}

// 删除
// 删除
async function handleDelete() {
  if (!detail.value) return

  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${detail.value.originalFilename}」吗？删除后无法恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: false
      }
    )

    loading.value = true
    await deleteDocument(detail.value.id)
    ElMessage.success('删除成功')

    // 延迟跳转，让用户看到成功提示
    setTimeout(() => {
      router.push('/documents')
    }, 500)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.document-detail-container {
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
        font-size: 24px;
        font-weight: 600;
        color: #333;
        margin: 0 0 16px;
        word-break: break-all;
      }

      .meta {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
      }
    }

    .metadata-section,
    .action-section {
      margin-top: 32px;

      .section-title {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin: 0 0 16px;
        padding-left: 12px;
        border-left: 4px solid #1890ff;
      }

      .abstract {
        line-height: 1.8;
        color: #666;
        white-space: pre-wrap;
      }

      .actions {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
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

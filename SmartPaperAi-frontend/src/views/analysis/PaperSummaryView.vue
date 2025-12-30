<template>
  <div class="paper-summary-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">论文总结</span>
        </div>
      </template>

      <!-- 文档选择 -->
      <div class="document-select">
        <el-alert title="功能说明" type="info" :closable="false" style="margin-bottom: 20px">
          <p>选择一篇论文文档，AI将为您生成结构化总结，包括研究背景、方法、结果、创新点和局限性</p>
        </el-alert>

        <el-form label-width="100px">
          <el-form-item label="选择论文">
            <el-select v-model="selectedDocumentId" placeholder="请选择论文文档" style="width: 100%"
              :loading="loadingDocuments" filterable @change="handleDocumentChange">
              <el-option v-for="doc in documentList" :key="doc.id" :label="doc.title" :value="doc.id">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="flex: 1;">{{ doc.title }}</span>
                  <el-tag size="small" type="info">{{ doc.type }}</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="generating" :disabled="!selectedDocumentId" @click="handleGenerate">
              {{ generating ? 'AI分析中...' : '生成总结' }}
            </el-button>
          </el-form-item>

          <el-alert v-if="generating" title="生成中" type="warning" :closable="false">
            <p>AI正在分析论文并生成总结，这可能需要20-40秒，请耐心等待...</p>
            <el-progress :percentage="generateProgress" :indeterminate="true" style="margin-top: 10px;" />
          </el-alert>
        </el-form>
      </div>

      <!-- 生成结果 -->
      <div v-if="summary" class="summary-result">
        <el-divider />

        <!-- 导出按钮 -->
        <div class="export-actions">
          <el-button type="success" @click="exportToMarkdown">
            <el-icon>
              <Download />
            </el-icon>
            导出为 Markdown
          </el-button>
        </div>

        <!-- 基本信息 -->
        <div class="paper-info">
          <h2>{{ summary.title }}</h2>
          <div class="meta">
            <span v-if="summary.authors?.length">作者: {{ summary.authors.join(', ') }}</span>
            <span v-if="summary.publicationYear">年份: {{ summary.publicationYear }}</span>
          </div>
          <div v-if="summary.keywords?.length" class="keywords">
            <el-tag v-for="keyword in summary.keywords" :key="keyword" style="margin: 5px">
              {{ keyword }}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <!-- 摘要内容 -->
        <div class="summary-content">
          <div class="section" v-if="summary.summary.background">
            <h3><el-icon>
                <Document />
              </el-icon> 研究背景</h3>
            <p>{{ summary.summary.background }}</p>
          </div>

          <div class="section" v-if="summary.summary.methodology">
            <h3><el-icon>
                <Setting />
              </el-icon> 研究方法</h3>
            <p>{{ summary.summary.methodology }}</p>
          </div>

          <div class="section" v-if="summary.summary.results">
            <h3><el-icon>
                <DataLine />
              </el-icon> 研究结果</h3>
            <p>{{ summary.summary.results }}</p>
          </div>

          <div class="section" v-if="summary.summary.innovations?.length">
            <h3><el-icon>
                <MagicStick />
              </el-icon> 创新点</h3>
            <ul>
              <li v-for="(innovation, index) in summary.summary.innovations" :key="index">
                {{ innovation }}
              </li>
            </ul>
          </div>

          <div class="section" v-if="summary.summary.limitations">
            <h3><el-icon>
                <Warning />
              </el-icon> 局限性</h3>
            <p>{{ summary.summary.limitations }}</p>
          </div>
        </div>

        <!-- 引用来源 -->
        <div v-if="citations?.length" class="citations">
          <el-divider content-position="left">引用来源</el-divider>
          <el-card v-for="citation in citations" :key="citation.chunkId" class="citation-card" shadow="hover">
            <div class="citation-header">
              <strong>{{ citation.title }}</strong>
              <el-tag size="small" type="success">相关度: {{ (citation.score * 100).toFixed(1) }}%</el-tag>
            </div>
            <p class="citation-content">{{ citation.snippet }}</p>
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Setting, DataLine, MagicStick, Warning, Download } from '@element-plus/icons-vue'
import { getDocumentList } from '@/api/document'
import { summarizeDocument } from '@/api/analysis'
import type { DocumentListVO, PaperSummaryVO, CitationVO } from '@/types'

const loadingDocuments = ref(false)
const generating = ref(false)
const generateProgress = ref(0)
const documentList = ref<DocumentListVO[]>([])
const selectedDocumentId = ref<number>()
const summary = ref<PaperSummaryVO | null>(null)
const citations = ref<CitationVO[]>([])

// 加载文档列表
async function loadDocuments() {
  loadingDocuments.value = true
  try {
    const response = await getDocumentList({
      current: 1,
      size: 100,
      status: 'completed'
    })

    if (response.success && response.data) {
      documentList.value = response.data.records || []
    }
  } catch (error: any) {
    console.error('加载文档列表失败:', error)
  } finally {
    loadingDocuments.value = false
  }
}

// 文档选择变化
function handleDocumentChange() {
  summary.value = null
  citations.value = []
}

// 生成总结
async function handleGenerate() {
  if (!selectedDocumentId.value) return

  generating.value = true
  generateProgress.value = 0

  // 模拟进度
  const progressTimer = setInterval(() => {
    if (generateProgress.value < 90) {
      generateProgress.value += 5
    }
  }, 1000)

  try {
    const response = await summarizeDocument(selectedDocumentId.value)

    console.log('论文总结响应:', response)

    if (response.success && response.data) {
      summary.value = response.data.answer
      citations.value = response.data.citations || []
      generateProgress.value = 100
      ElMessage.success('生成成功！')
    } else {
      ElMessage.error(response.msg || '生成失败')
    }
  } catch (error: any) {
    console.error('生成失败:', error)
    ElMessage.error(error.message || '生成失败')
  } finally {
    clearInterval(progressTimer)
    generating.value = false
    generateProgress.value = 0
  }
}

// 导出为Markdown
function exportToMarkdown() {
  if (!summary.value) return

  let markdown = '# 论文总结\n\n'
  markdown += `生成时间: ${new Date().toLocaleString('zh-CN')}\n\n`

  // 基本信息
  markdown += `## ${summary.value.title}\n\n`
  if (summary.value.authors?.length) {
    markdown += `**作者:** ${summary.value.authors.join(', ')}\n\n`
  }
  if (summary.value.publicationYear) {
    markdown += `**年份:** ${summary.value.publicationYear}\n\n`
  }
  if (summary.value.keywords?.length) {
    markdown += `**关键词:** ${summary.value.keywords.join(', ')}\n\n`
  }

  // 摘要内容
  if (summary.value.summary.background) {
    markdown += '## 研究背景\n\n'
    markdown += `${summary.value.summary.background}\n\n`
  }

  if (summary.value.summary.methodology) {
    markdown += '## 研究方法\n\n'
    markdown += `${summary.value.summary.methodology}\n\n`
  }

  if (summary.value.summary.results) {
    markdown += '## 研究结果\n\n'
    markdown += `${summary.value.summary.results}\n\n`
  }

  if (summary.value.summary.innovations?.length) {
    markdown += '## 创新点\n\n'
    summary.value.summary.innovations.forEach((innovation, index) => {
      markdown += `${index + 1}. ${innovation}\n`
    })
    markdown += '\n'
  }

  if (summary.value.summary.limitations) {
    markdown += '## 局限性\n\n'
    markdown += `${summary.value.summary.limitations}\n\n`
  }

  // 引用来源
  if (citations.value?.length) {
    markdown += '## 引用来源\n\n'
    citations.value.forEach((citation, index) => {
      markdown += `### ${index + 1}. ${citation.title}\n\n`
      markdown += `- 相关度: ${(citation.score * 100).toFixed(1)}%\n`
      markdown += `- 内容: ${citation.snippet}\n\n`
    })
  }

  // 下载文件
  const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `论文总结_${summary.value.title.slice(0, 20)}_${new Date().getTime()}.md`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('导出成功！')
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped lang="scss">
.paper-summary-container {
  .card-header {
    .title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }

  .document-select {
    max-width: 800px;
  }

  .summary-result {
    .export-actions {
      text-align: right;
      margin-bottom: 20px;
    }

    .paper-info {
      text-align: center;
      margin: 20px 0;

      h2 {
        font-size: 24px;
        margin-bottom: 15px;
        color: #333;
      }

      .meta {
        display: flex;
        gap: 20px;
        justify-content: center;
        font-size: 14px;
        color: #666;
        margin-bottom: 15px;
      }

      .keywords {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: 5px;
      }
    }

    .summary-content {
      .section {
        margin-bottom: 30px;

        h3 {
          font-size: 18px;
          font-weight: 600;
          color: #333;
          margin-bottom: 15px;
          display: flex;
          align-items: center;
          gap: 8px;

          .el-icon {
            color: #409eff;
          }
        }

        p {
          line-height: 1.8;
          color: #606266;
          text-align: justify;
        }

        ul {
          padding-left: 25px;

          li {
            line-height: 2;
            color: #606266;
            margin-bottom: 8px;
          }
        }
      }
    }

    .citations {
      margin-top: 30px;

      .citation-card {
        margin-bottom: 12px;

        .citation-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          strong {
            color: #303133;
            font-size: 14px;
          }
        }

        .citation-content {
          color: #606266;
          font-size: 13px;
          line-height: 1.6;
          margin: 0;
        }
      }
    }
  }
}
</style>

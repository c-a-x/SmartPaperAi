<template>
  <div class="paper-comparison-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">论文对比</span>
        </div>
      </template>

      <!-- 文档选择 -->
      <div class="document-select">
        <el-alert title="功能说明" type="info" :closable="false" style="margin-bottom: 20px">
          <p>选择2-5篇论文进行多维度对比分析，包括研究方法、数据集、创新点、结论等维度</p>
        </el-alert>

        <el-form label-width="120px">
          <el-form-item label="选择论文">
            <el-select v-model="selectedDocumentIds" multiple placeholder="请选择2-5篇论文" style="width: 100%"
              :loading="loadingDocuments" filterable collapse-tags @change="handleDocumentChange">
              <el-option v-for="doc in documentList" :key="doc.id" :label="doc.title" :value="doc.id">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="flex: 1;">{{ doc.title }}</span>
                  <el-tag size="small" type="info">{{ doc.type }}</el-tag>
                </div>
              </el-option>
            </el-select>
            <div style="margin-top: 8px; font-size: 12px; color: #909399;">
              已选择 {{ selectedDocumentIds.length }} 篇论文
            </div>
          </el-form-item>

          <el-form-item label="对比维度">
            <el-input v-model="dimensionInput" placeholder="例如：研究方法,数据集,创新点,实验结果" type="textarea" :rows="3"
              style="width: 100%" clearable />
            <div style="margin-top: 8px; font-size: 12px; color: #909399;">
              <div><strong>输入说明：</strong>多个维度用逗号分隔，支持中英文逗号（, ，）和顿号（、）</div>
              <div style="margin-top: 4px;"><strong>常用维度：</strong>研究方法、数据集、创新点、结论、实验结果、理论基础、应用场景</div>
              <div style="margin-top: 4px;"><strong>默认维度：</strong>留空将使用【研究方法、数据集、创新点、结论】</div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="generating" :disabled="selectedDocumentIds.length < 2"
              @click="handleGenerate">
              {{ generating ? 'AI对比中...' : '开始对比' }}
            </el-button>
          </el-form-item>

          <el-alert v-if="generating" title="对比中" type="warning" :closable="false">
            <p>AI正在对比分析多篇论文，这可能需要30-50秒，请耐心等待...</p>
            <el-progress :percentage="generateProgress" :indeterminate="true" style="margin-top: 10px;" />
          </el-alert>
        </el-form>
      </div>

      <!-- 对比结果 -->
      <div v-if="comparisonResult" class="comparison-result">
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

        <!-- 论文列表 -->
        <div class="papers-info">
          <h3>对比论文</h3>
          <el-table :data="comparisonResult.papers" style="width: 100%">
            <el-table-column prop="documentId" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="authors" label="作者" width="150" show-overflow-tooltip />
            <el-table-column prop="year" label="年份" width="100" />
          </el-table>
        </div>

        <el-divider />

        <!-- 对比矩阵 -->
        <div class="comparison-matrix">
          <h3>对比分析</h3>
          <div v-for="row in comparisonResult.matrix" :key="row.dimensionId" class="dimension-row">
            <h4>{{ row.dimensionName }}</h4>
            <el-row :gutter="20">
              <el-col v-for="(value, index) in row.values" :key="index" :span="24 / row.values.length">
                <el-card shadow="hover">
                  <div class="paper-title">{{ comparisonResult.papers[index]?.title }}</div>
                  <div class="value-content">{{ value }}</div>
                </el-card>
              </el-col>
            </el-row>
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
import { Download } from '@element-plus/icons-vue'
import { getDocumentList } from '@/api/document'
import { compareDocuments } from '@/api/analysis'
import type { DocumentListVO, PaperComparisonVO, CitationVO } from '@/types'

const loadingDocuments = ref(false)
const generating = ref(false)
const generateProgress = ref(0)
const documentList = ref<DocumentListVO[]>([])
const selectedDocumentIds = ref<number[]>([])
const dimensionInput = ref<string>('')
const comparisonResult = ref<PaperComparisonVO | null>(null)
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
  comparisonResult.value = null
  citations.value = []
}

// 生成对比
async function handleGenerate() {
  if (selectedDocumentIds.value.length < 2) {
    ElMessage.warning('请至少选择2篇论文')
    return
  }

  if (selectedDocumentIds.value.length > 5) {
    ElMessage.warning('最多选择5篇论文')
    return
  }

  generating.value = true
  generateProgress.value = 0

  // 模拟进度
  const progressTimer = setInterval(() => {
    if (generateProgress.value < 90) {
      generateProgress.value += 5
    }
  }, 1200)

  try {
    // 解析维度输入
    const dimensions = dimensionInput.value
      .split(/[,，、]/) // 支持中英文逗号和顿号分隔
      .map(d => d.trim())
      .filter(d => d.length > 0)

    const response = await compareDocuments({
      documentIds: selectedDocumentIds.value,
      dimensions: dimensions.length > 0 ? dimensions : undefined
    })

    console.log('论文对比响应:', response)

    if (response.success && response.data) {
      comparisonResult.value = response.data.answer
      citations.value = response.data.citations || []
      generateProgress.value = 100
      ElMessage.success('对比完成！')
    } else {
      ElMessage.error(response.msg || '对比失败')
    }
  } catch (error: any) {
    console.error('对比失败:', error)
    ElMessage.error(error.message || '对比失败')
  } finally {
    clearInterval(progressTimer)
    generating.value = false
    generateProgress.value = 0
  }
}

// 导出为Markdown
function exportToMarkdown() {
  if (!comparisonResult.value) return

  let markdown = '# 论文对比分析\n\n'
  markdown += `生成时间: ${new Date().toLocaleString('zh-CN')}\n\n`

  // 对比论文列表
  markdown += '## 对比论文\n\n'
  markdown += '| ID | 标题 | 作者 | 年份 |\n'
  markdown += '|---|---|---|---|\n'
  comparisonResult.value.papers.forEach(paper => {
    markdown += `| ${paper.documentId} | ${paper.title} | ${paper.authors || '-'} | ${paper.year || '-'} |\n`
  })
  markdown += '\n'

  // 对比矩阵
  markdown += '## 对比分析\n\n'
  comparisonResult.value.matrix.forEach(row => {
    markdown += `### ${row.dimensionName}\n\n`
    comparisonResult.value!.papers.forEach((paper, index) => {
      markdown += `**${paper.title}:**\n\n`
      markdown += `${row.values[index]}\n\n`
    })
  })

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
  a.download = `论文对比_${new Date().getTime()}.md`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('导出成功！')
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped lang="scss">
.paper-comparison-container {
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

  .comparison-result {
    .export-actions {
      text-align: right;
      margin-bottom: 20px;
    }

    .papers-info,
    .comparison-matrix {
      margin-bottom: 30px;

      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin-bottom: 15px;
      }
    }

    .comparison-matrix {
      .dimension-row {
        margin-bottom: 30px;

        h4 {
          font-size: 16px;
          font-weight: 600;
          color: #409eff;
          margin-bottom: 15px;
          padding-left: 12px;
          border-left: 4px solid #409eff;
        }

        .el-card {
          height: 100%;
          min-height: 150px;

          .paper-title {
            font-weight: 600;
            color: #303133;
            margin-bottom: 10px;
            font-size: 13px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .value-content {
            color: #606266;
            font-size: 14px;
            line-height: 1.8;
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

<template>
  <div class="innovation-cluster-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">创新点聚合</span>
        </div>
      </template>

      <!-- 文档选择 -->
      <div class="document-select">
        <el-alert title="功能说明" type="info" :closable="false" style="margin-bottom: 20px">
          <p>选择多篇论文，AI将提取创新点并进行主题聚类，帮助您发现研究趋势和创新方向</p>
        </el-alert>

        <el-form label-width="100px">
          <el-form-item label="选择论文">
            <el-select v-model="selectedDocumentIds" multiple placeholder="请选择论文文档" style="width: 100%"
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

          <el-form-item>
            <el-button type="primary" :loading="generating" :disabled="selectedDocumentIds.length === 0"
              @click="handleGenerate">
              {{ generating ? 'AI分析中...' : '开始聚合' }}
            </el-button>
          </el-form-item>

          <el-alert v-if="generating" title="聚合中" type="warning" :closable="false">
            <p>AI正在提取创新点并进行主题聚类，这可能需要30-50秒，请耐心等待...</p>
            <el-progress :percentage="generateProgress" :indeterminate="true" style="margin-top: 10px;" />
          </el-alert>
        </el-form>
      </div>

      <!-- 聚类结果 -->
      <div v-if="clusters.length > 0" class="cluster-result">
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

        <div class="clusters-overview">
          <h3>创新主题概览</h3>
          <el-row :gutter="20">
            <el-col :span="6" v-for="cluster in clusters" :key="cluster.topic">
              <el-card shadow="hover" class="cluster-card">
                <div class="cluster-topic">{{ cluster.topic }}</div>
                <div class="cluster-meta">
                  <el-tag type="warning">重要性: {{ cluster.importance.toFixed(2) }}</el-tag>
                  <el-tag type="info">{{ cluster.paperCount }} 篇论文</el-tag>
                  <el-tag type="success">{{ cluster.innovations.length }} 个创新点</el-tag>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <el-divider />

        <!-- 详细创新点 -->
        <div class="clusters-detail">
          <h3>创新点详情</h3>
          <el-collapse v-model="activeCollapse">
            <el-collapse-item v-for="cluster in clusters" :key="cluster.topic" :name="cluster.topic">
              <template #title>
                <div class="collapse-title">
                  <strong>{{ cluster.topic }}</strong>
                  <el-tag size="small" style="margin-left: 12px;">{{ cluster.innovations.length }} 个创新点</el-tag>
                </div>
              </template>

              <el-table :data="cluster.innovations" style="width: 100%">
                <el-table-column label="创新点描述" min-width="300">
                  <template #default="{ row }">
                    <p style="margin: 0; line-height: 1.6;">{{ row.description }}</p>
                  </template>
                </el-table-column>
                <el-table-column prop="paperTitle" label="来源论文" width="200" show-overflow-tooltip />
                <el-table-column prop="documentId" label="文档ID" width="100" />
                <el-table-column label="新颖性分数" width="120">
                  <template #default="{ row }">
                    <el-progress :percentage="row.noveltyScore * 100" :color="getScoreColor(row.noveltyScore)" />
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
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
import { aggregateInnovations } from '@/api/analysis'
import type { DocumentListVO, InnovationClusterVO } from '@/types'

const loadingDocuments = ref(false)
const generating = ref(false)
const generateProgress = ref(0)
const documentList = ref<DocumentListVO[]>([])
const selectedDocumentIds = ref<number[]>([])
const clusters = ref<InnovationClusterVO[]>([])
const activeCollapse = ref<string[]>([])

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
  clusters.value = []
  activeCollapse.value = []
}

// 生成聚合
async function handleGenerate() {
  if (selectedDocumentIds.value.length === 0) {
    ElMessage.warning('请至少选择一篇论文')
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
    const response = await aggregateInnovations(selectedDocumentIds.value)

    console.log('创新点聚合响应:', response)

    if (response.success && response.data) {
      clusters.value = response.data
      // 默认展开第一个主题
      if (clusters.value.length > 0 && clusters.value[0]) {
        activeCollapse.value = [clusters.value[0].topic]
      }
      generateProgress.value = 100
      ElMessage.success('聚合完成！')
    } else {
      ElMessage.error(response.msg || '聚合失败')
    }
  } catch (error: any) {
    console.error('聚合失败:', error)
    ElMessage.error(error.message || '聚合失败')
  } finally {
    clearInterval(progressTimer)
    generating.value = false
    generateProgress.value = 0
  }
}

// 根据分数获取颜色
function getScoreColor(score: number) {
  if (score >= 0.8) return '#67c23a'
  if (score >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

// 导出为Markdown
function exportToMarkdown() {
  if (clusters.value.length === 0) return

  let markdown = '# 创新点聚合分析\n\n'
  markdown += `生成时间: ${new Date().toLocaleString('zh-CN')}\n\n`

  // 概览
  markdown += '## 创新主题概览\n\n'
  markdown += '| 主题 | 重要性 | 论文数 | 创新点数 |\n'
  markdown += '|---|---|---|---|\n'
  clusters.value.forEach(cluster => {
    markdown += `| ${cluster.topic} | ${cluster.importance.toFixed(2)} | ${cluster.paperCount} | ${cluster.innovations.length} |\n`
  })
  markdown += '\n'

  // 详细创新点
  markdown += '## 创新点详情\n\n'
  clusters.value.forEach(cluster => {
    markdown += `### ${cluster.topic}\n\n`
    markdown += `**重要性:** ${cluster.importance.toFixed(2)} | **论文数:** ${cluster.paperCount} | **创新点数:** ${cluster.innovations.length}\n\n`

    cluster.innovations.forEach((innovation, index) => {
      markdown += `#### ${index + 1}. ${innovation.paperTitle}\n\n`
      markdown += `- **描述:** ${innovation.description}\n`
      markdown += `- **文档ID:** ${innovation.documentId}\n`
      markdown += `- **新颖性分数:** ${(innovation.noveltyScore * 100).toFixed(1)}%\n\n`
    })
  })

  // 下载文件
  const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `创新点聚合_${new Date().getTime()}.md`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('导出成功！')
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped lang="scss">
.innovation-cluster-container {
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

  .cluster-result {
    .export-actions {
      text-align: right;
      margin-bottom: 20px;
    }

    .clusters-overview,
    .clusters-detail {
      margin-bottom: 30px;

      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin-bottom: 15px;
      }

      .cluster-card {
        text-align: center;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }

        .cluster-topic {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 15px;
          min-height: 48px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .cluster-meta {
          display: flex;
          flex-direction: column;
          gap: 8px;

          .el-tag {
            width: 100%;
          }
        }
      }
    }

    .clusters-detail {
      .collapse-title {
        display: flex;
        align-items: center;
        width: 100%;

        strong {
          font-size: 16px;
        }
      }
    }
  }
}
</style>

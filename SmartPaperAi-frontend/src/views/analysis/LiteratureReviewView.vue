<template>
  <div class="literature-review-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">文献综述生成</span>
        </div>
      </template>

      <!-- 文档选择 -->
      <div class="document-select">
        <el-alert title="功能说明" type="info" :closable="false" style="margin-bottom: 20px">
          <p>选择多篇论文，AI将综合分析并生成结构化综述，包括研究现状、方法对比、发展趋势、研究空白和未来方向</p>
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
              {{ generating ? 'AI生成中...' : '生成综述' }}
            </el-button>
          </el-form-item>

          <el-alert v-if="generating" title="生成中" type="warning" :closable="false">
            <p>AI正在分析多篇论文并生成综述，这可能需要30-60秒，请耐心等待...</p>
            <el-progress :percentage="generateProgress" :indeterminate="true" style="margin-top: 10px;" />
          </el-alert>
        </el-form>
      </div>

      <!-- 综述结果 -->
      <div v-if="review" class="review-result">
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
        <div class="review-header">
          <h2>{{ review.topic || '文献综述' }}</h2>
          <div class="meta">
            <el-statistic title="论文总数" :value="review.paperCount" />
          </div>
        </div>

        <!-- 标签页 -->
        <el-tabs v-model="activeTab" type="border-card">
          <!-- 研究现状 -->
          <el-tab-pane label="研究现状" name="status">
            <div class="section">
              <h3>总体概述</h3>
              <p>{{ review.researchStatus.overview }}</p>
            </div>

            <div class="section" v-if="review.researchStatus.mainThemes?.length">
              <h3>主要研究主题</h3>
              <el-tag v-for="theme in review.researchStatus.mainThemes" :key="theme" style="margin: 5px" type="success">
                {{ theme }}
              </el-tag>
            </div>

            <div class="section" v-if="review.researchStatus.representativeWorks?.length">
              <h3>代表性工作</h3>
              <el-table :data="review.researchStatus.representativeWorks" style="width: 100%">
                <el-table-column prop="title" label="论文标题" min-width="200" show-overflow-tooltip />
                <el-table-column prop="contribution" label="贡献" min-width="300" />
                <el-table-column label="影响力" width="150">
                  <template #default="{ row }">
                    <el-progress :percentage="row.impactScore * 100" :color="getImpactColor(row.impactScore)" />
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 方法对比 -->
          <el-tab-pane label="方法对比" name="methodology">
            <div class="section">
              <h3>总结</h3>
              <p>{{ review.methodologyComparison.summary }}</p>
            </div>

            <div class="section" v-if="review.methodologyComparison.categories?.length">
              <h3>方法分类</h3>
              <div v-for="category in review.methodologyComparison.categories" :key="category.categoryName"
                class="method-category">
                <h4>{{ category.categoryName }}</h4>
                <p><strong>优缺点：</strong>{{ category.prosAndCons }}</p>
                <div class="doc-ids">
                  <el-tag v-for="docId in category.documentIds" :key="docId" size="small">
                    文档 {{ docId }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div class="section" v-if="review.methodologyComparison.evolution">
              <h3>方法演进</h3>
              <p>{{ review.methodologyComparison.evolution }}</p>
            </div>
          </el-tab-pane>

          <!-- 趋势分析 -->
          <el-tab-pane label="趋势分析" name="trend">
            <div class="section" v-if="review.trendAnalysis.timeline">
              <h3>时间线</h3>
              <p>{{ review.trendAnalysis.timeline }}</p>
            </div>

            <div class="section" v-if="review.trendAnalysis.hotTopics?.length">
              <h3>热点话题</h3>
              <el-tag v-for="topic in review.trendAnalysis.hotTopics" :key="topic" style="margin: 5px" type="danger">
                🔥 {{ topic }}
              </el-tag>
            </div>

            <div class="section" v-if="review.trendAnalysis.emergingTechnologies?.length">
              <h3>新兴技术</h3>
              <el-tag v-for="tech in review.trendAnalysis.emergingTechnologies" :key="tech" style="margin: 5px"
                type="warning">
                ⭐ {{ tech }}
              </el-tag>
            </div>

            <div class="section" v-if="review.trendAnalysis.focusShift">
              <h3>研究重点转移</h3>
              <p>{{ review.trendAnalysis.focusShift }}</p>
            </div>
          </el-tab-pane>

          <!-- 研究空白 -->
          <el-tab-pane label="研究空白" name="gaps">
            <div class="section" v-if="review.researchGaps.unsolvedProblems?.length">
              <h3>未解决的问题</h3>
              <ul>
                <li v-for="problem in review.researchGaps.unsolvedProblems" :key="problem">
                  {{ problem }}
                </li>
              </ul>
            </div>

            <div class="section" v-if="review.researchGaps.methodologicalLimitations?.length">
              <h3>方法局限性</h3>
              <ul>
                <li v-for="limitation in review.researchGaps.methodologicalLimitations" :key="limitation">
                  {{ limitation }}
                </li>
              </ul>
            </div>

            <div class="section" v-if="review.researchGaps.dataGaps?.length">
              <h3>数据缺口</h3>
              <ul>
                <li v-for="gap in review.researchGaps.dataGaps" :key="gap">
                  {{ gap }}
                </li>
              </ul>
            </div>
          </el-tab-pane>

          <!-- 未来方向 -->
          <el-tab-pane label="未来方向" name="future">
            <div class="section" v-if="review.futureDirections.directions?.length">
              <h3>研究方向</h3>
              <ul>
                <li v-for="direction in review.futureDirections.directions" :key="direction">
                  {{ direction }}
                </li>
              </ul>
            </div>

            <div class="section" v-if="review.futureDirections.interdisciplinaryOpportunities?.length">
              <h3>跨学科机会</h3>
              <ul>
                <li v-for="opportunity in review.futureDirections.interdisciplinaryOpportunities" :key="opportunity">
                  {{ opportunity }}
                </li>
              </ul>
            </div>

            <div class="section" v-if="review.futureDirections.applicationProspects">
              <h3>应用前景</h3>
              <p>{{ review.futureDirections.applicationProspects }}</p>
            </div>
          </el-tab-pane>

          <!-- 关键词云 -->
          <el-tab-pane label="关键词" name="keywords">
            <div class="keywords-cloud" v-if="review.keywordCloud?.length">
              <el-tag v-for="kw in review.keywordCloud" :key="kw.keyword"
                :style="{ fontSize: getKeywordSize(kw.frequency) + 'px', margin: '8px' }"
                :type="getKeywordType(kw.frequency)">
                {{ kw.keyword }} ({{ kw.frequency }})
              </el-tag>
            </div>
          </el-tab-pane>
        </el-tabs>

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
import { generateLiteratureReview } from '@/api/analysis'
import type { DocumentListVO, LiteratureReviewVO, CitationVO } from '@/types'

const loadingDocuments = ref(false)
const generating = ref(false)
const generateProgress = ref(0)
const documentList = ref<DocumentListVO[]>([])
const selectedDocumentIds = ref<number[]>([])
const review = ref<LiteratureReviewVO | null>(null)
const citations = ref<CitationVO[]>([])
const activeTab = ref('status')

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
  review.value = null
  citations.value = []
}

// 生成综述
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
  }, 1500)

  try {
    const response = await generateLiteratureReview(selectedDocumentIds.value)

    console.log('文献综述响应:', response)

    if (response.success && response.data) {
      review.value = response.data.answer
      citations.value = response.data.citations || []
      generateProgress.value = 100
      ElMessage.success('综述生成成功！')
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

// 影响力颜色
function getImpactColor(score: number) {
  if (score >= 0.8) return '#67c23a'
  if (score >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

// 关键词字体大小
function getKeywordSize(frequency: number) {
  return Math.min(12 + frequency * 2, 32)
}

// 关键词类型
function getKeywordType(frequency: number): any {
  if (frequency >= 10) return 'danger'
  if (frequency >= 5) return 'warning'
  return 'info'
}

// 导出为Markdown
function exportToMarkdown() {
  if (!review.value) return

  let markdown = '# 文献综述\n\n'
  markdown += `生成时间: ${new Date().toLocaleString('zh-CN')}\n\n`

  // 基本信息
  markdown += `## ${review.value.topic || '文献综述'}\n\n`
  markdown += `**论文总数:** ${review.value.paperCount}\n\n`

  // 研究现状
  markdown += '## 研究现状\n\n'
  markdown += `${review.value.researchStatus.overview}\n\n`

  if (review.value.researchStatus.mainThemes?.length) {
    markdown += '### 主要研究主题\n\n'
    review.value.researchStatus.mainThemes.forEach(theme => {
      markdown += `- ${theme}\n`
    })
    markdown += '\n'
  }

  if (review.value.researchStatus.representativeWorks?.length) {
    markdown += '### 代表性工作\n\n'
    markdown += '| 论文标题 | 贡献 | 影响力 |\n'
    markdown += '|---|---|---|\n'
    review.value.researchStatus.representativeWorks.forEach(work => {
      markdown += `| ${work.title} | ${work.contribution} | ${(work.impactScore * 100).toFixed(1)}% |\n`
    })
    markdown += '\n'
  }

  // 方法对比
  markdown += '## 方法对比\n\n'
  markdown += `${review.value.methodologyComparison.summary}\n\n`

  if (review.value.methodologyComparison.categories?.length) {
    markdown += '### 方法分类\n\n'
    review.value.methodologyComparison.categories.forEach(category => {
      markdown += `#### ${category.categoryName}\n\n`
      markdown += `**优缺点:** ${category.prosAndCons}\n\n`
      markdown += `**相关文档:** ${category.documentIds.join(', ')}\n\n`
    })
  }

  if (review.value.methodologyComparison.evolution) {
    markdown += '### 方法演进\n\n'
    markdown += `${review.value.methodologyComparison.evolution}\n\n`
  }

  // 趋势分析
  markdown += '## 趋势分析\n\n'

  if (review.value.trendAnalysis.timeline) {
    markdown += '### 时间线\n\n'
    markdown += `${review.value.trendAnalysis.timeline}\n\n`
  }

  if (review.value.trendAnalysis.hotTopics?.length) {
    markdown += '### 热点话题\n\n'
    review.value.trendAnalysis.hotTopics.forEach(topic => {
      markdown += `- 🔥 ${topic}\n`
    })
    markdown += '\n'
  }

  if (review.value.trendAnalysis.emergingTechnologies?.length) {
    markdown += '### 新兴技术\n\n'
    review.value.trendAnalysis.emergingTechnologies.forEach(tech => {
      markdown += `- ⭐ ${tech}\n`
    })
    markdown += '\n'
  }

  if (review.value.trendAnalysis.focusShift) {
    markdown += '### 研究重点转移\n\n'
    markdown += `${review.value.trendAnalysis.focusShift}\n\n`
  }

  // 研究空白
  markdown += '## 研究空白\n\n'

  if (review.value.researchGaps.unsolvedProblems?.length) {
    markdown += '### 未解决的问题\n\n'
    review.value.researchGaps.unsolvedProblems.forEach(problem => {
      markdown += `- ${problem}\n`
    })
    markdown += '\n'
  }

  if (review.value.researchGaps.methodologicalLimitations?.length) {
    markdown += '### 方法局限性\n\n'
    review.value.researchGaps.methodologicalLimitations.forEach(limitation => {
      markdown += `- ${limitation}\n`
    })
    markdown += '\n'
  }

  if (review.value.researchGaps.dataGaps?.length) {
    markdown += '### 数据缺口\n\n'
    review.value.researchGaps.dataGaps.forEach(gap => {
      markdown += `- ${gap}\n`
    })
    markdown += '\n'
  }

  // 未来方向
  markdown += '## 未来方向\n\n'

  if (review.value.futureDirections.directions?.length) {
    markdown += '### 研究方向\n\n'
    review.value.futureDirections.directions.forEach(direction => {
      markdown += `- ${direction}\n`
    })
    markdown += '\n'
  }

  if (review.value.futureDirections.interdisciplinaryOpportunities?.length) {
    markdown += '### 跨学科机会\n\n'
    review.value.futureDirections.interdisciplinaryOpportunities.forEach(opportunity => {
      markdown += `- ${opportunity}\n`
    })
    markdown += '\n'
  }

  if (review.value.futureDirections.applicationProspects) {
    markdown += '### 应用前景\n\n'
    markdown += `${review.value.futureDirections.applicationProspects}\n\n`
  }

  // 关键词云
  if (review.value.keywordCloud?.length) {
    markdown += '## 关键词\n\n'
    review.value.keywordCloud.forEach(kw => {
      markdown += `- **${kw.keyword}** (${kw.frequency})\n`
    })
    markdown += '\n'
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
  a.download = `文献综述_${review.value.topic || '综述'}_${new Date().getTime()}.md`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('导出成功！')
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped lang="scss">
.literature-review-container {
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

  .review-result {
    .export-actions {
      text-align: right;
      margin-bottom: 20px;
    }

    .review-header {
      text-align: center;
      margin: 20px 0;

      h2 {
        font-size: 24px;
        margin-bottom: 20px;
        color: #333;
      }

      .meta {
        display: flex;
        justify-content: center;
        gap: 40px;
      }
    }

    .el-tabs {
      margin-top: 20px;
    }

    .section {
      margin-bottom: 30px;

      &:last-child {
        margin-bottom: 0;
      }

      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin-bottom: 15px;
        padding-left: 12px;
        border-left: 4px solid #409eff;
      }

      h4 {
        font-size: 16px;
        font-weight: 600;
        color: #409eff;
        margin-bottom: 10px;
      }

      p {
        line-height: 1.8;
        color: #606266;
        text-align: justify;
        margin-bottom: 15px;
      }

      ul {
        padding-left: 25px;

        li {
          line-height: 2;
          color: #606266;
          margin-bottom: 8px;
        }
      }

      .method-category {
        background: #f5f7fa;
        padding: 15px;
        border-radius: 8px;
        margin-bottom: 15px;

        .doc-ids {
          margin-top: 10px;
        }
      }
    }

    .keywords-cloud {
      padding: 20px;
      text-align: center;
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

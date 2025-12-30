<template>
  <div class="knowledge-graph-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">知识图谱</span>
        </div>
      </template>

      <!-- 模式选择 -->
      <div class="mode-selector">
        <el-segmented v-model="graphMode" :options="modeOptions" size="large" />
      </div>

      <!-- 全局图谱 -->
      <div v-if="graphMode === 'global'" class="graph-config">
        <el-form :inline="true">
          <el-form-item label="查询关键词">
            <el-input v-model="globalQuery" placeholder="输入关键词筛选相关概念（可选）" style="width: 300px;" clearable />
          </el-form-item>
          <el-form-item label="节点数量">
            <el-input-number v-model="globalLimit" :min="10" :max="500" :step="10" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="loadGlobalGraph">
              生成全局图谱
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 文档图谱 -->
      <div v-if="graphMode === 'document'" class="graph-config">
        <el-form :inline="true">
          <el-form-item label="选择文档">
            <el-select v-model="selectedDocumentId" placeholder="请选择文档" style="width: 300px;" filterable
              :loading="loadingDocuments">
              <el-option v-for="doc in documentList" :key="doc.id" :label="doc.title" :value="doc.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="查询内容">
            <el-input v-model="documentQuery" placeholder="输入查询内容提取相关概念" style="width: 300px;" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" :disabled="!selectedDocumentId || !documentQuery"
              @click="loadDocumentGraph">
              生成文档图谱
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 知识库图谱 -->
      <div v-if="graphMode === 'knowledgeBase'" class="graph-config">
        <el-form :inline="true">
          <el-form-item label="选择知识库">
            <el-select v-model="selectedKbId" placeholder="请选择知识库" style="width: 300px;" filterable
              :loading="loadingKbs">
              <el-option v-for="kb in knowledgeBaseList" :key="kb.id" :label="kb.name" :value="kb.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="查询关键词">
            <el-input v-model="kbQuery" placeholder="输入关键词筛选相关概念（可选）" style="width: 300px;" clearable />
          </el-form-item>
          <el-form-item label="节点数量">
            <el-input-number v-model="kbLimit" :min="10" :max="200" :step="10" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" :disabled="!selectedKbId" @click="loadKbGraph">
              生成知识库图谱
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <!-- 统计信息 -->
      <div v-if="graphData" class="graph-stats">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="节点总数" :value="graphData.stats.totalNodes">
              <template #suffix>个</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="关系总数" :value="graphData.stats.totalRelationships">
              <template #suffix>个</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="概念节点" :value="graphData.stats.conceptNodes">
              <template #suffix>个</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="文档节点" :value="graphData.stats.documentNodes">
              <template #suffix>个</template>
            </el-statistic>
          </el-col>
        </el-row>
      </div>

      <!-- 图谱可视化区域 -->
      <div v-if="graphData" class="graph-visualization">
        <div ref="graphContainer" class="graph-container"></div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!graphData && !loading" description="请配置参数并生成知识图谱" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getGlobalKnowledgeGraph, getDocumentKnowledgeGraph, getKnowledgeBaseKnowledgeGraph } from '@/api/knowledgeGraph'
import { getDocumentList } from '@/api/document'
import { getKnowledgeBaseList } from '@/api/knowledge'
import type { KnowledgeGraphVO, DocumentListVO, KnowledgeBaseDO } from '@/types'

// 模式选择
const graphMode = ref<'global' | 'document' | 'knowledgeBase'>('global')
const modeOptions = [
  { label: '全局图谱', value: 'global' },
  { label: '文档图谱', value: 'document' },
  { label: '知识库图谱', value: 'knowledgeBase' }
]

// 全局图谱参数
const globalQuery = ref('')
const globalLimit = ref(100)

// 文档图谱参数
const selectedDocumentId = ref<number>()
const documentQuery = ref('')
const documentList = ref<DocumentListVO[]>([])
const loadingDocuments = ref(false)

// 知识库图谱参数
const selectedKbId = ref<number>()
const kbQuery = ref('')
const kbLimit = ref(50)
const knowledgeBaseList = ref<KnowledgeBaseDO[]>([])
const loadingKbs = ref(false)

// 图谱数据
const graphData = ref<KnowledgeGraphVO | null>(null)
const loading = ref(false)

// 图表容器
const graphContainer = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

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

// 加载知识库列表
async function loadKnowledgeBases() {
  loadingKbs.value = true
  try {
    const response = await getKnowledgeBaseList({
      current: 1,
      size: 100
    })
    if (response.success && response.data) {
      knowledgeBaseList.value = response.data.records || []
    }
  } catch (error: any) {
    console.error('加载知识库列表失败:', error)
  } finally {
    loadingKbs.value = false
  }
}

// 加载全局图谱
async function loadGlobalGraph() {
  loading.value = true
  try {
    const response = await getGlobalKnowledgeGraph({
      query: globalQuery.value || undefined,
      limit: globalLimit.value
    })

    if (response.success && response.data) {
      graphData.value = response.data
      await nextTick()
      renderGraph()
      ElMessage.success('全局图谱生成成功！')
    } else {
      ElMessage.error(response.msg || '生成失败')
    }
  } catch (error: any) {
    console.error('生成全局图谱失败:', error)
    ElMessage.error(error.message || '生成失败')
  } finally {
    loading.value = false
  }
}

// 加载文档图谱
async function loadDocumentGraph() {
  if (!selectedDocumentId.value || !documentQuery.value) {
    ElMessage.warning('请选择文档并输入查询内容')
    return
  }

  loading.value = true
  try {
    const response = await getDocumentKnowledgeGraph({
      documentId: selectedDocumentId.value,
      query: documentQuery.value
    })

    if (response.success && response.data) {
      graphData.value = response.data
      await nextTick()
      renderGraph()
      ElMessage.success('文档图谱生成成功！')
    } else {
      ElMessage.error(response.msg || '生成失败')
    }
  } catch (error: any) {
    console.error('生成文档图谱失败:', error)
    ElMessage.error(error.message || '生成失败')
  } finally {
    loading.value = false
  }
}

// 加载知识库图谱
async function loadKbGraph() {
  if (!selectedKbId.value) {
    ElMessage.warning('请选择知识库')
    return
  }

  loading.value = true
  try {
    const response = await getKnowledgeBaseKnowledgeGraph({
      knowledgeBaseId: selectedKbId.value,
      query: kbQuery.value || undefined,
      limit: kbLimit.value
    })

    if (response.success && response.data) {
      graphData.value = response.data
      await nextTick()
      renderGraph()
      ElMessage.success('知识库图谱生成成功！')
    } else {
      ElMessage.error(response.msg || '生成失败')
    }
  } catch (error: any) {
    console.error('生成知识库图谱失败:', error)
    ElMessage.error(error.message || '生成失败')
  } finally {
    loading.value = false
  }
}

// 渲染图谱
function renderGraph() {
  if (!graphContainer.value || !graphData.value) return

  // 销毁旧实例
  if (chartInstance) {
    chartInstance.dispose()
  }

  // 创建新实例
  chartInstance = echarts.init(graphContainer.value)

  // 转换数据格式
  const nodes = graphData.value.nodes.map(node => ({
    id: node.id,
    name: node.label,
    category: node.type,
    symbolSize: getNodeSize(node),
    value: node.properties.importance || 0.5,
    label: {
      show: true
    },
    itemStyle: {
      color: getNodeColor(node.type)
    }
  }))

  const links = graphData.value.relationships.map(rel => ({
    source: rel.source,
    target: rel.target,
    name: rel.label,
    lineStyle: {
      width: (rel.properties.weight || 0.5) * 3,
      curveness: 0.2
    }
  }))

  const categories = [
    { name: 'Concept' },
    { name: 'Document' },
    { name: 'Author' }
  ]

  const option = {
    title: {
      text: '知识图谱',
      left: 'center'
    },
    tooltip: {
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const node = graphData.value!.nodes.find(n => n.id === params.data.id)
          if (node) {
            let html = `<strong>${node.label}</strong><br/>`
            html += `类型: ${node.type}<br/>`
            if (node.properties.description) {
              html += `描述: ${node.properties.description}<br/>`
            }
            if (node.properties.importance) {
              html += `重要度: ${(node.properties.importance * 100).toFixed(1)}%<br/>`
            }
            if (node.properties.frequency) {
              html += `频次: ${node.properties.frequency}`
            }
            return html
          }
        } else if (params.dataType === 'edge') {
          return `${params.data.name}`
        }
        return params.name
      }
    },
    legend: {
      data: categories.map(c => c.name),
      top: 30
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes,
        links: links,
        categories: categories,
        roam: true,
        label: {
          position: 'right',
          formatter: '{b}'
        },
        labelLayout: {
          hideOverlap: true
        },
        scaleLimit: {
          min: 0.4,
          max: 2
        },
        force: {
          repulsion: 500,
          edgeLength: 150
        }
      }
    ]
  }

  chartInstance.setOption(option)

  // 响应式调整
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
}

// 获取节点大小
function getNodeSize(node: any) {
  const baseSize = 30
  const importance = node.properties.importance || 0.5
  return baseSize + importance * 40
}

// 获取节点颜色
function getNodeColor(type: string) {
  const colorMap: Record<string, string> = {
    'Concept': '#5470c6',
    'Document': '#91cc75',
    'Author': '#fac858'
  }
  return colorMap[type] || '#73c0de'
}

// 监听模式切换，清空数据
watch(graphMode, () => {
  graphData.value = null
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

onMounted(() => {
  loadDocuments()
  loadKnowledgeBases()
})
</script>

<style scoped lang="scss">
.knowledge-graph-container {
  .card-header {
    .title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }

  .mode-selector {
    margin-bottom: 20px;
    display: flex;
    justify-content: center;
  }

  .graph-config {
    margin-bottom: 20px;
  }

  .graph-stats {
    margin-bottom: 30px;
  }

  .graph-visualization {
    .graph-container {
      width: 100%;
      height: 700px;
      border: 1px solid #e8e8e8;
      border-radius: 4px;
    }
  }
}
</style>

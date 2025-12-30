<template>
  <div class="teaching-edit-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span class="title">AI生成教学设计</span>
          <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
        </div>
      </template>

      <!-- 第一步：输入生成参数 -->
      <div v-if="!generatedPlan" class="generate-form">
        <el-alert title="AI助手" type="info" :closable="false" style="margin-bottom: 20px">
          <p>请输入课题、学段和学科，AI将为您生成完整的教学设计方案</p>
          <p style="margin-top: 8px; font-size: 12px; color: #909399;">
            提示：生成过程可能需要10-30秒，请耐心等待
          </p>
        </el-alert>

        <el-form ref="generateFormRef" :model="generateParams" :rules="generateRules" label-width="120px">
          <el-form-item label="课题名称" prop="topic">
            <el-input v-model="generateParams.topic" placeholder="例如：圆的面积" maxlength="100" show-word-limit
              :disabled="generating" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="学段" prop="grade">
                <el-input v-model="generateParams.grade" placeholder="例如：小学、初中、高中" maxlength="50"
                  :disabled="generating" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="学科" prop="subject">
                <el-input v-model="generateParams.subject" placeholder="例如：数学、语文、英语" maxlength="50"
                  :disabled="generating" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="参考文档">
            <el-select v-model="selectedDocumentIds" multiple placeholder="请选择参考文档（可选）" style="width: 100%"
              :disabled="generating" :loading="loadingDocuments" collapse-tags collapse-tags-tooltip filterable>
              <template #empty>
                <el-empty description="暂无可用文档" :image-size="60" />
              </template>
              <el-option v-for="doc in documentList" :key="doc.id" :label="doc.title" :value="doc.id">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                    {{ doc.title }}
                  </span>
                  <el-tag size="small" type="info" style="margin-left: 8px;">{{ doc.type }}</el-tag>
                </div>
              </el-option>
            </el-select>
            <div style="margin-top: 8px; font-size: 12px; color: #909399;">
              <span v-if="selectedDocumentIds.length > 0">
                已选择 {{ selectedDocumentIds.length }} 个文档 ·
              </span>
              提示：选择相关文档后，AI将基于这些文档内容生成更精准的教学设计
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="generating" :icon="MagicStick" @click="handleGenerate">
              {{ generating ? 'AI生成中...' : 'AI生成教学设计' }}
            </el-button>
            <el-button @click="router.back()" :disabled="generating">取消</el-button>
          </el-form-item>

          <el-alert v-if="generating" title="生成中" type="warning" :closable="false" style="margin-top: 20px">
            <p>AI正在为您生成教学设计，请稍候...</p>
            <el-progress :percentage="generateProgress" :indeterminate="true" style="margin-top: 10px;" />
          </el-alert>
        </el-form>
      </div>

      <!-- 第二步：展示生成结果并允许编辑保存 -->
      <div v-else class="generated-content">
        <el-alert title="生成成功" type="success" :closable="false" style="margin-bottom: 20px">
          <p>AI已为您生成教学设计，您可以直接保存或点击"重新生成"获取新的方案</p>
        </el-alert>

        <div class="plan-preview">
          <h2>{{ generatedPlan.title }}</h2>
          <div class="meta">
            <el-tag size="large">{{ generatedPlan.grade }}</el-tag>
            <el-tag size="large" type="success">{{ generatedPlan.subject }}</el-tag>
            <el-tag size="large" type="info">{{ generatedPlan.duration }}</el-tag>
          </div>
        </div>

        <el-divider />

        <!-- 展示生成的内容 -->
        <div class="detail-section">
          <h3>教学目标</h3>
          <div class="objectives">
            <div v-if="generatedPlan.objectives.knowledge?.length">
              <strong>知识目标：</strong>
              <ul>
                <li v-for="(item, index) in generatedPlan.objectives.knowledge" :key="index">{{ item }}
                </li>
              </ul>
            </div>
            <div v-if="generatedPlan.objectives.skills?.length">
              <strong>能力目标：</strong>
              <ul>
                <li v-for="(item, index) in generatedPlan.objectives.skills" :key="index">{{ item }}</li>
              </ul>
            </div>
            <div v-if="generatedPlan.objectives.values?.length">
              <strong>情感态度价值观：</strong>
              <ul>
                <li v-for="(item, index) in generatedPlan.objectives.values" :key="index">{{ item }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h3>教学重点</h3>
          <ul>
            <li v-for="(point, index) in generatedPlan.keyPoints" :key="index">{{ point }}</li>
          </ul>
        </div>

        <div class="detail-section">
          <h3>教学难点</h3>
          <ul>
            <li v-for="(difficulty, index) in generatedPlan.difficulties" :key="index">{{ difficulty }}</li>
          </ul>
        </div>

        <div class="detail-section">
          <h3>教学过程</h3>
          <div class="steps">
            <div v-for="step in generatedPlan.teachingSteps" :key="step.step" class="step-item">
              <h4>第{{ step.step }}步：{{ step.name }} ({{ step.duration }})</h4>
              <p>{{ step.content }}</p>
              <div v-if="step.activities?.length">
                <strong>教学活动：</strong>
                <ul>
                  <li v-for="(activity, index) in step.activities" :key="index">{{ activity }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="generatedPlan.assignments?.length">
          <h3>作业布置</h3>
          <ul>
            <li v-for="(assignment, index) in generatedPlan.assignments" :key="index">{{ assignment }}</li>
          </ul>
        </div>

        <div class="detail-section" v-if="generatedPlan.resources?.length">
          <h3>教学资源</h3>
          <ul>
            <li v-for="(resource, index) in generatedPlan.resources" :key="index">{{ resource }}</li>
          </ul>
        </div>

        <!-- 引用来源 -->
        <div class="detail-section" v-if="citations?.length">
          <h3>引用来源</h3>
          <div class="citations">
            <el-card v-for="citation in citations" :key="citation.chunkId" class="citation-card" shadow="hover">
              <div class="citation-header">
                <strong>{{ citation.title }}</strong>
                <el-tag size="small" type="success">相关度: {{ (citation.score * 100).toFixed(1) }}%</el-tag>
              </div>
              <p class="citation-content">{{ citation.snippet }}</p>
            </el-card>
          </div>
        </div>

        <!-- 使用的参考文档 -->
        <div class="detail-section" v-if="selectedDocumentIds.length > 0">
          <h3>参考文档</h3>
          <div class="reference-docs">
            <el-tag v-for="docId in selectedDocumentIds" :key="docId" size="large" type="info" style="margin: 5px;">
              {{documentList.find(d => String(d.id) === String(docId))?.title || `文档 ${docId}`}}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <div class="action-buttons">
          <el-button type="primary" :loading="saving" :icon="Check" @click="handleSave">
            保存教学设计
          </el-button>
          <el-button :loading="generating" :icon="MagicStick" @click="handleRegenerate">
            {{ generating ? '重新生成中...' : '重新生成' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, MagicStick, Check } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { generateTeachingPlan, createTeachingPlan } from '@/api/teaching'
import { getDocumentList } from '@/api/document'
import type { TeachingPlanVO, CitationVO, DocumentListVO } from '@/types'

const router = useRouter()

const loading = ref(false)
const generating = ref(false)
const saving = ref(false)
const generateFormRef = ref<FormInstance>()
const generateProgress = ref(0)

// 文档列表相关
const documentList = ref<DocumentListVO[]>([])
const loadingDocuments = ref(false)
const selectedDocumentIds = ref<number[]>([])

// AI生成参数
const generateParams = reactive({
  topic: '',
  grade: '',
  subject: ''
})

// 生成规则
const generateRules: FormRules = {
  topic: [{ required: true, message: '请输入课题名称', trigger: 'blur' }],
  grade: [{ required: true, message: '请选择学段', trigger: 'change' }],
  subject: [{ required: true, message: '请选择学科', trigger: 'change' }]
}

// 生成的教学设计
const generatedPlan = ref<TeachingPlanVO | null>(null)
const citations = ref<CitationVO[]>([])

// 加载文档列表
async function loadDocuments() {
  loadingDocuments.value = true
  try {
    const response = await getDocumentList({
      current: 1,
      size: 100,
      status: 'completed' // 只加载已完成的文档
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

// AI生成教学设计
const handleGenerate = async () => {
  if (!generateFormRef.value) return

  await generateFormRef.value.validate(async (valid) => {
    if (!valid) return

    generating.value = true
    generateProgress.value = 0

    // 模拟进度条
    const progressTimer = setInterval(() => {
      if (generateProgress.value < 90) {
        generateProgress.value += 10
      }
    }, 1000)

    try {
      const response = await generateTeachingPlan({
        topic: generateParams.topic,
        grade: generateParams.grade,
        subject: generateParams.subject,
        documentIds: selectedDocumentIds.value.length > 0 ? selectedDocumentIds.value : undefined
      })

      console.log('生成教学设计响应:', response)

      if (response.success && response.data) {
        generatedPlan.value = response.data.answer
        citations.value = response.data.citations || []
        generateProgress.value = 100
        ElMessage.success('AI生成成功！')
      } else {
        ElMessage.error(response.msg || '生成失败，请重试')
      }
    } catch (error: any) {
      console.error('生成失败:', error)
      ElMessage.error(error.message || '生成失败，请重试')
    } finally {
      clearInterval(progressTimer)
      generating.value = false
      generateProgress.value = 0
    }
  })
}

// 保存生成的教学设计
const handleSave = async () => {
  if (!generatedPlan.value) return

  saving.value = true
  try {
    const response = await createTeachingPlan(generatedPlan.value)

    console.log('保存教学设计响应:', response)

    if (response.success) {
      ElMessage.success('保存成功')
      router.push('/teaching')
    } else {
      ElMessage.error(response.msg || '保存失败')
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 重新生成
const handleRegenerate = async () => {
  // 清空当前生成的结果，重新触发生成
  generatedPlan.value = null
  citations.value = []

  // 等待DOM更新后自动触发生成
  await new Promise(resolve => setTimeout(resolve, 100))
  handleGenerate()
}

// 组件挂载时加载文档列表
onMounted(() => {
  loadDocuments()
})
</script>

<style scoped lang="scss">
.teaching-edit-container {
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

  .generate-form {
    max-width: 800px;
    margin: 0 auto;
    padding: 20px;

    :deep(.el-select) {
      width: 100%;
    }

    :deep(.el-select__tags) {
      max-width: 100%;
    }
  }

  .generated-content {
    .plan-preview {
      text-align: center;
      margin-bottom: 20px;

      h2 {
        font-size: 24px;
        margin-bottom: 15px;
      }

      .meta {
        display: flex;
        gap: 10px;
        justify-content: center;

        .el-tag {
          font-size: 14px;
        }
      }
    }

    .detail-section {
      margin-bottom: 30px;

      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin-bottom: 15px;
        padding-bottom: 8px;
        border-bottom: 2px solid #409eff;
      }

      .objectives {
        div {
          margin-bottom: 15px;

          strong {
            color: #606266;
          }

          ul {
            margin-top: 8px;
            padding-left: 25px;

            li {
              line-height: 1.8;
              color: #606266;
            }
          }
        }
      }

      ul {
        padding-left: 25px;

        li {
          line-height: 1.8;
          color: #606266;
          margin-bottom: 5px;
        }
      }

      .steps {
        .step-item {
          background: #f5f7fa;
          padding: 20px;
          border-radius: 8px;
          margin-bottom: 15px;

          h4 {
            font-size: 16px;
            font-weight: 600;
            color: #409eff;
            margin-bottom: 10px;
          }

          p {
            line-height: 1.8;
            color: #606266;
            margin-bottom: 10px;
          }

          strong {
            color: #303133;
          }

          ul {
            margin-top: 8px;
            padding-left: 20px;

            li {
              color: #909399;
              line-height: 1.6;
            }
          }
        }
      }

      .citations {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .citation-card {
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

      .reference-docs {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }
    }

    .action-buttons {
      display: flex;
      justify-content: center;
      gap: 15px;
      padding: 20px 0;
    }
  }

  :deep(.el-form) {
    max-width: 1200px;
  }
}
</style>

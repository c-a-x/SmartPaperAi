<template>
  <div class="teaching-detail-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
          <div class="header-actions">
            <el-button type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
          </div>
        </div>
      </template>

      <div v-if="detail" class="detail-content">
        <!-- 标题和基本信息 -->
        <div class="detail-header">
          <h1 class="title">{{ detail.title }}</h1>
          <div class="meta">
            <el-tag>{{ detail.grade }}</el-tag>
            <el-tag type="success">{{ detail.subject }}</el-tag>
            <el-tag type="info">{{ detail.duration }}</el-tag>
          </div>
        </div>

        <el-divider />

        <!-- 教学目标 -->
        <div class="detail-section" v-if="detail.objectives">
          <h3 class="section-title">教学目标</h3>
          <div class="objectives-content">
            <div class="objective-item" v-if="detail.objectives.knowledge?.length">
              <h4>知识目标</h4>
              <ul>
                <li v-for="(item, index) in detail.objectives.knowledge" :key="index">{{ item }}</li>
              </ul>
            </div>
            <div class="objective-item" v-if="detail.objectives.skills?.length">
              <h4>能力目标</h4>
              <ul>
                <li v-for="(item, index) in detail.objectives.skills" :key="index">{{ item }}</li>
              </ul>
            </div>
            <div class="objective-item" v-if="detail.objectives.values?.length">
              <h4>情感态度价值观</h4>
              <ul>
                <li v-for="(item, index) in detail.objectives.values" :key="index">{{ item }}</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 教学重点 -->
        <div class="detail-section" v-if="detail.keyPoints?.length">
          <h3 class="section-title">教学重点</h3>
          <ul class="point-list">
            <li v-for="(point, index) in detail.keyPoints" :key="index">{{ point }}</li>
          </ul>
        </div>

        <!-- 教学难点 -->
        <div class="detail-section" v-if="detail.difficulties?.length">
          <h3 class="section-title">教学难点</h3>
          <ul class="point-list">
            <li v-for="(difficulty, index) in detail.difficulties" :key="index">{{ difficulty }}</li>
          </ul>
        </div>

        <!-- 教学步骤 -->
        <div class="detail-section" v-if="detail.teachingSteps?.length">
          <h3 class="section-title">教学过程</h3>
          <div class="teaching-steps">
            <el-timeline>
              <el-timeline-item v-for="step in detail.teachingSteps" :key="step.step"
                :timestamp="`第${step.step}步 · ${step.duration}`" placement="top">
                <el-card>
                  <h4>{{ step.name }}</h4>
                  <p class="step-content">{{ step.content }}</p>
                  <div v-if="step.activities?.length" class="activities">
                    <strong>教学活动：</strong>
                    <ul>
                      <li v-for="(activity, index) in step.activities" :key="index">{{ activity }}</li>
                    </ul>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>

        <!-- 作业布置 -->
        <div class="detail-section" v-if="detail.assignments?.length">
          <h3 class="section-title">作业布置</h3>
          <ul class="point-list">
            <li v-for="(assignment, index) in detail.assignments" :key="index">{{ assignment }}</li>
          </ul>
        </div>

        <!-- 教学资源 -->
        <div class="detail-section" v-if="detail.resources?.length">
          <h3 class="section-title">教学资源</h3>
          <ul class="point-list">
            <li v-for="(resource, index) in detail.resources" :key="index">{{ resource }}</li>
          </ul>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && !detail" description="未找到教学设计" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'
import { getTeachingPlanDetail, deleteTeachingPlan } from '@/api/teaching'
import type { TeachingPlanVO } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<TeachingPlanVO | null>(null)

// 加载详情
async function loadDetail() {
  const id = route.params.id
  if (!id) {
    ElMessage.error('参数错误')
    router.back()
    return
  }

  loading.value = true
  try {
    const response = await getTeachingPlanDetail(id)
    console.log('教学设计详情响应:', response)

    if (response.success && response.data) {
      detail.value = response.data
    } else {
      ElMessage.error(response.msg || '加载详情失败')
      router.back()
    }
  } catch (error: any) {
    console.error('加载详情失败:', error)
    ElMessage.error(error.message || '加载详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 删除
async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这个教学设计吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteTeachingPlan(route.params.id)
    ElMessage.success('删除成功')
    router.push('/teaching')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.teaching-detail-container {
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
        margin: 0 0 16px;
      }

      .meta {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }

    .detail-section {
      margin-bottom: 32px;

      &:last-child {
        margin-bottom: 0;
      }

      .section-title {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin: 0 0 16px;
        padding-left: 12px;
        border-left: 4px solid #1890ff;
      }

      .objectives-content {
        padding: 16px;
        background-color: #fafafa;
        border-radius: 4px;

        .objective-item {
          margin-bottom: 20px;

          &:last-child {
            margin-bottom: 0;
          }

          h4 {
            font-size: 16px;
            font-weight: 600;
            color: #409eff;
            margin: 0 0 8px;
          }

          ul {
            margin: 0;
            padding-left: 24px;

            li {
              line-height: 1.8;
              color: #666;
            }
          }
        }
      }

      .point-list {
        padding: 16px;
        background-color: #fafafa;
        border-radius: 4px;
        margin: 0;
        padding-left: 40px;

        li {
          line-height: 2;
          color: #666;
        }
      }

      .teaching-steps {
        :deep(.el-timeline-item__timestamp) {
          font-weight: 600;
          color: #409eff;
        }

        .el-card {
          h4 {
            font-size: 16px;
            font-weight: 600;
            color: #333;
            margin: 0 0 12px;
          }

          .step-content {
            line-height: 1.8;
            color: #666;
            margin: 0 0 16px;
            white-space: pre-wrap;
          }

          .activities {
            strong {
              color: #409eff;
            }

            ul {
              margin: 8px 0 0;
              padding-left: 24px;

              li {
                line-height: 1.8;
                color: #666;
              }
            }
          }
        }
      }
    }
  }
}
</style>

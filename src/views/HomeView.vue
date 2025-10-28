<template>
  <div class="home-container">
    <el-row :gutter="24">
      <!-- 欢迎卡片 -->
      <el-col :span="24">
        <el-card class="welcome-card">
          <div class="welcome-content">
            <div class="welcome-text">
              <h2>欢迎使用 SmartPaperAI</h2>
              <p>基于 RAG 技术的智能论文问答系统，让学术研究更高效</p>
            </div>
            <el-icon class="welcome-icon">
              <Reading />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon teaching">
              <Reading />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.teachingCount }}</div>
              <div class="stat-label">教学设计</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon document">
              <Document />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.documentCount }}</div>
              <div class="stat-label">文档数量</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon knowledge">
              <Collection />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.knowledgeCount }}</div>
              <div class="stat-label">知识库</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon chat">
              <ChatDotRound />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.chatCount }}</div>
              <div class="stat-label">对话数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-row :gutter="24" class="quick-actions-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span class="card-title">快速操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" :icon="Reading" @click="router.push('/teaching/create')">
              创建教学设计
            </el-button>
            <el-button type="success" :icon="Upload" @click="router.push('/documents')">
              上传文档
            </el-button>
            <el-button type="warning" :icon="Collection" @click="router.push('/knowledge')">
              管理知识库
            </el-button>
            <el-button type="info" :icon="ChatDotRound" @click="router.push('/chat')">
              AI对话
            </el-button>
            <el-button type="primary" plain @click="router.push('/rag-chat')">
              知识库问答
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近活动 -->
    <el-row :gutter="24">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <span class="card-title">最近教学设计</span>
          </template>
          <el-empty v-if="recentTeaching.length === 0" description="暂无数据" />
          <div v-else class="recent-list">
            <div v-for="item in recentTeaching" :key="item.id" class="recent-item"
              @click="router.push(`/teaching/${item.id}`)">
              <div class="item-info">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-meta">{{ item.grade }} · {{ item.subject }}</div>
              </div>
              <div class="item-time">{{ formatRelativeTime(item.updateTime) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <span class="card-title">最近文档</span>
          </template>
          <el-empty v-if="recentDocuments.length === 0" description="暂无数据" />
          <div v-else class="recent-list">
            <div v-for="item in recentDocuments" :key="item.id" class="recent-item" @click="router.push(`/documents`)">
              <div class="item-info">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-meta">
                  <el-tag size="small" type="info">{{ item.type }}</el-tag>
                  <span style="margin-left: 8px;">{{ formatFileSize(item.fileSize) }}</span>
                </div>
              </div>
              <div class="item-time">{{ formatRelativeTime(item.createTime) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Reading, Document, Collection, ChatDotRound, Upload } from '@element-plus/icons-vue'
import { getTeachingPlanList } from '@/api/teaching'
import { getDocumentList } from '@/api/document'
import { getKnowledgeBaseList } from '@/api/knowledge'
import { getConversationList } from '@/api/chat'
import { formatRelativeTime, formatFileSize } from '@/utils/format'
import type { TeachingPlanListVO, DocumentListVO } from '@/types'

const router = useRouter()

// 统计数据
const stats = ref({
  teachingCount: 0,
  documentCount: 0,
  knowledgeCount: 0,
  chatCount: 0
})

// 最近教学设计
const recentTeaching = ref<TeachingPlanListVO[]>([])

// 最近文档
const recentDocuments = ref<DocumentListVO[]>([])

// 加载数据
async function loadData() {
  try {
    // 加载教学设计
    const teachingRes = await getTeachingPlanList()
    if (teachingRes.success && teachingRes.data) {
      stats.value.teachingCount = teachingRes.data.length
      recentTeaching.value = teachingRes.data.slice(0, 5)
    }

    // 加载文档（分页数据）
    const docRes = await getDocumentList({
      current: 1,
      size: 10000
    })
    if (docRes.success && docRes.data) {
      stats.value.documentCount = docRes.data.total || docRes.data.records?.length || 0
      recentDocuments.value = docRes.data.records?.slice(0, 5) || []
    }

    // 加载知识库（分页数据）
    const kbRes = await getKnowledgeBaseList({
      current: 1,
      size: 10000
    })
    if (kbRes.success && kbRes.data) {
      stats.value.knowledgeCount = kbRes.data.total || kbRes.data.records?.length || 0
    }

    // 加载会话列表
    const conversationRes = await getConversationList()
    if (conversationRes.success && conversationRes.data) {
      stats.value.chatCount = conversationRes.data.length
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.home-container {
  .welcome-card {
    margin-bottom: 24px;

    .welcome-content {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .welcome-text {
        h2 {
          font-size: 28px;
          font-weight: 600;
          color: #333;
          margin: 0 0 8px;
        }

        p {
          font-size: 16px;
          color: #666;
          margin: 0;
        }
      }

      .welcome-icon {
        font-size: 80px;
        color: #1890ff;
        opacity: 0.2;
      }
    }
  }

  .stats-row {
    margin-bottom: 24px;

    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;
        gap: 16px;

        .stat-icon {
          font-size: 48px;

          &.teaching {
            color: #1890ff;
          }

          &.document {
            color: #52c41a;
          }

          &.knowledge {
            color: #faad14;
          }

          &.chat {
            color: #722ed1;
          }
        }

        .stat-info {
          flex: 1;

          .stat-value {
            font-size: 32px;
            font-weight: 600;
            color: #333;
            line-height: 1;
            margin-bottom: 8px;
          }

          .stat-label {
            font-size: 14px;
            color: #999;
          }
        }
      }
    }
  }

  .quick-actions-row {
    margin-bottom: 24px;

    .quick-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  .recent-list {
    .recent-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;
      cursor: pointer;
      transition: background-color 0.3s;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background-color: #fafafa;
      }

      .item-info {
        flex: 1;
        min-width: 0;

        .item-title {
          font-size: 14px;
          color: #333;
          margin-bottom: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .item-meta {
          font-size: 12px;
          color: #999;
        }
      }

      .item-time {
        font-size: 12px;
        color: #999;
        white-space: nowrap;
        margin-left: 12px;
      }
    }
  }
}
</style>

<template>
  <div class="rag-chat-container">
    <el-row :gutter="20" style="height: 100%">
      <!-- 左侧知识库和文档列表 -->
      <el-col :xs="24" :sm="8" :md="6" class="rag-sidebar">
        <el-card class="sidebar-card">
          <template #header>
            <el-tabs v-model="activeTab" class="resource-tabs">
              <el-tab-pane label="知识库" name="knowledge"></el-tab-pane>
              <el-tab-pane label="文档" name="document"></el-tab-pane>
            </el-tabs>
          </template>

          <!-- 知识库列表 -->
          <div v-show="activeTab === 'knowledge'" class="resource-list">
            <el-input v-model="kbSearchKeyword" placeholder="搜索知识库..." clearable class="search-input">
              <template #prefix>
                <el-icon>
                  <Search />
                </el-icon>
              </template>
            </el-input>

            <div class="list-content">
              <div v-for="kb in filteredKnowledgeBases" :key="kb.id" class="resource-item"
                :class="{ active: selectedKbId === kb.id }" @click="selectKnowledgeBase(kb.id)">
                <div class="item-icon">
                  <el-icon>
                    <Collection />
                  </el-icon>
                </div>
                <div class="item-content">
                  <div class="item-title">{{ kb.name }}</div>
                  <div class="item-meta">{{ kb.documentCount || 0 }} 篇文档</div>
                </div>
              </div>
              <el-empty v-if="filteredKnowledgeBases.length === 0" description="暂无知识库" :image-size="60" />
            </div>
          </div>

          <!-- 文档列表 -->
          <div v-show="activeTab === 'document'" class="resource-list">
            <el-input v-model="docSearchKeyword" placeholder="搜索文档..." clearable class="search-input">
              <template #prefix>
                <el-icon>
                  <Search />
                </el-icon>
              </template>
            </el-input>

            <div class="list-content">
              <div v-for="doc in filteredDocuments" :key="doc.id" class="resource-item"
                :class="{ active: selectedDocId === doc.id }" @click="selectDocument(doc.id)">
                <div class="item-icon">
                  <el-icon>
                    <Document />
                  </el-icon>
                </div>
                <div class="item-content">
                  <div class="item-title">{{ doc.title }}</div>
                  <div class="item-meta">{{ formatRelativeTime(doc.createTime) }}</div>
                </div>
              </div>
              <el-empty v-if="filteredDocuments.length === 0" description="暂无文档" :image-size="60" />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧对话区域 -->
      <el-col :xs="24" :sm="16" :md="18" class="rag-main">
        <el-card class="chat-card">
          <!-- 当前选择提示 -->
          <div class="current-selection">
            <el-tag v-if="selectedKbId" type="success" size="large">
              <el-icon>
                <Collection />
              </el-icon>
              <span>知识库: {{ currentKbName }}</span>
            </el-tag>
            <el-tag v-else-if="selectedDocId" type="primary" size="large">
              <el-icon>
                <Document />
              </el-icon>
              <span>文档: {{ currentDocName }}</span>
            </el-tag>
            <el-tag v-else type="info" size="large">
              <el-icon>
                <Warning />
              </el-icon>
              <span>请先选择知识库或文档</span>
            </el-tag>
          </div>

          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
              <div class="message-avatar">
                <el-avatar v-if="msg.role === 'user'" :size="36">
                  <el-icon>
                    <User />
                  </el-icon>
                </el-avatar>
                <el-avatar v-else :size="36" style="background-color: #1890ff">
                  <el-icon>
                    <ChatLineRound />
                  </el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-text" v-html="formatMessage(msg.content)"></div>
                <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
                  <el-divider content-position="left">引用来源</el-divider>
                  <div v-for="(source, idx) in msg.sources" :key="idx" class="source-item">
                    <div class="source-header">
                      <span class="source-name">{{ source.documentName }}</span>
                      <el-tag size="small" type="info">
                        相关度: {{ (source.score * 100).toFixed(1) }}%
                      </el-tag>
                    </div>
                    <div class="source-content">{{ source.content }}</div>
                  </div>
                </div>
                <div class="message-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>

            <!-- 加载中 -->
            <div v-if="isLoading" class="message-item assistant">
              <div class="message-avatar">
                <el-avatar :size="36" style="background-color: #1890ff">
                  <el-icon>
                    <ChatLineRound />
                  </el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-text">
                  <el-icon class="loading-icon">
                    <Loading />
                  </el-icon>
                  正在分析文档...
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <el-empty v-if="messages.length === 0 && !isLoading" description="开始向文档提问吧" :image-size="120">
              <div class="quick-questions">
                <p>你可以问我：</p>
                <el-button v-for="(q, idx) in quickQuestions" :key="idx" text @click="sendQuickQuestion(q)">
                  {{ q }}
                </el-button>
              </div>
            </el-empty>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <el-input v-model="inputMessage" type="textarea" :rows="3" placeholder="输入你的问题... (Ctrl+Enter 发送)"
              @keydown.ctrl.enter="sendMessage" :disabled="!selectedKbId && !selectedDocId" />
            <div class="input-actions">
              <div class="left-actions">
                <!-- <el-switch v-model="useGraphEnhancement" active-text="知识图谱增强" /> -->
              </div>
              <div class="right-actions">
                <el-button size="small" @click="clearMessages">清空</el-button>
                <el-button type="primary" size="small" :icon="Promotion" :loading="isLoading"
                  :disabled="!inputMessage.trim() || (!selectedKbId && !selectedDocId)" @click="sendMessage">
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Collection,
  Document,
  User,
  ChatLineRound,
  Loading,
  Promotion,
  Warning
} from '@element-plus/icons-vue'
import { documentChat, knowledgeBaseChat } from '@/api/chat'
import { getDocumentList } from '@/api/document'
import { getKnowledgeBaseList } from '@/api/knowledge'
import { formatRelativeTime } from '@/utils/format'
import type { ChatMessage, DocumentListVO, KnowledgeBaseDO } from '@/types'
import { marked } from 'marked'
import 'highlight.js/styles/github-dark.css'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

// 状态
const activeTab = ref<'knowledge' | 'document'>('knowledge')
const kbSearchKeyword = ref('')
const docSearchKeyword = ref('')
const selectedKbId = ref<number | null>(null)
const selectedDocId = ref<string | null>(null)
const knowledgeBases = ref<KnowledgeBaseDO[]>([])
const documents = ref<DocumentListVO[]>([])
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const useGraphEnhancement = ref(false)
const messageListRef = ref<HTMLElement>()

// Markdown 渲染缓存
const markdownCache = new Map<string, string>()

// 快速问题
const quickQuestions = [
  '请总结这篇文档的主要内容',
  '这个知识库包含哪些主题？',
  '文档中有哪些关键观点？',
  '请解释一下核心概念'
]

// 过滤后的知识库列表
const filteredKnowledgeBases = computed(() => {
  if (!kbSearchKeyword.value) return knowledgeBases.value
  return knowledgeBases.value.filter(kb =>
    kb.name.toLowerCase().includes(kbSearchKeyword.value.toLowerCase())
  )
})

// 过滤后的文档列表
const filteredDocuments = computed(() => {
  if (!docSearchKeyword.value) return documents.value
  return documents.value.filter(doc =>
    doc.title.toLowerCase().includes(docSearchKeyword.value.toLowerCase())
  )
})

// 当前知识库名称
const currentKbName = computed(() => {
  const kb = knowledgeBases.value.find(k => k.id === selectedKbId.value)
  return kb?.name || ''
})

// 当前文档名称
const currentDocName = computed(() => {
  const doc = documents.value.find(d => d.id === selectedDocId.value)
  return doc?.title || ''
})

// 加载知识库列表
async function loadKnowledgeBases() {
  try {
    const response = await getKnowledgeBaseList()
    if (response.success && response.data) {
      knowledgeBases.value = response.data.records || []
    }
  } catch (error: any) {
    console.error('加载知识库列表失败:', error)
  }
}

// 加载文档列表
async function loadDocuments() {
  try {
    const response = await getDocumentList()
    if (response.success && response.data) {
      documents.value = response.data.records || []
    }
  } catch (error: any) {
    console.error('加载文档列表失败:', error)
  }
}

// 选择知识库
function selectKnowledgeBase(kbId: number) {
  selectedKbId.value = kbId
  selectedDocId.value = null
  messages.value = []
  activeTab.value = 'knowledge'
}

// 选择文档
function selectDocument(docId: string) {
  selectedDocId.value = docId
  selectedKbId.value = null
  messages.value = []
  activeTab.value = 'document'
}

// 发送消息
async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return
  if (!selectedKbId.value && !selectedDocId.value) {
    ElMessage.warning('请先选择知识库或文档')
    return
  }

  // 添加用户消息
  const userMessage: ChatMessage = {
    id: Date.now(),
    role: 'user',
    content,
    createTime: new Date().toISOString()
  }
  messages.value.push(userMessage)
  inputMessage.value = ''
  scrollToBottom()

  // 发送请求
  isLoading.value = true
  try {
    let response

    if (selectedKbId.value) {
      // 知识库问答
      response = await knowledgeBaseChat(selectedKbId.value, content, useGraphEnhancement.value)
    } else if (selectedDocId.value) {
      // 文档问答
      response = await documentChat(selectedDocId.value, content, useGraphEnhancement.value)
    }

    if (response && response.success && response.data) {
      const assistantMessage: ChatMessage = {
        id: Date.now(),
        role: 'assistant',
        content: response.data.answer,
        createTime: new Date().toISOString(),
        sources: response.data.citations.map(citation => ({
          documentId: citation.documentId,
          documentName: citation.title,
          content: citation.snippet,
          score: citation.score
        }))
      }
      messages.value.push(assistantMessage)
      scrollToBottom()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发送消息失败')
  } finally {
    isLoading.value = false
  }
}

// 快速提问
function sendQuickQuestion(question: string) {
  inputMessage.value = question
  sendMessage()
}

// 清空消息
function clearMessages() {
  messages.value = []
}

// 格式化消息 - 使用 Markdown 渲染（带缓存）
function formatMessage(content: string) {
  if (!content) return ''

  // 检查缓存
  if (markdownCache.has(content)) {
    return markdownCache.get(content)!
  }

  try {
    const html = marked.parse(content) as string
    // 只缓存完整的消息
    if (content.length > 10 && !content.endsWith('```')) {
      markdownCache.set(content, html)
    }
    return html
  } catch (error) {
    console.error('Markdown 解析失败:', error)
    return content.replace(/\n/g, '<br>')
  }
}

// 格式化时间
function formatTime(timestamp: string) {
  return formatRelativeTime(timestamp)
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

onMounted(() => {
  loadKnowledgeBases()
  loadDocuments()
})
</script>

<style scoped lang="scss">
.rag-chat-container {
  height: calc(100vh - 160px);

  .rag-sidebar,
  .rag-main {
    height: 100%;
  }

  .sidebar-card,
  .chat-card {
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
  }

  .resource-tabs {
    :deep(.el-tabs__header) {
      margin: 0;
    }
  }

  .resource-list {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .search-input {
      margin-bottom: 12px;
    }

    .list-content {
      flex: 1;
      overflow-y: auto;

      .resource-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        border-radius: 8px;
        margin-bottom: 8px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          background-color: #f5f7fa;
        }

        &.active {
          background-color: #e6f7ff;
          border: 1px solid #1890ff;
        }

        .item-icon {
          font-size: 24px;
          color: #1890ff;
        }

        .item-content {
          flex: 1;
          min-width: 0;

          .item-title {
            font-size: 14px;
            font-weight: 500;
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
      }
    }
  }

  .current-selection {
    margin-bottom: 16px;

    .el-tag {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
  }

  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background-color: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 16px;

    .message-item {
      display: flex;
      gap: 12px;
      margin-bottom: 24px;

      &.user {
        flex-direction: row-reverse;

        .message-content {
          align-items: flex-end;

          .message-text {
            background-color: #1890ff;
            color: #fff;
          }
        }
      }

      &.assistant {
        .message-text {
          background-color: #fff;
        }
      }

      .message-avatar {
        flex-shrink: 0;
      }

      .message-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 8px;
        min-width: 0;

        .message-text {
          padding: 12px 16px;
          border-radius: 12px;
          line-height: 1.6;
          word-wrap: break-word;
          max-width: 70%;

          .loading-icon {
            animation: rotate 1s linear infinite;
          }

          // Markdown 样式
          :deep(h1),
          :deep(h2),
          :deep(h3),
          :deep(h4),
          :deep(h5),
          :deep(h6) {
            margin: 0.5em 0;
            font-weight: 600;
          }

          :deep(p) {
            margin: 0.5em 0;
          }

          :deep(code) {
            background-color: rgba(0, 0, 0, 0.05);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 0.9em;
          }

          :deep(pre) {
            background-color: #282c34;
            padding: 12px;
            border-radius: 8px;
            overflow-x: auto;
            margin: 0.5em 0;

            code {
              background-color: transparent;
              padding: 0;
              color: #abb2bf;
              font-size: 0.9em;
            }
          }

          :deep(ul),
          :deep(ol) {
            margin: 0.5em 0;
            padding-left: 1.5em;
          }

          :deep(blockquote) {
            border-left: 4px solid #1890ff;
            padding-left: 12px;
            margin: 0.5em 0;
            color: #666;
          }

          :deep(table) {
            border-collapse: collapse;
            width: 100%;
            margin: 0.5em 0;

            th,
            td {
              border: 1px solid #ddd;
              padding: 8px;
              text-align: left;
            }

            th {
              background-color: #f5f7fa;
              font-weight: 600;
            }
          }

          :deep(a) {
            color: #1890ff;
            text-decoration: none;

            &:hover {
              text-decoration: underline;
            }
          }
        }

        .message-sources {
          max-width: 70%;

          .source-item {
            background-color: #fff;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 8px;
            border-left: 3px solid #1890ff;

            .source-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 8px;

              .source-name {
                font-weight: 500;
                color: #333;
              }
            }

            .source-content {
              font-size: 14px;
              color: #666;
              line-height: 1.6;
            }
          }
        }

        .message-time {
          font-size: 12px;
          color: #999;
          padding: 0 8px;
        }
      }
    }

    .quick-questions {
      margin-top: 16px;

      p {
        color: #666;
        margin-bottom: 12px;
      }

      .el-button {
        display: block;
        margin: 8px auto;
      }
    }
  }

  .input-area {
    .input-actions {
      display: flex;
      justify-content: space-between;
      margin-top: 12px;

      .left-actions,
      .right-actions {
        display: flex;
        gap: 8px;
        align-items: center;
      }
    }
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>

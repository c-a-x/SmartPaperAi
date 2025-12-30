<template>
  <div class="chat-container">
    <el-row :gutter="20" style="height: 100%">
      <!-- 左侧会话列表 -->
      <el-col :xs="24" :sm="8" :md="6" class="chat-sidebar">
        <el-card class="sidebar-card">
          <template #header>
            <div class="sidebar-header">
              <span>对话历史</span>
              <el-button :icon="Plus" size="small" type="primary" @click="createNewConversation">
                新建
              </el-button>
            </div>
          </template>
          <div class="conversation-list">
            <div v-for="conv in conversations" :key="conv.conversationId" class="conversation-item"
              :class="{ active: currentConversationId === conv.conversationId }">
              <div class="conv-content" @click="selectConversation(conv.conversationId)">
                <div class="conv-title">{{ conv.title || '新对话' }}</div>
                <div class="conv-meta">
                  <!-- <span>{{ conv.messageCount }} 条消息</span> -->
                  <span>{{ formatRelativeTime(conv.updateTime) }}</span>
                </div>
              </div>
              <el-button :icon="Delete" size="small" type="danger" text
                @click.stop="handleDeleteConversation(conv.conversationId)" title="删除对话" />
            </div>
            <el-empty v-if="conversations.length === 0" description="暂无对话" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧聊天区域 -->
      <el-col :xs="24" :sm="16" :md="18" class="chat-main">
        <el-card class="chat-card">
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
                      <el-tag size="small" type="info">相关度: {{ (source.score * 100).toFixed(1)
                      }}%</el-tag>
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
                  正在思考...
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <el-empty v-if="messages.length === 0 && !isLoading" description="开始新的对话吧" :image-size="120">
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
              @keydown.ctrl.enter="sendMessage" />
            <div class="input-actions">
              <div class="left-actions">
                <el-switch v-model="enableRag" active-text="启用RAG检索" />
                <el-popover v-if="enableRag" placement="top" :width="300" trigger="click">
                  <template #reference>
                    <el-button size="small" text>高级设置</el-button>
                  </template>
                  <div class="rag-settings">
                    <el-form label-width="120px" size="small">
                      <el-form-item label="检索文档数">
                        <el-input-number v-model="ragTopK" :min="1" :max="999" />
                      </el-form-item>
                      <el-form-item label="相似度阈值">
                        <el-slider v-model="similarityThreshold" :min="0" :max="1" :step="0.1" show-input />
                      </el-form-item>
                      <el-form-item label="温度参数">
                        <el-slider v-model="temperature" :min="0" :max="2" :step="0.1" show-input />
                      </el-form-item>
                      <el-form-item label="最大Token数">
                        <el-input-number v-model="maxTokens" :min="100" :max="4000" :step="100" />
                      </el-form-item>
                      <el-form-item label="对话记忆">
                        <el-switch v-model="enableMemory" />
                      </el-form-item>
                    </el-form>
                  </div>
                </el-popover>
              </div>
              <div class="right-actions">
                <el-button v-if="currentConversationId" :icon="Delete" size="small" @click="handleClearHistory">
                  清空历史
                </el-button>
                <el-button type="primary" size="small" :icon="Promotion" :loading="isLoading"
                  :disabled="!inputMessage.trim()" @click="sendMessage">
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
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus,
  User,
  ChatLineRound,
  Loading,
  Paperclip,
  Promotion,
  Delete
} from '@element-plus/icons-vue'
import {
  getConversationList,
  createConversation,
  getConversationHistory,
  deleteConversation,
  clearConversationHistory
} from '@/api/chat'
import { ElMessageBox } from 'element-plus'
import { formatRelativeTime } from '@/utils/format'
import type { ChatMessage, ConversationVO } from '@/types'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { config } from '@/config'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

const route = useRoute()

// 状态
const isLoading = ref(false)
const inputMessage = ref('')
const messages = ref<ChatMessage[]>([])
const conversations = ref<ConversationVO[]>([])
const currentConversationId = ref<string>('')
const messageListRef = ref<HTMLElement>()

// RAG 配置
const enableRag = ref(false)
const ragTopK = ref(99)
const similarityThreshold = ref(0.3)
const temperature = ref(0.3)
const maxTokens = ref(1000)
const enableMemory = ref(true)

// Markdown 渲染缓存
const markdownCache = new Map<string, string>()

// 快速问题
const quickQuestions = [
  '请帮我总结这篇论文的主要内容',
  '这篇论文的研究方法是什么？',
  '论文中有哪些创新点？',
  '请解释一下论文的核心观点'
]

// 加载会话列表
async function loadConversations() {
  try {
    const response = await getConversationList()
    if (response.success) {
      conversations.value = response.data || []
    }
  } catch (error: any) {
    console.error('加载会话列表失败:', error)
  }
}

// 创建新对话
async function createNewConversation() {
  try {
    const response = await createConversation()
    if (response.success && response.data) {
      currentConversationId.value = response.data
      messages.value = []
      await loadConversations()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '创建对话失败')
  }
}

// 选择会话
async function selectConversation(conversationId: string) {
  if (conversationId === currentConversationId.value) return

  currentConversationId.value = conversationId
  try {
    const response = await getConversationHistory(conversationId)
    if (response.success) {
      messages.value = response.data || []
      scrollToBottom()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载对话历史失败')
  }
}

// 发送消息
async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

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
    // 调用流式 API
    const response = await fetch(`${config.apiBaseUrl}/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': localStorage.getItem(config.tokenKey) || ''
      },
      body: JSON.stringify({
        message: content,
        conversationId: currentConversationId.value || undefined,
        enableRag: enableRag.value,
        ragTopK: enableRag.value ? ragTopK.value : undefined,
        customTemperature: temperature.value,
        customMaxTokens: maxTokens.value,
        customSimilarityThreshold: enableRag.value ? similarityThreshold.value : undefined,
        enableMemory: enableMemory.value
      })
    })

    if (!response.ok) {
      throw new Error('流式请求失败')
    }

    // 创建助手消息并添加到列表
    const assistantMessage: ChatMessage = {
      id: Date.now(),
      role: 'assistant',
      content: '',
      createTime: new Date().toISOString()
    }
    messages.value.push(assistantMessage)

    // 开始接收流式响应后，关闭加载状态
    isLoading.value = false
    scrollToBottom()

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    console.log('response', response)
    console.log('response.body', response.body)
    console.log('reader', reader)

    if (reader) {
      let buffer = '' // 用于缓存不完整的行
      let updateTimer: number | null = null

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        buffer += chunk
        const lines = buffer.split('\n')

        // 保留最后一个可能不完整的行
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') {
              break
            }
            try {
              const parsed = JSON.parse(data)
              // 优先使用 delta，兼容 content
              const content = parsed.delta || parsed.content
              if (content) {
                assistantMessage.content += content

                // 使用防抖优化渲染性能（每 50ms 更新一次）
                if (updateTimer) {
                  clearTimeout(updateTimer)
                }
                updateTimer = setTimeout(() => {
                  // 强制触发响应式更新
                  messages.value = [...messages.value]
                  nextTick(() => scrollToBottom())
                }, 50)
              }
              if (parsed.conversationId && !currentConversationId.value) {
                currentConversationId.value = parsed.conversationId
              }
            } catch (e) {
              console.error('解析流式数据失败:', e, '原始数据:', line)
            }
          }
        }
      }

      // 确保最后一次更新
      if (updateTimer) {
        clearTimeout(updateTimer)
      }
      messages.value = [...messages.value]
      await nextTick()
      scrollToBottom()

      // 流式响应完成后更新会话列表
      if (currentConversationId.value) {
        await loadConversations()
      }
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
  currentConversationId.value = ''
}

// 删除会话
async function handleDeleteConversation(conversationId: string) {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个对话吗？删除后无法恢复。',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await deleteConversation(conversationId)
    if (response.success) {
      ElMessage.success('删除成功')

      // 如果删除的是当前会话，清空消息列表
      if (conversationId === currentConversationId.value) {
        messages.value = []
        currentConversationId.value = ''
      }

      // 重新加载会话列表
      await loadConversations()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 清空会话历史
async function handleClearHistory() {
  if (!currentConversationId.value) {
    ElMessage.warning('请先选择一个对话')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要清空当前对话的所有历史记录吗？清空后无法恢复。',
      '清空确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await clearConversationHistory(currentConversationId.value)
    if (response.success) {
      ElMessage.success('清空成功')
      messages.value = []

      // 重新加载会话列表（更新消息计数）
      await loadConversations()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清空失败')
    }
  }
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
    // 只缓存完整的消息（长度超过 10 且不以代码块标记结尾）
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
  loadConversations()
})
</script>

<style scoped lang="scss">
.chat-container {
  height: calc(100vh - 160px);

  .chat-sidebar,
  .chat-main {
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

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .conversation-list {
    overflow-y: auto;
    flex: 1;

    .conversation-item {
      padding: 12px;
      border-radius: 8px;
      margin-bottom: 8px;
      transition: all 0.3s;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;

      &:hover {
        background-color: #f5f7fa;

        .el-button {
          opacity: 1;
        }
      }

      &.active {
        background-color: #e6f7ff;
        border: 1px solid #1890ff;
      }

      .conv-content {
        flex: 1;
        cursor: pointer;
        min-width: 0;
      }

      .conv-title {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .conv-meta {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        color: #999;
      }

      .el-button {
        opacity: 0;
        transition: opacity 0.3s;
        flex-shrink: 0;
      }
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

  .rag-settings {
    :deep(.el-form-item) {
      margin-bottom: 16px;
    }

    :deep(.el-slider) {
      padding: 0 8px;
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

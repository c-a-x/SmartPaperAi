// 流式请求工具函数
import { config } from '@/config'

/**
 * SSE 流式聊天请求
 * @param data 请求数据
 * @param onChunk 接收到数据块时的回调
 * @param onDone 流式传输完成时的回调
 */
export async function streamChat(
  data: { message: string; conversationId?: string },
  onChunk: (content: string, conversationId?: string) => void,
  onDone?: () => void
) {
  const token = localStorage.getItem(config.tokenKey)

  const response = await fetch(`${config.apiBaseUrl}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    throw new Error(`流式请求失败: ${response.status} ${response.statusText}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法获取响应流')
  }

  const decoder = new TextDecoder()
  let buffer = '' // 用于处理不完整的 JSON

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      // 解码数据块
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')

      // 保留最后一个可能不完整的行
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()

          // 检测结束标记
          if (data === '[DONE]') {
            onDone?.()
            return
          }

          try {
            const parsed = JSON.parse(data)
            const content = parsed.delta || parsed.content

            if (content) {
              onChunk(content, parsed.conversationId)
            }
          } catch (e) {
            console.warn('解析流式数据失败:', e, '原始数据:', line)
          }
        }
      }
    }

    // 处理完成
    onDone?.()
  } finally {
    reader.releaseLock()
  }
}

# 流式请求重构说明

## 📦 新增文件

### `src/utils/stream.ts`

创建了统一的流式请求工具函数，用于处理 SSE（Server-Sent Events）流式响应。

#### 核心功能

```typescript
streamChat(
  data: { message: string; conversationId?: string },
  onChunk: (content: string, conversationId?: string) => void,
  onDone?: () => void
)
```

**参数说明：**
- `data`: 请求数据（消息内容和会话 ID）
- `onChunk`: 接收到数据块时的回调函数
- `onDone`: 流式传输完成时的回调函数（可选）

#### 特性

1. **统一封装**：将流式请求逻辑封装成可复用的工具函数
2. **自动处理**：
   - ✅ Token 自动添加到请求头
   - ✅ 响应流自动解码
   - ✅ SSE 数据格式自动解析
   - ✅ 不完整 JSON 数据的缓冲处理
3. **错误处理**：完善的错误捕获和日志
4. **资源管理**：自动释放 reader 锁

## 🔧 重构内容

### ChatView.vue

**之前（直接使用 fetch）：**
```typescript
const response = await fetch(`${config.apiBaseUrl}/ai/chat/stream`, {
  method: 'POST',
  headers: { ... },
  body: JSON.stringify({ ... })
})

const reader = response.body?.getReader()
const decoder = new TextDecoder()

while (true) {
  const { done, value } = await reader.read()
  // ... 50+ 行的流式处理逻辑
}
```

**现在（使用工具函数）：**
```typescript
await streamChat(
  {
    message: content,
    conversationId: currentConversationId.value || undefined
  },
  // 接收数据块
  (chunkContent, convId) => {
    assistantMessage.content += chunkContent
    scrollToBottom()
    if (convId && !currentConversationId.value) {
      currentConversationId.value = convId
    }
  },
  // 完成回调
  async () => {
    if (currentConversationId.value) {
      await loadConversations()
    }
  }
)
```

## ✨ 优势

### 1. 代码简洁
- 从 50+ 行精简到 15 行
- 更清晰的业务逻辑
- 更易于维护

### 2. 可复用性
- 其他组件也可以使用 `streamChat`
- 统一的流式请求处理逻辑
- 减少重复代码

### 3. 更好的错误处理
- 统一的异常捕获
- 详细的错误日志
- 自动资源清理

### 4. 性能优化
- 使用缓冲区处理不完整数据
- 避免 JSON 解析失败
- 正确处理多字节 UTF-8 字符

### 5. 类型安全
- TypeScript 类型定义
- 参数类型检查
- 回调函数类型推导

## 🎯 使用场景

### 场景 1：普通流式聊天（已实现）
```typescript
await streamChat(
  { message: '你好' },
  (content) => {
    // 实时显示内容
  }
)
```

### 场景 2：带会话的流式聊天
```typescript
await streamChat(
  { 
    message: '继续之前的话题',
    conversationId: 'conv-123'
  },
  (content, convId) => {
    // 实时显示内容
    // 保存会话 ID
  }
)
```

### 场景 3：完成后的处理
```typescript
await streamChat(
  { message: '生成报告' },
  (content) => {
    // 实时显示
  },
  async () => {
    // 生成完成后的操作
    await saveReport()
  }
)
```

## 📝 技术细节

### SSE 数据格式

```
data:{"delta":"你"}
data:{"delta":"好"}
data:{"delta":"，"}
data:{"delta":"世界"}
data:"[DONE]"
```

### 处理流程

1. **发送请求** → 使用 fetch API
2. **获取流** → `response.body.getReader()`
3. **读取数据块** → `reader.read()`
4. **解码** → `TextDecoder.decode()`
5. **缓冲处理** → 处理不完整的行
6. **解析 SSE** → 提取 `data:` 后的 JSON
7. **回调通知** → `onChunk(content)`
8. **检测结束** → 识别 `[DONE]` 标记
9. **完成回调** → `onDone()`
10. **资源清理** → `reader.releaseLock()`

## 🔍 为什么不使用 axios？

### axios 的局限性

1. **浏览器环境限制**：
   - `responseType: 'stream'` 主要为 Node.js 设计
   - 浏览器中对流式响应支持有限

2. **SSE 处理复杂**：
   - 需要手动处理 `onDownloadProgress`
   - 数据拼接和解析更复杂

3. **ReadableStream API**：
   - fetch 原生支持 `ReadableStream`
   - 更适合处理流式数据

### fetch 的优势

- ✅ 浏览器原生支持
- ✅ `ReadableStream` API 完美支持 SSE
- ✅ 逐块读取数据更高效
- ✅ 资源管理更简单

## 🚀 未来扩展

### 可能的增强功能

1. **中断支持**：
   ```typescript
   const controller = new AbortController()
   await streamChat(data, onChunk, onDone, controller.signal)
   ```

2. **进度追踪**：
   ```typescript
   await streamChat(data, onChunk, onDone, {
     onProgress: (bytes) => console.log(bytes)
   })
   ```

3. **重连机制**：
   ```typescript
   await streamChat(data, onChunk, onDone, {
     retry: 3,
     retryDelay: 1000
   })
   ```

4. **多种流格式**：
   - SSE（当前）
   - WebSocket
   - gRPC stream

## 💡 最佳实践

1. **始终处理错误**：
   ```typescript
   try {
     await streamChat(...)
   } catch (error) {
     ElMessage.error('流式请求失败')
   }
   ```

2. **及时更新 UI**：
   ```typescript
   onChunk: (content) => {
     message.content += content
     scrollToBottom() // 每次都滚动
   }
   ```

3. **保存会话状态**：
   ```typescript
   onChunk: (content, convId) => {
     if (convId) saveConversationId(convId)
   }
   ```

## ✅ 总结

通过创建 `streamChat` 工具函数，我们实现了：

- 📦 代码复用和模块化
- 🎯 清晰的业务逻辑分离
- 🛡️ 完善的错误处理
- ⚡ 更好的性能和体验
- 🔧 易于维护和扩展

这种封装方式既保持了代码的简洁性，又提供了足够的灵活性！

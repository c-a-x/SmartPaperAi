# 聊天功能说明

## ✨ 新功能

### 1. Markdown 格式渲染

AI 回复现在支持完整的 Markdown 语法渲染，包括：

- **标题**：# H1, ## H2, ### H3 等
- **粗体/斜体**：**粗体**, *斜体*
- **代码块**：支持语法高亮的代码块
- **行内代码**：`代码`
- **列表**：有序和无序列表
- **引用**：> 引用文本
- **表格**：完整的表格支持
- **链接**：[链接文本](URL)

### 2. 流式输出

普通对话模式现在使用流式输出（SSE），特点：

- ✅ 实时显示 AI 回复
- ✅ 逐字显示，体验更流畅
- ✅ 自动滚动到最新内容
- ✅ 支持长文本回复

### 3. 代码高亮

代码块支持语法高亮，使用 highlight.js：

```python
def hello_world():
    print("Hello, World!")
```

```javascript
function helloWorld() {
    console.log("Hello, World!");
}
```

## 🔧 技术实现

### 依赖库

- **marked**: Markdown 解析和渲染
- **highlight.js**: 代码语法高亮
- **@types/marked**: TypeScript 类型定义

### 核心代码

#### Markdown 渲染

```typescript
import { marked } from 'marked'
import hljs from 'highlight.js'

// 格式化消息 - 使用 Markdown 渲染
function formatMessage(content: string) {
  if (!content) return ''
  try {
    return marked.parse(content) as string
  } catch (error) {
    console.error('Markdown 解析失败:', error)
    return content.replace(/\n/g, '<br>')
  }
}
```

#### 流式输出

```typescript
// 调用流式 API
const response = await fetch(`${config.apiBaseUrl}/ai/chat/stream`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': localStorage.getItem(config.tokenKey) || ''
  },
  body: JSON.stringify({
    message: content,
    conversationId: currentConversationId.value || undefined
  })
})

const reader = response.body?.getReader()
const decoder = new TextDecoder()

if (reader) {
  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, { stream: true })
    const lines = chunk.split('\n')

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = line.slice(6).trim()
        if (data === '[DONE]') break
        
        const parsed = JSON.parse(data)
        if (parsed.content) {
          assistantMessage.content += parsed.content
          scrollToBottom()
        }
      }
    }
  }
}
```

## 📝 使用示例

### 测试 Markdown 渲染

在聊天框中发送：

```
请用 Markdown 格式回复，包含：
1. 标题
2. 代码块
3. 表格
```

AI 会以 Markdown 格式回复，并在界面上正确渲染。

### 测试流式输出

在"普通对话"模式下发送任何问题，即可看到 AI 回复逐字显示。

## 🎨 样式定制

### 代码主题

当前使用 `github-dark` 主题。如需更换，修改导入：

```typescript
// 可选主题：
import 'highlight.js/styles/github.css'           // GitHub 亮色
import 'highlight.js/styles/github-dark.css'      // GitHub 暗色
import 'highlight.js/styles/monokai.css'          // Monokai
import 'highlight.js/styles/atom-one-dark.css'    // Atom One Dark
```

### Markdown 样式

在 `<style scoped>` 中的 `.message-text :deep()` 部分可以自定义各种 Markdown 元素的样式。

## 🔍 注意事项

1. **流式输出仅用于普通对话**：文档问答和知识库问答仍使用普通 API（一次性返回）
2. **网络要求**：流式输出需要稳定的网络连接
3. **浏览器兼容性**：需要支持 Fetch API 和 ReadableStream 的现代浏览器

## 🚀 未来优化

- [ ] 支持中断流式输出
- [ ] 添加复制代码功能
- [ ] 支持 LaTeX 数学公式渲染
- [ ] 支持 Mermaid 图表渲染
- [ ] 文档问答和知识库问答也支持流式输出

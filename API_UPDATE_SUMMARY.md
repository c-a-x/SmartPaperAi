# API 接口更新总结

## 更新日期
2025年10月20日

## 主要变更

### 1. 会话管理接口更新

#### 1.1 会话列表响应结构变更
**接口**: `GET /ai/sessions`

**旧结构** (`ConversationVO`):
```typescript
{
  conversationId: string
  title: string
  messageCount: number  // 已移除
  createTime: string
  updateTime: string
}
```

**新结构** (`ChatSessionVO`):
```typescript
{
  id: number           // 新增
  conversationId: string
  title: string
  createTime: string
  updateTime: string
}
```

**主要变化**:
- ✅ 新增 `id` 字段（会话ID，integer类型）
- ❌ 移除 `messageCount` 字段（消息计数）

**影响的文件**:
- `src/types/index.ts` - 更新了 `ConversationVO` 接口定义
- `src/views/chat/ChatView.vue` - 已注释掉 `messageCount` 的使用

---

#### 1.2 获取会话列表
**接口**: `GET /ai/sessions`

**新增参数**:
- `userId` (query, optional): 用户ID

**更新**:
```typescript
// 旧
export function getConversationList() {
  return request.get<ApiResponse<ConversationVO[]>>('/ai/sessions')
}

// 新
export function getConversationList(userId?: number) {
  return request.get<ApiResponse<ConversationVO[]>>('/ai/sessions', {
    params: { userId }
  })
}
```

---

#### 1.3 创建新会话
**接口**: `POST /ai/sessions`

**参数方式变更**: Body → Query Parameters

**更新**:
```typescript
// 旧
export function createConversation(title?: string) {
  return request.post<ApiResponse<string>>('/ai/sessions', { title })
}

// 新
export function createConversation(title?: string, userId?: number) {
  return request.post<ApiResponse<string>>('/ai/sessions', null, {
    params: { title, userId }
  })
}
```

---

### 2. RAG 问答接口重构

#### 2.1 文档对话接口
**接口**: `POST /api/rag/document-chat`

**参数方式变更**: Body → Query Parameters

**旧参数** (Body):
```typescript
{
  question: string
  documentIds: number[]
  conversationId?: string
  topK?: number
}
```

**新参数** (Query):
- `documentId` (required): 文档ID
- `query` (required): 用户问题
- `useGraphEnhancement` (optional): 是否启用知识图谱增强

**更新**:
```typescript
// 旧
export function ragChat(data: RagChatRequest) {
  return request.post<ApiResponse<RagChatResponse>>('/api/rag/chat', data)
}

// 新
export function documentChat(documentId: number, query: string, useGraphEnhancement?: boolean) {
  return request.post<ApiResponse<RagChatResponse>>('/api/rag/document-chat', null, {
    params: { documentId, query, useGraphEnhancement }
  })
}
```

---

#### 2.2 知识库问答接口
**接口**: `POST /api/rag/knowledge-base-chat`

**参数方式变更**: Body → Query Parameters

**旧参数** (Body):
```typescript
{
  question: string
  kbIds: number[]
  conversationId?: string
  topK?: number
}
```

**新参数** (Query):
- `knowledgeBaseId` (required): 知识库ID
- `query` (required): 用户问题
- `useGraphEnhancement` (optional): 是否启用知识图谱增强

**更新**:
```typescript
// 旧
export function knowledgeBaseChat(data: RagChatRequest) {
  return request.post<ApiResponse<RagChatResponse>>('/api/rag/knowledge-base-chat', data)
}

// 新
export function knowledgeBaseChat(knowledgeBaseId: number, query: string, useGraphEnhancement?: boolean) {
  return request.post<ApiResponse<RagChatResponse>>('/api/rag/knowledge-base-chat', null, {
    params: { knowledgeBaseId, query, useGraphEnhancement }
  })
}
```

---

#### 2.3 RAG 响应结构变更

**旧响应** (`RagChatResponse`):
```typescript
{
  conversationId: string
  answer: string
  sources: SourceReference[]  // 简单的引用列表
  timestamp: string
}
```

**新响应** (`RagChatResultVO`):
```typescript
{
  answer: string
  citations: CitationVO[]      // 详细的引用信息
  knowledgeGraph?: KnowledgeGraphVO  // 知识图谱（可选）
}
```

**新增类型定义**:

```typescript
// 引用来源（Citation）- 更详细的结构
export interface CitationVO {
  documentId: number
  title: string
  chunkId: number          // 文档块ID
  chunkIndex: number       // 文档块索引
  score: number            // 相似度得分
  snippet: string          // 引用片段
  keywords: string[]       // 关键词列表（用于高亮）
  position?: string        // 页码/位置
  metadata?: string        // 元数据
}

// 知识图谱节点
export interface GraphNode {
  id: string
  type: 'Concept' | 'Document' | 'Author'
  label: string
  properties: NodeProperties
}

// 知识图谱关系
export interface GraphRelationship {
  id: string
  source: string
  target: string
  type: 'CONTAINS' | 'RELATED_TO' | 'CITES' | 'SIMILAR_TO'
  label: string
  properties: RelationshipProperties
}

// 知识图谱
export interface KnowledgeGraphVO {
  nodes: GraphNode[]
  relationships: GraphRelationship[]
  stats: GraphStats
}
```

---

### 3. 聊天视图更新

#### 3.1 文档问答模式
**文件**: `src/views/chat/ChatView.vue`

**更新**:
```typescript
// 旧
const ragRequest: RagChatRequest = {
  question: content,
  documentIds: [documentId],
  conversationId: currentConversationId.value || undefined
}
const response = await ragChat(ragRequest)

// 新
const response = await documentChat(documentId, content, false)

// 处理响应 - 将 citations 转换为 sources
sources: response.data.citations.map(citation => ({
  documentId: citation.documentId,
  documentName: citation.title,
  content: citation.snippet,
  score: citation.score
}))
```

#### 3.2 知识库问答模式
**文件**: `src/views/chat/ChatView.vue`

**更新**:
```typescript
// 旧
const ragRequest: RagChatRequest = {
  question: content,
  kbIds: [kbId],
  conversationId: currentConversationId.value || undefined
}
const response = await knowledgeBaseChat(ragRequest)

// 新
const response = await knowledgeBaseChat(kbId, content, false)

// 处理响应 - 将 citations 转换为 sources
sources: response.data.citations.map(citation => ({
  documentId: citation.documentId,
  documentName: citation.title,
  content: citation.snippet,
  score: citation.score
}))
```

---

## 兼容性说明

### 保留的旧类型
为了向后兼容，保留了以下类型定义：
- `RagChatRequest` - 标记为"已废弃"
- `SourceReference` - 标记为"旧版，保留兼容性"

### 移除的导入
从 `ChatView.vue` 中移除了不再使用的类型导入：
- ❌ `RagChatRequest`
- ❌ `ChatResponse`
- ❌ `RagChatResponse`（旧版）

---

## 新增功能

### 1. 知识图谱增强
新的 RAG 接口支持知识图谱增强功能，通过 `useGraphEnhancement` 参数启用。

返回的知识图谱包含：
- **节点**: 概念、文档、作者
- **关系**: 包含、相关、引用、相似
- **统计**: 各类节点和关系的数量

### 2. 更详细的引用信息
新的 `CitationVO` 提供了更丰富的引用信息：
- 文档块ID和索引
- 关键词列表（用于高亮显示）
- 页码/位置信息
- 元数据

---

## 测试建议

### 1. 会话管理测试
- ✅ 创建新会话
- ✅ 获取会话列表
- ✅ 删除会话
- ✅ 清空会话历史

### 2. RAG 问答测试
- ✅ 文档对话（不启用图谱增强）
- ✅ 文档对话（启用图谱增强）
- ✅ 知识库问答（不启用图谱增强）
- ✅ 知识库问答（启用图谱增强）

### 3. 引用显示测试
- ✅ 引用片段显示
- ✅ 关键词高亮（待实现）
- ✅ 知识图谱可视化（待实现）

---

## 后续工作

### 待实现功能
1. **关键词高亮**: 使用 `citation.keywords` 在引用片段中高亮关键词
2. **知识图谱可视化**: 展示 `knowledgeGraph` 数据
3. **增强引用展示**: 显示页码、文档块索引等详细信息

### 优化建议
1. 考虑缓存知识图谱数据以提高性能
2. 实现引用片段的交互式展示
3. 添加图谱增强的开关配置

---

## 文件清单

### 已修改文件
1. ✅ `src/types/index.ts` - 更新类型定义
2. ✅ `src/api/chat.ts` - 更新API函数签名
3. ✅ `src/views/chat/ChatView.vue` - 更新组件逻辑

### 新增文件
1. ✅ `API_UPDATE_SUMMARY.md` - 本文档

---

## 版本信息
- **文档版本**: 1.0
- **API 版本**: 0.0.1-SNAPSHOT
- **更新时间**: 2025年10月20日

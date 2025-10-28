# 路由参数验证修复

## 🐛 问题描述

```
Method parameter 'kbId': Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: "list"
```

## 🔍 问题原因

当访问某些路由时（如 `/knowledge/list`），路由参数 `route.params.id` 会被解析为 `"list"` 字符串，然后作为查询参数传递给聊天页面：

```typescript
// 错误的代码
router.push({
  path: '/chat',
  query: { kbId: route.params.id }  // ← 这里可能是 "list"
})
```

在聊天页面中，代码没有验证参数是否为有效数字：

```typescript
// 问题代码
const kbId = route.query.kbId ? Number(route.query.kbId) : undefined
// Number("list") = NaN，但代码没有检查
```

当 `NaN` 被传递到后端时，后端尝试将字符串 `"list"` 转换为 `Long` 类型，导致错误。

## ✅ 解决方案

### 1. 文档问答参数验证

```typescript
// 修复前
const documentId = route.query.documentId ? Number(route.query.documentId) : undefined
const ragRequest: RagChatRequest = {
  question: content,
  documentIds: documentId ? [documentId] : undefined,  // 可能传入 [NaN]
  conversationId: currentConversationId.value || undefined
}

// 修复后
const docIdParam = route.query.documentId
const documentId = docIdParam && !isNaN(Number(docIdParam)) ? Number(docIdParam) : undefined

if (!documentId) {
  ElMessage.error('文档 ID 无效')
  isLoading.value = false
  return
}

const ragRequest: RagChatRequest = {
  question: content,
  documentIds: [documentId],  // 确保是有效数字
  conversationId: currentConversationId.value || undefined
}
```

### 2. 知识库问答参数验证

```typescript
// 修复前
const kbId = route.query.kbId ? Number(route.query.kbId) : undefined
const ragRequest: RagChatRequest = {
  question: content,
  kbIds: kbId ? [kbId] : undefined,  // 可能传入 [NaN]
  conversationId: currentConversationId.value || undefined
}

// 修复后
const kbIdParam = route.query.kbId
const kbId = kbIdParam && !isNaN(Number(kbIdParam)) ? Number(kbIdParam) : undefined

if (!kbId) {
  ElMessage.error('知识库 ID 无效')
  isLoading.value = false
  return
}

const ragRequest: RagChatRequest = {
  question: content,
  kbIds: [kbId],  // 确保是有效数字
  conversationId: currentConversationId.value || undefined
}
```

### 3. 路由监听参数验证

```typescript
// 修复前
watch(
  () => route.query,
  (newQuery) => {
    if (newQuery.documentId) {
      chatMode.value = 'document'
    } else if (newQuery.kbId) {
      chatMode.value = 'knowledge'
    }
  },
  { immediate: true }
)

// 修复后
watch(
  () => route.query,
  (newQuery) => {
    // 验证并切换聊天模式
    const docId = newQuery.documentId
    const kbIdVal = newQuery.kbId
    
    if (docId && !isNaN(Number(docId))) {
      chatMode.value = 'document'
    } else if (kbIdVal && !isNaN(Number(kbIdVal))) {
      chatMode.value = 'knowledge'
    } else {
      chatMode.value = 'normal'
    }
  },
  { immediate: true }
)
```

## 🔧 验证逻辑

### 检查是否为有效数字

```typescript
const param = route.query.someId
const validId = param && !isNaN(Number(param)) ? Number(param) : undefined

if (!validId) {
  // 处理无效参数
  ElMessage.error('参数无效')
  return
}

// 使用有效的 ID
```

### 为什么这样写？

1. **`param && ...`**：确保参数存在
2. **`!isNaN(Number(param))`**：确保参数可以转换为数字
   - `Number("123")` → `123` ✅
   - `Number("list")` → `NaN` ❌
   - `isNaN(NaN)` → `true`
   - `!isNaN(NaN)` → `false`
3. **三元运算符**：只有在参数有效时才转换为数字

## 📊 可能的无效输入

| 输入值 | `Number(value)` | `isNaN(Number(value))` | 结果 |
|--------|----------------|----------------------|------|
| `"123"` | `123` | `false` | ✅ 有效 |
| `"list"` | `NaN` | `true` | ❌ 无效 |
| `""` | `0` | `false` | ⚠️ 边缘情况 |
| `undefined` | `NaN` | `true` | ❌ 无效 |
| `null` | `0` | `false` | ⚠️ 边缘情况 |
| `"0"` | `0` | `false` | ✅ 有效 |
| `"-1"` | `-1` | `false` | ✅ 有效 |
| `"1.5"` | `1.5` | `false` | ✅ 有效 |

## 🎯 最佳实践

### 1. 总是验证路由参数

```typescript
// ❌ 不好的做法
const id = Number(route.params.id)
// 如果 id 是 "list"，会得到 NaN

// ✅ 好的做法
const idParam = route.params.id
const id = idParam && !isNaN(Number(idParam)) ? Number(idParam) : undefined
if (!id) {
  // 处理错误
  return
}
```

### 2. 早期返回

```typescript
// ❌ 嵌套逻辑
if (validId) {
  // 很多代码...
  if (anotherCondition) {
    // 更多代码...
  }
}

// ✅ 早期返回
if (!validId) {
  ElMessage.error('参数无效')
  return
}

// 继续正常流程
doSomething(validId)
```

### 3. 提供用户友好的错误消息

```typescript
if (!documentId) {
  ElMessage.error('文档 ID 无效')
  isLoading.value = false
  return
}

if (!kbId) {
  ElMessage.error('知识库 ID 无效')
  isLoading.value = false
  return
}
```

### 4. 考虑边缘情况

```typescript
// 处理空字符串、null 等情况
const isValidId = (value: any): value is string => {
  return value != null && value !== '' && !isNaN(Number(value))
}

const id = isValidId(route.query.id) ? Number(route.query.id) : undefined
```

## 🚀 扩展：通用验证函数

可以创建一个通用的验证工具函数：

```typescript
// src/utils/validation.ts
export function parseNumericParam(
  param: string | string[] | undefined,
  errorMessage: string = '参数无效'
): number | null {
  if (!param || Array.isArray(param)) {
    ElMessage.error(errorMessage)
    return null
  }
  
  const num = Number(param)
  if (isNaN(num)) {
    ElMessage.error(errorMessage)
    return null
  }
  
  return num
}

// 使用
const documentId = parseNumericParam(route.query.documentId, '文档 ID 无效')
if (!documentId) return

const kbId = parseNumericParam(route.query.kbId, '知识库 ID 无效')
if (!kbId) return
```

## ✅ 修复效果

- ✅ 阻止无效参数传递到后端
- ✅ 提供友好的错误提示
- ✅ 避免后端报错
- ✅ 提升用户体验
- ✅ 代码更健壮

## 📝 注意事项

1. **查询参数类型**：`route.query` 的值类型是 `string | string[]`
2. **路径参数类型**：`route.params` 的值类型是 `string | string[]`
3. **数字 0**：`Number("0")` 是有效的，但在布尔上下文中是 `false`
4. **负数**：`Number("-1")` 是有效的数字
5. **小数**：`Number("1.5")` 是有效的，但可能不符合业务需求

## 🔍 调试技巧

如果遇到类似问题，可以添加日志：

```typescript
console.log('Route params:', route.params)
console.log('Route query:', route.query)
console.log('kbId param:', route.query.kbId)
console.log('Parsed kbId:', Number(route.query.kbId))
console.log('Is NaN:', isNaN(Number(route.query.kbId)))
```

这样可以快速定位问题所在！

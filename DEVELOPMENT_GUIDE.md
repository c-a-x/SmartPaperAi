# SmartPaperAI 开发指南 🚀

## 📋 目录

1. [项目已完成功能](#已完成功能)
2. [启动项目](#启动项目)
3. [项目架构](#项目架构)
4. [开发建议](#开发建议)
5. [API 对接](#api-对接)
6. [常用代码片段](#常用代码片段)

## ✅ 已完成功能

### 1. 基础设施 ✅
- [x] Axios HTTP 客户端封装（请求/响应拦截器）
- [x] TypeScript 类型定义（全局类型接口）
- [x] 本地存储工具（Token、用户信息）
- [x] 格式化工具（日期、文件大小等）
- [x] 全局配置文件

### 2. 用户认证 ✅
- [x] 登录页面（支持验证码）
- [x] 注册页面
- [x] Pinia 状态管理（auth store）
- [x] 路由守卫（未登录拦截）
- [x] Token 自动注入
- [x] 401 自动跳转登录

### 3. 主布局 ✅
- [x] 侧边栏导航（可折叠）
- [x] 顶部导航栏（搜索、通知、用户信息）
- [x] 面包屑导航
- [x] 响应式设计

### 4. 教学设计模块 ✅
- [x] 列表页（搜索、排序）
- [x] 创建/编辑页（完整表单）
- [x] 详情页
- [x] 删除功能

### 5. 文档管理模块 ✅
- [x] 列表页（上传、搜索、过滤）
- [x] 详情页（元数据展示）
- [x] 上传功能
- [x] 删除功能

### 6. 知识库模块 ✅
- [x] 列表页（卡片式展示）
- [x] 创建/编辑对话框
- [x] 详情页（文档管理）
- [x] 删除功能

### 7. AI 对话模块 ✅
- [x] 会话列表（左侧）
- [x] 聊天界面（右侧）
- [x] 三种模式（普通/文档/知识库）
- [x] 引用来源展示
- [x] 消息历史

### 8. 其他 ✅
- [x] 首页（统计+快捷入口）
- [x] 404 页面
- [x] 路由配置
- [x] Element Plus 集成
- [x] SASS 支持

## 🚀 启动项目

### 第一次启动

```bash
# 1. 进入项目目录
cd c:\Users\26870\Desktop\smartPaperAi

# 2. 安装依赖（已完成）
npm install

# 3. 启动开发服务器
npm run dev

# 4. 浏览器访问
# http://localhost:5173
```

### 日常开发

```bash
# 启动开发服务器
npm run dev

# 在另一个终端运行类型检查
npm run type-check

# 代码检查
npm run lint

# 格式化代码
npm run format
```

## 🏗️ 项目架构

### 目录结构说明

```
src/
├── api/              # API 接口层（与后端交互）
│   ├── auth.ts       # 认证接口：login, register, logout
│   ├── teaching.ts   # 教学设计接口
│   ├── document.ts   # 文档管理接口
│   ├── knowledge.ts  # 知识库接口
│   └── chat.ts       # AI 对话接口
│
├── config/           # 配置层
│   └── index.ts      # 全局配置（API地址、Token键名等）
│
├── types/            # 类型层
│   └── index.ts      # 所有 TypeScript 接口定义
│
├── utils/            # 工具层
│   ├── request.ts    # Axios 封装（拦截器）
│   ├── storage.ts    # 本地存储工具
│   └── format.ts     # 格式化工具
│
├── stores/           # 状态管理层（Pinia）
│   └── auth.ts       # 认证状态（token, userInfo, isLoggedIn）
│
├── router/           # 路由层
│   └── index.ts      # 路由配置和守卫
│
├── layouts/          # 布局层
│   └── MainLayout.vue # 主布局（侧边栏+顶栏+内容区）
│
└── views/            # 视图层（页面组件）
    ├── HomeView.vue
    ├── LoginView.vue
    ├── teaching/     # 教学设计模块
    ├── document/     # 文档管理模块
    ├── knowledge/    # 知识库模块
    └── chat/         # AI对话模块
```

### 数据流向

```
用户操作 → 页面组件 → API 调用 → Axios 拦截器 → 后端
                ↓
              Pinia Store（状态更新）
                ↓
              页面组件（UI 更新）
```

## 💡 开发建议

### 1. 修改后端 API 地址

编辑 `src/config/index.ts`：

```typescript
export const config = {
  apiBaseUrl: 'http://your-backend-api.com', // ← 改这里
  // ...
}
```

### 2. 添加新的 API 接口

在对应的 API 文件中添加：

```typescript
// src/api/example.ts
import { request } from '@/utils/request'
import type { ApiResponse } from '@/types'

export function getExample() {
  return request.get<ApiResponse<any>>('/api/example')
}

export function createExample(data: any) {
  return request.post<ApiResponse<void>>('/api/example', data)
}
```

### 3. 添加新的类型定义

在 `src/types/index.ts` 中添加：

```typescript
export interface ExampleVO {
  id: number
  name: string
  // ...
}
```

### 4. 创建新的页面

```vue
<!-- src/views/example/ExampleView.vue -->
<template>
  <div class="example-container">
    <el-card>
      <!-- 页面内容 -->
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getExample } from '@/api/example'

const data = ref([])

async function loadData() {
  try {
    const response = await getExample()
    if (response.success) {
      data.value = response.data
    }
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.example-container {
  // 样式
}
</style>
```

### 5. 添加新的路由

在 `src/router/index.ts` 中添加：

```typescript
{
  path: 'example',
  name: 'Example',
  component: () => import('@/views/example/ExampleView.vue'),
  meta: { title: '示例页面' }
}
```

### 6. 在侧边栏添加菜单

编辑 `src/layouts/MainLayout.vue`：

```vue
<el-menu-item index="/example">
  <el-icon><Document /></el-icon>
  <template #title>示例页面</template>
</el-menu-item>
```

## 🔌 API 对接

### 1. 登录流程

```typescript
// 1. 用户填写表单
const loginForm = {
  username: 'admin',
  password: '123456'
}

// 2. 调用登录 API
const response = await authStore.login(loginForm)

// 3. authStore 自动保存 token 和 userInfo 到 localStorage
// 4. 路由守卫允许访问受保护的页面
// 5. 所有后续请求自动带上 token
```

### 2. 调用 API 的标准流程

```typescript
// 1. 导入 API 函数
import { getDocumentList } from '@/api/document'

// 2. 调用并处理响应
try {
  const response = await getDocumentList()
  if (response.success) {
    // 成功处理
    console.log(response.data)
  } else {
    // 失败处理
    ElMessage.error(response.msg)
  }
} catch (error) {
  // 错误处理（网络错误、401等）
  ElMessage.error('请求失败')
}
```

### 3. 文件上传

```typescript
// 使用封装的 upload 方法
import { request } from '@/utils/request'

const file = /* 获取的文件对象 */

const response = await request.upload(
  '/api/documents/upload',
  file,
  (percent) => {
    console.log(`上传进度：${percent}%`)
  }
)
```

### 4. Token 失效处理

系统已自动处理：
- 响应拦截器检测到 401 状态码
- 自动清除 token 和用户信息
- 自动跳转到登录页
- 保存当前页面地址，登录后自动跳回

## 📝 常用代码片段

### 1. 列表页模板

```typescript
const loading = ref(false)
const tableData = ref([])
const searchKeyword = ref('')

async function loadData() {
  loading.value = true
  try {
    const response = await getListAPI()
    if (response.success) {
      tableData.value = response.data || []
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
```

### 2. 表单提交

```typescript
const formRef = ref<FormInstance>()
const formData = reactive({
  name: '',
  description: ''
})

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createAPI(formData)
        ElMessage.success('操作成功')
        router.back()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}
```

### 3. 删除确认

```typescript
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteAPI(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}
```

### 4. 日期格式化

```typescript
import { formatDateTime, formatRelativeTime } from '@/utils/format'

// 完整日期时间
formatDateTime('2025-01-19T12:00:00') // "2025-01-19 12:00:00"

// 相对时间
formatRelativeTime('2025-01-19T11:00:00') // "1小时前"
```

### 5. 文件大小格式化

```typescript
import { formatFileSize } from '@/utils/format'

formatFileSize(1024) // "1 KB"
formatFileSize(1048576) // "1 MB"
formatFileSize(1073741824) // "1 GB"
```

## 🎯 下一步建议

### 功能完善
1. ⚡ 实现文档预览功能
2. ⚡ 实现文档下载功能
3. ⚡ 添加个人中心页面
4. ⚡ 添加设置页面
5. ⚡ 实现通知功能
6. ⚡ 添加文档标签功能
7. ⚡ 添加搜索历史

### 性能优化
1. 📈 添加虚拟滚动（大列表）
2. 📈 添加骨架屏加载
3. 📈 图片懒加载
4. 📈 路由懒加载优化
5. 📈 API 请求去重

### 用户体验
1. 💅 添加 Loading 动画
2. 💅 完善错误提示
3. 💅 添加空状态插画
4. 💅 添加操作引导
5. 💅 优化移动端体验

### 测试
1. 🧪 添加单元测试
2. 🧪 添加 E2E 测试
3. 🧪 API Mock 数据

## 📞 遇到问题？

1. **编译错误**：检查 TypeScript 类型是否正确
2. **API 调用失败**：检查网络和后端地址配置
3. **页面空白**：打开浏览器控制台查看错误
4. **样式问题**：检查 SASS 语法和 Element Plus 主题

## 🎉 开始开发吧！

现在你可以：
1. ✅ 启动项目：`npm run dev`
2. ✅ 访问：http://localhost:5173
3. ✅ 开始开发新功能！

祝开发顺利！ 🚀

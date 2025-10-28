## 🎨 设计规范
**色彩**：主色#1890ff、成功#52c41a、警告#faad14、错误#f5222d  
**字体**：标题16-24px粗体/正文14px/辅助12px | **间距**：8/16/24px
### 1. 首页
```
Academic dashboard: 4 stat cards (cases/docs/KBs/chats) + document table (title/badge/size/status/time). Blue #1890ff theme, minimalist. --ar 16:9 --v 6
```

### 2. 文档管理
```
Doc manager: drag-drop upload zone, table with file badges (PDF-red/DOCX-blue/TXT-green), status icons (parsing-spin/done-check/fail-X). Search+filter top. --ar 16:9 --v 6
```

### 3. 知识库
```
KB cards grid (3-4/row): folder icon, title, description, doc badge, date, hover edit/delete. White cards, blue accents. --ar 16:9 --v 6
```

### 4. 智能对话
```
AI chat: sidebar (25%) conversation list, main (75%) bubbles - user blue right/AI white left. Input+RAG toggle+send. Markdown+code. Settings: temp/topK/threshold. --ar 16:9 --v 6
```

### 5. RAG 问答
```
RAG split: left (30%) KB/Docs tabs, right (70%) chat+citation cards (source/score/snippet). Graph toggle. Empty: "Select resource". --ar 16:9 --v 6
```

### 6. 知识图谱
```
Force-directed graph: blue-docs/green-concepts/orange-authors nodes, edge weight. Type selector+filter+limit slider. Stats sidebar. Legend. Drag/zoom. --ar 16:9 --v 6
```
### 7. 智能分析
```
Paper summary: split view - info 40% / summary cards 60% (Background/Methods/Findings). Progress+export. --ar 16:9 --v 6
Paper compare: matrix table, yellow-diff/green-sim, radar chart. --ar 16:9 --v 6
Innovation cluster: scatter/network by theme, sidebar theme list, trend chart. --ar 16:9 --v 6
Literature review: chip tags, sections (History/Status/Gaps), timeline viz. --ar 16:9 --v 6
```

### 8. 教学案例
```
Case table: title/subject badge/difficulty stars/doc count/actions. Create dialog: markdown editor split preview. --ar 16:9 --v 6
```

### 9. 登录页
```
Split login: left 60% blue gradient+books/graphs illustration, right 40% form - logo+inputs+button. --ar 16:9 --v 6
```

## 交互 & 响应式
**状态**：Hover阴影↑/Active色深/Disabled灰/Loading转圈  
**暗色**：背景#1f1f1f/卡片#2d2d2d/文字#e0e0e0  
**资源**: [Element Plus](https://element-plus.org/) | [ECharts](https://echarts.apache.org/)


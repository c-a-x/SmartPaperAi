// 通用响应类型
export interface ApiResponse<T = any> {
  code: string
  msg: string
  success: boolean
  timestamp: number
  data: T
}

// 分页响应类型
export interface PageResult<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 用户信息类型
export interface UserInfo {
  userId: number
  username: string
  nickname: string
  email: string
  avatar: string
  gender: number
  phone: string
  lastLoginTime: string
  lastLoginIp: string
}

// 登录响应
export interface LoginVO {
  token: string
  tokenName: string
  tokenPrefix: string
  tokenTimeout: number
  userInfo: UserInfo
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
}

// 注册请求
export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email?: string
  captcha?: string
  captchaKey?: string
}

// 教学设计列表项
export interface TeachingPlanListVO {
  id: number
  title: string
  grade: string
  subject: string
  createTime: string
  updateTime: string
}

// 教学目标
export interface TeachingObjectives {
  knowledge: string[]  // 知识目标
  skills: string[]     // 能力目标
  values: string[]     // 情感态度价值观
}

// 教学步骤
export interface TeachingStep {
  step: number         // 步骤序号
  name: string         // 步骤名称
  duration: string     // 时长
  content: string      // 内容
  activities: string[] // 活动列表
}

// 教学设计详情（API返回的结构）
export interface TeachingPlanVO {
  title: string
  grade: string
  subject: string
  duration: string
  objectives: TeachingObjectives
  keyPoints: string[]       // 教学重点
  difficulties: string[]    // 教学难点
  teachingSteps: TeachingStep[]
  assignments: string[]     // 作业
  resources: string[]       // 资源
}

// 教学设计详情（旧版，保留兼容性）
export interface TeachingPlanDetailVO {
  id: number
  title: string
  grade: string
  subject: string
  textbook: string
  lessonType: string
  classHours: number
  teachingObjectives: string
  teachingKeyPoints: string
  teachingDifficulties: string
  teachingMethods: string
  teachingAids: string
  teachingProcess: string
  boardDesign: string
  teachingReflection: string
  createTime: string
  updateTime: string
}

// 创建/更新教学设计请求（保存时使用TeachingPlanVO）
export interface CreateTeachingPlanDTO extends TeachingPlanVO {
  // 保存时直接使用TeachingPlanVO结构
}

// RAG答案包装类型（包含引用）
export interface RagAnswerVO<T> {
  answer: T
  citations: CitationVO[]
  metadata?: any
}

// 文档列表项
export interface DocumentListVO {
  id: string  // 改为字符串类型
  title: string  // 文档标题
  type: string  // 文档类型（如：学术知识）
  fileSize: number  // 文件大小
  status: string  // 状态（completed等）
  createTime: string  // 创建时间
  updateTime: string  // 更新时间
}

// 文档详情
export interface DocumentDetailVO {
  id: number
  filename: string
  originalFilename: string
  filePath: string
  fileSize: number
  fileType: string
  status: string
  uploadTime: string
  processTime?: string
  errorMessage?: string
  userId: number
  metadata?: DocumentMetadata
}

// 文档元数据
export interface DocumentMetadata {
  title?: string
  authors?: string[]
  abstract?: string
  keywords?: string[]
  publishDate?: string
  journal?: string
  doi?: string
  citations?: number
}

// 文档搜索请求
export interface DocumentSearchRequest {
  title?: string
  fileType?: string
  startDate?: string
  endDate?: string
  status?: string
  current: number
  size: number
  kbId?: any  // 知识库ID，用于查询指定知识库的文档
}

// 知识库
export interface KnowledgeBaseDO {
  id: number
  name: string
  description: string
  userId: number
  documentCount: number
  isDeleted: number
  createTime: string
  updateTime: string
}

// 创建知识库请求
export interface CreateKBDTO {
  name: string
  description?: string
}

// 知识库文档
export interface KBDocumentVO {
  documentId: number
  kbId: number
  addTime: string
  documentInfo: DocumentListVO
}

// AI 聊天请求（普通和流式共用）
export interface ChatRequest {
  conversationId?: string // 会话ID(可选,不传则创建新会话)
  message: string // 用户消息
  enableRag?: boolean // 是否启用RAG(检索增强生成),默认false
  ragTopK?: number // RAG检索文档数量,默认5
  customTemperature?: number // 自定义温度参数(0.0-2.0),覆盖默认配置
  customMaxTokens?: number // 自定义最大Token数,覆盖默认配置
  customSimilarityThreshold?: number // 自定义相似度阈值(RAG时使用),覆盖默认配置
  enableMemory?: boolean // 是否启用对话记忆,默认true
}

// AI 聊天响应（普通非流式）
export interface ChatResponse {
  conversationId: string // 会话ID
  userMessage: string // 用户消息
  aiResponse: string // AI 回复
  timestamp: string // 响应时间
}

// AI 流式聊天响应片段（SSE 事件数据）
export interface ChatStreamChunk {
  content?: string // 流式内容片段
  conversationId?: string // 会话ID（首次发送）
  done?: boolean // 是否完成
}

// RAG 聊天请求（已废弃，新接口使用 query 参数）
export interface RagChatRequest {
  question: string
  documentIds?: number[]
  kbIds?: number[]
  conversationId?: string
  topK?: number
}

// 引用来源（Citation）
export interface CitationVO {
  documentId: number
  title: string
  chunkId: number
  chunkIndex: number
  score: number
  snippet: string
  keywords: string[]
  position?: string
  metadata?: string
}

// 知识图谱节点属性
export interface NodeProperties {
  name?: string
  description?: string
  category?: string
  field?: string
  importance?: number
  frequency?: number
  documentId?: number
  title?: string
  userId?: number
}

// 知识图谱节点
export interface GraphNode {
  id: string
  type: 'Concept' | 'Document' | 'Author'
  label: string
  properties: NodeProperties
}

// 知识图谱关系属性
export interface RelationshipProperties {
  weight?: number
  confidence?: number
  description?: string
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

// 知识图谱统计
export interface GraphStats {
  totalNodes: number
  totalRelationships: number
  conceptNodes: number
  documentNodes: number
  authorNodes: number
}

// 知识图谱
export interface KnowledgeGraphVO {
  nodes: GraphNode[]
  relationships: GraphRelationship[]
  stats: GraphStats
}

// RAG 聊天响应
export interface RagChatResponse {
  answer: string
  citations: CitationVO[]
  knowledgeGraph?: KnowledgeGraphVO
}

// 引用来源（旧版，保留兼容性）
export interface SourceReference {
  documentId: number
  documentName: string
  content: string
  score: number
}

// 会话列表项
export interface ConversationVO {
  id: number
  conversationId: string
  title: string
  createTime: string
  updateTime: string
}

// 聊天消息
export interface ChatMessage {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
  createTime: string
  sources?: SourceReference[]
}

// 分页响应
export interface PageResponse<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

// ========== 论文分析相关类型 ==========

// 论文摘要内容
export interface SummaryContent {
  background: string
  methodology: string
  results: string
  innovations: string[]
  limitations: string
}

// 论文摘要
export interface PaperSummaryVO {
  title: string
  authors: string[]
  publicationYear: number
  summary: SummaryContent
  keywords: string[]
}

// 论文对比 - 维度
export interface ComparisonDimension {
  id: string
  name: string
  description: string
}

// 论文对比 - 论文信息
export interface PaperInComparison {
  documentId: number
  title: string
  authors: string
  year: string
}

// 论文对比 - 对比行
export interface ComparisonRow {
  dimensionId: string
  dimensionName: string
  values: string[]
}

// 论文对比结果
export interface PaperComparisonVO {
  dimensions: ComparisonDimension[]
  papers: PaperInComparison[]
  matrix: ComparisonRow[]
}

// 论文对比请求
export interface PaperComparisonDTO {
  documentIds: number[]
  dimensions?: string[]
}

// 创新点
export interface InnovationPoint {
  description: string
  paperTitle: string
  documentId: number
  noveltyScore: number
}

// 创新点聚类
export interface InnovationClusterVO {
  topic: string
  innovations: InnovationPoint[]
  importance: number
  paperCount: number
}

// 文献综述 - 论文信息
export interface PaperInfo {
  documentId: number
  title: string
  authors: string[]
  year: number
}

// 代表性工作
export interface RepresentativeWork {
  documentId: number
  title: string
  contribution: string
  impactScore: number
}

// 研究现状
export interface ResearchStatus {
  overview: string
  mainThemes: string[]
  representativeWorks: RepresentativeWork[]
}

// 方法分类
export interface MethodCategory {
  categoryName: string
  documentIds: number[]
  prosAndCons: string
}

// 方法对比
export interface MethodologyComparison {
  summary: string
  categories: MethodCategory[]
  evolution: string
}

// 趋势分析
export interface TrendAnalysis {
  timeline: string
  hotTopics: string[]
  emergingTechnologies: string[]
  focusShift: string
}

// 研究空白
export interface ResearchGaps {
  unsolvedProblems: string[]
  methodologicalLimitations: string[]
  dataGaps: string[]
}

// 未来方向
export interface FutureDirections {
  directions: string[]
  interdisciplinaryOpportunities: string[]
  applicationProspects: string
}

// 关键词频率
export interface KeywordFrequency {
  keyword: string
  frequency: number
}

// 文献综述
export interface LiteratureReviewVO {
  topic: string
  paperCount: number
  papers: PaperInfo[]
  researchStatus: ResearchStatus
  methodologyComparison: MethodologyComparison
  trendAnalysis: TrendAnalysis
  researchGaps: ResearchGaps
  futureDirections: FutureDirections
  keywordCloud: KeywordFrequency[]
}

你是一位资深的学术研究专家，擅长撰写高质量的文献综述。请基于以下论文信息，生成一篇结构化的文献综述。

【论文信息】
{paperSummaries}

【任务要求】
请综合分析这些文献，生成JSON格式的综述报告，包含以下内容：

1. **研究现状** (researchStatus):
   - overview: 研究领域的整体概述（200-300字）
   - mainThemes: 提取3-5个主要研究主题
   - representativeWorks: 列出3-5项代表性工作（包含documentId、title、contribution、impactScore 1-5分）

2. **研究方法对比** (methodologyComparison):
   - summary: 方法分类总结（150-200字）
   - categories: 将论文按方法分类（每类包含categoryName、documentIds、prosAndCons）
   - evolution: 方法演进趋势描述（100-150字）

3. **发展趋势分析** (trendAnalysis):
   - timeline: 按时间轴描述研究发展（200-250字）
   - hotTopics: 列出3-5个热点主题
   - emergingTechnologies: 列出2-4个新兴技术
   - focusShift: 研究重心转移描述（100-150字）

4. **研究空白与挑战** (researchGaps):
   - unsolvedProblems: 列出3-5个尚未解决的问题
   - methodologicalLimitations: 列出2-3个方法论上的局限
   - dataGaps: 列出2-3个数据/资源缺口

5. **未来研究方向** (futureDirections):
   - directions: 列出3-5个潜在研究方向
   - interdisciplinaryOpportunities: 列出2-3个跨学科融合机会
   - applicationProspects: 实际应用前景描述（150-200字）

【JSON格式要求】
{
  "topic": "提取的综述主题（10-20字）",
  "paperCount": 论文数量,
  "papers": [
    {"documentId": 123, "title": "论文标题", "authors": ["作者1", "作者2"], "year": 2023}
  ],
  "researchStatus": {
    "overview": "研究领域整体概述...",
    "mainThemes": ["主题1", "主题2", "主题3"],
    "representativeWorks": [
      {"documentId": 123, "title": "论文标题", "contribution": "核心贡献描述", "impactScore": 4.5}
    ]
  },
  "methodologyComparison": {
    "summary": "方法分类总结...",
    "categories": [
      {"categoryName": "深度学习方法", "documentIds": [123, 456], "prosAndCons": "优点：准确率高；缺点：计算资源需求大"}
    ],
    "evolution": "方法演进趋势..."
  },
  "trendAnalysis": {
    "timeline": "时间轴趋势描述...",
    "hotTopics": ["热点1", "热点2"],
    "emergingTechnologies": ["技术1", "技术2"],
    "focusShift": "研究重心转移..."
  },
  "researchGaps": {
    "unsolvedProblems": ["问题1", "问题2"],
    "methodologicalLimitations": ["局限1", "局限2"],
    "dataGaps": ["数据缺口1", "数据缺口2"]
  },
  "futureDirections": {
    "directions": ["方向1", "方向2"],
    "interdisciplinaryOpportunities": ["机会1", "机会2"],
    "applicationProspects": "应用前景描述..."
  },
  "keywordCloud": [
    {"keyword": "关键词1", "frequency": 5},
    {"keyword": "关键词2", "frequency": 3}
  ]
}

【关键要求】
1. 严格基于提供的论文信息，不要编造内容
2. documentId 必须使用实际的论文ID
3. 分析要深入、客观、有洞察力
4. 注重论文之间的关联和对比
5. 突出研究脉络和演进趋势
6. 识别研究空白和未来机会
7. 关键词频率基于实际出现次数统计
8. 所有数组至少包含1个元素，字符串字段不能为空

请直接返回JSON，不要包含任何其他文字说明。

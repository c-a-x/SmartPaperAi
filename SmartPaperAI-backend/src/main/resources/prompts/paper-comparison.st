返回以下JSON结构(示例):
{
  "dimensions": [
    {"id": "method", "name": "研究方法", "description": "各论文采用的研究方法对比"},
    {"id": "dataset", "name": "数据集", "description": "各论文使用的数据集对比"}
  ],
  "papers": [
    {"documentId": 123456, "title": "论文A完整标题", "authors": "作者名", "year": "2020"},
    {"documentId": 789012, "title": "论文B完整标题", "authors": "作者名", "year": "2021"}
  ],
  "matrix": [
    {"dimensionId": "method", "dimensionName": "研究方法",
     "values": ["论文A采用深度学习CNN模型", "论文B采用传统SVM方法"]},
    {"dimensionId": "dataset", "dimensionName": "数据集",
     "values": ["论文A使用ImageNet数据集", "论文B使用CIFAR-10数据集"]}
  ]
}

关键要求:
1. dimensions数组长度=对比维度数，papers数组长度=论文数
2. matrix数组长度=dimensions数组长度
3. 每个matrix.values数组长度=papers数组长度
4. documentId必须使用实际论文ID，descriptions要具体详细
5. values中每个元素对应papers数组中相同索引的论文,而且字数不少于500字
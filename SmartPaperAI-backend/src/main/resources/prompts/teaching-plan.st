生成教学设计JSON:
课题: {topic}
学段: {grade}
学科: {subject}
参考: {context}

JSON结构:
{
  "title": "课题标题",
  "grade": "{grade}",
  "subject": "{subject}",
  "duration": "课时时长",
  "objectives": {
    "knowledge": ["知识目标1", "知识目标2", "知识目标3",等等根据实际内容自行调整],
    "skills": ["能力目标1", "能力目标2", "能力目标3",等等根据实际内容自行调整],
    "values": ["情感态度目标1", "情感态度目标2",等等根据实际内容自行调整]
  },
  "keyPoints": ["教学重点1", "教学重点2", "教学重点3",等等根据实际内容自行调整],
  "difficulties": ["教学难点1", "教学难点2",等等根据实际内容自行调整],
  "teachingSteps": [
    {
      "step": 1,
      "name": "导入环节",
      "duration": "时长",
      "content": "详细教学过程描述",
      "activities": ["活动1", "活动2", "活动3",等等根据实际内容自行调整]
    },
    {
      "step": 2,
      "name": "新知探究",
      "duration": "时长",
      "content": "详细教学过程描述",
      "activities": ["活动1", "活动2", "活动3",等等根据实际内容自行调整]
    },
    {
      "step": 3,
      "name": "巩固练习",
      "duration": "时长",
      "content": "详细教学过程描述",
      "activities": ["活动1", "活动2",等等根据实际内容自行调整]
    },
    {
      "step": 4,
      "name": "课堂总结",
      "duration": "时长",
      "content": "详细教学过程描述",
      "activities": ["活动1", "活动2",等等根据实际内容自行调整]
    },{
    实际步骤数量根据实际内容自行调整,但最少4个
    }
  ],
  "assignments": ["作业1", "作业2", "作业3",等等根据实际内容自行调整],
  "resources": ["资源1", "资源2", "资源3",等等根据实际内容自行调整]
}

要求:
1. 只返回JSON,无markdown标记
2. objectives包含knowledge/skills/values三维,各2-4项
3. keyPoints和difficulties各2-4项
4. teachingSteps包含4个阶段,每个step的content 100-150字,activities 3-5项
5. assignments和resources各3-5项
6. 体现{grade}学段和{subject}学科特点
7. 内容具体,不用"..."等占位符
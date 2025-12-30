// 全局配置文件
export const config = {
  // API 基础地址
  apiBaseUrl: 'http://101.132.70.68:8069',

  // Token 存储键名
  tokenKey: 'smart_paper_token',

  // 用户信息存储键名
  userInfoKey: 'smart_paper_user_info',

  // 请求超时时间
  timeout: 300000,

  // 文件上传大小限制（MB）
  uploadMaxSize: 100,

  // 分页默认配置
  pagination: {
    defaultPageSize: 5,
    pageSizes: [5, 10, 15, 20]
  }
}

<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="layout-aside">
      <div class="sidebar">
        <!-- Logo -->
        <div class="sidebar-logo">
          <img src="@/assets/logo.png" alt="Logo" class="logo-icon" />
          <transition name="fade">
            <span v-show="!isCollapse" class="logo-text">SmartPaperAI</span>
          </transition>
        </div>

        <!-- 菜单 -->
        <el-menu :default-active="activeMenu" :collapse="isCollapse" :collapse-transition="false" class="sidebar-menu"
          router>
          <el-menu-item index="/">
            <el-icon>
              <HomeFilled />
            </el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <el-menu-item index="/teaching">
            <el-icon>
              <Reading />
            </el-icon>
            <template #title>教学设计</template>
          </el-menu-item>

          <el-menu-item index="/documents">
            <el-icon>
              <Document />
            </el-icon>
            <template #title>文档管理</template>
          </el-menu-item>

          <el-menu-item index="/knowledge">
            <el-icon>
              <Collection />
            </el-icon>
            <template #title>知识库</template>
          </el-menu-item>

          <el-menu-item index="/chat">
            <el-icon>
              <ChatDotRound />
            </el-icon>
            <template #title>AI 对话</template>
          </el-menu-item>

          <el-menu-item index="/rag-chat">
            <el-icon>
              <ChatLineSquare />
            </el-icon>
            <template #title>知识库问答</template>
          </el-menu-item>

          <el-menu-item index="/analysis/summary">
            <el-icon>
              <Document />
            </el-icon>
            <template #title>论文总结</template>
          </el-menu-item>

          <el-menu-item index="/analysis/comparison">
            <el-icon>
              <Grid />
            </el-icon>
            <template #title>论文对比</template>
          </el-menu-item>

          <el-menu-item index="/analysis/innovation">
            <el-icon>
              <MagicStick />
            </el-icon>
            <template #title>创新点聚合</template>
          </el-menu-item>

          <el-menu-item index="/analysis/review">
            <el-icon>
              <Reading />
            </el-icon>
            <template #title>文献综述</template>
          </el-menu-item>

          <el-menu-item index="/knowledge-graph">
            <el-icon>
              <Share />
            </el-icon>
            <template #title>知识图谱</template>
          </el-menu-item>
        </el-menu>

        <!-- 折叠按钮 -->
        <div class="collapse-button" @click="toggleCollapse">
          <el-icon>
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
        </div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="layout-main">
      <!-- 顶部导航栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 搜索 -->
          <el-input v-model="searchKeyword" placeholder="搜索文档..." class="search-input" clearable
            @keyup.enter="handleSearch">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>

          <!-- 通知 -->
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="header-item">
            <el-icon class="header-icon" :size="20">
              <Bell />
            </el-icon>
          </el-badge>

          <!-- 用户信息 -->
          <el-dropdown class="header-item user-dropdown" @command="handleCommand">
            <div class="user-info">
              <el-avatar :src="userInfo?.avatar" :size="32">
                <el-icon>
                  <User />
                </el-icon>
              </el-avatar>
              <span class="username">{{ userInfo?.nickname || userInfo?.username }}</span>
              <el-icon class="dropdown-icon">
                <ArrowDown />
              </el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- <el-dropdown-item command="profile">
                  <el-icon>
                    <User />
                  </el-icon>
                  <span>个人中心</span>
                </el-dropdown-item> -->
                <!-- <el-dropdown-item command="settings">
                  <el-icon>
                    <Setting />
                  </el-icon>
                  <span>设置</span>
                </el-dropdown-item> -->
                <el-dropdown-item divided command="logout">
                  <el-icon>
                    <SwitchButton />
                  </el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="layout-content">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  DocumentCopy,
  HomeFilled,
  Reading,
  Document,
  Collection,
  ChatDotRound,
  ChatLineSquare,
  Grid,
  MagicStick,
  Share,
  Expand,
  Fold,
  Search,
  Bell,
  User,
  ArrowDown,
  Setting,
  SwitchButton
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 侧边栏折叠状态
const isCollapse = ref(false)

// 用户信息
const userInfo = computed(() => authStore.userInfo)

// 通知数量
const notificationCount = ref(0)

// 搜索关键词
const searchKeyword = ref('')

// 缓存的视图
const cachedViews = ref<string[]>([])

// 当前激活的菜单
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/teaching')) return '/teaching'
  if (path.startsWith('/documents')) return '/documents'
  if (path.startsWith('/knowledge-graph')) return '/knowledge-graph'
  if (path.startsWith('/knowledge')) return '/knowledge'
  if (path.startsWith('/chat')) return '/chat'
  if (path.startsWith('/rag-chat')) return '/rag-chat'
  if (path.startsWith('/analysis/summary')) return '/analysis/summary'
  if (path.startsWith('/analysis/comparison')) return '/analysis/comparison'
  if (path.startsWith('/analysis/innovation')) return '/analysis/innovation'
  if (path.startsWith('/analysis/review')) return '/analysis/review'
  return '/'
})

// 面包屑导航
const breadcrumbs = computed(() => {
  const matched = route.matched.filter((item) => item.meta && item.meta.title)
  return matched.map((item) => ({
    path: item.path,
    title: item.meta.title as string
  }))
})

// 切换侧边栏折叠状态
function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

// 处理搜索
function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/documents',
      query: { keyword: searchKeyword.value }
    })
  }
}

// 处理下拉菜单命令
async function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      await handleLogout()
      break
  }
}

// 退出登录
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    // 用户取消操作
  }
}

// 监听路由变化，更新面包屑
watch(
  () => route.path,
  () => {
    // 可以在这里添加缓存逻辑
  }
)
</script>

<style scoped lang="scss">
.layout-container {
  display: flex;
  min-height: 100vh;
  height: auto;
  overflow: hidden;
}

.layout-aside {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(180deg, #001529 0%, #000c17 100%);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15),
    4px 0 20px rgba(24, 144, 255, 0.1);
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 1px;
    height: 100%;
    background: linear-gradient(180deg,
        rgba(24, 144, 255, 0) 0%,
        rgba(24, 144, 255, 0.3) 50%,
        rgba(24, 144, 255, 0) 100%);
    animation: edgeGlow 3s ease-in-out infinite;
  }
}

@keyframes edgeGlow {

  0%,
  100% {
    opacity: 0.3;
  }

  50% {
    opacity: 0.8;
  }
}

.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;

  .sidebar-logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 0 16px;
    color: #fff;
    font-size: 20px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    position: relative;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.3s ease;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg,
          rgba(24, 144, 255, 0) 0%,
          rgba(24, 144, 255, 0.1) 50%,
          rgba(24, 144, 255, 0) 100%);
      transform: translateX(-100%);
      transition: transform 0.6s ease;
    }

    &:hover {
      background-color: rgba(24, 144, 255, 0.1);
      box-shadow: 0 0 20px rgba(24, 144, 255, 0.3),
        inset 0 0 10px rgba(24, 144, 255, 0.1);
    }

    &:hover::before {
      transform: translateX(100%);
    }

    .logo-icon {
      width: 40px;
      height: 40px;
      object-fit: contain;
      transition: all 0.4s ease;
      filter: drop-shadow(0 2px 4px rgba(24, 144, 255, 0.3));
    }

    &:hover .logo-icon {
      transform: rotate(360deg) scale(1.1);
      filter: drop-shadow(0 0 15px rgba(24, 144, 255, 0.8)) drop-shadow(0 0 25px rgba(24, 144, 255, 0.4));
    }

    .logo-text {
      white-space: nowrap;
      transition: all 0.3s ease;
      position: relative;
    }

    &:hover .logo-text {
      letter-spacing: 1px;
      text-shadow: 0 0 10px rgba(24, 144, 255, 0.6),
        0 0 20px rgba(24, 144, 255, 0.3);
    }
  }

  .sidebar-menu {
    flex: 1;
    border-right: none;
    background-color: #001529;

    :deep(.el-menu-item) {
      color: rgba(255, 255, 255, 0.65);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      border-radius: 8px;
      margin: 4px 8px;

      &:hover {
        color: #fff;
        background-color: rgba(255, 255, 255, 0.08);
        box-shadow: 0 0 20px rgba(24, 144, 255, 0.4),
          0 0 40px rgba(24, 144, 255, 0.2),
          inset 0 0 10px rgba(24, 144, 255, 0.1);
        transform: translateX(4px);
      }

      &.is-active {
        color: #fff;
        background-color: #1890ff !important;
        box-shadow: 0 0 25px rgba(24, 144, 255, 0.6),
          0 0 50px rgba(24, 144, 255, 0.3),
          inset 0 0 15px rgba(255, 255, 255, 0.2);
        animation: menuItemGlow 2s ease-in-out infinite;
      }

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 0;
        background: linear-gradient(180deg, #1890ff, #69c0ff);
        border-radius: 0 2px 2px 0;
        transition: height 0.3s ease;
        box-shadow: 0 0 10px rgba(24, 144, 255, 0.8);
      }

      &:hover::before {
        height: 60%;
      }

      &.is-active::before {
        height: 80%;
        box-shadow: 0 0 15px rgba(24, 144, 255, 1),
          0 0 25px rgba(24, 144, 255, 0.6);
      }
    }

    :deep(.el-icon) {
      font-size: 18px;
      transition: all 0.3s ease;
    }

    :deep(.el-menu-item:hover .el-icon) {
      transform: scale(1.1);
      filter: drop-shadow(0 0 8px rgba(24, 144, 255, 0.8));
    }

    :deep(.el-menu-item.is-active .el-icon) {
      filter: drop-shadow(0 0 10px rgba(255, 255, 255, 0.8));
    }
  }

  /* 菜单项发光动画 */
  @keyframes menuItemGlow {

    0%,
    100% {
      box-shadow: 0 0 25px rgba(24, 144, 255, 0.6),
        0 0 50px rgba(24, 144, 255, 0.3),
        inset 0 0 15px rgba(255, 255, 255, 0.2);
    }

    50% {
      box-shadow: 0 0 30px rgba(24, 144, 255, 0.8),
        0 0 60px rgba(24, 144, 255, 0.4),
        inset 0 0 20px rgba(255, 255, 255, 0.3);
    }
  }

  .collapse-button {
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: rgba(255, 255, 255, 0.65);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 0;
      height: 0;
      border-radius: 50%;
      background: rgba(24, 144, 255, 0.3);
      transition: width 0.6s ease, height 0.6s ease;
    }

    &:hover {
      color: #fff;
      background-color: rgba(24, 144, 255, 0.15);
      box-shadow: 0 0 15px rgba(24, 144, 255, 0.4),
        inset 0 0 10px rgba(24, 144, 255, 0.2);
    }

    &:hover::before {
      width: 100%;
      height: 100%;
    }

    .el-icon {
      font-size: 18px;
      transition: all 0.3s ease;
      position: relative;
      z-index: 1;
    }

    &:hover .el-icon {
      transform: scale(1.2);
      filter: drop-shadow(0 0 8px rgba(24, 144, 255, 0.8));
    }

    &:active .el-icon {
      transform: scale(1.1) rotate(90deg);
    }
  }
}

.layout-main {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  position: relative;
  background: linear-gradient(135deg, #fafafa 0%, #f5f7fa 100%);

  /* 科技感背景网格 */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image:
      linear-gradient(rgba(24, 144, 255, 0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(24, 144, 255, 0.03) 1px, transparent 1px);
    background-size: 20px 20px;
    pointer-events: none;
    opacity: 0.5;
  }

  /* 动态光效 */
  &::after {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle,
        rgba(24, 144, 255, 0.05) 0%,
        transparent 50%);
    animation: lightMove 15s ease-in-out infinite;
    pointer-events: none;
  }
}

@keyframes lightMove {

  0%,
  100% {
    transform: translate(0, 0);
  }

  25% {
    transform: translate(10%, 10%);
  }

  50% {
    transform: translate(-5%, 15%);
  }

  75% {
    transform: translate(15%, -5%);
  }
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px) saturate(180%);
  border-bottom: 1px solid rgba(24, 144, 255, 0.1);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.08),
    0 1px 0 rgba(24, 144, 255, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
  animation: headerSlideDown 0.6s ease-out;

  /* 顶部发光线 */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg,
        transparent 0%,
        rgba(24, 144, 255, 0.6) 25%,
        rgba(24, 144, 255, 0.8) 50%,
        rgba(24, 144, 255, 0.6) 75%,
        transparent 100%);
    background-size: 200% 100%;
    animation: headerGlow 3s linear infinite;
  }

  .header-left {
    flex: 1;
    position: relative;
    z-index: 1;

    :deep(.el-breadcrumb) {
      animation: breadcrumbFadeIn 0.8s ease-out;
    }

    :deep(.el-breadcrumb__item) {
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;
    position: relative;
    z-index: 1;

    .search-input {
      width: 280px;

      :deep(.el-input__wrapper) {
        background: rgba(245, 247, 250, 0.8);
        border: 1px solid rgba(24, 144, 255, 0.1);
        border-radius: 20px;
        transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
        box-shadow: 0 2px 4px rgba(24, 144, 255, 0.05);

        &:hover {
          background: rgba(255, 255, 255, 1);
          border-color: rgba(24, 144, 255, 0.3);
          box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.08),
            0 4px 8px rgba(24, 144, 255, 0.1);
          transform: translateY(-1px);
        }

        &.is-focus {
          background: rgba(255, 255, 255, 1);
          border-color: var(--primary-color);
          box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.15),
            0 0 20px rgba(24, 144, 255, 0.2),
            0 4px 12px rgba(24, 144, 255, 0.15);
          transform: translateY(-2px);
        }
      }

      :deep(.el-input__prefix) {
        transition: all 0.3s ease;
      }

      &:hover :deep(.el-input__prefix) {
        color: var(--primary-color);
        transform: scale(1.1);
      }
    }

    .header-item {
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .header-icon {
      color: var(--text-secondary);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      z-index: 1;

      &:hover {
        color: var(--primary-color);
        transform: scale(1.15);
        filter: drop-shadow(0 0 8px rgba(24, 144, 255, 0.6));
      }
    }

    :deep(.el-badge__content) {
      background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
      border: 2px solid var(--bg-white);
      font-weight: 600;
      animation: badgePulse 2s ease-in-out infinite;
    }

    .user-dropdown {
      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;

        .username {
          font-size: 14px;
          color: #333;
          max-width: 120px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .dropdown-icon {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
}

@keyframes headerSlideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes headerGlow {
  0% {
    background-position: 0% 50%;
  }

  100% {
    background-position: 200% 50%;
  }
}

@keyframes breadcrumbFadeIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes badgePulse {

  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4);
  }

  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 4px rgba(255, 77, 79, 0);
  }
}

.layout-content {
  flex: 1 1 auto;
  overflow-y: auto;
  overflow-x: hidden;
  background: transparent;
  padding: 24px;
  box-sizing: border-box;
  position: relative;
  z-index: 1;

  /* 优化滚动体验 */
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;

  /* 内容区域入场动画 */
  animation: contentFadeIn 0.8s ease-out;

  /* 科技感装饰角 */
  &::before,
  &::after {
    content: '';
    position: fixed;
    width: 100px;
    height: 100px;
    border: 2px solid rgba(24, 144, 255, 0.15);
    pointer-events: none;
    transition: all 0.3s ease;
  }

  &::before {
    top: 64px;
    left: 240px;
    border-right: none;
    border-bottom: none;
    border-top-left-radius: 8px;
  }

  &::after {
    bottom: 24px;
    right: 24px;
    border-left: none;
    border-top: none;
    border-bottom-right-radius: 8px;
  }

  /* 滚动时的效果 */
  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(245, 247, 250, 0.5);
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: linear-gradient(180deg,
        rgba(24, 144, 255, 0.3),
        rgba(24, 144, 255, 0.5));
    border-radius: 4px;
    transition: all 0.3s ease;

    &:hover {
      background: linear-gradient(180deg,
          rgba(24, 144, 255, 0.5),
          rgba(24, 144, 255, 0.7));
      box-shadow: 0 0 8px rgba(24, 144, 255, 0.4);
    }
  }
}

@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式优化 */
@media (max-width: 768px) {
  .layout-header {
    padding: 0 16px;

    .header-right {
      gap: 12px;

      .search-input {
        width: 180px;
      }

      .username {
        display: none;
      }
    }
  }

  .layout-content {
    padding: 16px;

    &::before,
    &::after {
      display: none;
    }
  }
}

// 页面过渡动画增强
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-transform-enter-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-transform-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px) scale(0.98);
  filter: blur(4px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px) scale(0.98);
  filter: blur(4px);
}

/* 给所有路由视图添加入场光效 */
.layout-content>div {
  animation: viewEnter 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes viewEnter {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
    box-shadow: 0 0 0 0 rgba(24, 144, 255, 0);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    box-shadow: 0 0 30px 0 rgba(24, 144, 255, 0);
  }
}
</style>

<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="layout-aside">
      <div class="sidebar">
        <!-- Logo -->
        <div class="sidebar-logo">
          <el-icon class="logo-icon">
            <DocumentCopy />
          </el-icon>
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
  transition: width 0.3s;
  background-color: #001529;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
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

    .logo-icon {
      font-size: 28px;
      color: #1890ff;
    }

    .logo-text {
      white-space: nowrap;
    }
  }

  .sidebar-menu {
    flex: 1;
    border-right: none;
    background-color: #001529;

    :deep(.el-menu-item) {
      color: rgba(255, 255, 255, 0.65);

      &:hover {
        color: #fff;
        background-color: rgba(255, 255, 255, 0.08);
      }

      &.is-active {
        color: #fff;
        background-color: #1890ff !important;
      }
    }

    :deep(.el-icon) {
      font-size: 18px;
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
    transition: all 0.3s;

    &:hover {
      color: #fff;
      background-color: rgba(255, 255, 255, 0.08);
    }

    .el-icon {
      font-size: 18px;
    }
  }
}

.layout-main {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  /* allow children to control overflow */
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-left {
    flex: 1;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 24px;

    .search-input {
      width: 300px;
    }

    .header-item {
      cursor: pointer;
    }

    .header-icon {
      color: #666;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
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
          transition: transform 0.3s;
        }

        &:hover .dropdown-icon {
          transform: rotate(180deg);
        }
      }
    }
  }
}

.layout-content {
  flex: 1 1 auto;
  overflow-y: auto;
  background-color: #f0f2f5;
  padding: 16px;
  box-sizing: border-box;
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>

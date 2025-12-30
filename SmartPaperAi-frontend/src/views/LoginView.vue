<template>
  <div class="login-container">
    <!-- D3.js 动态背景 -->
    <svg ref="backgroundSvg" class="background-animation"></svg>
    <div class="login-card">
      <div class="login-header">
        <h1 class="logo">
          <img src="@/assets/logo.png" alt="Logo" class="logo-icon" />
          SmartPaperAI
        </h1>
        <p class="subtitle">智能论文问答系统</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 登录表单 -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form"
            @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" clearable>
                <template #prefix>
                  <el-icon>
                    <User />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password
                clearable>
                <template #prefix>
                  <el-icon>
                    <Lock />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="captcha" v-if="showCaptcha">
              <div class="captcha-wrapper">
                <el-input v-model="loginForm.captcha" placeholder="请输入验证码" size="large" clearable style="flex: 1">
                  <template #prefix>
                    <el-icon>
                      <Key />
                    </el-icon>
                  </template>
                </el-input>
                <div class="captcha-image" @click="refreshCaptcha">
                  <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                  <span v-else>点击获取</span>
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" :loading="loginLoading" class="submit-button" @click="handleLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册表单 -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名（4-20个字符）" size="large" clearable>
                <template #prefix>
                  <el-icon>
                    <User />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" size="large" clearable>
                <template #prefix>
                  <el-icon>
                    <Message />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码（6-20个字符）" size="large"
                show-password clearable>
                <template #prefix>
                  <el-icon>
                    <Lock />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" size="large"
                show-password clearable>
                <template #prefix>
                  <el-icon>
                    <Lock />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="captcha" v-if="showCaptcha">
              <div class="captcha-wrapper">
                <el-input v-model="registerForm.captcha" placeholder="请输入验证码" size="large" clearable style="flex: 1">
                  <template #prefix>
                    <el-icon>
                      <Key />
                    </el-icon>
                  </template>
                </el-input>
                <div class="captcha-image" @click="refreshCaptcha">
                  <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                  <span v-else>点击获取</span>
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" :loading="registerLoading" class="submit-button"
                @click="handleRegister">
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="footer">
      <p>© 2025 SmartPaperAI. All rights reserved.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Key, Message, DocumentCopy } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getCaptcha } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/types'
import * as d3 from 'd3'

const router = useRouter()
const authStore = useAuthStore()

// D3.js 背景动画引用
const backgroundSvg = ref<SVGSVGElement | null>(null)

// 当前激活的标签页
const activeTab = ref('login')

// 是否显示验证码（可配置）
const showCaptcha = ref(true)

// 验证码相关
const captchaImage = ref('')
const captchaKey = ref('')

// 表单引用
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

// 加载状态
const loginLoading = ref(false)
const registerLoading = ref(false)

// 登录表单
const loginForm = reactive<LoginRequest>({
  username: '',
  password: '',
  captcha: '',
  captchaKey: ''
})

// 注册表单
const registerForm = reactive<RegisterRequest>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  captcha: '',
  captchaKey: ''
})

// 验证规则
const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: showCaptcha.value
    ? [{ required: true, message: '请输入验证码', trigger: 'blur' }]
    : []
}

const validatePassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('密码长度为6-20个字符'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请确认密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为4-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  captcha: showCaptcha.value
    ? [{ required: true, message: '请输入验证码', trigger: 'blur' }]
    : []
}

// 获取验证码
async function refreshCaptcha() {
  try {
    const response = await getCaptcha()
    captchaKey.value = response.data.captchaKey
    captchaImage.value = response.data.captchaImage
    loginForm.captchaKey = captchaKey.value
    registerForm.captchaKey = captchaKey.value
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

// 处理登录
async function handleLogin() {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loginLoading.value = true
      try {
        await authStore.login(loginForm)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
        if (showCaptcha.value) {
          refreshCaptcha()
        }
      } finally {
        loginLoading.value = false
      }
    }
  })
}

// 处理注册
async function handleRegister() {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      registerLoading.value = true
      try {
        await authStore.register(registerForm)
        ElMessage.success('注册成功，请登录')
        activeTab.value = 'login'
        // 清空注册表单
        registerFormRef.value?.resetFields()
      } catch (error: any) {
        ElMessage.error(error.message || '注册失败')
        if (showCaptcha.value) {
          refreshCaptcha()
        }
      } finally {
        registerLoading.value = false
      }
    }
  })
}

// 定义粒子和连线类型
interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  radius: number
}

interface Line {
  x1: number
  y1: number
  x2: number
  y2: number
  opacity: number
}

// 初始化 D3.js 背景动画
function initBackgroundAnimation() {
  if (!backgroundSvg.value) return

  const svg = d3.select(backgroundSvg.value)
  const width = window.innerWidth
  const height = window.innerHeight

  svg.attr('width', width).attr('height', height)

  // 创建粒子数据
  const particleCount = 250
  const particles: Particle[] = Array.from({ length: particleCount }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.5,
    vy: (Math.random() - 0.5) * 0.5,
    radius: Math.random() * 3 + 1
  }))

  // 创建粒子圆圈
  const circles = svg
    .selectAll('circle')
    .data(particles)
    .enter()
    .append('circle')
    .attr('cx', (d: Particle) => d.x)
    .attr('cy', (d: Particle) => d.y)
    .attr('r', (d: Particle) => d.radius)
    .attr('fill', 'rgba(24, 144, 255, 0.4)')
    .attr('stroke', 'rgba(105, 192, 255, 0.3)')
    .attr('stroke-width', 1)

  // 创建连接线组
  const linesGroup = svg.append('g').attr('class', 'lines')

  // 动画更新函数
  function animate() {
    // 更新粒子位置
    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy

      // 边界检测
      if (p.x < 0 || p.x > width) p.vx *= -1
      if (p.y < 0 || p.y > height) p.vy *= -1
    })

    // 更新圆圈位置
    circles
      .attr('cx', (d: Particle) => d.x)
      .attr('cy', (d: Particle) => d.y)

    // 绘制连接线
    const lines: Line[] = []
    const maxDistance = 150

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const particleI = particles[i]
        const particleJ = particles[j]
        if (!particleI || !particleJ) continue

        const dx = particleI.x - particleJ.x
        const dy = particleI.y - particleJ.y
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < maxDistance) {
          lines.push({
            x1: particleI.x,
            y1: particleI.y,
            x2: particleJ.x,
            y2: particleJ.y,
            opacity: (1 - distance / maxDistance) * 0.3
          })
        }
      }
    }

    // 更新连接线
    const lineSelection = linesGroup.selectAll('line').data(lines)

    lineSelection.exit().remove()

    lineSelection
      .enter()
      .append('line')
      .merge(lineSelection as any)
      .attr('x1', (d: Line) => d.x1)
      .attr('y1', (d: Line) => d.y1)
      .attr('x2', (d: Line) => d.x2)
      .attr('y2', (d: Line) => d.y2)
      .attr('stroke', 'rgba(24, 144, 255, 0.4)')
      .attr('stroke-width', 1)
      .attr('opacity', (d: Line) => d.opacity)

    requestAnimationFrame(animate)
  }

  animate()
}

// 组件挂载时获取验证码和初始化动画
onMounted(() => {
  if (showCaptcha.value) {
    refreshCaptcha()
  }
  // 延迟初始化动画，确保 DOM 已渲染
  setTimeout(() => {
    initBackgroundAnimation()
  }, 100)
})

// 组件卸载时清理
onUnmounted(() => {
  if (backgroundSvg.value) {
    d3.select(backgroundSvg.value).selectAll('*').remove()
  }
})
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: url('@/assets/bg.png') no-repeat center center;
  background-size: cover;
  padding: 20px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 1;
  }

  .background-animation {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 2;
    pointer-events: none;
  }
}

.login-card {
  width: 100%;
  max-width: 450px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 40px;
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  .logo {
    font-size: 32px;
    font-weight: 600;
    color: #1890ff;
    margin: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    font-family: 'Segoe UI', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
    letter-spacing: 2px;
    text-transform: uppercase;
    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: -8px;
      left: 50%;
      transform: translateX(-50%);
      width: 60%;
      height: 2px;
      background: linear-gradient(90deg, transparent, #1890ff, transparent);
      opacity: 0.6;
    }

    .logo-icon {
      width: 40px;
      height: 40px;
      object-fit: contain;
      filter: drop-shadow(0 2px 8px rgba(24, 144, 255, 0.3));
    }
  }

  .subtitle {
    font-size: 15px;
    color: #555;
    margin: 16px 0 0;
    font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
    font-weight: 300;
    letter-spacing: 1px;
  }
}

.login-tabs {
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    font-size: 16px;
    font-weight: 500;
    font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
    letter-spacing: 0.5px;
  }

  :deep(.el-tabs__item.is-active) {
    color: #1890ff;
    font-weight: 600;
  }
}

.login-form {
  margin-top: 24px;

  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  .captcha-wrapper {
    display: flex;
    gap: 12px;
    align-items: center;

    .captcha-image {
      width: 120px;
      height: 40px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      background-color: #f5f7fa;

      &:hover {
        border-color: #c0c4cc;
      }

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      span {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .submit-button {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 2px;
    font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
    text-transform: uppercase;
    background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
    border: none;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 16px rgba(24, 144, 255, 0.4);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

.footer {
  margin-top: 32px;
  text-align: center;
  position: relative;
  z-index: 10;

  p {
    color: rgba(255, 255, 255, 0.95);
    font-size: 13px;
    margin: 0;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
    font-weight: 300;
    letter-spacing: 1px;
  }
}
</style>

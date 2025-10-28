<template>
    <div class="not-found-container">
        <div class="not-found-content">
            <div class="error-code">404</div>
            <div class="error-message">页面不存在</div>
            <div class="error-description">抱歉，您访问的页面不存在或已被删除</div>
            <el-button type="primary" size="large" @click="router.push('/')">
                返回首页
            </el-button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found-container {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f0f2f5;
}

.not-found-content {
    text-align: center;

    .error-code {
        font-size: 120px;
        font-weight: bold;
        color: #1890ff;
        line-height: 1;
        margin-bottom: 24px;
    }

    .error-message {
        font-size: 32px;
        font-weight: 600;
        color: #333;
        margin-bottom: 16px;
    }

    .error-description {
        font-size: 16px;
        color: #666;
        margin-bottom: 32px;
    }
}
</style>

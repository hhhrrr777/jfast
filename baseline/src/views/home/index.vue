<script setup lang="ts">
import { onMounted } from 'vue'
import { authState, fetchCurrentUser } from '@/api/auth'

onMounted(async () => {
  // 路由守卫已保证权限装载;此处兜底刷新用户信息(如多标签页场景)
  if (!authState.user) {
    try {
      await fetchCurrentUser()
    } catch {
      // 拦截器已处理跳转
    }
  }
})
</script>

<template>
  <el-card>
    <el-result
      title="jfast-baseline"
      :sub-title="`欢迎,${authState.user?.nickName || authState.user?.username || ''}`"
    />
  </el-card>
</template>

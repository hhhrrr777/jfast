<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authState, fetchCurrentUser, logout } from '@/api/auth'

const router = useRouter()
const loading = ref(true)

onMounted(async () => {
  try {
    await fetchCurrentUser()
  } catch {
    // 拦截器已处理跳转
  } finally {
    loading.value = false
  }
})

async function handleLogout() {
  await logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div class="page-center">
    <el-result v-if="!loading" title="jfast-baseline" :sub-title="`欢迎,${authState.user?.nickName || authState.user?.username || ''}`">
      <template #extra>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </template>
    </el-result>
  </div>
</template>

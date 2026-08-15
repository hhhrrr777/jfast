<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login, fetchCurrentUser } from '@/api/auth'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await login(form.username, form.password)
      await fetchCurrentUser()
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="login-title">jfast-baseline</h2>
      <p class="login-subtitle">请登录以继续</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-button" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-hint">默认账号 admin / admin123,首次登录后请修改密码</p>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.login-card {
  width: 380px;
  padding: 8px 12px;
}

.login-title {
  margin: 8px 0 4px;
  text-align: center;
}

.login-subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: #909399;
}

.login-button {
  width: 100%;
}

.login-hint {
  margin: 8px 0 0;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
</style>

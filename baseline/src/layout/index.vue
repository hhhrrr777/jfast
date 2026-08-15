<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authState, logout } from '@/api/auth'
import { changeMyPassword } from '@/api/system'
import { permissionState, resetPermissions, type RouterNode } from '@/store/permission'

/**
 * 后台布局:左侧动态菜单(数据来自 /auth/routers)+ 顶栏(用户昵称/改密/退出)。
 */

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

interface MenuItem {
  index: string
  title: string
  icon: string
  children?: MenuItem[]
}

/** RouterNode 树 → el-menu 项;目录递归,菜单为叶子(路径拼 /<目录>/<菜单>)。 */
function toMenuItems(nodes: RouterNode[], parentPath = ''): MenuItem[] {
  return nodes
    .filter((n) => n.path)
    .map((node) => {
      const fullPath = parentPath ? `${parentPath}/${node.path}` : `/${node.path}`
      if (node.children && node.children.length > 0) {
        return {
          index: fullPath,
          title: node.title,
          icon: node.icon,
          children: toMenuItems(node.children, fullPath)
        }
      }
      return { index: fullPath, title: node.title, icon: node.icon }
    })
}

const menuItems = computed(() => toMenuItems(permissionState.routers))

async function handleLogout() {
  await logout()
  resetPermissions()
  ElMessage.success('已退出登录')
  router.push('/login')
}

/* 修改自己的密码(首登改密入口):弹窗校验旧密码,成功后强制重新登录。 */
const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度必须在 5 到 20 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v: string, cb) =>
        v === pwdForm.newPassword ? cb() : cb(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ]
}

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdVisible.value = true
}

async function submitPwd() {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdSubmitting.value = true
    try {
      await changeMyPassword(pwdForm.oldPassword, pwdForm.newPassword)
      pwdVisible.value = false
      ElMessage.success('密码已修改,请重新登录')
      await logout()
      resetPermissions()
      router.push('/login')
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '修改失败')
    } finally {
      pwdSubmitting.value = false
    }
  })
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">jfast-baseline</div>
      <el-menu :default-active="activeMenu" router class="aside-menu">
        <template v-for="item in menuItems" :key="item.index">
          <el-sub-menu v-if="item.children" :index="item.index">
            <template #title>{{ item.title }}</template>
            <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.index">{{ item.title }}</el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div />
        <div class="header-right">
          <span class="user-name">{{ authState.user?.nickName || authState.user?.username }}</span>
          <el-button link type="primary" @click="openPwdDialog">修改密码</el-button>
          <el-button link type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="5-20 位" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdSubmitting" @click="submitPwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.layout-aside {
  border-right: 1px solid #e4e7ed;
}

.logo {
  height: 56px;
  line-height: 56px;
  padding: 0 20px;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.aside-menu {
  border-right: none;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: #606266;
  font-size: 14px;
}

.layout-main {
  background: #f0f2f5;
}
</style>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addUser,
  getUser,
  listAllRoles,
  updateUser,
  type RoleVO
} from '@/api/system'

/**
 * 用户表单页(新增/编辑共用,独立路由页)。编辑态从 /system/user/:id 取回;
 * 新增含密码字段,编辑不含(重置密码走列表页独立动作)。
 */

const route = useRoute()
const router = useRouter()

const userId = computed(() => {
  const raw = route.params.id
  const id = Array.isArray(raw) ? raw[0] : (raw as string | undefined)
  return id !== undefined && id !== '' && !Number.isNaN(Number(id)) ? Number(id) : null
})
const isEdit = computed(() => userId.value !== null)

const formRef = ref<FormInstance>()
const loading = ref(false)
const roleOptions = ref<RoleVO[]>([])

const form = reactive({
  userName: '',
  nickName: '',
  password: '',
  status: '0',
  remark: '',
  roleIds: [] as number[]
})

const rules: FormRules = {
  userName: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 30, message: '长度不能超过 30 个字符', trigger: 'blur' }
  ],
  nickName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 30, message: '长度不能超过 30 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度必须在 5 到 20 个字符之间', trigger: 'blur' }
  ]
}

onMounted(async () => {
  try {
    const roles = await listAllRoles()
    roleOptions.value = roles.data.data
  } catch {
    // 角色下拉加载失败不阻塞表单
  }
  if (userId.value !== null) {
    loading.value = true
    try {
      const res = await getUser(userId.value)
      const user = res.data.data
      form.userName = user.userName
      form.nickName = user.nickName
      form.status = user.status
      form.remark = user.remark ?? ''
      form.roleIds = user.roleIds ?? []
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '加载用户失败')
    } finally {
      loading.value = false
    }
  }
})

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (userId.value !== null) {
        await updateUser({
          userId: userId.value,
          nickName: form.nickName,
          status: form.status,
          remark: form.remark,
          roleIds: form.roleIds
        })
        ElMessage.success('修改成功')
      } else {
        await addUser({
          userName: form.userName,
          nickName: form.nickName,
          password: form.password,
          status: form.status,
          remark: form.remark,
          roleIds: form.roleIds
        })
        ElMessage.success('新增成功')
      }
      router.back()
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '保存失败')
    }
  })
}

function cancel() {
  router.back()
}
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <span>{{ isEdit ? '编辑用户' : '新增用户' }}</span>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 520px">
      <el-form-item label="登录账号" prop="userName">
        <el-input v-model="form.userName" :disabled="isEdit" placeholder="登录账号" maxlength="30" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickName">
        <el-input v-model="form.nickName" placeholder="昵称" maxlength="30" />
      </el-form-item>
      <el-form-item v-if="!isEdit" label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="5-20 位" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
          <el-option
            v-for="role in roleOptions"
            :key="role.roleId"
            :label="role.roleName"
            :value="role.roleId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio value="0">正常</el-radio>
          <el-radio value="1">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">保存</el-button>
        <el-button @click="cancel">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

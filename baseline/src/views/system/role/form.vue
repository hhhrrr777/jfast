<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { TreeInstance } from 'element-plus'
import {
  addRole,
  getMenuTree,
  getRole,
  updateRole,
  type MenuTreeNode
} from '@/api/system'

/**
 * 角色表单页(新增/编辑共用):基本信息 + 菜单权限树勾选(绑菜单即绑权限标识)。
 */

const route = useRoute()
const router = useRouter()

const roleId = computed(() => {
  const raw = route.params.id
  const id = Array.isArray(raw) ? raw[0] : (raw as string | undefined)
  return id !== undefined && id !== '' && !Number.isNaN(Number(id)) ? Number(id) : null
})
const isEdit = computed(() => roleId.value !== null)

const formRef = ref<FormInstance>()
const treeRef = ref<TreeInstance>()
const loading = ref(false)
const menuTree = ref<MenuTreeNode[]>([])

const form = reactive({
  roleName: '',
  roleKey: '',
  roleSort: 0,
  status: '0',
  remark: '',
  menuIds: [] as number[]
})

const rules: FormRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 30, message: '长度不能超过 30 个字符', trigger: 'blur' }
  ],
  roleKey: [
    { required: true, message: '请输入权限字符', trigger: 'blur' },
    { max: 100, message: '长度不能超过 100 个字符', trigger: 'blur' }
  ]
}

/** 目录/菜单/按钮的类型标签。 */
const typeLabels: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }

onMounted(async () => {
  loading.value = true
  try {
    const tree = await getMenuTree()
    menuTree.value = tree.data.data
    if (roleId.value !== null) {
      const res = await getRole(roleId.value)
      const role = res.data.data
      form.roleName = role.roleName
      form.roleKey = role.roleKey
      form.roleSort = role.roleSort
      form.status = role.status
      form.remark = role.remark ?? ''
      form.menuIds = role.menuIds ?? []
      // 父子不联动(check-strictly):按 menuIds 精确回显,目录/菜单/按钮各自独立勾选
      setTimeout(() => {
        form.menuIds.forEach((id) => treeRef.value?.setChecked(id, true, false))
      })
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    // 父子不联动:直接取勾选集合,目录/菜单/按钮各自独立提交
    const menuIds = (treeRef.value?.getCheckedKeys() ?? []) as number[]
    try {
      if (roleId.value !== null) {
        await updateRole({ roleId: roleId.value, ...form, menuIds })
        ElMessage.success('修改成功')
      } else {
        await addRole({ ...form, menuIds })
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
      <span>{{ isEdit ? '编辑角色' : '新增角色' }}</span>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 560px">
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" placeholder="角色名称" maxlength="30" />
      </el-form-item>
      <el-form-item label="权限字符" prop="roleKey">
        <el-input v-model="form.roleKey" placeholder="如 readonly" maxlength="100" :disabled="isEdit && form.roleKey === 'admin'" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.roleSort" :min="0" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio value="0">正常</el-radio>
          <el-radio value="1">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单权限">
        <div class="menu-tip">勾选按钮时需同时勾上其所在菜单与目录,否则侧边栏不显示入口(父子不联动)。</div>
        <el-tree
          ref="treeRef"
          :data="menuTree"
          node-key="menuId"
          show-checkbox
          :check-strictly="true"
          default-expand-all
          :props="{ label: 'menuName', children: 'children' }"
          class="menu-tree"
        >
          <template #default="{ data }">
            <span>{{ data.menuName }}</span>
            <el-tag size="small" type="info" class="type-tag">{{ typeLabels[data.menuType] }}</el-tag>
            <span v-if="data.perms" class="perms-text">{{ data.perms }}</span>
          </template>
        </el-tree>
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

<style scoped>
.menu-tip {
  color: #e6a23c;
  font-size: 12px;
  line-height: 1.4;
  margin-bottom: 6px;
}

.menu-tree {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 6px;
  max-height: 360px;
  overflow: auto;
}

.type-tag {
  margin-left: 8px;
}

.perms-text {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

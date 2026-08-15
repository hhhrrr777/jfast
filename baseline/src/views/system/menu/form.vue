<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { addMenu, getMenu, getMenuTree, updateMenu, type MenuTreeNode } from '@/api/system'

/**
 * 菜单表单页(新增/编辑共用):三类型(目录/菜单/按钮)字段联动。
 */

const route = useRoute()
const router = useRouter()

const menuId = computed(() => {
  const raw = route.params.id
  const id = Array.isArray(raw) ? raw[0] : (raw as string | undefined)
  return id !== undefined && id !== '' && !Number.isNaN(Number(id)) ? Number(id) : null
})
const isEdit = computed(() => menuId.value !== null)

const formRef = ref<FormInstance>()
const loading = ref(false)
const parentOptions = ref<MenuTreeNode[]>([])

const form = reactive({
  menuName: '',
  parentId: 0,
  orderNum: 0,
  path: '',
  component: '',
  menuType: 'C' as 'M' | 'C' | 'F',
  visible: '0',
  status: '0',
  perms: '',
  icon: '',
  remark: ''
})

const rules: FormRules = {
  menuName: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' },
    { max: 50, message: '长度不能超过 50 个字符', trigger: 'blur' }
  ]
}

/** 树拍平为父级下拉选项(根 + 全部目录/菜单)。 */
function flatten(nodes: MenuTreeNode[], out: MenuTreeNode[]) {
  for (const node of nodes) {
    out.push(node)
    if (node.children) flatten(node.children, out)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const tree = await getMenuTree()
    const flat: MenuTreeNode[] = []
    flatten(tree.data.data, flat)
    parentOptions.value = [
      { menuId: 0, menuName: '根目录', children: [] } as unknown as MenuTreeNode,
      ...flat.filter((n) => n.menuType !== 'F')
    ]
    if (menuId.value !== null) {
      const res = await getMenu(menuId.value)
      const menu = res.data.data
      Object.assign(form, {
        menuName: menu.menuName,
        parentId: menu.parentId,
        orderNum: menu.orderNum,
        path: menu.path,
        component: menu.component,
        menuType: menu.menuType,
        visible: menu.visible,
        status: menu.status,
        perms: menu.perms,
        icon: menu.icon,
        remark: menu.remark ?? ''
      })
    } else {
      const parentIdParam = route.query.parentId as string | undefined
      if (parentIdParam) form.parentId = Number(parentIdParam)
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
    try {
      if (menuId.value !== null) {
        await updateMenu({ menuId: menuId.value, ...form })
        ElMessage.success('修改成功')
      } else {
        await addMenu({ ...form })
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
      <span>{{ isEdit ? '编辑菜单' : '新增菜单' }}</span>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 560px">
      <el-form-item label="上级菜单">
        <el-tree-select
          v-model="form.parentId"
          :data="parentOptions"
          node-key="menuId"
          :props="{ label: 'menuName', children: 'children' }"
          check-strictly
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="菜单类型">
        <el-radio-group v-model="form.menuType">
          <el-radio value="M">目录</el-radio>
          <el-radio value="C">菜单</el-radio>
          <el-radio value="F">按钮</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单名称" prop="menuName">
        <el-input v-model="form.menuName" placeholder="菜单名称" maxlength="50" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.orderNum" :min="0" />
      </el-form-item>
      <template v-if="form.menuType !== 'F'">
        <el-form-item label="路由地址">
          <el-input v-model="form.path" placeholder="如 system/user" maxlength="200" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标名" maxlength="100" />
        </el-form-item>
      </template>
      <el-form-item v-if="form.menuType === 'C'" label="组件路径">
        <el-input v-model="form.component" placeholder="如 system/user/index" maxlength="255" />
      </el-form-item>
      <el-form-item v-if="form.menuType !== 'F'" label="显示状态">
        <el-radio-group v-model="form.visible">
          <el-radio value="0">显示</el-radio>
          <el-radio value="1">隐藏</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单状态">
        <el-radio-group v-model="form.status">
          <el-radio value="0">正常</el-radio>
          <el-radio value="1">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="权限标识">
        <el-input v-model="form.perms" placeholder="如 system:user:add" maxlength="100" />
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

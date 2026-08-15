<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMenu, getMenuTree, type MenuTreeNode } from '@/api/system'

/**
 * 菜单管理列表页:树表展示目录/菜单/按钮三类型;新增/编辑跳独立表单页。
 */

const router = useRouter()

const loading = ref(false)
const tree = ref<MenuTreeNode[]>([])

const typeLabels: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }
const typeTags: Record<string, 'primary' | 'success' | 'info'> = { M: 'primary', C: 'success', F: 'info' }

async function load() {
  loading.value = true
  try {
    const res = await getMenuTree()
    tree.value = res.data.data
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function toAdd(parentId?: number) {
  router.push({
    path: '/system/menu/form',
    query: parentId !== undefined ? { parentId: String(parentId) } : {}
  })
}

function toEdit(row: MenuTreeNode) {
  router.push(`/system/menu/form/${row.menuId}`)
}

async function handleDelete(row: MenuTreeNode) {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteMenu(row.menuId)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-hasPermi="['system:menu:add']" type="primary" @click="toAdd()">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="tree" row-key="menuId" default-expand-all>
      <el-table-column prop="menuName" label="菜单名称" min-width="180" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeTags[row.menuType]">{{ typeLabels[row.menuType] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="icon" label="图标" width="110" />
      <el-table-column prop="orderNum" label="排序" width="70" />
      <el-table-column prop="perms" label="权限标识" min-width="170" />
      <el-table-column prop="path" label="路由地址" min-width="120" />
      <el-table-column prop="component" label="组件路径" min-width="170" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">
            {{ row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['system:menu:add']" link type="primary" @click="toAdd(row.menuId)">
            新增子项
          </el-button>
          <el-button v-hasPermi="['system:menu:edit']" link type="primary" @click="toEdit(row)">编辑</el-button>
          <el-button v-hasPermi="['system:menu:remove']" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>

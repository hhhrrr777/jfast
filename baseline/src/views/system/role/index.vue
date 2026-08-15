<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteRole, listRoles, type RoleQuery, type RoleVO } from '@/api/system'

/**
 * 角色管理列表页:搜索 + 表格 + 分页;绑菜单在独立表单页(菜单树勾选)。
 */

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const rows = ref<RoleVO[]>([])
const selected = ref<RoleVO[]>([])

const query = reactive<RoleQuery>({
  pageNum: 1,
  pageSize: 10,
  roleName: '',
  roleKey: '',
  status: ''
})

async function load() {
  loading.value = true
  try {
    const res = await listRoles(query)
    total.value = res.data.total
    rows.value = res.data.rows
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  query.roleName = ''
  query.roleKey = ''
  query.status = ''
  search()
}

function toAdd() {
  router.push('/system/role/form')
}

function toEdit(row: RoleVO) {
  router.push(`/system/role/form/${row.roleId}`)
}

async function handleDelete(targets: RoleVO[]) {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 个角色?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteRole(targets.map((r) => r.roleId))
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}
</script>

<template>
  <el-card>
    <el-form inline :model="query" class="search-bar" @submit.prevent>
      <el-form-item label="角色名称">
        <el-input v-model="query.roleName" placeholder="角色名称" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="权限字符">
        <el-input v-model="query.roleKey" placeholder="如 admin" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button v-hasPermi="['system:role:add']" type="primary" @click="toAdd">新增</el-button>
      <el-button
        v-hasPermi="['system:role:remove']"
        type="danger"
        :disabled="selected.length === 0"
        @click="handleDelete(selected)"
      >
        批量删除
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      row-key="roleId"
      @selection-change="(vals: RoleVO[]) => (selected = vals)"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="roleId" label="ID" width="70" />
      <el-table-column prop="roleName" label="角色名称" min-width="140" />
      <el-table-column prop="roleKey" label="权限字符" min-width="120" />
      <el-table-column prop="roleSort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">
            {{ row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['system:role:edit']" link type="primary" @click="toEdit(row)">编辑</el-button>
          <el-button v-hasPermi="['system:role:remove']" link type="danger" @click="handleDelete([row])">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      :page-sizes="[10, 20, 50]"
      class="pager"
      @current-change="load"
      @size-change="search"
    />
  </el-card>
</template>

<style scoped>
.search-bar {
  margin-bottom: 4px;
}

.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
